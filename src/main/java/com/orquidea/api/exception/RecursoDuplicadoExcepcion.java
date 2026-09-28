package com.orquidea.api.exception;

/** El registro choca con uno existente (p. ej. nombre científico repetido). Responde 409. */
public class RecursoDuplicadoExcepcion extends RuntimeException {

    public RecursoDuplicadoExcepcion(String mensaje) {
        super(mensaje);
    }
}
