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
import org.springframework.http.HttpMethod;
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

    // HU-8: edición

    @Test
    @DisplayName("HU-8 escenario 1: edición exitosa sin enviar foto conserva la foto actual")
    void edicionExitosaSinFoto() throws Exception {
        String nombreCientifico = nombreCientificoUnico();
        String creada = crearFicha(nombreCientifico);
        String id = JsonPath.read(creada, "$.id");
        Map<String, String> datos = formulario(nombreCientifico);
        datos.put("nombreComun", "Azulejo común");
        datos.put("alimentacion", "Principalmente frutos");
        datos.put("estadoConservacion", "NT");

        String editada = editar(tokenAdministrador, id, datos, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombreComun").value("Azulejo común"))
                .andExpect(jsonPath("$.alimentacion").value("Principalmente frutos"))
                .andExpect(jsonPath("$.estadoConservacion").value("NT"))
                .andExpect(jsonPath("$.urlFoto").value((String) JsonPath.read(creada, "$.urlFoto")))
                .andExpect(jsonPath("$.fechaCreacion").value((String) JsonPath.read(creada, "$.fechaCreacion")))
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(editada, "$.fechaActualizacion"))
                .isNotEqualTo(JsonPath.read(creada, "$.fechaActualizacion"));

        mockMvc.perform(get(RUTA_FICHAS + "/" + id))
                .andExpect(jsonPath("$.nombreComun").value("Azulejo común"));
    }

    @Test
    @DisplayName("HU-8 escenario 1: un campo de archivo vacío (así lo envía el navegador) conserva la foto")
    void edicionConCampoDeFotoVacio() throws Exception {
        String nombreCientifico = nombreCientificoUnico();
        String creada = crearFicha(nombreCientifico);
        MockMultipartFile sinSeleccionar = new MockMultipartFile("foto", "", "application/octet-stream", new byte[0]);

        editar(tokenAdministrador, JsonPath.read(creada, "$.id"), formulario(nombreCientifico), sinSeleccionar)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlFoto").value((String) JsonPath.read(creada, "$.urlFoto")));
    }

    @Test
    @DisplayName("HU-8 escenario 1: una foto nueva reemplaza a la anterior, que se borra del almacenamiento")
    void edicionConFotoNueva() throws Exception {
        String nombreCientifico = nombreCientificoUnico();
        String creada = crearFicha(nombreCientifico);
        String urlAnterior = JsonPath.read(creada, "$.urlFoto");
        MockMultipartFile fotoJpg = new MockMultipartFile("foto", "nueva.jpg", "image/jpeg", jpeg);

        String editada = editar(tokenAdministrador, JsonPath.read(creada, "$.id"), formulario(nombreCientifico), fotoJpg)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlFoto", endsWith(".jpg")))
                .andReturn().getResponse().getContentAsString();

        assertThat(estadoHttp(JsonPath.read(editada, "$.urlFoto"))).isEqualTo(200);
        assertThat(estadoHttp(urlAnterior)).isEqualTo(404);
    }

    @Test
    @DisplayName("HU-8: el superadministrador también puede editar (HU-6)")
    void edicionComoSuperadministrador() throws Exception {
        String nombreCientifico = nombreCientificoUnico();
        String id = JsonPath.read(crearFicha(nombreCientifico), "$.id");
        String token = obtenerToken(CORREO_SUPERADMIN, CONTRASENA_SUPERADMIN);

        editar(token, id, formulario(nombreCientifico), null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("HU-8: un campo obligatorio vacío no modifica la ficha")
    void edicionConCampoFaltante() throws Exception {
        String nombreCientifico = nombreCientificoUnico();
        String id = JsonPath.read(crearFicha(nombreCientifico), "$.id");
        Map<String, String> datos = formulario(nombreCientifico);
        datos.put("nombreComun", "Nombre que no debe guardarse");
        datos.remove("familia");

        esperarError(editar(tokenAdministrador, id, datos, null), 400, MENSAJE_OBLIGATORIOS);
        mockMvc.perform(get(RUTA_FICHAS + "/" + id))
                .andExpect(jsonPath("$.nombreComun").value("Azulejo"))
                .andExpect(jsonPath("$.familia").value("Thraupidae"));
    }

    @Test
    @DisplayName("HU-8: una foto con formato no permitido no modifica la ficha ni su foto")
    void edicionConFotoInvalida() throws Exception {
        String nombreCientifico = nombreCientificoUnico();
        String creada = crearFicha(nombreCientifico);
        String id = JsonPath.read(creada, "$.id");
        MockMultipartFile gif = new MockMultipartFile("foto", "foto.gif", "image/gif", imagen("gif"));

        esperarError(editar(tokenAdministrador, id, formulario(nombreCientifico), gif), 400, MENSAJE_FORMATO);
        mockMvc.perform(get(RUTA_FICHAS + "/" + id))
                .andExpect(jsonPath("$.urlFoto").value((String) JsonPath.read(creada, "$.urlFoto")));
    }

    @Test
    @DisplayName("HU-8: no se puede usar el nombre científico de otra ficha")
    void edicionConNombreDeOtraFicha() throws Exception {
        String nombreOtra = nombreCientificoUnico();
        crearFicha(nombreOtra);
        String nombreCientifico = nombreCientificoUnico();
        String id = JsonPath.read(crearFicha(nombreCientifico), "$.id");

        esperarError(editar(tokenAdministrador, id, formulario(nombreOtra), null), 409,
                "Ya existe una ficha con el nombre científico '" + nombreOtra + "'.");
    }

    @Test
    @DisplayName("HU-8: la ficha puede conservar su propio nombre científico, incluso cambiando mayúsculas")
    void edicionConservandoSuNombre() throws Exception {
        String nombreCientifico = nombreCientificoUnico();
        String id = JsonPath.read(crearFicha(nombreCientifico), "$.id");

        editar(tokenAdministrador, id, formulario(nombreCientifico.toUpperCase()), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCientifico").value(nombreCientifico.toUpperCase()));
    }

    @Test
    @DisplayName("HU-8: editar una ficha que no existe responde 404")
    void edicionDeFichaInexistente() throws Exception {
        esperarError(editar(tokenAdministrador, UUID.randomUUID().toString(), formulario(nombreCientificoUnico()), null),
                404, "La ficha taxonómica solicitada no existe.");
    }

    @Test
    @DisplayName("HU-8: solo administradores pueden editar")
    void edicionSinPermisos() throws Exception {
        String nombreCientifico = nombreCientificoUnico();
        String id = JsonPath.read(crearFicha(nombreCientifico), "$.id");

        esperarError(editar(tokenDeUsuarioNuevo(Rol.USUARIO_REGISTRADO), id, formulario(nombreCientifico), null), 403,
                "No tiene permisos para realizar esta acción.");
        esperarError(editar(null, id, formulario(nombreCientifico), null), 401,
                "Debe iniciar sesión para acceder a este recurso.");
    }

    // Utilidades

    private ResultActions crear(String token, Map<String, String> datos, MockMultipartFile foto) throws Exception {
        return enviar(multipart(RUTA_FICHAS), token, datos, foto);
    }

    private ResultActions editar(String token, String id, Map<String, String> datos, MockMultipartFile foto) throws Exception {
        return enviar(multipart(HttpMethod.PUT, RUTA_FICHAS + "/" + id), token, datos, foto);
    }

    private ResultActions enviar(MockMultipartHttpServletRequestBuilder solicitud, String token,
                                 Map<String, String> datos, MockMultipartFile foto) throws Exception {
        if (foto != null) {
            solicitud.file(foto);
        }
        datos.forEach(solicitud::param);
        if (token != null) {
            solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(solicitud);
    }

    /** Crea una ficha válida con foto png como administrador y devuelve la respuesta JSON. */
    private String crearFicha(String nombreCientifico) throws Exception {
        return crear(tokenAdministrador, formulario(nombreCientifico), fotoPng())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private static int estadoHttp(String url) throws Exception {
        return HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.discarding())
                .statusCode();
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
