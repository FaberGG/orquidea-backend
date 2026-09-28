package com.orquidea.api.repository;

import com.orquidea.api.model.FichaTaxonomica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FichaTaxonomicaRepositorio extends JpaRepository<FichaTaxonomica, UUID> {

    boolean existsByNombreCientificoIgnoreCase(String nombreCientifico);

    /** Para editar: el nombre solo choca si lo usa otra ficha distinta a la que se edita. */
    boolean existsByNombreCientificoIgnoreCaseAndIdNot(String nombreCientifico, UUID id);
}
