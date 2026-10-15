package com.orquidea.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Reporte ciudadano de avistamiento (HU-18). Es una observación de un organismo en un lugar, que en
 * Darwin Core se llama Occurrence; sus atributos usan los términos DwC y los propios de la app van en español.
 */
@Entity
@Table(name = "reportes_avistamiento")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Occurrence {

    /** Asignado por el servicio o enviado por el cliente (HU-22), por eso no es @GeneratedValue. */
    @Id
    private UUID id;

    /** DwC: occurrenceRemarks */
    @Column(name = "descripcion", nullable = false, length = 2000)
    private String occurrenceRemarks;

    /** DwC: kingdom */
    @Enumerated(EnumType.STRING)
    @Column(name = "reino", nullable = false, length = 20)
    private Kingdom kingdom;

    /** DwC: vernacularName. Clasificación específica que escribe el ciudadano, o "Desconocido". */
    @Column(name = "clasificacion", nullable = false, length = 150)
    private String vernacularName;

    /** DwC: decimalLatitude. Lugar del avistamiento; se guardan coordenadas porque un texto puede cambiar de sentido. */
    @Column(name = "latitud", precision = 9, scale = 6)
    private BigDecimal decimalLatitude;

    /** DwC: decimalLongitude */
    @Column(name = "longitud", precision = 9, scale = 6)
    private BigDecimal decimalLongitude;

    /** Clave del objeto en el almacenamiento; null si el reporte no trae foto. */
    @Column(name = "foto_clave", length = 300)
    private String fotoClave;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus estado;

    /** DwC: recordedBy. Siempre es el usuario autenticado que envía el reporte (HU-18, escenario 4). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private User recordedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisor_id")
    private User revisadoPor;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_revision")
    private Instant fechaRevision;

    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = ahora();
        }
    }

    /** Pasa de PENDIENTE al estado indicado; el servicio valida antes que siga pendiente. */
    public void revisar(ReviewStatus nuevoEstado, User revisor) {
        this.estado = nuevoEstado;
        this.revisadoPor = revisor;
        this.fechaRevision = ahora();
    }

    /** PostgreSQL guarda microsegundos; así la respuesta coincide con lo que devolverá una consulta posterior. */
    private static Instant ahora() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
