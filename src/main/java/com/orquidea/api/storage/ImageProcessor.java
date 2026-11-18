package com.orquidea.api.storage;

import com.orquidea.api.config.StorageProperties;
import com.orquidea.api.exception.ImageResolutionTooLargeException;
import com.orquidea.api.exception.InvalidImageFormatException;
import com.orquidea.api.storage.ImageValidator.ValidatedImage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

/**
 * Optimiza las fotos antes de guardarlas, buscando la mayor fidelidad que permite un tamaño razonable:
 * <ul>
 *   <li>endereza la foto según su orientación EXIF;</li>
 *   <li>la reduce al ancho máximo, por pasos de la mitad con interpolación bicúbica para no crear moiré en
 *       plumas y follaje; nunca la agranda;</li>
 *   <li>la codifica en JPEG con color completo (4:4:4), tablas Huffman optimizadas y modo progresivo, o en PNG si
 *       tiene transparencia, para no romperla;</li>
 *   <li>quita los metadatos (EXIF con el GPS, XMP, IPTC, comentarios) y conserva el perfil de color;</li>
 *   <li>si no hacía falta reducirla ni enderezarla y recomprimirla no ahorra lo suficiente, guarda la original
 *       sin metadatos, sin perder calidad;</li>
 *   <li>genera una miniatura para los listados.</li>
 * </ul>
 * Ver docs/api/procesamiento-de-fotos.md.
 */
@Slf4j
@Component
public class ImageProcessor {

    private static final String FORMATO_METADATOS_JPEG = "javax_imageio_jpeg_image_1.0";
    /** Recomprimir una foto que no hace falta reducir ni enderezar solo compensa si ahorra al menos esto. */
    private static final double AHORRO_MINIMO = 0.10;

    private final StorageProperties.Imagen configuracion;
    private final Semaphore turnos;

    public ImageProcessor(StorageProperties propiedades) {
        this.configuracion = propiedades.imagen();
        this.turnos = new Semaphore(configuracion.procesamientosSimultaneos(), true);
        // Registra el lector JPEG de TwelveMonkeys aunque ImageIO se haya iniciado antes con otro cargador de clases
        ImageIO.scanForPlugins();
        // Todo ocurre en memoria; sin esto ImageIO crea archivos temporales para cada imagen
        ImageIO.setUseCache(false);
    }

    /**
     * Decodificar una foto de 12 MP ocupa unos 50 MB de memoria; los turnos limitan cuántas se procesan a la vez.
     *
     * @throws InvalidImageFormatException       si la imagen no se puede leer
     * @throws ImageResolutionTooLargeException si supera los megapíxeles configurados
     */
    public ProcessedImage procesar(ValidatedImage original) {
        turnos.acquireUninterruptibly();
        try {
            return optimizar(original.contenido(), original.tipo());
        } finally {
            turnos.release();
        }
    }

    /** Foto ya derecha y con su tamaño final; la decodificada a resolución completa ya se puede liberar. */
    private record Prepared(BufferedImage imagen, boolean transparente, boolean cambiada) {
    }

    private ProcessedImage optimizar(byte[] contenido, ImageType tipo) {
        StrippedImage limpia = tipo == ImageType.JPEG ? JpegSegments.limpiar(contenido) : PngChunks.limpiar(contenido);
        Prepared preparada = preparar(limpia);
        BufferedImage principal = preparada.imagen();

        ImageType tipoFinal = preparada.transparente() ? ImageType.PNG : ImageType.JPEG;
        byte[] perfil = limpia.perfilIcc();
        byte[] optimizada = codificar(principal, tipoFinal, perfil);
        if (!preparada.cambiada() && limpia.conservable()
                && optimizada.length > limpia.sinMetadatos().length * (1 - AHORRO_MINIMO)) {
            optimizada = limpia.sinMetadatos();
            tipoFinal = tipo;
        }

        int anchoMiniatura = configuracion.anchoMiniatura();
        byte[] miniatura = principal.getWidth() > anchoMiniatura
                ? codificar(redimensionar(principal, anchoMiniatura,
                        proporcional(principal.getHeight(), anchoMiniatura, principal.getWidth()),
                        preparada.transparente()), tipoFinal, perfil)
                : optimizada;
        return new ProcessedImage(optimizada, tipoFinal, miniatura);
    }

