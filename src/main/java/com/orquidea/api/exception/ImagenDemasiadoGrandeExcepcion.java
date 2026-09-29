package com.orquidea.api.exception;

import org.springframework.util.unit.DataSize;

public class ImagenDemasiadoGrandeExcepcion extends RuntimeException {

    public ImagenDemasiadoGrandeExcepcion(DataSize tamanoMaximo) {
        super("La imagen supera el tamaño máximo permitido de " + tamanoMaximo.toMegabytes() + " MB.");
    }
}
