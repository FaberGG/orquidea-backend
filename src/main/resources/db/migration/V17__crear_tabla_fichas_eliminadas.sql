-- HU-21: ids de las fichas eliminadas, para que la app sin conexión borre su copia al sincronizar.
-- La llena un trigger, así queda registrado cualquier borrado: desde la API o directamente en la base de datos.
CREATE TABLE fichas_eliminadas (
    id                UUID                     PRIMARY KEY,
    fecha_eliminacion TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX ix_fichas_eliminadas_fecha ON fichas_eliminadas (fecha_eliminacion);

-- clock_timestamp() y no now(), que es la hora de inicio de la transacción y puede ser muy anterior al borrado.
-- Si una ficha restaurada se vuelve a borrar, solo se actualiza la fecha.
CREATE FUNCTION registrar_ficha_eliminada() RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO fichas_eliminadas (id, fecha_eliminacion)
    VALUES (OLD.id, clock_timestamp())
    ON CONFLICT (id) DO UPDATE SET fecha_eliminacion = EXCLUDED.fecha_eliminacion;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_fichas_eliminadas
    AFTER DELETE ON fichas_taxonomicas
    FOR EACH ROW EXECUTE FUNCTION registrar_ficha_eliminada();

-- La sincronización busca las fichas modificadas desde una fecha
CREATE INDEX ix_fichas_fecha_actualizacion ON fichas_taxonomicas (fecha_actualizacion);
