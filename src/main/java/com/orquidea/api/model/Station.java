package com.orquidea.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Estación del recorrido en el mapa (HU-13). La ubicación usa los términos Darwin Core decimalLatitude y
 * decimalLongitude, igual que los reportes de avistamiento.
 */
@Entity
@Table(name = "estaciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 2000)
    private String descripcion;

    /** Clave del objeto en el almacenamiento (R2/S3); la URL pública se arma al leer. */
    @Column(name = "foto_clave", nullable = false, length = 300)
    private String fotoClave;

    /** DwC: decimalLatitude */
    @Column(name = "latitud", nullable = false, precision = 9, scale = 6)
    private BigDecimal decimalLatitude;

    /** DwC: decimalLongitude */
    @Column(name = "longitud", nullable = false, precision = 9, scale = 6)
    private BigDecimal decimalLongitude;

    /** Fichas que la estación enlaza en el mapa; el servicio las ordena por nombre común. */
    @Builder.Default
    @ManyToMany
    @JoinTable(name = "estaciones_fichas",
            joinColumns = @JoinColumn(name = "estacion_id"),
            inverseJoinColumns = @JoinColumn(name = "ficha_id"))
    private List<Taxon> fichas = new ArrayList<>();

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @PrePersist
    void alCrear() {
        Instant ahora = ahora();
        fechaCreacion = ahora;
        fechaActualizacion = ahora;
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = ahora();
    }

    /** PostgreSQL guarda microsegundos; así la respuesta coincide con lo que devolverá una consulta posterior. */
    private static Instant ahora() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