    /** El ancho máximo se mide con la foto derecha; se reduce antes de girarla, que es más barato. */
    private Prepared preparar(StrippedImage limpia) {
        BufferedImage decodificada = decodificar(limpia.paraDecodificar());
        boolean transparente = tieneTransparencia(decodificada);
        int orientacion = limpia.orientacion();
        boolean intercambiaEjes = orientacion >= 5;
        int ancho = intercambiaEjes ? decodificada.getHeight() : decodificada.getWidth();
        int alto = intercambiaEjes ? decodificada.getWidth() : decodificada.getHeight();
        boolean reducir = ancho > configuracion.anchoMaximo();
        int anchoFinal = reducir ? configuracion.anchoMaximo() : ancho;
        int altoFinal = reducir ? proporcional(alto, anchoFinal, ancho) : alto;
        BufferedImage reducida = redimensionar(decodificada,
                intercambiaEjes ? altoFinal : anchoFinal, intercambiaEjes ? anchoFinal : altoFinal, transparente);
        return new Prepared(orientar(reducida, orientacion), transparente,
                reducir || orientacion != ExifOrientation.DERECHA);
    }

    /** Lee primero las dimensiones de la cabecera, para rechazar sin decodificarla una imagen enorme. */
    private BufferedImage decodificar(byte[] contenido) {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(contenido))) {
            Iterator<ImageReader> lectores = ImageIO.getImageReaders(entrada);
            if (!lectores.hasNext()) {
                throw new InvalidImageFormatException();
            }
            ImageReader lector = lectores.next();
            try {
                lector.setInput(entrada, true, true);
                long pixeles = (long) lector.getWidth(0) * lector.getHeight(0);
                if (pixeles > configuracion.megapixelesMaximos() * 1_000_000L) {
                    throw new ImageResolutionTooLargeException(configuracion.megapixelesMaximos());
                }
                return lector.read(0);
            } finally {
                lector.dispose();
            }
        } catch (ImageResolutionTooLargeException | InvalidImageFormatException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            log.debug("No se pudo decodificar la imagen", e);
            throw new InvalidImageFormatException();
        }
    }

    /** Una imagen con canal alfa pero sin ningún píxel transparente se trata como opaca. */
    private static boolean tieneTransparencia(BufferedImage imagen) {
        if (!imagen.getColorModel().hasAlpha()) {
            return false;
        }
        int ancho = imagen.getWidth();
        int[] fila = new int[ancho];
        for (int y = 0; y < imagen.getHeight(); y++) {
            imagen.getRGB(0, y, ancho, 1, fila, 0, ancho);
            for (int argb : fila) {
                if (argb >>> 24 != 0xFF) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int proporcional(int lado, int nuevo, int referencia) {
        return Math.max(1, (int) Math.round((double) lado * nuevo / referencia));
    }

    /**
     * Reduce por pasos de máximo la mitad: un solo salto grande con bicúbica deja dientes de sierra y moiré. Si el
     * tamaño no cambia, solo copia a un formato de trabajo conocido. Con transparencia trabaja con alfa
     * premultiplicado, para que el color de los píxeles invisibles no manche los bordes.
     */
    private static BufferedImage redimensionar(BufferedImage origen, int anchoDestino, int altoDestino,
                                               boolean transparente) {
        int tipo = transparente ? BufferedImage.TYPE_INT_ARGB_PRE : BufferedImage.TYPE_INT_RGB;
        BufferedImage actual = origen;
        int ancho = origen.getWidth();
        int alto = origen.getHeight();
        do {
            ancho = Math.max(anchoDestino, ancho / 2);
            alto = Math.max(altoDestino, alto / 2);
            BufferedImage siguiente = new BufferedImage(ancho, alto, tipo);
            Graphics2D lienzo = siguiente.createGraphics();
            try {
                lienzo.setComposite(AlphaComposite.Src);
                lienzo.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                lienzo.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                lienzo.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
                lienzo.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                        RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                lienzo.drawImage(actual, 0, 0, ancho, alto, null);
            } finally {
                lienzo.dispose();
            }
            actual = siguiente;
        } while (ancho != anchoDestino || alto != altoDestino);
        return actual;
    }

    /**
     * Aplica la orientación EXIF moviendo píxeles, sin interpolar. Recibe una imagen de redimensionar(), que
     * siempre guarda un entero por píxel.
     */
    private static BufferedImage orientar(BufferedImage imagen, int orientacion) {
        if (orientacion == ExifOrientation.DERECHA) {
            return imagen;
        }
        int ancho = imagen.getWidth();
        int alto = imagen.getHeight();
        boolean intercambiaEjes = orientacion >= 5;
        BufferedImage derecha = new BufferedImage(intercambiaEjes ? alto : ancho, intercambiaEjes ? ancho : alto,
                imagen.getType());
        int[] origen = ((DataBufferInt) imagen.getRaster().getDataBuffer()).getData();
        int[] destino = ((DataBufferInt) derecha.getRaster().getDataBuffer()).getData();
        int anchoDestino = derecha.getWidth();
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int indice = switch (orientacion) {
                    case 2 -> y * anchoDestino + (ancho - 1 - x);                // espejo horizontal
                    case 3 -> (alto - 1 - y) * anchoDestino + (ancho - 1 - x);   // 180°
                    case 4 -> (alto - 1 - y) * anchoDestino + x;                 // espejo vertical
                    case 5 -> x * anchoDestino + y;                              // transpuesta
                    case 6 -> x * anchoDestino + (alto - 1 - y);                 // 90° horario
                    case 7 -> (ancho - 1 - x) * anchoDestino + (alto - 1 - y);   // transversa
                    default -> (ancho - 1 - x) * anchoDestino + y;               // 8: 90° antihorario
                };
                destino[indice] = origen[y * ancho + x];
            }
        }
        return derecha;
    }

    private byte[] codificar(BufferedImage imagen, ImageType tipo, byte[] perfil) {
        if (tipo == ImageType.JPEG) {
            byte[] jpeg = codificarJpeg(imagen);
            return perfil == null ? jpeg : JpegSegments.insertarIcc(jpeg, perfil);
        }
        byte[] png = codificarPng(sinPremultiplicar(imagen));
        return perfil == null ? png : PngChunks.insertarIcc(png, perfil);
    }

    /**
     * Color completo (4:4:4): con el submuestreo habitual (4:2:0) el color se guarda a la mitad de resolución y
     * se corre en los bordes de colores saturados, como un ave azul sobre el follaje. Las tablas Huffman
     * optimizadas y el modo progresivo reducen el archivo sin cambiar ni un píxel.
     */
    private byte[] codificarJpeg(BufferedImage imagen) {
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        try {
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(configuracion.calidadJpeg());
            parametros.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
            if (parametros instanceof JPEGImageWriteParam parametrosJpeg) {
                parametrosJpeg.setOptimizeHuffmanTables(true);
            }
            IIOMetadata metadatos = escritor.getDefaultImageMetadata(
                    ImageTypeSpecifier.createFromRenderedImage(imagen), parametros);
            Element raiz = (Element) metadatos.getAsTree(FORMATO_METADATOS_JPEG);
            NodeList componentes = raiz.getElementsByTagName("componentSpec");
            for (int i = 0; i < componentes.getLength(); i++) {
                Element componente = (Element) componentes.item(i);
                componente.setAttribute("HsamplingFactor", "1");
                componente.setAttribute("VsamplingFactor", "1");
            }
            metadatos.setFromTree(FORMATO_METADATOS_JPEG, raiz);
            return escribir(escritor, new IIOImage(imagen, null, metadatos), parametros);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo codificar la imagen en JPEG", e);
        } finally {
            escritor.dispose();
        }
    }

    /** PNG no tiene pérdida: solo se elige la compresión más alta. */
    private static byte[] codificarPng(BufferedImage imagen) {
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("png").next();
        try {
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            if (parametros.canWriteCompressed()) {
                parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parametros.setCompressionQuality(0f);
            }
            return escribir(escritor, new IIOImage(imagen, null, null), parametros);
        } finally {
            escritor.dispose();
        }
    }

    private static byte[] escribir(ImageWriter escritor, IIOImage imagen, ImageWriteParam parametros) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ImageOutputStream flujo = ImageIO.createImageOutputStream(salida)) {
            escritor.setOutput(flujo);
            escritor.write(null, imagen, parametros);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo codificar la imagen", e);
        }
        return salida.toByteArray();
    }

    /** El codificador PNG guarda alfa sin premultiplicar. */
    private static BufferedImage sinPremultiplicar(BufferedImage imagen) {
        if (imagen.getType() != BufferedImage.TYPE_INT_ARGB_PRE) {
            return imagen;
        }
        BufferedImage convertida = new BufferedImage(imagen.getWidth(), imagen.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D lienzo = convertida.createGraphics();
        try {
            lienzo.setComposite(AlphaComposite.Src);
            lienzo.drawImage(imagen, 0, 0, null);
        } finally {
            lienzo.dispose();
        }
        return convertida;
    }
}
