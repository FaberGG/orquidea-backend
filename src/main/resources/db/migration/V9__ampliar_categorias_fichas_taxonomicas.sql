-- v2.0 (HU-7, HU-10): se agregan las categorías MAMIFERO, REPTIL, ANFIBIO y HONGO.
ALTER TABLE fichas_taxonomicas DROP CONSTRAINT ck_fichas_categoria;

ALTER TABLE fichas_taxonomicas
    ADD CONSTRAINT ck_fichas_categoria
        CHECK (categoria IN ('AVE', 'PLANTA', 'INSECTO', 'MAMIFERO', 'REPTIL', 'ANFIBIO', 'HONGO'));
