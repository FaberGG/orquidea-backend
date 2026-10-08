package com.orquidea.api.model;

/**
 * Reino del organismo avistado (Darwin Core: kingdom). DESCONOCIDO cubre el caso en que el ciudadano
 * no lo identifica (HU-18, escenario 3).
 */
public enum Kingdom {
    ANIMALIA,
    PLANTAE,
    FUNGI,
    DESCONOCIDO
}
