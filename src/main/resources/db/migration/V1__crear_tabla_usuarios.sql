CREATE TABLE usuarios (
    id              UUID                     PRIMARY KEY,
    nombre_completo VARCHAR(150)             NOT NULL,
    correo          VARCHAR(254)             NOT NULL,
    contrasena_hash VARCHAR(100)             NOT NULL,
    telefono        VARCHAR(20),
    rol             VARCHAR(30)              NOT NULL,
    habilitado      BOOLEAN                  NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_usuarios_correo UNIQUE (correo),
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('SUPERADMINISTRADOR', 'ADMINISTRADOR', 'USUARIO_REGISTRADO'))
);
