package com.orquidea.api.repository;

import com.orquidea.api.model.Station;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StationRepository extends JpaRepository<Station, UUID> {
}
