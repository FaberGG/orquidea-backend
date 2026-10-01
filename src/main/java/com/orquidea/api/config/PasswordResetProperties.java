package com.orquidea.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param vigencia        tiempo durante el que el código es válido
 * @param intentosMaximos intentos fallidos permitidos antes de invalidar el código
 * @param esperaReenvio   tiempo mínimo entre dos códigos para el mismo usuario; evita llenar su bandeja
 *                        y que pedir códigos nuevos sirva para reiniciar el límite de intentos
 */
@ConfigurationProperties(prefix = "app.recuperacion-contrasena")
public record PasswordResetProperties(Duration vigencia, int intentosMaximos, Duration esperaReenvio) {
}
