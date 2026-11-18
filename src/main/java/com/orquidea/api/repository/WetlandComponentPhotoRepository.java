package com.orquidea.api.repository;

import com.orquidea.api.model.WetlandComponentPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Las fotos se crean y eliminan a través de su componente; este repositorio solo sirve para reprocesarlas. */
public interface WetlandComponentPhotoRepository extends JpaRepository<WetlandComponentPhoto, UUID> {

    /** Fotos subidas antes de las miniaturas y aún sin reprocesar. */
    List<WetlandComponentPhoto> findByMiniaturaClaveIsNull();
}
