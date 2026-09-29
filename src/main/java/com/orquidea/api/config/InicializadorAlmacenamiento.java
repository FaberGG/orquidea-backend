package com.orquidea.api.config;

import com.orquidea.api.storage.AlmacenamientoServicio;
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
public class InicializadorAlmacenamiento implements ApplicationRunner {

    private final PropiedadesStorage propiedades;
    private final AlmacenamientoServicio almacenamientoServicio;

    @Override
    public void run(ApplicationArguments args) {
        if (propiedades.crearBucket()) {
            almacenamientoServicio.asegurarBucketPublico();
        }
    }
}
