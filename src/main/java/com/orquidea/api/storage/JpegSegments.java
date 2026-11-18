package com.orquidea.api.storage;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Recorre los segmentos de un JPEG sin decodificarlo. Quita los metadatos (EXIF con el GPS y el modelo del
 * teléfono, XMP, IPTC, comentarios y las imágenes extra que algunos celulares añaden al final) sin tocar los
 * datos de la imagen, y conserva lo que afecta a cómo se ven los colores: el perfil ICC y el segmento Adobe.
 */
final class JpegSegments {

    private static final int SOI = 0xD8;
    private static final int EOI = 0xD9;
    private static final int SOS = 0xDA;
    private static final int APP0 = 0xE0;
    private static final int APP1 = 0xE1;
    private static final int APP2 = 0xE2;
    private static final int APP14 = 0xEE;
    private static final int APP15 = 0xEF;
    private static final int COM = 0xFE;

    private static final byte[] FIRMA_JFIF = "JFIF\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] FIRMA_EXIF = "Exif\0\0".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] FIRMA_ICC = "ICC_PROFILE\0".getBytes(StandardCharsets.US_ASCII);
    /** Cabecera de un segmento APP2 de perfil: firma, número de trozo y total de trozos. */
    private static final int CABECERA_ICC = FIRMA_ICC.length + 2;
    /** Lo que cabe de perfil en un segmento: 65535 menos los 2 bytes de longitud y la cabecera. */
    private static final int MAXIMO_ICC_POR_SEGMENTO = 0xFFFF - 2 - CABECERA_ICC;

    private JpegSegments() {
    }

    static StrippedImage limpiar(byte[] jpeg) {
        try {
            return recorrer(jpeg);
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            return StrippedImage.ilegible(jpeg);
        }
    }

    /**
     * Inserta el perfil de color detrás de SOI y del segmento JFIF, que debe ir primero. Un perfil de más de 64 KB
     * se parte en varios segmentos numerados.
     */
    static byte[] insertarIcc(byte[] jpeg, byte[] perfil) {
        int posicion = 2;
        if (marcador(jpeg, posicion) == APP0) {
            posicion += 2 + longitud(jpeg, posicion);
        }
        int trozos = (perfil.length + MAXIMO_ICC_POR_SEGMENTO - 1) / MAXIMO_ICC_POR_SEGMENTO;
        ByteArrayOutputStream salida = new ByteArrayOutputStream(jpeg.length + perfil.length + trozos * 18);
        salida.write(jpeg, 0, posicion);
        for (int i = 0; i < trozos; i++) {
            int desde = i * MAXIMO_ICC_POR_SEGMENTO;
            int tamano = Math.min(MAXIMO_ICC_POR_SEGMENTO, perfil.length - desde);
            int longitud = 2 + CABECERA_ICC + tamano;
            salida.write(0xFF);
            salida.write(APP2);
            salida.write(longitud >> 8);
            salida.write(longitud & 0xFF);
            salida.writeBytes(FIRMA_ICC);
            salida.write(i + 1);
            salida.write(trozos);
            salida.write(perfil, desde, tamano);
        }
        salida.write(jpeg, posicion, jpeg.length - posicion);
        return salida.toByteArray();
    }

    private static StrippedImage recorrer(byte[] jpeg) {
        if (marcadorEn(jpeg, 0) != SOI) {
            throw new IllegalArgumentException("No empieza con SOI");
        }
        ByteArrayOutputStream limpio = new ByteArrayOutputStream(jpeg.length);
        // Cabecera sin SOI: según el perfil, el decodificable lleva los segmentos ICC delante
        ByteArrayOutputStream decodificable = new ByteArrayOutputStream(jpeg.length);
        limpio.write(jpeg, 0, 2);

        byte[] exif = null;
        SortedMap<Integer, byte[]> trozosIcc = new TreeMap<>();
        ByteArrayOutputStream segmentosIcc = new ByteArrayOutputStream();
        int componentes = 0;
        int posicion = 2;

        // Cabecera: hasta el primer SOS
        while (true) {
            posicion = saltarRelleno(jpeg, posicion);
            int marcador = marcadorEn(jpeg, posicion);
            if (marcador == SOS) {
                break;
            }
            int fin = posicion + 2 + longitud(jpeg, posicion);
            if (fin > jpeg.length) {
                throw new IllegalArgumentException("Segmento truncado");
            }
            int datos = posicion + 4;
            if (marcador == APP0) {
                if (empiezaCon(jpeg, datos, FIRMA_JFIF)) {
                    copiar(jpeg, posicion, fin, limpio, decodificable);
                }
            } else if (marcador == APP1) {
                if (empiezaCon(jpeg, datos, FIRMA_EXIF)) {
                    exif = Arrays.copyOfRange(jpeg, datos + FIRMA_EXIF.length, fin);
                }
            } else if (marcador == APP2) {
                if (empiezaCon(jpeg, datos, FIRMA_ICC) && fin - datos > CABECERA_ICC) {
                    trozosIcc.put(jpeg[datos + FIRMA_ICC.length] & 0xFF,
                            Arrays.copyOfRange(jpeg, datos + CABECERA_ICC, fin));
                    limpio.write(jpeg, posicion, fin - posicion);
                    segmentosIcc.write(jpeg, posicion, fin - posicion);
                }
            } else if (marcador == APP14) {
                copiar(jpeg, posicion, fin, limpio, decodificable);
            } else if (marcador == COM || (marcador > APP0 && marcador <= APP15)) {
                // Metadatos: se descartan
            } else {
                if (esSof(marcador)) {
                    componentes = jpeg[posicion + 9] & 0xFF;
                }
                copiar(jpeg, posicion, fin, limpio, decodificable);
            }
            posicion = fin;
        }

        // Imagen: de SOS a EOI tal cual. Lo que sigue a EOI (imágenes extra de algunos celulares) se descarta.
        int finImagen = buscarFin(jpeg, posicion);
        boolean completa = finImagen > 0;
        int hasta = completa ? finImagen : jpeg.length;
        limpio.write(jpeg, posicion, hasta - posicion);
        decodificable.write(jpeg, posicion, hasta - posicion);

        ByteArrayOutputStream paraDecodificar = new ByteArrayOutputStream(jpeg.length);
        paraDecodificar.write(jpeg, 0, 2);
        byte[] perfil = unirPerfil(trozosIcc);
        if (!StrippedImage.esPerfilRgb(perfil)) {
            // Otro espacio de color (CMYK, gris): el decodificador lo necesita para convertir a RGB
            paraDecodificar.writeBytes(segmentosIcc.toByteArray());
            perfil = null;
        }
        paraDecodificar.writeBytes(decodificable.toByteArray());

        boolean conservable = completa && (componentes == 1 || componentes == 3);
        return new StrippedImage(limpio.toByteArray(), paraDecodificar.toByteArray(), ExifOrientation.leer(exif),
                perfil, conservable);
    }

