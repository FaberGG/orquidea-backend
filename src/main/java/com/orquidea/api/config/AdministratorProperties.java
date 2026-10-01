package com.orquidea.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.administradores")
public record AdministratorProperties(int limite) {
}