package com.orquidea.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param remitente dirección desde la que se envían las notificaciones (variable MAIL_REMITENTE)
 */
@ConfigurationProperties(prefix = "app.correo")
public record PropiedadesCorreo(String remitente) {
}