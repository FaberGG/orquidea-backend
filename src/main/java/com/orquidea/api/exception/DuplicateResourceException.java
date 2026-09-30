package com.orquidea.api.exception;

/** El registro choca con uno existente (p. ej. nombre científico repetido). Responde 409. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String mensaje) {
        super(mensaje);
    }
}
