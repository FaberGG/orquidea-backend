-- V8__agregar_prioridad_estado_y_vigencia_a_anuncios.sql
ALTER TABLE ANUNCIOS
    ADD COLUMN prioridad         VARCHAR(30)              NOT NULL DEFAULT 'MEDIA',
    ADD COLUMN estado            VARCHAR(30)              NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN fecha_expiracion  TIMESTAMP WITH TIME ZONE NULL;

ALTER TABLE ANUNCIOS
    ADD CONSTRAINT ck_anuncios_prioridad CHECK (prioridad IN ('ALTA', 'MEDIA', 'BAJA')),
    ADD CONSTRAINT ck_anuncios_estado    CHECK (estado IN ('ACTIVO', 'INACTIVO'));

CREATE INDEX idx_anuncios_estado_expiracion ON ANUNCIOS (estado, fecha_expiracion);