-- HU-25: los componentes del humedal dejan de ser un enum fijo y pasan a identificarse por un código
-- de texto único generado a partir del nombre (p. ej. COMPONENTE_HIDRICO). Los cuatro actuales conservan
-- sus códigos (INSECTOS, AVES, FLORA, OTROS).

ALTER TABLE fotos_componente DROP CONSTRAINT fk_fotos_componente_componente;
ALTER TABLE componentes_humedal DROP CONSTRAINT ck_componentes_tipo;

ALTER TABLE componentes_humedal ALTER COLUMN tipo TYPE VARCHAR(100);
ALTER TABLE fotos_componente ALTER COLUMN componente_tipo TYPE VARCHAR(100);

ALTER TABLE fotos_componente
    ADD CONSTRAINT fk_fotos_componente_componente FOREIGN KEY (componente_tipo)
        REFERENCES componentes_humedal (tipo) ON DELETE CASCADE;

-- Orden de presentación: los cuatro existentes conservan el suyo y los nuevos se agregan al final
ALTER TABLE componentes_humedal ADD COLUMN orden INTEGER;
UPDATE componentes_humedal SET orden = 1 WHERE tipo = 'INSECTOS';
UPDATE componentes_humedal SET orden = 2 WHERE tipo = 'AVES';
UPDATE componentes_humedal SET orden = 3 WHERE tipo = 'FLORA';
UPDATE componentes_humedal SET orden = 4 WHERE tipo = 'OTROS';
ALTER TABLE componentes_humedal ALTER COLUMN orden SET NOT NULL;

-- El nombre no se repite, sin distinguir mayúsculas
CREATE UNIQUE INDEX ux_componentes_nombre ON componentes_humedal (lower(nombre));