    /**
     * Posición siguiente a EOI, o -1 si el archivo está truncado. Dentro de los datos comprimidos un 0xFF va
     * seguido de 0x00 (relleno) o de un marcador de reinicio; cualquier otro marcador abre un segmento (las tablas
     * y los barridos siguientes de un JPEG progresivo), que se salta entero por su longitud.
     */
    private static int buscarFin(byte[] jpeg, int posicion) {
        int i = posicion;
        while (i + 1 < jpeg.length) {
            if ((jpeg[i] & 0xFF) != 0xFF) {
                i++;
                continue;
            }
            int siguiente = jpeg[i + 1] & 0xFF;
            if (siguiente == EOI) {
                return i + 2;
            }
            if (siguiente == 0x00 || siguiente == 0xFF || (siguiente >= 0xD0 && siguiente <= 0xD7)) {
                i += siguiente == 0xFF ? 1 : 2;
            } else if (i + 3 < jpeg.length) {
                i += 2 + longitud(jpeg, i);
            } else {
                return -1;
            }
        }
        return -1;
    }

    /** Los trozos van numerados desde 1; si falta alguno, el perfil no se puede reconstruir. */
    private static byte[] unirPerfil(SortedMap<Integer, byte[]> trozos) {
        if (trozos.isEmpty() || trozos.firstKey() != 1 || trozos.lastKey() != trozos.size()) {
            return null;
        }
        ByteArrayOutputStream perfil = new ByteArrayOutputStream();
        trozos.values().forEach(perfil::writeBytes);
        return perfil.toByteArray();
    }

    /** Marcadores de inicio de cuadro (SOF0 a SOF15), menos DHT (C4), JPG (C8) y DAC (CC), que comparten rango. */
    private static boolean esSof(int marcador) {
        return marcador >= 0xC0 && marcador <= 0xCF && marcador != 0xC4 && marcador != 0xC8 && marcador != 0xCC;
    }

    /** Entre segmentos puede haber bytes 0xFF de relleno antes del marcador. */
    private static int saltarRelleno(byte[] jpeg, int posicion) {
        while ((jpeg[posicion] & 0xFF) == 0xFF && (jpeg[posicion + 1] & 0xFF) == 0xFF) {
            posicion++;
        }
        return posicion;
    }

    private static int marcadorEn(byte[] jpeg, int posicion) {
        if ((jpeg[posicion] & 0xFF) != 0xFF) {
            throw new IllegalArgumentException("Se esperaba un marcador");
        }
        return jpeg[posicion + 1] & 0xFF;
    }

    private static int marcador(byte[] jpeg, int posicion) {
        return posicion + 1 < jpeg.length && (jpeg[posicion] & 0xFF) == 0xFF ? jpeg[posicion + 1] & 0xFF : -1;
    }

    /** La longitud de un segmento incluye sus propios 2 bytes, pero no los del marcador. */
    private static int longitud(byte[] jpeg, int posicion) {
        int longitud = ((jpeg[posicion + 2] & 0xFF) << 8) | (jpeg[posicion + 3] & 0xFF);
        if (longitud < 2) {
            throw new IllegalArgumentException("Longitud de segmento inválida");
        }
        return longitud;
    }

    private static boolean empiezaCon(byte[] jpeg, int posicion, byte[] firma) {
        return posicion + firma.length <= jpeg.length
                && Arrays.equals(jpeg, posicion, posicion + firma.length, firma, 0, firma.length);
    }

    private static void copiar(byte[] jpeg, int desde, int hasta, ByteArrayOutputStream... destinos) {
        for (ByteArrayOutputStream destino : destinos) {
            destino.write(jpeg, desde, hasta - desde);
        }
    }
}
