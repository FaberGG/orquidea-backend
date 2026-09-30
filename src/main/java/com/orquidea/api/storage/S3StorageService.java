package com.orquidea.api.storage;

import com.orquidea.api.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private static final int INTENTOS_BUCKET = 10;
    private static final long ESPERA_ENTRE_INTENTOS_MS = 2000;

    private final S3Client clienteS3;
    private final StorageProperties propiedades;

    @Override
    public void subir(String clave, byte[] contenido, String tipoContenido) {
        clienteS3.putObject(solicitud -> solicitud
                        .bucket(propiedades.bucket())
                        .key(clave)
                        .contentType(tipoContenido),
                RequestBody.fromBytes(contenido));
    }

    @Override
    public void eliminar(String clave) {
        try {
            clienteS3.deleteObject(solicitud -> solicitud.bucket(propiedades.bucket()).key(clave));
        } catch (SdkException e) {
            // Solo deja un objeto huérfano; no debe tapar el error original de quien llama.
            log.warn("No se pudo eliminar el objeto {} del almacenamiento", clave, e);
        }
    }

    @Override
    public String urlPublica(String clave) {
        String base = propiedades.urlPublica();
        return (base.endsWith("/") ? base : base + "/") + clave;
    }

    /**
     * Reintenta porque, al arrancar con Docker Compose, el contenedor de almacenamiento puede tardar
     * un poco más que la aplicación.
     */
    @Override
    public void asegurarBucketPublico() {
        String bucket = propiedades.bucket();
        for (int intento = 1; intento <= INTENTOS_BUCKET; intento++) {
            try {
                crearBucketSiNoExiste(bucket);
                clienteS3.putBucketPolicy(solicitud -> solicitud.bucket(bucket).policy(politicaLecturaPublica(bucket)));
                log.info("Bucket de almacenamiento listo: {}", bucket);
                return;
            } catch (SdkException e) {
                if (intento == INTENTOS_BUCKET) {
                    log.warn("No se pudo preparar el bucket {}; la subida de archivos fallará hasta que exista", bucket, e);
                    return;
                }
                esperar();
            }
        }
    }

    private void crearBucketSiNoExiste(String bucket) {
        try {
            clienteS3.headBucket(solicitud -> solicitud.bucket(bucket));
        } catch (S3Exception e) {
            if (e.statusCode() != 404) {
                throw e;
            }
            clienteS3.createBucket(solicitud -> solicitud.bucket(bucket));
        }
    }

    private static String politicaLecturaPublica(String bucket) {
        return """
                {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":"*",\
                "Action":["s3:GetObject"],"Resource":["arn:aws:s3:::%s/*"]}]}""".formatted(bucket);
    }

    private static void esperar() {
        try {
            Thread.sleep(ESPERA_ENTRE_INTENTOS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
