package com.orquidea.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/**
 * Ficha taxonómica eliminada (HU-21), para que la app sin conexión borre su copia al sincronizar. La escribe un
 * trigger de la base de datos al borrar la ficha (V17), así que cubre cualquier forma de borrarla; la API solo la lee.
 */
@Entity
@Immutable
@Table(name = "fichas_eliminadas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeletedTaxon {

    /** Id que tenía la ficha. */
    @Id
    private UUID id;

    @Column(name = "fecha_eliminacion", nullable = false)
    private Instant fechaEliminacion;
}
