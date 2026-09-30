package com.orquidea.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @param origenesPermitidos patrones de origen, p. ej. http://localhost:* (cualquier puerto local)
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> origenesPermitidos) {
}
