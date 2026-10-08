package com.orquidea.api.repository;

import com.orquidea.api.model.Occurrence;
import com.orquidea.api.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OccurrenceRepository extends JpaRepository<Occurrence, UUID> {

    /** Trae el autor en la misma consulta, porque el listado siempre lo muestra. */
    @EntityGraph(attributePaths = "recordedBy")
    Page<Occurrence> findByEstado(ReviewStatus estado, Pageable paginacion);
}
