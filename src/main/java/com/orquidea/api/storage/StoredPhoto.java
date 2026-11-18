package com.orquidea.api.storage;

/** Claves en el almacenamiento de una foto subida y de su miniatura. */
public record StoredPhoto(String clave, String miniaturaClave) {
}
