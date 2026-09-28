package com.orquidea.api.controllers;

import com.jayway.jsonpath.JsonPath;
import com.orquidea.api.PruebaIntegracionBase;
import com.orquidea.api.model.Rol;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FichaTaxonomicaControladorTest extends PruebaIntegracionBase {

    private static final String RUTA_FICHAS = "/api/fichas-taxonomicas";
    private static final String MENSAJE_OBLIGATORIOS = "Debes completar todos los campos obligatorios.";
    private static final String MENSAJE_FORMATO = "El formato de la imagen no es válido.";

    private static byte[] png;
    private static byte[] jpeg;

    private String tokenAdministrador;

    @BeforeAll
    static void generarImagenes() throws Exception {
        png = imagen("png");
        jpeg = imagen("jpg");
    }

    @BeforeEach
    void iniciarSesionComoAdministrador() throws Exception {
        tokenAdministrador = tokenDeUsuarioNuevo(Rol.ADMINISTRADOR);
    }

    // Escenario 1: creación exitosa

    @Test
    @DisplayName("Escenario 1: el administrador crea la ficha y queda publicada con su foto")
    void creacionExitosa() throws Exception {
        String nombreCientifico = nombreCientificoUnico();

        String respuesta = crear(tokenAdministrador, formulario(nombreCientifico), fotoPng())
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("http://localhost" + RUTA_FICHAS + "/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.categoria").value("AVE"))
                .andExpect(jsonPath("$.orden").value("Passeriformes"))
                .andExpect(jsonPath("$.familia").value("Thraupidae"))
                .andExpect(jsonPath("$.genero").value("Thraupis"))
                .andExpect(jsonPath("$.nombreCientifico").value(nombreCientifico))
                .andExpect(jsonPath("$.nombreComun").value("Azulejo"))
                .andExpect(jsonPath("$.alimentacion").value("Frutos e insectos"))
                .andExpect(jsonPath("$.rolEnHumedal").value("Dispersor de semillas"))
                .andExpect(jsonPath("$.estadoConservacion").value("LC"))
                .andExpect(jsonPath("$.urlFoto", endsWith(".png")))
                .andExpect(jsonPath("$.fechaCreacion").isNotEmpty())
                .andExpect(jsonPath("$.fotoClave").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        // Publicada: cualquiera la consulta sin sesión
        String id = JsonPath.read(respuesta, "$.id");
        mockMvc.perform(get(RUTA_FICHAS + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCientifico").value(nombreCientifico));
        mockMvc.perform(get(RUTA_FICHAS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombreCientifico", hasItem(nombreCientifico)));

        // La foto quedó en el almacenamiento y se lee desde su URL pública
        String urlFoto = JsonPath.read(respuesta, "$.urlFoto");
        HttpResponse<byte[]> foto = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(urlFoto)).build(), HttpResponse.BodyHandlers.ofByteArray());
        assertThat(foto.statusCode()).isEqualTo(200);
        assertThat(foto.headers().firstValue("Content-Type")).hasValue("image/png");
        assertThat(foto.body()).isEqualTo(png);
    }

    @Test
    @DisplayName("Escenario 1: el superadministrador también puede crear fichas (HU-6), con foto jpg")
    void creacionExitosaComoSuperadministrador() throws Exception {
        String token = obtenerToken(CORREO_SUPERADMIN, CONTRASENA_SUPERADMIN);

        crear(token, formulario(nombreCientificoUnico()), new MockMultipartFile("foto", "azulejo.jpg", "image/jpeg", jpeg))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.urlFoto", endsWith(".jpg")));
    }

    // Escenario 2: campos obligatorios incompletos

    @ParameterizedTest
    @ValueSource(strings = {"categoria", "orden", "familia", "genero", "nombreCientifico", "nombreComun",
            "alimentacion", "rolEnHumedal", "estadoConservacion"})
    @DisplayName("Escenario 2: falta un campo obligatorio")
    void campoObligatorioFaltante(String campo) throws Exception {
        Map<String, String> datos = formulario(nombreCientificoUnico());
        datos.remove(campo);

        esperarError(crear(tokenAdministrador, datos, fotoPng()), 400, MENSAJE_OBLIGATORIOS);
    }

    @Test
    @DisplayName("Escenario 2: un campo con solo espacios cuenta como vacío")
    void campoObligatorioEnBlanco() throws Exception {
        Map<String, String> datos = formulario(nombreCientificoUnico());
        datos.put("nombreComun", "   ");

        esperarError(crear(tokenAdministrador, datos, fotoPng()), 400, MENSAJE_OBLIGATORIOS);
    }

    @Test
    @DisplayName("Escenario 2: la foto es obligatoria")
    void sinFoto() throws Exception {
        esperarError(crear(tokenAdministrador, formulario(nombreCientificoUnico()), null), 400, MENSAJE_OBLIGATORIOS);
    }

    @Test
    @DisplayName("Escenario 2: un archivo vacío cuenta como foto faltante")
    void fotoVacia() throws Exception {
        MockMultipartFile vacia = new MockMultipartFile("foto", "vacia.png", "image/png", new byte[0]);

        esperarError(crear(tokenAdministrador, formulario(nombreCientificoUnico()), vacia), 400, MENSAJE_OBLIGATORIOS);
    }

    // Escenario 3: formato de imagen no soportado

    @Test
    @DisplayName("Escenario 3: un archivo que no es imagen, aunque diga ser png")
    void archivoDisfrazadoDePng() throws Exception {
        MockMultipartFile falsa = new MockMultipartFile("foto", "foto.png", "image/png",
                "esto no es una imagen".getBytes(StandardCharsets.UTF_8));

        esperarError(crear(tokenAdministrador, formulario(nombreCientificoUnico()), falsa), 400, MENSAJE_FORMATO);
    }

    @Test
    @DisplayName("Escenario 3: una imagen real en formato no permitido (gif)")
    void imagenGif() throws Exception {
        MockMultipartFile gif = new MockMultipartFile("foto", "foto.gif", "image/gif", imagen("gif"));

        esperarError(crear(tokenAdministrador, formulario(nombreCientificoUnico()), gif), 400, MENSAJE_FORMATO);
    }

    // Reglas adicionales

    @Test
    @DisplayName("La foto no puede superar 10 MB")
    void imagenDemasiadoGrande() throws Exception {
        byte[] grande = Arrays.copyOf(png, 10 * 1024 * 1024 + 1);
        MockMultipartFile foto = new MockMultipartFile("foto", "grande.png", "image/png", grande);

        esperarError(crear(tokenAdministrador, formulario(nombreCientificoUnico()), foto), 413,
                "La imagen supera el tamaño máximo permitido de 10 MB.");
    }

    @Test
    @DisplayName("No se permiten dos fichas con el mismo nombre científico, sin importar mayúsculas")
    void nombreCientificoDuplicado() throws Exception {
        String nombreCientifico = nombreCientificoUnico();
        crear(tokenAdministrador, formulario(nombreCientifico), fotoPng()).andExpect(status().isCreated());

        String repetido = "  " + nombreCientifico.toUpperCase() + " ";
        crear(tokenAdministrador, formulario(repetido), fotoPng())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        "Ya existe una ficha con el nombre científico '" + nombreCientifico.toUpperCase() + "'."));
    }

    @Test
    @DisplayName("Una categoría que no existe se rechaza con un mensaje claro")
    void categoriaInvalida() throws Exception {
        Map<String, String> datos = formulario(nombreCientificoUnico());
        datos.put("categoria", "PEZ");

        esperarError(crear(tokenAdministrador, datos, fotoPng()), 400, "El valor del campo 'categoria' no es válido.");
    }

    @Test
    @DisplayName("Un usuario registrado no puede crear fichas")
    void usuarioRegistradoNoPuedeCrear() throws Exception {
        String token = tokenDeUsuarioNuevo(Rol.USUARIO_REGISTRADO);

        esperarError(crear(token, formulario(nombreCientificoUnico()), fotoPng()), 403,
                "No tiene permisos para realizar esta acción.");
    }

    @Test
    @DisplayName("Sin sesión no se pueden crear fichas")
    void sinSesionNoPuedeCrear() throws Exception {
        esperarError(crear(null, formulario(nombreCientificoUnico()), fotoPng()), 401,
                "Debe iniciar sesión para acceder a este recurso.");
    }

    @Test
    @DisplayName("Consultar una ficha que no existe responde 404")
    void fichaInexistente() throws Exception {
        esperarError(mockMvc.perform(get(RUTA_FICHAS + "/" + UUID.randomUUID())), 404,
                "La ficha taxonómica solicitada no existe.");
        esperarError(mockMvc.perform(get(RUTA_FICHAS + "/no-es-un-uuid")), 404, "El recurso solicitado no existe.");
    }

    // Utilidades

    private ResultActions crear(String token, Map<String, String> datos, MockMultipartFile foto) throws Exception {
        MockMultipartHttpServletRequestBuilder solicitud = multipart(RUTA_FICHAS);
        if (foto != null) {
            solicitud.file(foto);
        }
        datos.forEach(solicitud::param);
        if (token != null) {
            solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(solicitud);
    }

    private static void esperarError(ResultActions resultado, int estado, String mensaje) throws Exception {
        resultado.andExpect(status().is(estado))
                .andExpect(jsonPath("$.estado").value(estado))
                .andExpect(jsonPath("$.mensaje").value(mensaje));
    }

    /** Formulario válido y mutable, para que cada prueba quite o cambie lo que necesite. */
    private static Map<String, String> formulario(String nombreCientifico) {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("categoria", "AVE");
        datos.put("orden", "Passeriformes");
        datos.put("familia", "Thraupidae");
        datos.put("genero", "Thraupis");
        datos.put("nombreCientifico", nombreCientifico);
        datos.put("nombreComun", "Azulejo");
        datos.put("alimentacion", "Frutos e insectos");
        datos.put("rolEnHumedal", "Dispersor de semillas");
        datos.put("estadoConservacion", "LC");
        return datos;
    }

    private static MockMultipartFile fotoPng() {
        return new MockMultipartFile("foto", "azulejo.png", "image/png", png);
    }

    private static String nombreCientificoUnico() {
        return "Thraupis prueba-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static byte[] imagen(String formato) throws Exception {
        BufferedImage imagen = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        if (!ImageIO.write(imagen, formato, salida)) {
            throw new IllegalStateException("ImageIO no soporta " + formato);
        }
        return salida.toByteArray();
    }
}
