package com.orquidea.api.storage;

/**
 * Foto lista para guardar: la optimizada y su miniatura para los listados, ambas en el mismo formato.
 *
 * @param tipo puede no ser el de la foto subida: un PNG sin transparencia se guarda como JPEG
 */
public record ProcessedImage(byte[] contenido, ImageType tipo, byte[] miniatura) {
}
