package com.orquidea.api.config;

import com.orquidea.api.config.StorageProperties.ReprocessingMode;
import com.orquidea.api.service.PhotoReprocessingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Con STORAGE_REPROCESAR_FOTOS=simular o aplicar, optimiza al arrancar las fotos subidas antes de la optimización.
 * Corre en segundo plano: la API atiende mientras tanto, y el resultado queda en el log.
 */
@Component
@RequiredArgsConstructor
public class PhotoReprocessingInitializer implements ApplicationRunner {

    private final StorageProperties propiedades;
    private final PhotoReprocessingService photoReprocessingService;

    @Override
    public void run(ApplicationArguments args) {
        ReprocessingMode modo = propiedades.reprocesarFotos();
        if (modo != null && modo != ReprocessingMode.NO) {
            photoReprocessingService.reprocesarEnSegundoPlano(modo == ReprocessingMode.APLICAR);
        }
    }
}
