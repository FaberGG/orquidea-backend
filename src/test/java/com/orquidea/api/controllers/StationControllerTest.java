package com.orquidea.api.controllers;

import com.jayway.jsonpath.JsonPath;
import com.orquidea.api.IntegrationTestBase;
import com.orquidea.api.model.ConservationStatus;
import com.orquidea.api.model.Role;
import com.orquidea.api.model.Taxon;
import com.orquidea.api.model.TaxonCategory;
import com.orquidea.api.repository.TaxonRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StationControllerTest extends IntegrationTestBase {

    private static final String RUTA_ESTACIONES = "/api/estaciones";
    private static final String MENSAJE_OBLIGATORIOS = "Debes completar todos los campos obligatorios.";
    private static final String MENSAJE_COORDENADAS = "Las coordenadas ingresadas no son válidas.";
    private static final String MENSAJE_NO_EXISTE = "La estación no existe.";

    private static byte[] png;
    private static byte[] jpeg;

    @Autowired
    private TaxonRepository taxonRepository;

    private String tokenAdministrador;

    @BeforeAll
    static void generarImagenes() throws Exception {
        png = imagen("png");
        jpeg = imagen("jpg");
    }

    @BeforeEach
    void iniciarSesionComoAdministrador() throws Exception {
        tokenAdministrador = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);
    }

    // CA1 y CA4: creación con fichas asociadas

    @Test
    @DisplayName("CA1 y CA4: la estación se crea con sus fichas, por nombre común, y aparece en el mapa sin sesión")
    void creacionExitosa() throws Exception {
        Taxon garza = crearFicha("Garza real");
        Taxon azulejo = crearFicha("Azulejo");
        Map<String, String> datos = formulario("Mirador de la laguna");
        datos.put("fichaIds", garza.getId() + "," + azulejo.getId());

        String respuesta = crear(tokenAdministrador, datos, fotoPng())
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("http://localhost" + RUTA_ESTACIONES + "/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.nombre").value("Mirador de la laguna"))
                .andExpect(jsonPath("$.descripcion").value("Desde aquí se observan garzas"))
                .andExpect(jsonPath("$.urlFoto", endsWith(".png")))
                .andExpect(jsonPath("$.decimalLatitude").value(2.4598))
                .andExpect(jsonPath("$.decimalLongitude").value(-76.6115))
                .andExpect(jsonPath("$.fichas.length()").value(2))
                .andExpect(jsonPath("$.fichas[0].id").value(azulejo.getId().toString()))
                .andExpect(jsonPath("$.fichas[0].vernacularName").value("Azulejo"))
                .andExpect(jsonPath("$.fichas[0].urlFoto", endsWith(".png")))
                .andExpect(jsonPath("$.fichas[1].vernacularName").value("Garza real"))
                .andExpect(jsonPath("$.fotoClave").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String id = JsonPath.read(respuesta, "$.id");
        mockMvc.perform(get(RUTA_ESTACIONES + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fichas.length()").value(2));
        mockMvc.perform(get(RUTA_ESTACIONES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(id)));
        assertThat(estadoHttp(JsonPath.read(respuesta, "$.urlFoto"))).isEqualTo(200);
    }

    @Test
    @DisplayName("CA4: fichaIds también se acepta repetido, y los ids repetidos cuentan una vez")
    void fichaIdsRepetido() throws Exception {
        Taxon garza = crearFicha("Garza real");
        Taxon azulejo = crearFicha("Azulejo");
        MockMultipartHttpServletRequestBuilder solicitud = multipart(RUTA_ESTACIONES);
        solicitud.param("fichaIds", garza.getId().toString(), azulejo.getId().toString(), garza.getId().toString());

        enviar(solicitud, tokenAdministrador, formulario("Sendero norte"), fotoPng())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fichas.length()").value(2));
    }

    @Test
    @DisplayName("CA1: sin fichaIds la estación queda sin fichas, con la lista vacía")
    void creacionSinFichas() throws Exception {
        crear(tokenAdministrador, formulario("Entrada"), fotoPng())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fichas").isArray())
                .andExpect(jsonPath("$.fichas.length()").value(0));
    }

    @Test
    @DisplayName("CA1: el superadministrador también puede crear estaciones, con foto jpg")
    void creacionComoSuperadministrador() throws Exception {
        String token = obtenerToken(CORREO_SUPERADMIN, CONTRASENA_SUPERADMIN);

        crear(token, formulario("Puente"), new MockMultipartFile("foto", "puente.jpg", "image/jpeg", jpeg))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.urlFoto", endsWith(".jpg")));
    }

    @Test
    @DisplayName("Las coordenadas se guardan con 6 decimales y la respuesta coincide con la consulta posterior")
    void coordenadasRedondeadas() throws Exception {
        Map<String, String> datos = formulario("Mirador");
        datos.put("decimalLatitude", "2.45981234");

        String creada = crear(tokenAdministrador, datos, fotoPng())
                .andExpect(jsonPath("$.decimalLatitude").value(2.459812))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get(RUTA_ESTACIONES + "/" + JsonPath.read(creada, "$.id")))
                .andExpect(jsonPath("$.decimalLatitude").value(2.459812));
    }

    // Campos obligatorios y CA3: coordenadas inválidas

    @ParameterizedTest
    @ValueSource(strings = {"nombre", "descripcion", "decimalLatitude", "decimalLongitude"})
    @DisplayName("Falta un campo obligatorio")
    void campoObligatorioFaltante(String campo) throws Exception {
        Map<String, String> datos = formulario("Estación incompleta");
        datos.remove(campo);

        esperarError(crear(tokenAdministrador, datos, fotoPng()), 400, MENSAJE_OBLIGATORIOS);
    }

    @Test
    @DisplayName("La foto es obligatoria al crear")
    void sinFoto() throws Exception {
        esperarError(crear(tokenAdministrador, formulario("Sin foto"), null), 400, MENSAJE_OBLIGATORIOS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"decimalLatitude=91", "decimalLatitude=-90.5", "decimalLongitude=180.1",
            "decimalLongitude=-181", "decimalLatitude=abc", "decimalLongitude=2,5"})
    @DisplayName("CA3: coordenadas fuera de rango o no numéricas")
    void coordenadasInvalidas(String campoYValor) throws Exception {
        String[] partes = campoYValor.split("=");
        Map<String, String> datos = formulario("Fuera del mapa");
        datos.put(partes[0], partes[1]);

        esperarError(crear(tokenAdministrador, datos, fotoPng()), 400, MENSAJE_COORDENADAS);
    }

    @Test
    @DisplayName("Una foto con formato no permitido se rechaza")
    void imagenGif() throws Exception {
        MockMultipartFile gif = new MockMultipartFile("foto", "foto.gif", "image/gif", imagen("gif"));

        esperarError(crear(tokenAdministrador, formulario("Con gif"), gif), 400, "El formato de la imagen no es válido.");
    }

    @Test
    @DisplayName("Una ficha asociada que no existe se rechaza")
    void fichaInexistente() throws Exception {
        Map<String, String> datos = formulario("Con ficha fantasma");
        datos.put("fichaIds", crearFicha("Garza real").getId() + "," + UUID.randomUUID());

        esperarError(crear(tokenAdministrador, datos, fotoPng()), 400, "Alguna de las fichas asociadas no existe.");
    }

    @Test
    @DisplayName("El nombre no puede superar 150 caracteres")
    void nombreDemasiadoLargo() throws Exception {
        esperarError(crear(tokenAdministrador, formulario("a".repeat(151)), fotoPng()), 400,
                "El nombre no puede superar 150 caracteres.");
    }

    // CA2: edición

    @Test
    @DisplayName("CA2: la edición sin foto conserva la actual y fichaIds reemplaza las asociaciones")
    void edicionExitosa() throws Exception {
        Taxon garza = crearFicha("Garza real");
        Taxon azulejo = crearFicha("Azulejo");
        Map<String, String> datos = formulario("Mirador");
        datos.put("fichaIds", garza.getId().toString());
        String creada = crearEstacion(datos);
        String id = JsonPath.read(creada, "$.id");

        Map<String, String> cambios = formulario("Mirador renovado");
        cambios.put("descripcion", "Nueva descripción");
        cambios.put("decimalLongitude", "-76.6");
        cambios.put("fichaIds", azulejo.getId().toString());
        editar(tokenAdministrador, id, cambios, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombre").value("Mirador renovado"))
                .andExpect(jsonPath("$.descripcion").value("Nueva descripción"))
                .andExpect(jsonPath("$.decimalLongitude").value(-76.6))
                .andExpect(jsonPath("$.urlFoto").value((String) JsonPath.read(creada, "$.urlFoto")))
                .andExpect(jsonPath("$.fichas.length()").value(1))
                .andExpect(jsonPath("$.fichas[0].vernacularName").value("Azulejo"));
    }

    @Test
    @DisplayName("CA2: si fichaIds no llega al editar, la estación queda sin fichas")
    void edicionSinFichas() throws Exception {
        Map<String, String> datos = formulario("Mirador");
        datos.put("fichaIds", crearFicha("Garza real").getId().toString());
        String id = JsonPath.read(crearEstacion(datos), "$.id");

        editar(tokenAdministrador, id, formulario("Mirador"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fichas.length()").value(0));
    }

    @Test
    @DisplayName("CA2: una foto nueva reemplaza a la anterior, que se borra del almacenamiento")
    void edicionConFotoNueva() throws Exception {
        String creada = crearEstacion(formulario("Mirador"));
        String urlAnterior = JsonPath.read(creada, "$.urlFoto");

        String editada = editar(tokenAdministrador, JsonPath.read(creada, "$.id"), formulario("Mirador"),
                new MockMultipartFile("foto", "nueva.jpg", "image/jpeg", jpeg))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlFoto", endsWith(".jpg")))
                .andReturn().getResponse().getContentAsString();

        assertThat(estadoHttp(JsonPath.read(editada, "$.urlFoto"))).isEqualTo(200);
        assertThat(estadoHttp(urlAnterior)).isEqualTo(404);
    }

    @Test
    @DisplayName("CA2: un campo de archivo vacío (así lo envía el navegador) conserva la foto")
    void edicionConCampoDeFotoVacio() throws Exception {
        String creada = crearEstacion(formulario("Mirador"));
        MockMultipartFile sinSeleccionar = new MockMultipartFile("foto", "", "application/octet-stream", new byte[0]);

        editar(tokenAdministrador, JsonPath.read(creada, "$.id"), formulario("Mirador"), sinSeleccionar)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlFoto").value((String) JsonPath.read(creada, "$.urlFoto")));
    }

    @Test
    @DisplayName("CA3 al editar: coordenadas inválidas no modifican la estación")
    void edicionConCoordenadasInvalidas() throws Exception {
        String id = JsonPath.read(crearEstacion(formulario("Mirador")), "$.id");
        Map<String, String> datos = formulario("Nombre que no debe guardarse");
        datos.put("decimalLatitude", "100");

        esperarError(editar(tokenAdministrador, id, datos, null), 400, MENSAJE_COORDENADAS);
        mockMvc.perform(get(RUTA_ESTACIONES + "/" + id))
                .andExpect(jsonPath("$.nombre").value("Mirador"))
                .andExpect(jsonPath("$.decimalLatitude").value(2.4598));
    }

    @Test
    @DisplayName("Editar o consultar una estación que no existe responde 404")
    void estacionInexistente() throws Exception {
        String id = UUID.randomUUID().toString();

        esperarError(editar(tokenAdministrador, id, formulario("Mirador"), null), 404, MENSAJE_NO_EXISTE);
        esperarError(mockMvc.perform(get(RUTA_ESTACIONES + "/" + id)), 404, MENSAJE_NO_EXISTE);
    }

    // CA5: eliminación

    @Test
    @DisplayName("CA5: la estación eliminada desaparece del mapa, su foto se borra y las fichas no cambian")
    void eliminacion() throws Exception {
        Taxon garza = crearFicha("Garza real");
        Map<String, String> datos = formulario("Mirador");
        datos.put("fichaIds", garza.getId().toString());
        String creada = crearEstacion(datos);
        String id = JsonPath.read(creada, "$.id");

        eliminar(tokenAdministrador, id).andExpect(status().isNoContent());

        esperarError(mockMvc.perform(get(RUTA_ESTACIONES + "/" + id)), 404, MENSAJE_NO_EXISTE);
        mockMvc.perform(get(RUTA_ESTACIONES))
                .andExpect(jsonPath("$[?(@.id == '" + id + "')]").isEmpty());
        assertThat(estadoHttp(JsonPath.read(creada, "$.urlFoto"))).isEqualTo(404);
        assertThat(taxonRepository.existsById(garza.getId())).isTrue();
    }

    @Test
    @DisplayName("CA5: eliminar una estación que no existe responde 404")
    void eliminarInexistente() throws Exception {
        esperarError(eliminar(tokenAdministrador, UUID.randomUUID().toString()), 404, MENSAJE_NO_EXISTE);
    }

    // Permisos

    @Test
    @DisplayName("Un usuario registrado no puede crear, editar ni eliminar estaciones")
    void usuarioRegistradoSinPermisos() throws Exception {
        String id = JsonPath.read(crearEstacion(formulario("Mirador")), "$.id");
        String token = tokenDeUsuarioNuevo(Role.USUARIO_REGISTRADO);
        String mensaje = "No tiene permisos para realizar esta acción.";

        esperarError(crear(token, formulario("Otra"), fotoPng()), 403, mensaje);
        esperarError(editar(token, id, formulario("Otra"), null), 403, mensaje);
        esperarError(eliminar(token, id), 403, mensaje);
    }

    @Test
    @DisplayName("Sin sesión no se pueden crear, editar ni eliminar estaciones")
    void sinSesion() throws Exception {
        String id = JsonPath.read(crearEstacion(formulario("Mirador")), "$.id");
        String mensaje = "Debe iniciar sesión para acceder a este recurso.";

        esperarError(crear(null, formulario("Otra"), fotoPng()), 401, mensaje);
        esperarError(editar(null, id, formulario("Otra"), null), 401, mensaje);
        esperarError(eliminar(null, id), 401, mensaje);
    }

    // Utilidades

    private ResultActions crear(String token, Map<String, String> datos, MockMultipartFile foto) throws Exception {
        return enviar(multipart(RUTA_ESTACIONES), token, datos, foto);
    }

    private ResultActions editar(String token, String id, Map<String, String> datos, MockMultipartFile foto) throws Exception {
        return enviar(multipart(HttpMethod.PUT, RUTA_ESTACIONES + "/" + id), token, datos, foto);
    }

    private ResultActions eliminar(String token, String id) throws Exception {
        return mockMvc.perform(conToken(delete(RUTA_ESTACIONES + "/" + id), token));
    }

    private ResultActions enviar(MockMultipartHttpServletRequestBuilder solicitud, String token,
                                 Map<String, String> datos, MockMultipartFile foto) throws Exception {
        if (foto != null) {
            solicitud.file(foto);
        }
        datos.forEach(solicitud::param);
        return mockMvc.perform(conToken(solicitud, token));
    }

    private static <B extends AbstractMockHttpServletRequestBuilder<B>> B conToken(B solicitud, String token) {
        if (token != null) {
            solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return solicitud;
    }

    /** Crea una estación válida con foto png como administrador y devuelve la respuesta JSON. */
    private String crearEstacion(Map<String, String> datos) throws Exception {
        return crear(tokenAdministrador, datos, fotoPng())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    /** Las fichas se crean directo en la base: aquí solo importa poder asociarlas. */
    private Taxon crearFicha(String vernacularName) {
        return taxonRepository.save(Taxon.builder()
                .categoria(TaxonCategory.AVE)
                .order("Pelecaniformes")
                .family("Ardeidae")
                .genus("Ardea")
                .scientificName("Ardea prueba-" + UUID.randomUUID().toString().substring(0, 8))
                .vernacularName(vernacularName)
                .alimentacion("Peces")
                .rolEnHumedal("Depredador")
                .estadoConservacion(ConservationStatus.LC)
                .fotoClave("fichas/" + UUID.randomUUID() + ".png")
                .build());
    }

    /** Formulario válido y mutable, para que cada prueba quite o cambie lo que necesite. */
    private static Map<String, String> formulario(String nombre) {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("nombre", nombre);
        datos.put("descripcion", "Desde aquí se observan garzas");
        datos.put("decimalLatitude", "2.4598");
        datos.put("decimalLongitude", "-76.6115");
        return datos;
    }

    private static MockMultipartFile fotoPng() {
        return new MockMultipartFile("foto", "mirador.png", "image/png", png);
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

    private static byte[] imagen(String formato) throws Exception {
        BufferedImage imagen = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        if (!ImageIO.write(imagen, formato, salida)) {
            throw new IllegalStateException("ImageIO no soporta " + formato);
        }
        return salida.toByteArray();
    }
}
