package com.orquidea.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Almacenamiento de objetos compatible con S3: Cloudflare R2 en producción, RustFS local en desarrollo.
 *
 * @param endpoint           URL del API S3 (R2: https://&lt;account-id&gt;.r2.cloudflarestorage.com)
 * @param region             región S3 (R2 usa "auto")
 * @param urlPublica         base pública desde la que el navegador lee los objetos (R2: dominio público del bucket)
 * @param crearBucket        crea el bucket con lectura pública al arrancar; solo para desarrollo
 * @param tamanoMaximoImagen límite de las fotos subidas
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String endpoint,
        String region,
        String accessKey,
        String secretKey,
        String bucket,
        String urlPublica,
        boolean crearBucket,
        DataSize tamanoMaximoImagen) {
}
