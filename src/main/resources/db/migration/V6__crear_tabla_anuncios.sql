CREATE TABLE ANUNCIOS (
    id              UUID                     PRIMARY KEY,
    titulo          VARCHAR(150)             NOT NULL,
    descripcion     VARCHAR(1000)             NOT NULL,
    fecha_creacion  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);