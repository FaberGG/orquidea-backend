-- Recuperación de contraseña: un código de 6 dígitos vigente por usuario. Se guarda el hash, nunca el código.
CREATE TABLE codigos_recuperacion (
    id               UUID                     PRIMARY KEY,
    usuario_id       UUID                     NOT NULL,
    codigo_hash      VARCHAR(100)             NOT NULL,
    intentos         INTEGER                  NOT NULL DEFAULT 0,
    fecha_emision    TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_expiracion TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_codigos_recuperacion_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_codigos_recuperacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);
