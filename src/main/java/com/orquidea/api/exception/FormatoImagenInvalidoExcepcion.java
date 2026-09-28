package com.orquidea.api.exception;

/** HU-7, escenario 3: la foto no es jpg ni png. */
public class FormatoImagenInvalidoExcepcion extends RuntimeException {

    public FormatoImagenInvalidoExcepcion() {
        super("El formato de la imagen no es válido.");
    }
}
