package com.orquidea.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class ConfiguracionStorage {

    /**
     * Path-style (endpoint/bucket/clave) funciona igual en R2 y en RustFS.
     * Los checksums solo cuando la operación los exige: R2 no soporta todos los que el SDK envía por defecto.
     */
    @Bean(destroyMethod = "close")
    public S3Client clienteS3(PropiedadesStorage propiedades) {
        return S3Client.builder()
                .endpointOverride(URI.create(propiedades.endpoint()))
                .region(Region.of(propiedades.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(propiedades.accessKey(), propiedades.secretKey())))
                .forcePathStyle(true)
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                .build();
    }
}
