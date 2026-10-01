package com.orquidea.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Los correos se envían con la API HTTP de Resend.
 *
 * @param remitente    dirección desde la que se envían las notificaciones (variable MAIL_REMITENTE);
 *                     debe pertenecer a un dominio verificado en Resend
 * @param claveApi     API key de Resend (variable RESEND_API_KEY). Si está vacía, los correos no se envían
 *                     y solo se registran en el log, como en desarrollo y en las pruebas
 * @param urlApi       URL base de la API de Resend
 * @param tiempoEspera límite para conectar y para recibir la respuesta de Resend
 */
@ConfigurationProperties(prefix = "app.correo")
public record MailProperties(String remitente, String claveApi, String urlApi, Duration tiempoEspera) {
}
