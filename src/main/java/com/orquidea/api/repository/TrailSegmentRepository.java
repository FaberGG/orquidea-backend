package com.orquidea.api.repository;

import com.orquidea.api.model.TrailSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface TrailSegmentRepository extends JpaRepository<TrailSegment, UUID> {

    /** Rectángulo que contiene todos los tramos; todos los valores son nulos si no hay tramos. */
    interface Bounds {
        Double getMinLon();

        Double getMinLat();

        Double getMaxLon();

        Double getMaxLat();
    }

    /** Se calcula en la base de datos a partir de las coordenadas de cada LineString, nunca con valores fijos. */
    @Query(value = """
            SELECT MIN(CAST(punto.value ->> 0 AS double precision)) AS "minLon",
                   MIN(CAST(punto.value ->> 1 AS double precision)) AS "minLat",
                   MAX(CAST(punto.value ->> 0 AS double precision)) AS "maxLon",
                   MAX(CAST(punto.value ->> 1 AS double precision)) AS "maxLat"
            FROM tramos_recorrido t
            CROSS JOIN LATERAL jsonb_array_elements(t.geometria -> 'coordinates') AS punto(value)
            """, nativeQuery = true)
    Bounds calcularLimites();
}
