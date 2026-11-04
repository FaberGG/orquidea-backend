package com.orquidea.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/**
 * Tramo del trazado del recorrido en el mapa (HU-12). No es un registro biológico, así que no usa términos
 * Darwin Core. El trazado no se edita desde la app: se carga con una migración y la API solo lo lee.
 */
@Entity
@Immutable
@Table(name = "tramos_recorrido")
@Getter
@NoArgsConstructor
public class TrailSegment {

    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nombre;

    /** Posición del tramo dentro del recorrido, de menor a mayor. */
    @Column(nullable = false)
    private int orden;

    /**
     * GeoJSON LineString como texto, con coordenadas [longitud, latitud]. Se lee tal cual desde la columna jsonb
     * y se entrega sin volver a procesarlo, así que no hace falta un mapeo JSON de Hibernate.
     */
    @Column(nullable = false, columnDefinition = "jsonb", insertable = false, updatable = false)
    private String geometria;

    @Column(name = "fecha_actualizacion", nullable = false, insertable = false, updatable = false)
    private Instant fechaActualizacion;
}
