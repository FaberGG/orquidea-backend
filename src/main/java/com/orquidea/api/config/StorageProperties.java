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
 * @param imagen             cómo se optimizan las fotos al subirlas
 * @param reprocesarFotos    optimiza al arrancar las fotos subidas antes de que existiera la optimización
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
        DataSize tamanoMaximoImagen,
        Imagen imagen,
        ReprocessingMode reprocesarFotos) {

    /**
     * @param anchoMaximo               ancho de la foto guardada, medido ya derecha; las más angostas no se agrandan
     * @param anchoMiniatura            ancho de la miniatura para los listados
     * @param calidadJpeg               de 0 a 1; se codifica con color completo (4:4:4)
     * @param megapixelesMaximos        resolución máxima aceptada: decodificar una foto ocupa 4 bytes por píxel
     * @param procesamientosSimultaneos fotos que se procesan a la vez, para acotar la memoria
     */
    public record Imagen(
            int anchoMaximo,
            int anchoMiniatura,
            float calidadJpeg,
            int megapixelesMaximos,
            int procesamientosSimultaneos) {
    }

    /** NO no hace nada; SIMULAR solo informa en el log cuánto se ahorraría; APLICAR reemplaza las fotos. */
    public enum ReprocessingMode {
        NO, SIMULAR, APLICAR
    }
}
