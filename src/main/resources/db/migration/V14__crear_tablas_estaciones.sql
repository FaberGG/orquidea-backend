-- HU-13: estaciones del recorrido en el mapa. No dependen de un humedal, para que el recorrido pueda crecer.
CREATE TABLE estaciones (
    id                  UUID                     PRIMARY KEY,
    nombre              VARCHAR(150)             NOT NULL,
    descripcion         VARCHAR(2000)            NOT NULL,
    foto_clave          VARCHAR(300)             NOT NULL,
    latitud             NUMERIC(9, 6)            NOT NULL,
    longitud            NUMERIC(9, 6)            NOT NULL,
    fecha_creacion      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_estaciones_latitud CHECK (latitud BETWEEN -90 AND 90),
    CONSTRAINT ck_estaciones_longitud CHECK (longitud BETWEEN -180 AND 180)
);

-- Fichas taxonómicas asociadas a cada estación; al borrar cualquiera de los dos lados se borra la asociación
CREATE TABLE estaciones_fichas (
    estacion_id UUID NOT NULL,
    ficha_id    UUID NOT NULL,
    CONSTRAINT pk_estaciones_fichas PRIMARY KEY (estacion_id, ficha_id),
    CONSTRAINT fk_estaciones_fichas_estacion FOREIGN KEY (estacion_id)
        REFERENCES estaciones (id) ON DELETE CASCADE,
    CONSTRAINT fk_estaciones_fichas_ficha FOREIGN KEY (ficha_id)
        REFERENCES fichas_taxonomicas (id) ON DELETE CASCADE
);

CREATE INDEX ix_estaciones_fichas_ficha ON estaciones_fichas (ficha_id);
