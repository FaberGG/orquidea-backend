package com.orquidea.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Ficha de una especie del humedal. Los campos taxonómicos corresponden a términos Darwin Core (DwC);
 * alimentacion, rolEnHumedal y estadoConservacion no tienen término DwC y se nombran en español.
 */
@Entity
@Table(name = "fichas_taxonomicas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FichaTaxonomica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaTaxon categoria;

    /** DwC: order */
    @Column(nullable = false, length = 100)
    private String orden;

    /** DwC: family */
    @Column(nullable = false, length = 100)
    private String familia;

    /** DwC: genus */
    @Column(nullable = false, length = 100)
    private String genero;

    /** DwC: scientificName. Único sin distinguir mayúsculas. */
    @Column(name = "nombre_cientifico", nullable = false, length = 200)
    private String nombreCientifico;

    /** DwC: vernacularName */
    @Column(name = "nombre_comun", nullable = false, length = 150)
    private String nombreComun;

    @Column(nullable = false, length = 2000)
    private String alimentacion;

    @Column(name = "rol_en_humedal", nullable = false, length = 2000)
    private String rolEnHumedal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_conservacion", nullable = false, length = 2)
    private EstadoConservacion estadoConservacion;

    /** Clave del objeto en el almacenamiento (R2/S3); la URL pública se arma al leer. */
    @Column(name = "foto_clave", nullable = false, length = 300)
    private String fotoClave;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @PrePersist
    void alCrear() {
        Instant ahora = Instant.now();
        fechaCreacion = ahora;
        fechaActualizacion = ahora;
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = Instant.now();
    }
}
