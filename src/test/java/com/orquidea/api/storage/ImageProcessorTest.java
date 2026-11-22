package com.orquidea.api.storage;

import com.jayway.jsonpath.JsonPath;
import com.orquidea.api.IntegrationTestBase;
import com.orquidea.api.model.Role;
import com.orquidea.api.storage.TestImages.Download;
import com.orquidea.api.storage.TestImages.Segment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.orquidea.api.storage.TestImages.APP1;
import static com.orquidea.api.storage.TestImages.APP2;
import static com.orquidea.api.storage.TestImages.SOF_PROGRESIVO;
import static com.orquidea.api.storage.TestImages.conSegmento;
import static com.orquidea.api.storage.TestImages.cuadrantes;
import static com.orquidea.api.storage.TestImages.descargar;
import static com.orquidea.api.storage.TestImages.exif;
import static com.orquidea.api.storage.TestImages.fotografia;
import static com.orquidea.api.storage.TestImages.icc;
import static com.orquidea.api.storage.TestImages.jpeg;
import static com.orquidea.api.storage.TestImages.leer;
import static com.orquidea.api.storage.TestImages.png;
import static com.orquidea.api.storage.TestImages.segmentos;
import static com.orquidea.api.storage.TestImages.sinSegmentos;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Optimización de las fotos, de punta a punta: se suben por la API (aquí, como foto de un reporte de avistamiento,
 * que admite cualquier usuario registrado) y se leen del almacenamiento por su URL pública.
 */
class ImageProcessorTest extends IntegrationTestBase {

    private static final String RUTA_REPORTES = "/api/reportes-avistamiento";
    private static final String TELEFONO = "TelefonoDePrueba";
    private static final int ROJO = 0xFF0000;
    private static final int VERDE = 0x00FF00;
    private static final int AZUL = 0x0000FF;
    private static final int AMARILLO = 0xFFFF00;
    private static final Map<Character, Integer> COLORES = Map.of('R', ROJO, 'G', VERDE, 'B', AZUL, 'Y', AMARILLO);

    private String token;

    /** Foto y miniatura guardadas, leídas desde sus URLs públicas. */
    private record Stored(String url, String urlMiniatura, Download foto, Download miniatura) {
    }

    @BeforeEach
    void iniciarSesion() throws Exception {
        token = tokenDeUsuarioNuevo(Role.USUARIO_REGISTRADO);
    }

    @Test
    @DisplayName("Una foto grande se guarda a 1600 px de ancho, mucho más liviana, y con una miniatura de 480 px")
    void reduceYRecomprime() throws Exception {
        byte[] original = jpeg(fotografia(3200, 2400), 0.98f);

        Stored guardada = subir(original, "foto.jpg", "image/jpeg");

        assertThat(guardada.foto().tipoContenido()).isEqualTo("image/jpeg");
        BufferedImage foto = guardada.foto().imagen();
        assertThat(foto.getWidth()).isEqualTo(1600);
        assertThat(foto.getHeight()).isEqualTo(1200);
        assertThat(guardada.foto().contenido().length).isLessThan(original.length / 4);

        assertThat(guardada.urlMiniatura()).contains("/reportes/miniaturas/").isNotEqualTo(guardada.url());
        assertThat(guardada.miniatura().tipoContenido()).isEqualTo("image/jpeg");
        BufferedImage miniatura = guardada.miniatura().imagen();
        assertThat(miniatura.getWidth()).isEqualTo(480);
        assertThat(miniatura.getHeight()).isEqualTo(360);
    }

    @Test
    @DisplayName("Quita el EXIF (marca del teléfono y GPS) y codifica en JPEG progresivo con color completo 4:4:4")
    void quitaMetadatosYUsaColorCompleto() throws Exception {
        byte[] original = conSegmento(jpeg(fotografia(2400, 1600), 0.98f), APP1, exif(1, TELEFONO));

        Stored guardada = subir(original, "foto.jpg", "image/jpeg");

        for (byte[] archivo : List.of(guardada.foto().contenido(), guardada.miniatura().contenido())) {
            List<Segment> segmentos = segmentos(archivo);
            assertThat(segmentos).noneMatch(segmento -> segmento.marcador() == APP1);
            assertThat(new String(archivo, StandardCharsets.ISO_8859_1)).doesNotContain(TELEFONO);

            Segment sof = segmentos.stream().filter(Segment::esSof).findFirst().orElseThrow();
            assertThat(sof.marcador()).as("JPEG progresivo").isEqualTo(SOF_PROGRESIVO);
            int componentes = sof.datos()[5];
            assertThat(componentes).isEqualTo(3);
            for (int i = 0; i < componentes; i++) {
                assertThat(sof.datos()[7 + i * 3]).as("muestreo del componente %d", i).isEqualTo((byte) 0x11);
            }
        }
    }

