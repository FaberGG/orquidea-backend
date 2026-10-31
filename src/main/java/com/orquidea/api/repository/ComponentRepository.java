package com.orquidea.api.repository;

import com.orquidea.api.model.WetlandComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ComponentRepository extends JpaRepository<WetlandComponent, String> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndTipoNot(String nombre, String tipo);

    @Query("select coalesce(max(c.orden), 0) from WetlandComponent c")
    int maximoOrden();
}
