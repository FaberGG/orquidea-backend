package com.orquidea.api.security;

import com.orquidea.api.model.Role;

import java.util.UUID;

/**
 * Identidad extraída de un JWT válido; es el principal de cada petición autenticada.
 */
public record JwtPrincipal(UUID id, String correo, Role rol) {
}