    /** Cuadrantes de origen: R G arriba, B Y abajo. El esperado lista arriba-izq, arriba-der, abajo-izq, abajo-der. */
    @ParameterizedTest(name = "orientación {0}")
    @CsvSource({
            "1, RGBY, 400, 200",
            "2, GRYB, 400, 200",
            "3, YBGR, 400, 200",
            "4, BYRG, 400, 200",
            "5, RBGY, 200, 400",
            "6, BRYG, 200, 400",
            "7, YGBR, 200, 400",
            "8, GYRB, 200, 400"})
    @DisplayName("Endereza la foto según la orientación EXIF, y la guarda sin ella para que no se gire dos veces")
    void corrigeOrientacion(int orientacion, String esperado, int ancho, int alto) throws Exception {
        byte[] original = conSegmento(jpeg(cuadrantes(400, 200, ROJO, VERDE, AZUL, AMARILLO), 0.95f), APP1,
                exif(orientacion, TELEFONO));

        Stored guardada = subir(original, "foto.jpg", "image/jpeg");

        assertThat(segmentos(guardada.foto().contenido())).noneMatch(segmento -> segmento.marcador() == APP1);
        BufferedImage foto = guardada.foto().imagen();
        assertThat(foto.getWidth()).isEqualTo(ancho);
        assertThat(foto.getHeight()).isEqualTo(alto);
        int[][] centros = {{ancho / 4, alto / 4}, {ancho * 3 / 4, alto / 4}, {ancho / 4, alto * 3 / 4},
                {ancho * 3 / 4, alto * 3 / 4}};
        for (int i = 0; i < 4; i++) {
            assertColor(foto.getRGB(centros[i][0], centros[i][1]), COLORES.get(esperado.charAt(i)));
        }
    }

    @Test
    @DisplayName("Un PNG con transparencia sigue siendo PNG y conserva el alfa, también en la miniatura")
    void pngTransparenteConservaElAlfa() throws Exception {
        BufferedImage imagen = new BufferedImage(3000, 1000, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 1000; y++) {
            for (int x = 0; x < 3000; x++) {
                // El rojo invisible no debe mancharse hacia el verde semitransparente al reducir
                imagen.setRGB(x, y, x < 1000 ? 0x00FF0000 : x < 2000 ? 0x8000FF00 : 0xFF0000FF);
            }
        }

        Stored guardada = subir(png(imagen), "logo.png", "image/png");

        assertThat(guardada.url()).endsWith(".png");
        assertThat(guardada.urlMiniatura()).endsWith(".png");
        assertThat(guardada.foto().tipoContenido()).isEqualTo("image/png");
        assertThat(guardada.miniatura().tipoContenido()).isEqualTo("image/png");
        for (BufferedImage resultado : List.of(guardada.foto().imagen(), guardada.miniatura().imagen())) {
            int ancho = resultado.getWidth();
            int medio = resultado.getHeight() / 2;
            assertThat(resultado.getColorModel().hasAlpha()).isTrue();
            assertThat(resultado.getRGB(ancho / 6, medio) >>> 24).as("transparente").isZero();
            int semitransparente = resultado.getRGB(ancho / 2, medio);
            assertThat(semitransparente >>> 24).as("semitransparente").isCloseTo(0x80, within(2));
            assertColor(semitransparente, VERDE);
            assertThat(resultado.getRGB(ancho * 5 / 6, medio)).as("opaco").isEqualTo(0xFF0000FF);
        }
        assertThat(guardada.foto().imagen().getWidth()).isEqualTo(1600);
        assertThat(guardada.miniatura().imagen().getWidth()).isEqualTo(480);
    }

    @Test
    @DisplayName("Un PNG sin transparencia se guarda como JPEG, que pesa mucho menos")
    void pngOpacoPasaAJpeg() throws Exception {
        byte[] original = png(fotografia(1800, 1350));

        Stored guardada = subir(original, "captura.png", "image/png");

        assertThat(guardada.url()).endsWith(".jpg");
        assertThat(guardada.urlMiniatura()).endsWith(".jpg");
        assertThat(guardada.foto().tipoContenido()).isEqualTo("image/jpeg");
        assertThat(guardada.foto().imagen().getWidth()).isEqualTo(1600);
        assertThat(guardada.foto().contenido().length).isLessThan(original.length / 4);
    }

