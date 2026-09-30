package com.orquidea.api.exception;

/** Error de datos de entrada que no se puede expresar con anotaciones de validación. Responde 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String mensaje) {
        super(mensaje);
    }
}
