package com.orquidea.api.controllers;

import com.jayway.jsonpath.JsonPath;
import com.orquidea.api.IntegrationTestBase;
import com.orquidea.api.model.Role;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OccurrenceControllerTest extends IntegrationTestBase {

    private static final String RUTA_REPORTES = "/api/reportes-avistamiento";
    private static final String RUTA_PENDIENTES = RUTA_REPORTES + "/pendientes";

    private static byte[] png;

    private String tokenUsuario;
    private String tokenAdministrador;

    @BeforeAll
    static void generarImagen() throws Exception {
        BufferedImage imagen = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        png = salida.toByteArray();
    }

    @BeforeEach
    void iniciarSesiones() throws Exception {
        tokenUsuario = tokenDeUsuarioNuevo(Role.USUARIO_REGISTRADO);
        tokenAdministrador = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);
    }

    // HU-18: crear reporte

    @Test
    @DisplayName("HU-18 escenario 1: el reporte queda PENDIENTE con los datos enviados")
    void creacionExitosa() throws Exception {
        crear(tokenUsuario, formulario(), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.occurrenceRemarks").value("Un ave azul comiendo frutos"))
                .andExpect(jsonPath("$.kingdom").value("ANIMALIA"))
                .andExpect(jsonPath("$.vernacularName").value("Azulejo"))
                .andExpect(jsonPath("$.decimalLatitude").value(2.4598))
                .andExpect(jsonPath("$.decimalLongitude").value(-76.6115))
                .andExpect(jsonPath("$.urlFoto").doesNotExist())
                .andExpect(jsonPath("$.fechaCreacion").isNotEmpty())
                .andExpect(jsonPath("$.fechaRevision").doesNotExist());
    }

    @Test
    @DisplayName("HU-18 escenario 1: la foto es opcional y, si llega, queda publicada")
    void creacionConFoto() throws Exception {
        crear(tokenUsuario, formulario(), new MockMultipartFile("foto", "ave.png", "image/png", png))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.urlFoto", endsWith(".png")));
    }

    @Test
    @DisplayName("HU-18 escenario 2: la descripción es obligatoria")
    void descripcionVacia() throws Exception {
        Map<String, String> sinDescripcion = formulario();
        sinDescripcion.remove("occurrenceRemarks");
        Map<String, String> enBlanco = formulario();
        enBlanco.put("occurrenceRemarks", "   ");

        esperarError(crear(tokenUsuario, sinDescripcion, null), 400, "La descripción es obligatoria.");
        esperarError(crear(tokenUsuario, enBlanco, null), 400, "La descripción es obligatoria.");
    }

    @Test
    @DisplayName("HU-18 escenario 3: reino y clasificación sin valor quedan como desconocidos")
    void reinoYClasificacionDesconocidos() throws Exception {
        Map<String, String> soloDescripcion = new LinkedHashMap<>();
        soloDescripcion.put("occurrenceRemarks", "Algo se movió entre los juncos");

        crear(tokenUsuario, soloDescripcion, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kingdom").value("DESCONOCIDO"))
                .andExpect(jsonPath("$.vernacularName").value("Desconocido"))
                .andExpect(jsonPath("$.decimalLatitude").doesNotExist())
                .andExpect(jsonPath("$.decimalLongitude").doesNotExist());
    }

    @Test
    @DisplayName("HU-18 escenario 3: el ciudadano también puede elegir DESCONOCIDO explícitamente")
    void reinoDesconocidoExplicito() throws Exception {
        Map<String, String> datos = formulario();
        datos.put("kingdom", "DESCONOCIDO");
        datos.put("vernacularName", "");

        crear(tokenUsuario, datos, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kingdom").value("DESCONOCIDO"))
                .andExpect(jsonPath("$.vernacularName").value("Desconocido"));
    }

    @Test
    @DisplayName("HU-18 escenario 4: el autor es el usuario autenticado, aunque el cliente intente enviar otro")
    void autorAutomatico() throws Exception {
        String correo = crearUsuario(Role.USUARIO_REGISTRADO, "Clave-Autor-1", true);
        String token = obtenerToken(correo, "Clave-Autor-1");
        Map<String, String> datos = formulario();
        datos.put("recordedBy", "Otra Persona");

        crear(token, datos, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recordedBy").value("Usuario De Prueba"));
    }

    @Test
    @DisplayName("HU-18: coordenadas fuera de rango se rechazan")
    void coordenadasFueraDeRango() throws Exception {
        Map<String, String> latitudInvalida = formulario();
        latitudInvalida.put("decimalLatitude", "91");
        Map<String, String> longitudInvalida = formulario();
        longitudInvalida.put("decimalLongitude", "-180.5");

        esperarError(crear(tokenUsuario, latitudInvalida, null), 400, "Las coordenadas ingresadas no son válidas.");
        esperarError(crear(tokenUsuario, longitudInvalida, null), 400, "Las coordenadas ingresadas no son válidas.");
    }

    @Test
    @DisplayName("HU-18: latitud y longitud van juntas; una sola se rechaza")
    void coordenadaIncompleta() throws Exception {
        Map<String, String> soloLatitud = formulario();
        soloLatitud.remove("decimalLongitude");
        Map<String, String> soloLongitud = formulario();
        soloLongitud.remove("decimalLatitude");

        esperarError(crear(tokenUsuario, soloLatitud, null), 400, "Las coordenadas ingresadas no son válidas.");
        esperarError(crear(tokenUsuario, soloLongitud, null), 400, "Las coordenadas ingresadas no son válidas.");
    }

    @Test
    @DisplayName("HU-18: un reino que no existe se rechaza con un mensaje claro")
    void reinoInvalido() throws Exception {
        Map<String, String> datos = formulario();
        datos.put("kingdom", "MINERAL");

        esperarError(crear(tokenUsuario, datos, null), 400, "El valor del campo 'kingdom' no es válido.");
    }

    @Test
    @DisplayName("HU-18: una foto con formato no permitido se rechaza")
    void fotoInvalida() throws Exception {
        MockMultipartFile falsa = new MockMultipartFile("foto", "ave.png", "image/png",
                "no es una imagen".getBytes(StandardCharsets.UTF_8));

        esperarError(crear(tokenUsuario, formulario(), falsa), 400, "El formato de la imagen no es válido.");
    }

    @Test
    @DisplayName("HU-18: reenviar el mismo id no duplica el reporte (reintentos y HU-22)")
    void reenvioConMismoIdNoDuplica() throws Exception {
        String id = UUID.randomUUID().toString();
        Map<String, String> datos = formulario();
        datos.put("id", id);

        crear(tokenUsuario, datos, null).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id));
        datos.put("occurrenceRemarks", "Texto distinto en el reintento");
        crear(tokenUsuario, datos, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.occurrenceRemarks").value("Un ave azul comiendo frutos"));
    }

    @Test
    @DisplayName("HU-18: no se puede reutilizar el id de un reporte de otro usuario")
    void idDeOtroUsuario() throws Exception {
        String id = UUID.randomUUID().toString();
        Map<String, String> datos = formulario();
        datos.put("id", id);
        crear(tokenUsuario, datos, null).andExpect(status().isCreated());

        esperarError(crear(tokenDeUsuarioNuevo(Role.USUARIO_REGISTRADO), datos, null), 409,
                "Ya existe un reporte con ese identificador.");
    }

    @Test
    @DisplayName("HU-18: sin sesión no se pueden crear reportes")
    void creacionSinSesion() throws Exception {
        esperarError(crear(null, formulario(), null), 401, "Debe iniciar sesión para acceder a este recurso.");
    }

    @Test
    @DisplayName("HU-18 (ajuste): un administrador también crea reportes, que quedan PENDIENTES para revisión")
    void creacionComoAdministrador() throws Exception {
        crear(tokenAdministrador, formulario(), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("HU-18 (ajuste): el superadministrador también crea reportes (HU-6)")
    void creacionComoSuperadministrador() throws Exception {
        String token = obtenerToken(CORREO_SUPERADMIN, CONTRASENA_SUPERADMIN);

        crear(token, formulario(), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    // HU-19: revisión

    @Test
    @DisplayName("HU-19 escenario 3: el reporte nuevo aparece en la cola marcado como pendiente")
    void colaDeRevision() throws Exception {
        String id = crearReporte();

        String cola = mockMvc.perform(conToken(get(RUTA_PENDIENTES).param("tamano", "50"), tokenAdministrador))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(campoDe(cola, id, "estado")).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("HU-19 escenario 1: al aprobar, el reporte sale de la cola y se publica en el foro")
    void aprobar() throws Exception {
        String id = crearReporte();

        mockMvc.perform(conToken(post(RUTA_REPORTES + "/" + id + "/aprobar"), tokenAdministrador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"))
                .andExpect(jsonPath("$.fechaRevision").isNotEmpty());

        esperarEnPendientes(id, false);
        esperarEnForo(id, true);
    }

    @Test
    @DisplayName("HU-19 escenario 2: al rechazar, el reporte sale de la cola y no se publica")
    void rechazar() throws Exception {
        String id = crearReporte();

        mockMvc.perform(conToken(post(RUTA_REPORTES + "/" + id + "/rechazar"), tokenAdministrador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADO"));

        esperarEnPendientes(id, false);
        esperarEnForo(id, false);
    }

    @Test
    @DisplayName("HU-19: un reporte ya revisado no se puede volver a revisar")
    void reporteYaRevisado() throws Exception {
        String id = crearReporte();
        mockMvc.perform(conToken(post(RUTA_REPORTES + "/" + id + "/rechazar"), tokenAdministrador))
                .andExpect(status().isOk());

        esperarError(mockMvc.perform(conToken(post(RUTA_REPORTES + "/" + id + "/aprobar"), tokenAdministrador)),
                409, "El reporte ya fue revisado.");
        esperarError(mockMvc.perform(conToken(post(RUTA_REPORTES + "/" + id + "/rechazar"), tokenAdministrador)),
                409, "El reporte ya fue revisado.");
    }

    @Test
    @DisplayName("HU-19: revisar un reporte que no existe responde 404")
    void reporteInexistente() throws Exception {
        esperarError(mockMvc.perform(conToken(post(RUTA_REPORTES + "/" + UUID.randomUUID() + "/aprobar"), tokenAdministrador)),
                404, "El reporte solicitado no existe.");
    }

    @Test
    @DisplayName("HU-19: el superadministrador también revisa reportes (HU-6)")
    void superadministradorRevisa() throws Exception {
        String id = crearReporte();
        String token = obtenerToken(CORREO_SUPERADMIN, CONTRASENA_SUPERADMIN);

        mockMvc.perform(conToken(post(RUTA_REPORTES + "/" + id + "/aprobar"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));
    }

    @Test
    @DisplayName("HU-19: solo administradores ven la cola y revisan")
    void permisosDeRevision() throws Exception {
        String id = crearReporte();

        esperarError(mockMvc.perform(get(RUTA_PENDIENTES)), 401, "Debe iniciar sesión para acceder a este recurso.");
        esperarError(mockMvc.perform(conToken(get(RUTA_PENDIENTES), tokenUsuario)), 403,
                "No tiene permisos para realizar esta acción.");
        esperarError(mockMvc.perform(conToken(post(RUTA_REPORTES + "/" + id + "/aprobar"), tokenUsuario)), 403,
                "No tiene permisos para realizar esta acción.");
        esperarError(mockMvc.perform(post(RUTA_REPORTES + "/" + id + "/rechazar")), 401,
                "Debe iniciar sesión para acceder a este recurso.");
    }

    // HU-20: foro

    @Test
    @DisplayName("HU-20 escenario 1: el foro es público y muestra los aprobados con sus datos y autor")
    void foroPublico() throws Exception {
        String aprobado = crearReporte();
        String pendiente = crearReporte();
        mockMvc.perform(conToken(post(RUTA_REPORTES + "/" + aprobado + "/aprobar"), tokenAdministrador));

        String foro = mockMvc.perform(get(RUTA_REPORTES).param("tamano", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[*].id", not(hasItem(pendiente))))
                .andReturn().getResponse().getContentAsString();

        assertThat(campoDe(foro, aprobado, "recordedBy")).isEqualTo("Usuario De Prueba");
        assertThat(campoDe(foro, aprobado, "kingdom")).isEqualTo("ANIMALIA");
        assertThat(campoDe(foro, aprobado, "occurrenceRemarks")).isEqualTo("Un ave azul comiendo frutos");
        assertThat(campoDe(foro, aprobado, "decimalLatitude")).isEqualTo(2.4598);
        assertThat(campoDe(foro, aprobado, "decimalLongitude")).isEqualTo(-76.6115);
    }

    @Test
    @DisplayName("HU-20: el foro muestra primero lo más recién publicado")
    void foroOrdenadoPorPublicacion() throws Exception {
        String primero = crearReporte();
        String segundo = crearReporte();
        mockMvc.perform(conToken(post(RUTA_REPORTES + "/" + segundo + "/aprobar"), tokenAdministrador));
        mockMvc.perform(conToken(post(RUTA_REPORTES + "/" + primero + "/aprobar"), tokenAdministrador));

        mockMvc.perform(get(RUTA_REPORTES).param("tamano", "2"))
                .andExpect(jsonPath("$.contenido[0].id").value(primero))
                .andExpect(jsonPath("$.contenido[1].id").value(segundo));
    }

    @Test
    @DisplayName("HU-20: el foro se pagina y el tamaño de página tiene un máximo")
    void foroPaginado() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(conToken(post(RUTA_REPORTES + "/" + crearReporte() + "/aprobar"), tokenAdministrador));
        }

        String respuesta = mockMvc.perform(get(RUTA_REPORTES).param("pagina", "0").param("tamano", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(1))
                .andReturn().getResponse().getContentAsString();
        int total = JsonPath.read(respuesta, "$.totalElementos");
        int paginas = JsonPath.read(respuesta, "$.totalPaginas");
        assertThat(paginas).isEqualTo(total).isGreaterThanOrEqualTo(2);

        mockMvc.perform(get(RUTA_REPORTES).param("tamano", "500"))
                .andExpect(jsonPath("$.tamano").value(50));
    }

    // Utilidades

    private ResultActions crear(String token, Map<String, String> datos, MockMultipartFile foto) throws Exception {
        MockMultipartHttpServletRequestBuilder solicitud = multipart(RUTA_REPORTES);
        if (foto != null) {
            solicitud.file(foto);
        }
        datos.forEach(solicitud::param);
        return mockMvc.perform(conToken(solicitud, token));
    }

    /** Crea un reporte válido como usuario registrado y devuelve su id. */
    private String crearReporte() throws Exception {
        String respuesta = crear(tokenUsuario, formulario(), null)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
    }

    private void esperarEnPendientes(String id, boolean presente) throws Exception {
        mockMvc.perform(conToken(get(RUTA_PENDIENTES).param("tamano", "50"), tokenAdministrador))
                .andExpect(jsonPath("$.contenido[*].id", presente ? hasItem(id) : not(hasItem(id))));
    }

    private void esperarEnForo(String id, boolean presente) throws Exception {
        mockMvc.perform(get(RUTA_REPORTES).param("tamano", "50"))
                .andExpect(jsonPath("$.contenido[*].id", presente ? hasItem(id) : not(hasItem(id))));
    }

    private static <T extends AbstractMockHttpServletRequestBuilder<?>> T conToken(T solicitud, String token) {
        if (token != null) {
            solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return solicitud;
    }

    /** Valor de un campo del reporte con ese id dentro de una página; falla si el reporte no está. */
    private static Object campoDe(String pagina, String id, String campo) {
        List<Object> valores = JsonPath.read(pagina, "$.contenido[?(@.id == '" + id + "')]." + campo);
        assertThat(valores).as("el reporte %s debe estar en la página", id).hasSize(1);
        return valores.get(0);
    }

    private static void esperarError(ResultActions resultado, int estado, String mensaje) throws Exception {
        resultado.andExpect(status().is(estado))
                .andExpect(jsonPath("$.estado").value(estado))
                .andExpect(jsonPath("$.mensaje").value(mensaje));
    }

    /** Formulario válido y mutable, para que cada prueba quite o cambie lo que necesite. */
    private static Map<String, String> formulario() {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("occurrenceRemarks", "Un ave azul comiendo frutos");
        datos.put("kingdom", "ANIMALIA");
        datos.put("vernacularName", "Azulejo");
        datos.put("decimalLatitude", "2.459800");
        datos.put("decimalLongitude", "-76.611500");
        return datos;
    }
}
