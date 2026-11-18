package com.orquidea.api.storage;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/** Imágenes y utilidades para las pruebas de las fotos. */
public final class TestImages {

    public static final int APP1 = 0xE1;
    public static final int APP2 = 0xE2;
    public static final int SOF_PROGRESIVO = 0xC2;
    private static final int SOS = 0xDA;
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private TestImages() {
    }

    /** Segmento de la cabecera de un JPEG, sin el marcador ni la longitud. */
    public record Segment(int marcador, byte[] datos) {

        public boolean esSof() {
            return marcador >= 0xC0 && marcador <= 0xCF && marcador != 0xC4 && marcador != 0xC8 && marcador != 0xCC;
        }

        public boolean empiezaCon(String firma) {
            byte[] bytes = firma.getBytes(StandardCharsets.ISO_8859_1);
            return datos.length >= bytes.length && Arrays.equals(datos, 0, bytes.length, bytes, 0, bytes.length);
        }
    }

    /** Objeto leído desde su URL pública, como lo haría un navegador. */
    public record Download(int estado, String tipoContenido, byte[] contenido) {

        public BufferedImage imagen() {
            return leer(contenido);
        }
    }

    /**
     * Imagen con apariencia de foto: degradados, ondas y algo de ruido, para que comprimirla cueste parecido a
     * comprimir una foto real (una imagen lisa se comprime casi a nada con cualquier calidad).
     */
    public static BufferedImage fotografia(int ancho, int alto) {
        Random aleatorio = new Random(ancho * 31L + alto);
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                double onda = Math.sin(x / 23.0) * Math.cos(y / 31.0);
                int rojo = canal(70 + 110.0 * x / ancho + 30 * onda + aleatorio.nextGaussian() * 6);
                int verde = canal(110 + 90.0 * y / alto - 25 * onda + aleatorio.nextGaussian() * 6);
                int azul = canal(150 + 60 * Math.sin((x + y) / 57.0) + aleatorio.nextGaussian() * 6);
                imagen.setRGB(x, y, rojo << 16 | verde << 8 | azul);
            }
        }
        return imagen;
    }

    /** Cuatro cuadrantes de colores, para comprobar hacia dónde quedó girada una imagen. */
    public static BufferedImage cuadrantes(int ancho, int alto, int arribaIzquierda, int arribaDerecha,
                                           int abajoIzquierda, int abajoDerecha) {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                boolean arriba = y < alto / 2;
                boolean izquierda = x < ancho / 2;
                imagen.setRGB(x, y, arriba
                        ? (izquierda ? arribaIzquierda : arribaDerecha)
                        : (izquierda ? abajoIzquierda : abajoDerecha));
            }
        }
        return imagen;
    }

    public static byte[] jpeg(BufferedImage imagen, float calidad) {
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ImageOutputStream flujo = ImageIO.createImageOutputStream(salida)) {
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(calidad);
            escritor.setOutput(flujo);
            escritor.write(null, new IIOImage(imagen, null, null), parametros);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            escritor.dispose();
        }
        return salida.toByteArray();
    }

    public static byte[] png(BufferedImage imagen) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            ImageIO.write(imagen, "png", salida);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return salida.toByteArray();
    }

    public static BufferedImage leer(byte[] contenido) {
        try {
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(contenido));
            if (imagen == null) {
                throw new IllegalArgumentException("No es una imagen");
            }
            return imagen;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Bloque EXIF ("Exif\0\0" + estructura TIFF) con la orientación y, como haría un celular, la marca del
     * teléfono y la ubicación GPS.
     */
    public static byte[] exif(int orientacion, String fabricante) {
        byte[] marca = (fabricante + "\0").getBytes(StandardCharsets.US_ASCII);
        int directorio = 8;
        int tamanoDirectorio = 2 + 3 * 12 + 4;
        int posicionMarca = directorio + tamanoDirectorio;
        int posicionGps = posicionMarca + marca.length + (marca.length % 2);
        int tamanoGps = 2 + 2 * 12 + 4;
        int posicionLatitud = posicionGps + tamanoGps;

        ByteBuffer tiff = ByteBuffer.allocate(posicionLatitud + 24).order(ByteOrder.LITTLE_ENDIAN);
        tiff.put((byte) 'I').put((byte) 'I').putShort((short) 42).putInt(directorio);
        tiff.putShort((short) 3);
        entrada(tiff, 0x010F, 2, marca.length, posicionMarca);       // Make
        tiff.putShort((short) 0x0112).putShort((short) 3).putInt(1)   // Orientation
                .putShort((short) orientacion).putShort((short) 0);
        entrada(tiff, 0x8825, 4, 1, posicionGps);                     // GPSInfo
        tiff.putInt(0);
        tiff.put(posicionMarca, marca);
        tiff.position(posicionGps);
        tiff.putShort((short) 2);
        tiff.putShort((short) 0x0001).putShort((short) 2).putInt(2)   // GPSLatitudeRef = "N"
                .put((byte) 'N').put((byte) 0).putShort((short) 0);
        entrada(tiff, 0x0002, 5, 3, posicionLatitud);                 // GPSLatitude
        tiff.putInt(0);
        tiff.position(posicionLatitud);
        tiff.putInt(2).putInt(1).putInt(27).putInt(1).putInt(3540).putInt(100); // 2° 27' 35.40"

        ByteArrayOutputStream bloque = new ByteArrayOutputStream();
        bloque.writeBytes("Exif\0\0".getBytes(StandardCharsets.US_ASCII));
        bloque.writeBytes(tiff.array());
        return bloque.toByteArray();
    }

    /** Segmento APP2 con un perfil de color completo en un solo trozo. */
    public static byte[] icc(byte[] perfil) {
        ByteArrayOutputStream bloque = new ByteArrayOutputStream();
        bloque.writeBytes("ICC_PROFILE\0".getBytes(StandardCharsets.US_ASCII));
        bloque.write(1);
        bloque.write(1);
        bloque.writeBytes(perfil);
        return bloque.toByteArray();
    }

    /** Inserta un segmento detrás de SOI y del segmento JFIF que escribe ImageIO. */
    public static byte[] conSegmento(byte[] jpeg, int marcador, byte[] datos) {
        int posicion = 2;
        if ((jpeg[3] & 0xFF) == 0xE0) {
            posicion += 2 + (((jpeg[4] & 0xFF) << 8) | (jpeg[5] & 0xFF));
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        salida.write(jpeg, 0, posicion);
        salida.write(0xFF);
        salida.write(marcador);
        salida.write((datos.length + 2) >> 8);
        salida.write((datos.length + 2) & 0xFF);
        salida.writeBytes(datos);
        salida.write(jpeg, posicion, jpeg.length - posicion);
        return salida.toByteArray();
    }

    /** Quita los segmentos con ese marcador; sirve para leer los píxeles sin que el lector aplique el perfil. */
    public static byte[] sinSegmentos(byte[] jpeg, int marcador) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        salida.write(jpeg, 0, 2);
        int posicion = 2;
        while ((jpeg[posicion + 1] & 0xFF) != SOS) {
            int fin = posicion + 2 + (((jpeg[posicion + 2] & 0xFF) << 8) | (jpeg[posicion + 3] & 0xFF));
            if ((jpeg[posicion + 1] & 0xFF) != marcador) {
                salida.write(jpeg, posicion, fin - posicion);
            }
            posicion = fin;
        }
        salida.write(jpeg, posicion, jpeg.length - posicion);
        return salida.toByteArray();
    }

    /** Segmentos de la cabecera, hasta el primer SOS. */
    public static List<Segment> segmentos(byte[] jpeg) {
        List<Segment> segmentos = new ArrayList<>();
        int posicion = 2;
        while ((jpeg[posicion + 1] & 0xFF) != SOS) {
            int longitud = ((jpeg[posicion + 2] & 0xFF) << 8) | (jpeg[posicion + 3] & 0xFF);
            segmentos.add(new Segment(jpeg[posicion + 1] & 0xFF,
                    Arrays.copyOfRange(jpeg, posicion + 4, posicion + 2 + longitud)));
            posicion += 2 + longitud;
        }
        return segmentos;
    }

    /** PNG válido en su cabecera, que declara esas dimensiones aunque casi no traiga datos. */
    public static byte[] pngConDimensiones(int ancho, int alto) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        salida.writeBytes(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        bloque(salida, "IHDR", ByteBuffer.allocate(13).putInt(ancho).putInt(alto)
                .put((byte) 8).put((byte) 2).put((byte) 0).put((byte) 0).put((byte) 0).array());
        Deflater compresor = new Deflater();
        compresor.setInput(new byte[16]);
        compresor.finish();
        byte[] datos = new byte[64];
        bloque(salida, "IDAT", Arrays.copyOf(datos, compresor.deflate(datos)));
        bloque(salida, "IEND", new byte[0]);
        return salida.toByteArray();
    }

    public static Download descargar(String url) {
        try {
            HttpResponse<byte[]> respuesta = HTTP.send(HttpRequest.newBuilder(URI.create(url)).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            return new Download(respuesta.statusCode(),
                    respuesta.headers().firstValue("Content-Type").orElse(null), respuesta.body());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void entrada(ByteBuffer tiff, int etiqueta, int tipo, int cantidad, int valor) {
        tiff.putShort((short) etiqueta).putShort((short) tipo).putInt(cantidad).putInt(valor);
    }

    private static void bloque(ByteArrayOutputStream salida, String tipo, byte[] datos) {
        byte[] tipoBytes = tipo.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(tipoBytes);
        crc.update(datos);
        salida.writeBytes(ByteBuffer.allocate(4).putInt(datos.length).array());
        salida.writeBytes(tipoBytes);
        salida.writeBytes(datos);
        salida.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }

    private static int canal(double valor) {
        return (int) Math.max(0, Math.min(255, Math.round(valor)));
    }
}
