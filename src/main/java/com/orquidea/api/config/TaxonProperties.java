package com.orquidea.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param fotosMaximas cantidad máxima de fotos por ficha taxonómica
 */
@ConfigurationProperties(prefix = "app.fichas")
public record TaxonProperties(int fotosMaximas) {
}
