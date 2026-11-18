-- Miniatura de cada foto, para los listados. Queda nula en las fotos subidas antes de esta versión hasta que se
-- reprocesen (STORAGE_REPROCESAR_FOTOS=aplicar); mientras tanto la API devuelve la foto completa como miniatura.
ALTER TABLE fotos_ficha ADD COLUMN miniatura_clave VARCHAR(300);
ALTER TABLE estaciones ADD COLUMN miniatura_clave VARCHAR(300);
ALTER TABLE fotos_componente ADD COLUMN miniatura_clave VARCHAR(300);
ALTER TABLE reportes_avistamiento ADD COLUMN miniatura_clave VARCHAR(300);
