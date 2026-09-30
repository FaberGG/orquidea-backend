package com.orquidea.api.model;

/**
 * Categorías de la Lista Roja de la UICN.
 */
public enum ConservationStatus {
    /** Extinto */
    EX,
    /** Extinto en estado silvestre */
    EW,
    /** En peligro crítico */
    CR,
    /** En peligro */
    EN,
    /** Vulnerable */
    VU,
    /** Casi amenazado */
    NT,
    /** Preocupación menor */
    LC,
    /** Datos insuficientes */
    DD,
    /** No evaluado */
    NE
}
