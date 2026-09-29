package com.orquidea.api.exception;

/**
 * Correo inexistente, contraseña errada o cuenta deshabilitada.
 * Se responde igual en los tres casos para no revelar qué correos están registrados.
 */
public class CredencialesInvalidasExcepcion extends RuntimeException {

    public CredencialesInvalidasExcepcion() {
        super("Correo o contraseña incorrectos.");
    }
}
