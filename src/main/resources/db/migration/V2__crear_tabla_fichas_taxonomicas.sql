CREATE TABLE fichas_taxonomicas (
    id                  UUID                     PRIMARY KEY,
    categoria           VARCHAR(20)              NOT NULL,
    orden               VARCHAR(100)             NOT NULL,
    familia             VARCHAR(100)             NOT NULL,
    genero              VARCHAR(100)             NOT NULL,
    nombre_cientifico   VARCHAR(200)             NOT NULL,
    nombre_comun        VARCHAR(150)             NOT NULL,
    alimentacion        VARCHAR(2000)            NOT NULL,
    rol_en_humedal      VARCHAR(2000)            NOT NULL,
    estado_conservacion VARCHAR(2)               NOT NULL,
    foto_clave          VARCHAR(300)             NOT NULL,
    fecha_creacion      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_fichas_categoria CHECK (categoria IN ('AVE', 'PLANTA', 'INSECTO')),
    CONSTRAINT ck_fichas_estado_conservacion CHECK (estado_conservacion IN ('EX', 'EW', 'CR', 'EN', 'VU', 'NT', 'LC', 'DD', 'NE'))
);

-- Una ficha por especie, sin distinguir mayúsculas
CREATE UNIQUE INDEX uk_fichas_nombre_cientifico ON fichas_taxonomicas (lower(nombre_cientifico));

-- Listado público por categoría (HU-10)
CREATE INDEX ix_fichas_categoria ON fichas_taxonomicas (categoria);
