package com.orquidea.api.storage;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Recorre los bloques de un PNG sin decodificarlo. Conserva los que definen la imagen y sus colores, y descarta
 * los metadatos: textos, fecha, EXIF y cualquier bloque auxiliar desconocido.
 */
final class PngChunks {

    private static final int FIRMA = 8;
    /** Además de los críticos (inicial mayúscula), que nunca se descartan. */
    private static final Set<String> AUXILIARES_CONSERVADOS = Set.of("tRNS", "gAMA", "cHRM", "sRGB", "sBIT", "iCCP");
    private static final String NOMBRE_PERFIL = "ICC Profile";

    private PngChunks() {
    }

    static StrippedImage limpiar(byte[] png) {
        try {
            return recorrer(png);
        } catch (IndexOutOfBoundsException | IllegalArgumentException | DataFormatException e) {
            return StrippedImage.ilegible(png);
        }
    }

    /** El perfil debe ir antes de los datos de la imagen; se pone justo después de IHDR. */
    static byte[] insertarIcc(byte[] png, byte[] perfil) {
        int finIhdr = FIRMA + 12 + ByteBuffer.wrap(png, FIRMA, 4).getInt();
        ByteArrayOutputStream datos = new ByteArrayOutputStream();
        datos.writeBytes(NOMBRE_PERFIL.getBytes(StandardCharsets.ISO_8859_1));
        datos.write(0);
        datos.write(0); // método de compresión: deflate
        datos.writeBytes(comprimir(perfil));

        ByteArrayOutputStream salida = new ByteArrayOutputStream(png.length + datos.size() + 12);
        salida.write(png, 0, finIhdr);
        escribirBloque(salida, "iCCP", datos.toByteArray());
        salida.write(png, finIhdr, png.length - finIhdr);
        return salida.toByteArray();
    }

    private static StrippedImage recorrer(byte[] png) throws DataFormatException {
        ByteArrayOutputStream limpio = new ByteArrayOutputStream(png.length);
        ByteArrayOutputStream decodificable = new ByteArrayOutputStream(png.length);
        limpio.write(png, 0, FIRMA);
        decodificable.write(png, 0, FIRMA);

        byte[] exif = null;
        byte[] perfil = null;
        boolean completo = false;
        int posicion = FIRMA;
        while (posicion < png.length) {
            int longitud = ByteBuffer.wrap(png, posicion, 4).getInt();
            int fin = posicion + 12 + longitud;
            if (longitud < 0 || fin > png.length) {
                throw new IllegalArgumentException("Bloque truncado");
            }
            String tipo = new String(png, posicion + 4, 4, StandardCharsets.US_ASCII);
            int datos = posicion + 8;
            if (tipo.equals("eXIf")) {
                exif = Arrays.copyOfRange(png, datos, datos + longitud);
            } else if (tipo.equals("iCCP")) {
                // El decodificador no lo recibe: los píxeles se leen tal cual y el perfil se vuelve a incrustar
                perfil = descomprimirPerfil(png, datos, datos + longitud);
                limpio.write(png, posicion, fin - posicion);
            } else if (Character.isUpperCase(tipo.charAt(0)) || AUXILIARES_CONSERVADOS.contains(tipo)) {
                limpio.write(png, posicion, fin - posicion);
                decodificable.write(png, posicion, fin - posicion);
            }
            posicion = fin;
            if (tipo.equals("IEND")) {
                completo = true;
                break;
            }
        }
        return new StrippedImage(limpio.toByteArray(), decodificable.toByteArray(), ExifOrientation.leer(exif),
                StrippedImage.esPerfilRgb(perfil) ? perfil : null, completo);
    }

    /** iCCP: nombre (1 a 79 bytes), un 0, el método de compresión y el perfil comprimido con zlib. */
    private static byte[] descomprimirPerfil(byte[] png, int desde, int hasta) throws DataFormatException {
        int finNombre = desde;
        while (finNombre < hasta && png[finNombre] != 0) {
            finNombre++;
        }
        int comprimido = finNombre + 2;
        if (comprimido > hasta) {
            return null;
        }
        Inflater descompresor = new Inflater();
        try {
            descompresor.setInput(png, comprimido, hasta - comprimido);
            ByteArrayOutputStream perfil = new ByteArrayOutputStream();
            byte[] bloque = new byte[8192];
            while (!descompresor.finished()) {
                int leidos = descompresor.inflate(bloque);
                if (leidos == 0 && (descompresor.needsInput() || descompresor.needsDictionary())) {
                    return null;
                }
                perfil.write(bloque, 0, leidos);
            }
            return perfil.toByteArray();
        } finally {
            descompresor.end();
        }
    }

    private static byte[] comprimir(byte[] datos) {
        Deflater compresor = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            compresor.setInput(datos);
            compresor.finish();
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            byte[] bloque = new byte[8192];
            while (!compresor.finished()) {
                salida.write(bloque, 0, compresor.deflate(bloque));
            }
            return salida.toByteArray();
        } finally {
            compresor.end();
        }
    }

    /** Longitud, tipo, datos y el CRC-32 de tipo y datos. */
    private static void escribirBloque(ByteArrayOutputStream salida, String tipo, byte[] datos) {
        byte[] tipoBytes = tipo.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(tipoBytes);
        crc.update(datos);
        salida.writeBytes(ByteBuffer.allocate(4).putInt(datos.length).array());
        salida.writeBytes(tipoBytes);
        salida.writeBytes(datos);
        salida.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }
}
