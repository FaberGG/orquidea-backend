package com.orquidea.api.exception;

import org.springframework.util.unit.DataSize;

public class ImageTooLargeException extends RuntimeException {

    public ImageTooLargeException(DataSize tamanoMaximo) {
        super("La imagen supera el tamaño máximo permitido de " + tamanoMaximo.toMegabytes() + " MB.");
    }
}
