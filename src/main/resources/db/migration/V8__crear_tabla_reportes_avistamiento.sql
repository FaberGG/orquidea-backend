-- HU-18 a HU-20: reportes ciudadanos de avistamiento (Darwin Core: Occurrence).
-- El id lo puede generar el cliente, para que la sincronización sin conexión (HU-22) no duplique reportes.
CREATE TABLE reportes_avistamiento (
    id             UUID                     PRIMARY KEY,
    descripcion    VARCHAR(2000)            NOT NULL,
    reino          VARCHAR(20)              NOT NULL,
    clasificacion  VARCHAR(150)             NOT NULL,
    latitud        NUMERIC(9, 6),
    longitud       NUMERIC(9, 6),
    foto_clave     VARCHAR(300),
    estado         VARCHAR(20)              NOT NULL,
    autor_id       UUID                     NOT NULL,
    revisor_id     UUID,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    fecha_revision TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_reportes_autor FOREIGN KEY (autor_id) REFERENCES usuarios (id),
    CONSTRAINT fk_reportes_revisor FOREIGN KEY (revisor_id) REFERENCES usuarios (id),
    CONSTRAINT ck_reportes_reino CHECK (reino IN ('ANIMALIA', 'PLANTAE', 'FUNGI', 'DESCONOCIDO')),
    CONSTRAINT ck_reportes_estado CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO')),
    CONSTRAINT ck_reportes_revision CHECK ((estado = 'PENDIENTE') = (fecha_revision IS NULL)),
    -- El lugar es opcional, pero latitud y longitud van siempre juntas y dentro de rango
    CONSTRAINT ck_reportes_coordenadas CHECK ((latitud IS NULL) = (longitud IS NULL)),
    CONSTRAINT ck_reportes_latitud CHECK (latitud BETWEEN -90 AND 90),
    CONSTRAINT ck_reportes_longitud CHECK (longitud BETWEEN -180 AND 180)
);

-- Cola de revisión (HU-19): pendientes, los más antiguos primero
CREATE INDEX ix_reportes_estado_creacion ON reportes_avistamiento (estado, fecha_creacion);

-- Foro público (HU-20): aprobados, los más recientes primero
CREATE INDEX ix_reportes_estado_revision ON reportes_avistamiento (estado, fecha_revision DESC);
