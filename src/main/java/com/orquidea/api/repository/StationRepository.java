package com.orquidea.api.repository;

import com.orquidea.api.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StationRepository extends JpaRepository<Station, UUID> {

    /** Estaciones cuya foto se subió antes de las miniaturas y aún no se reprocesa. */
    List<Station> findByMiniaturaClaveIsNull();
}
