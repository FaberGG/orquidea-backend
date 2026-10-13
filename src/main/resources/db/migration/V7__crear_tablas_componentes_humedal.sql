-- HU-14 / HU-15: componentes informativos del humedal. Son cuatro fijos; el administrador edita su contenido.
CREATE TABLE componentes_humedal (
    tipo                VARCHAR(20)              PRIMARY KEY,
    nombre              VARCHAR(100)             NOT NULL,
    descripcion         VARCHAR(5000),
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT ck_componentes_tipo CHECK (tipo IN ('INSECTOS', 'AVES', 'FLORA', 'OTROS'))
);

-- Sin descripcion ni fotos un componente cuenta como "sin contenido" (HU-14, escenario 2)
INSERT INTO componentes_humedal (tipo, nombre) VALUES
    ('INSECTOS', 'Insectos'),
    ('AVES', 'Aves'),
    ('FLORA', 'Flora'),
    ('OTROS', 'Otros');

CREATE TABLE fotos_componente (
    id             UUID                     PRIMARY KEY,
    componente_tipo VARCHAR(20)             NOT NULL,
    foto_clave     VARCHAR(300)             NOT NULL,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_fotos_componente_componente FOREIGN KEY (componente_tipo)
        REFERENCES componentes_humedal (tipo) ON DELETE CASCADE
);

CREATE INDEX ix_fotos_componente_componente ON fotos_componente (componente_tipo);
