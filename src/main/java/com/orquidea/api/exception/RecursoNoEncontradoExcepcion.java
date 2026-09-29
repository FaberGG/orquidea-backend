package com.orquidea.api.exception;

/** Responde 404. */
public class RecursoNoEncontradoExcepcion extends RuntimeException {

    public RecursoNoEncontradoExcepcion(String mensaje) {
        super(mensaje);
    }
}
