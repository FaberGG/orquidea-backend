package com.orquidea.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Foto de una ficha taxonómica; se muestran por posición (orden de carga) y la primera es la principal. */
@Entity
@Table(name = "fotos_ficha")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxonPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ficha_id", nullable = false)
    private Taxon ficha;

    /** Clave del objeto en el almacenamiento (R2/S3); la URL pública se arma al leer. */
    @Column(name = "foto_clave", nullable = false, length = 300)
    private String fotoClave;

    /** Al crear llegan varias fotos en la misma petición, así que el orden no puede depender de la fecha. */
    @Column(nullable = false)
    private int posicion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @PrePersist
    void alCrear() {
        fechaCreacion = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
