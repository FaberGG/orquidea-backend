package com.orquidea.api.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Contenido informativo de un componente del humedal (HU-14, HU-15). No es un registro biológico
 * (no describe una especie ni un avistamiento), así que no tiene términos Darwin Core y sus atributos
 * van en español; las fichas por especie siguen en {@link Taxon}.
 * Su identificador es el propio tipo: hay exactamente una fila por componente.
 */
@Entity
@Table(name = "componentes_humedal")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WetlandComponent {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ComponentType tipo;

    @Column(nullable = false, length = 100)
    private String nombre;

    /** Nulo mientras el componente no tenga información publicada. */
    @Column(length = 5000)
    private String descripcion;

    @Builder.Default
    @OneToMany(mappedBy = "componente", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaCreacion ASC")
    private List<WetlandComponentPhoto> fotos = new ArrayList<>();

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @PreUpdate
    void alActualizar() {
        // PostgreSQL guarda microsegundos; así la respuesta coincide con una consulta posterior.
        fechaActualizacion = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
