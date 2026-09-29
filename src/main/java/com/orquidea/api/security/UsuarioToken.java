package com.orquidea.api.security;

import com.orquidea.api.model.Rol;

import java.util.UUID;

/**
 * Identidad extraída de un JWT válido; es el principal de cada petición autenticada.
 */
public record UsuarioToken(UUID id, String correo, Rol rol) {
}
