package com.orquidea.api.repository;

import com.orquidea.api.model.TaxonPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Las fotos se crean y eliminan a través de su ficha; este repositorio solo sirve para reprocesarlas. */
public interface TaxonPhotoRepository extends JpaRepository<TaxonPhoto, UUID> {

    /** Fotos subidas antes de las miniaturas y aún sin reprocesar. */
    List<TaxonPhoto> findByMiniaturaClaveIsNull();
}
