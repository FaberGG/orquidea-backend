package com.orquidea.api.config;

import com.orquidea.api.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * En desarrollo deja listo el bucket local (RustFS) con lectura pública. En producción (R2)
 * el bucket se crea y se hace público desde el panel de Cloudflare, con STORAGE_CREAR_BUCKET=false.
 */
@Component
@RequiredArgsConstructor
public class StorageInitializer implements ApplicationRunner {

    private final StorageProperties propiedades;
    private final StorageService storageService;

    @Override
    public void run(ApplicationArguments args) {
        if (propiedades.crearBucket()) {
            storageService.asegurarBucketPublico();
        }
    }
}
