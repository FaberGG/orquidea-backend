package com.orquidea.api.repository;

import com.orquidea.api.model.Taxon;
import org.springframework.data.jpa.repository.JpaRepository;
import com.orquidea.api.model.TaxonCategory;
import java.util.List;
import org.springframework.data.domain.Sort;

import java.util.UUID;

public interface TaxonRepository extends JpaRepository<Taxon, UUID> {

    boolean existsByScientificNameIgnoreCase(String scientificName);

    /** Para editar: el nombre solo choca si lo usa otra ficha distinta a la que se edita. */
    boolean existsByScientificNameIgnoreCaseAndIdNot(String scientificName, UUID id);

    List<Taxon> findByCategoria(TaxonCategory categoria, Sort order);
}
