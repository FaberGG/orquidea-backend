package com.orquidea.api.exception;

/** HU-7, escenario 3: la foto no es jpg ni png. */
public class InvalidImageFormatException extends RuntimeException {

    public InvalidImageFormatException() {
        super("El formato de la imagen no es válido.");
    }
}
