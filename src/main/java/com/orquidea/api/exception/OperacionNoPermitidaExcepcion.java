package com.orquidea.api.exception;

/** La operación viola una regla de negocio sobre el estado actual (p. ej. revocar al único superadministrador). Responde 409. */
public class OperacionNoPermitidaExcepcion extends RuntimeException {
    public OperacionNoPermitidaExcepcion(String mensaje) {
        super(mensaje);
    }
}
