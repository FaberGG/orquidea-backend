--La HU2 de registro no pide el telefono como campo para registrarse, por lo que se elimina de la tabla usuarios.
ALTER TABLE usuarios
DROP COLUMN telefono;