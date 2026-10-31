package com.orquidea.api.model;

/**
 * Estado de revisión de un reporte ciudadano (HU-19). Todo reporte nace PENDIENTE y solo desde ahí
 * puede pasar a APROBADO (visible en el foro, HU-20) o a RECHAZADO.
 */
public enum ReviewStatus {
    PENDIENTE,
    APROBADO,
    RECHAZADO
}
