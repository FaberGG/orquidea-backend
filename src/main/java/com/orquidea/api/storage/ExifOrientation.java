package com.orquidea.api.storage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Lee la etiqueta Orientation (0x0112) del primer directorio de un bloque EXIF, que es una estructura TIFF. Los
 * celulares guardan la foto como sale del sensor y anotan aquí cómo girarla para verla derecha.
 */
final class ExifOrientation {

    static final int DERECHA = 1;

    private static final int ETIQUETA_ORIENTACION = 0x0112;
    private static final int TIPO_SHORT = 3;
    private static final int TAMANO_ENTRADA = 12;

    private ExifOrientation() {
    }

    /** @return de 1 a 8; 1 si no hay EXIF, no trae la etiqueta o está dañado */
    static int leer(byte[] tiff) {
        if (tiff == null || tiff.length < 8) {
            return DERECHA;
        }
        ByteBuffer datos = ByteBuffer.wrap(tiff);
        if (tiff[0] == 'I' && tiff[1] == 'I') {
            datos.order(ByteOrder.LITTLE_ENDIAN);
        } else if (tiff[0] != 'M' || tiff[1] != 'M') {
            return DERECHA;
        }
        try {
            if (datos.getShort(2) != 42) {
                return DERECHA;
            }
            int directorio = datos.getInt(4);
            int entradas = Short.toUnsignedInt(datos.getShort(directorio));
            for (int i = 0; i < entradas; i++) {
                int entrada = directorio + 2 + i * TAMANO_ENTRADA;
                if (Short.toUnsignedInt(datos.getShort(entrada)) == ETIQUETA_ORIENTACION) {
                    int valor = Short.toUnsignedInt(datos.getShort(entrada + 8));
                    boolean valido = datos.getShort(entrada + 2) == TIPO_SHORT && valor >= 1 && valor <= 8;
                    return valido ? valor : DERECHA;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            // Desplazamientos fuera del bloque: EXIF dañado, se asume derecha
        }
        return DERECHA;
    }
}
