-- La HU-2 (registro) pide nombre y apellido por separado.
ALTER TABLE usuarios ADD COLUMN nombre   VARCHAR(100);
ALTER TABLE usuarios ADD COLUMN apellido VARCHAR(100);

-- Usuarios existentes: la primera palabra es el nombre y el resto el apellido (vacío si no hay más palabras)
UPDATE usuarios
SET nombre   = split_part(trim(nombre_completo), ' ', 1),
    apellido = trim(substr(trim(nombre_completo), length(split_part(trim(nombre_completo), ' ', 1)) + 1));

ALTER TABLE usuarios ALTER COLUMN nombre SET NOT NULL;
ALTER TABLE usuarios ALTER COLUMN apellido SET NOT NULL;
ALTER TABLE usuarios DROP COLUMN nombre_completo;
