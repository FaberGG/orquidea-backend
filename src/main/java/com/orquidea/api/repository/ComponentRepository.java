package com.orquidea.api.repository;

import com.orquidea.api.model.ComponentType;
import com.orquidea.api.model.WetlandComponent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComponentRepository extends JpaRepository<WetlandComponent, ComponentType> {
}
