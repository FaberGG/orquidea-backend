-- HU-21: el listado de fichas se pagina, así que el orden alfabético tiene que resolverlo la base de datos.
-- La intercalación por defecto de PostgreSQL en Alpine compara byte a byte ("Zorzal" antes que "águila"); la de
-- ICU en español ordena sin importar mayúsculas ni tildes y pone la "ñ" después de la "n".
ALTER TABLE fichas_taxonomicas ALTER COLUMN nombre_comun TYPE VARCHAR(150) COLLATE "es-x-icu";
