package com.orquidea.api.storage;

/**
 * Un archivo de imagen sin sus metadatos, obtenido sin decodificarlo.
 *
 * @param sinMetadatos    el archivo original sin EXIF ni otros metadatos, con los mismos datos de imagen y el
 *                        perfil de color; solo es válido si {@code conservable}
 * @param paraDecodificar lo que se entrega al decodificador: sin metadatos y sin el perfil de color RGB, para
 *                        que los píxeles lleguen tal cual y el perfil se vuelva a incrustar al codificar
 * @param orientacion     orientación EXIF, de 1 (derecha) a 8
 * @param perfilIcc       perfil de color RGB que se incrusta en la foto optimizada, o null
 * @param conservable     el original se puede guardar tal cual: está completo y el formato lo leen todos los clientes
 */
record StrippedImage(byte[] sinMetadatos, byte[] paraDecodificar, int orientacion, byte[] perfilIcc,
                     boolean conservable) {

    /** Estructura dañada: se deja al decodificador y se recodifica desde los píxeles, que no llevan metadatos. */
    static StrippedImage ilegible(byte[] original) {
        return new StrippedImage(null, original, 1, null, false);
    }

    /** Los perfiles ICC declaran el espacio de color de los datos en los bytes 16 a 19. */
    static boolean esPerfilRgb(byte[] perfil) {
        return perfil != null && perfil.length >= 20
                && perfil[16] == 'R' && perfil[17] == 'G' && perfil[18] == 'B' && perfil[19] == ' ';
    }
}
