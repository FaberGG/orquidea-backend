package com.orquidea.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param secreto    clave HMAC de al menos 32 caracteres (variable de entorno JWT_SECRETO)
 * @param expiracion validez del token, p. ej. 8h
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secreto, Duration expiracion) {
}
