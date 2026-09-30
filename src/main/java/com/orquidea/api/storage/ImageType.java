package com.orquidea.api.storage;

import java.util.Arrays;
import java.util.Optional;

/**
 * Formatos de imagen aceptados (HU-7: jpg y png). Se reconocen por su firma de bytes,
 * no por la extensión ni por el Content-Type que declara el cliente.
 */
public enum ImageType {

    JPEG("image/jpeg", "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("image/png", "png", new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});

    private final String tipoContenido;
    private final String extension;
    private final byte[] firma;

    ImageType(String tipoContenido, String extension, byte[] firma) {
        this.tipoContenido = tipoContenido;
        this.extension = extension;
        this.firma = firma;
    }

    public String tipoContenido() {
        return tipoContenido;
    }

    public String extension() {
        return extension;
    }

    public static Optional<ImageType> detectar(byte[] contenido) {
        return Arrays.stream(values())
                .filter(tipo -> contenido.length >= tipo.firma.length
                        && Arrays.equals(contenido, 0, tipo.firma.length, tipo.firma, 0, tipo.firma.length))
                .findFirst();
    }
}
