-- HU-7 (v2.0): una ficha admite varias fotos; la de menor posicion es la principal del listado.
CREATE TABLE fotos_ficha (
    id             UUID                     PRIMARY KEY,
    ficha_id       UUID                     NOT NULL,
    foto_clave     VARCHAR(300)             NOT NULL,
    posicion       INTEGER                  NOT NULL,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_fotos_ficha_ficha FOREIGN KEY (ficha_id)
        REFERENCES fichas_taxonomicas (id) ON DELETE CASCADE
);

CREATE INDEX ix_fotos_ficha_ficha ON fotos_ficha (ficha_id);

-- La foto única de cada ficha existente pasa a ser su foto principal
INSERT INTO fotos_ficha (id, ficha_id, foto_clave, posicion, fecha_creacion)
SELECT gen_random_uuid(), id, foto_clave, 0, fecha_creacion FROM fichas_taxonomicas;

ALTER TABLE fichas_taxonomicas DROP COLUMN foto_clave;
