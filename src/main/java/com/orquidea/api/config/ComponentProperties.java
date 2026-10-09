package com.orquidea.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param fotosMaximas cantidad máxima de fotos por componente del humedal
 */
@ConfigurationProperties(prefix = "app.componentes")
public record ComponentProperties(int fotosMaximas) {
}