    @Test
    @DisplayName("Una foto más angosta que el máximo no se agranda")
    void noAgranda() throws Exception {
        Stored guardada = subir(jpeg(fotografia(800, 600), 0.98f), "foto.jpg", "image/jpeg");

        BufferedImage foto = guardada.foto().imagen();
        assertThat(foto.getWidth()).isEqualTo(800);
        assertThat(foto.getHeight()).isEqualTo(600);
        assertThat(guardada.miniatura().imagen().getWidth()).isEqualTo(480);
    }

    @Test
    @DisplayName("Una foto que ya venía optimizada se guarda sin recomprimir: solo pierde los metadatos")
    void conservaLaOptimizadaSinRecomprimir() throws Exception {
        byte[] optimizada = jpeg(fotografia(1200, 900), 0.75f);

        Stored guardada = subir(conSegmento(optimizada, APP1, exif(1, TELEFONO)), "foto.jpg", "image/jpeg");

        assertThat(guardada.foto().contenido()).isEqualTo(optimizada);
    }

    @Test
    @DisplayName("Conserva el perfil de color y los valores de los píxeles, sin convertirlos")
    void conservaElPerfilDeColor() throws Exception {
        byte[] perfil = ICC_Profile.getInstance(ColorSpace.CS_LINEAR_RGB).getData();
        BufferedImage gris = new BufferedImage(2000, 1000, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 1000; y++) {
            for (int x = 0; x < 2000; x++) {
                gris.setRGB(x, y, 0x808080);
            }
        }

        Stored guardada = subir(conSegmento(jpeg(gris, 0.98f), APP2, icc(perfil)), "foto.jpg", "image/jpeg");

        for (byte[] archivo : List.of(guardada.foto().contenido(), guardada.miniatura().contenido())) {
            ByteArrayOutputStream incrustado = new ByteArrayOutputStream();
            segmentos(archivo).stream()
                    .filter(segmento -> segmento.marcador() == APP2 && segmento.empiezaCon("ICC_PROFILE\0"))
                    .forEach(segmento -> incrustado.write(segmento.datos(), 14, segmento.datos().length - 14));
            assertThat(incrustado.toByteArray()).isEqualTo(perfil);
            // Leído sin el perfil: si se hubiera convertido de lineal a sRGB, el 128 pasaría a unos 188
            int pixel = leer(sinSegmentos(archivo, APP2)).getRGB(100, 100) & 0xFF;
            assertThat(pixel).isCloseTo(0x80, within(3));
        }
    }

    @Test
    @DisplayName("Un archivo dañado, aunque empiece como JPEG, se rechaza como formato no válido")
    void imagenDanada() throws Exception {
        byte[] danada = Arrays.copyOf(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, 300);
        for (int i = 4; i < danada.length; i++) {
            danada[i] = (byte) (i * 37);
        }

        crear(new MockMultipartFile("foto", "foto.jpg", "image/jpeg", danada))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El formato de la imagen no es válido."));
    }

    @Test
    @DisplayName("Una imagen de resolución enorme se rechaza antes de decodificarla")
    void resolucionExcesiva() throws Exception {
        crear(new MockMultipartFile("foto", "enorme.png", "image/png", TestImages.pngConDimensiones(20_000, 20_000)))
                .andExpect(status().is(413))
                .andExpect(jsonPath("$.mensaje")
                        .value("La imagen supera la resolución máxima permitida de 50 megapíxeles."));
    }

    private Stored subir(byte[] contenido, String nombre, String tipo) throws Exception {
        String respuesta = crear(new MockMultipartFile("foto", nombre, tipo, contenido))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String url = JsonPath.read(respuesta, "$.urlFoto");
        String urlMiniatura = JsonPath.read(respuesta, "$.urlMiniatura");
        Download foto = descargar(url);
        Download miniatura = descargar(urlMiniatura);
        assertThat(foto.estado()).isEqualTo(200);
        assertThat(miniatura.estado()).isEqualTo(200);
        return new Stored(url, urlMiniatura, foto, miniatura);
    }

    private ResultActions crear(MockMultipartFile foto) throws Exception {
        MockMultipartHttpServletRequestBuilder solicitud = multipart(RUTA_REPORTES);
        solicitud.file(foto);
        solicitud.param("occurrenceRemarks", "Foto de prueba");
        solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return mockMvc.perform(solicitud);
    }

    /** JPEG cambia un poco los colores incluso en zonas lisas. */
    private static void assertColor(int argb, int esperado) {
        for (int desplazamiento : new int[]{16, 8, 0}) {
            assertThat((argb >> desplazamiento) & 0xFF)
                    .as("canal en %06X (esperado %06X)", argb & 0xFFFFFF, esperado)
                    .isCloseTo((esperado >> desplazamiento) & 0xFF, within(24));
        }
    }
}
