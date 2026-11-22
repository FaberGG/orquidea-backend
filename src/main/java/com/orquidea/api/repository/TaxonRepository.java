package com.orquidea.api.repository;

import com.orquidea.api.model.Taxon;
import com.orquidea.api.model.TaxonCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TaxonRepository extends JpaRepository<Taxon, UUID> {

    boolean existsByScientificNameIgnoreCase(String scientificName);

    /** Para editar: el nombre solo choca si lo usa otra ficha distinta a la que se edita. */
    boolean existsByScientificNameIgnoreCaseAndIdNot(String scientificName, UUID id);

    Page<Taxon> findByCategoria(TaxonCategory categoria, Pageable paginacion);

    /** HU-21: fichas creadas o modificadas desde esa fecha (también cuando cambian sus fotos). */
    List<Taxon> findByFechaActualizacionGreaterThanEqual(Instant desde, Sort orden);
}
