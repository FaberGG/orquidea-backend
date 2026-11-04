-- HU-12: trazado georreferenciado del recorrido. Es una lista de tramos (no una sola línea) para poder
-- sumar después el recorrido entre humedales cargando tramos nuevos, sin cambiar código.
-- La geometría es GeoJSON (LineString, coordenadas [longitud, latitud]) en jsonb. No hay HU para
-- editarla desde la app: se carga con una migración generada por scripts/gpx_a_sql.py.
CREATE TABLE tramos_recorrido (
    id                  UUID                     PRIMARY KEY,
    nombre              VARCHAR(150)             NOT NULL,
    orden               INTEGER                  NOT NULL,
    geometria           JSONB                    NOT NULL,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ux_tramos_recorrido_orden UNIQUE (orden),
    CONSTRAINT ck_tramos_recorrido_geometria CHECK (
        CASE
            WHEN jsonb_typeof(geometria) = 'object'
                AND geometria ->> 'type' = 'LineString'
            AND jsonb_typeof(geometria -> 'coordinates') = 'array'
        THEN jsonb_array_length(geometria -> 'coordinates') >= 2
        ELSE FALSE
        END
)
);
