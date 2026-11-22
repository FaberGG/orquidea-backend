package com.orquidea.api.controllers;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.jayway.jsonpath.JsonPath;
import com.orquidea.api.IntegrationTestBase;
import com.orquidea.api.model.Role;
import com.orquidea.api.model.User;
import com.orquidea.api.model.TaxonCategory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.not;

class TaxonControllerTest extends IntegrationTestBase {

    private static final String RUTA_FICHAS = "/api/fichas-taxonomicas";
    private static final String RUTA_SINCRONIZACION = RUTA_FICHAS + "/sincronizacion";
    private static final String MENSAJE_OBLIGATORIOS = "Debes completar todos los campos obligatorios.";
    private static final String MENSAJE_FORMATO = "El formato de la imagen no es válido.";
    private static final String MENSAJE_SIN_FOTO = "Debes seleccionar una imagen.";

    private static byte[] png;
    private static byte[] jpeg;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    // Escenario 1: creación exitosa

    @Test
    @DisplayName("Escenario 1: el administrador crea la ficha y queda publicada con su foto")
    void creacionExitosa() throws Exception {
        String scientificName = nombreCientificoUnico();

        String respuesta = crear(tokenAdministrador, formulario(scientificName), fotoPng())
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("http://localhost" + RUTA_FICHAS + "/")))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.categoria").value("AVE"))
                .andExpect(jsonPath("$.order").value("Passeriformes"))
                .andExpect(jsonPath("$.family").value("Thraupidae"))
                .andExpect(jsonPath("$.genus").value("Thraupis"))
                .andExpect(jsonPath("$.scientificName").value(scientificName))
                .andExpect(jsonPath("$.vernacularName").value("Azulejo"))
                .andExpect(jsonPath("$.alimentacion").value("Frutos e insectos"))
                .andExpect(jsonPath("$.rolEnHumedal").value("Dispersor de semillas"))
                .andExpect(jsonPath("$.estadoConservacion").value("LC"))
                .andExpect(jsonPath("$.urlFoto", endsWith(".png")))
                .andExpect(jsonPath("$.urlMiniatura", containsString("/fichas/miniaturas/")))
                .andExpect(jsonPath("$.fotos.length()").value(1))
                .andExpect(jsonPath("$.fotos[0].id").isNotEmpty())
                .andExpect(jsonPath("$.fotos[0].urlMiniatura", containsString("/fichas/miniaturas/")))
                .andExpect(jsonPath("$.fechaCreacion").isNotEmpty())
                .andExpect(jsonPath("$.fotoClave").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        // Publicada: cualquiera la consulta sin sesión
        String id = JsonPath.read(respuesta, "$.id");
        mockMvc.perform(get(RUTA_FICHAS + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scientificName").value(scientificName));
        assertThat(listadoCompleto(null)).extracting(ficha -> ficha.get("scientificName")).contains(scientificName);

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

        crear(token, formulario(nombreCientificoUnico()), new MockMultipartFile("fotos", "azulejo.jpg", "image/jpeg", jpeg))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.urlFoto", endsWith(".jpg")));
    }

    // Escenario 2: campos obligatorios incompletos

    @ParameterizedTest
    @ValueSource(strings = {"categoria", "order", "family", "genus", "scientificName", "vernacularName",
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
        datos.put("vernacularName", "   ");

        esperarError(crear(tokenAdministrador, datos, fotoPng()), 400, MENSAJE_OBLIGATORIOS);
    }

    @Test
    @DisplayName("Escenario 2: la foto es obligatoria")
    void sinFoto() throws Exception {
        esperarError(crear(tokenAdministrador, formulario(nombreCientificoUnico()), null), 400, MENSAJE_SIN_FOTO);
    }

    @Test
    @DisplayName("Escenario 2: un archivo vacío cuenta como foto faltante")
    void fotoVacia() throws Exception {
        MockMultipartFile vacia = new MockMultipartFile("fotos", "vacia.png", "image/png", new byte[0]);

        esperarError(crear(tokenAdministrador, formulario(nombreCientificoUnico()), vacia), 400, MENSAJE_SIN_FOTO);
    }

    // Escenario 3: formato de imagen no soportado

    @Test
    @DisplayName("Escenario 3: un archivo que no es imagen, aunque diga ser png")
    void archivoDisfrazadoDePng() throws Exception {
        MockMultipartFile falsa = new MockMultipartFile("fotos", "foto.png", "image/png",
                "esto no es una imagen".getBytes(StandardCharsets.UTF_8));

        esperarError(crear(tokenAdministrador, formulario(nombreCientificoUnico()), falsa), 400, MENSAJE_FORMATO);
    }

    @Test
    @DisplayName("Escenario 3: una imagen real en formato no permitido (gif)")
    void imagenGif() throws Exception {
        MockMultipartFile gif = new MockMultipartFile("fotos", "foto.gif", "image/gif", imagen("gif"));

        esperarError(crear(tokenAdministrador, formulario(nombreCientificoUnico()), gif), 400, MENSAJE_FORMATO);
    }

    // Reglas adicionales

    @Test
    @DisplayName("La foto no puede superar 10 MB")
    void imagenDemasiadoGrande() throws Exception {
        byte[] grande = Arrays.copyOf(png, 10 * 1024 * 1024 + 1);
        MockMultipartFile foto = new MockMultipartFile("fotos", "grande.png", "image/png", grande);

        esperarError(crear(tokenAdministrador, formulario(nombreCientificoUnico()), foto), 413,
                "La imagen supera el tamaño máximo permitido de 10 MB.");
    }

    @Test
    @DisplayName("No se permiten dos fichas con el mismo nombre científico, sin importar mayúsculas")
    void nombreCientificoDuplicado() throws Exception {
        String scientificName = nombreCientificoUnico();
        crear(tokenAdministrador, formulario(scientificName), fotoPng()).andExpect(status().isCreated());

        String repetido = "  " + scientificName.toUpperCase() + " ";
        crear(tokenAdministrador, formulario(repetido), fotoPng())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        "Ya existe una ficha con el nombre científico '" + scientificName.toUpperCase() + "'."));
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
        String token = tokenDeUsuarioNuevo(Role.USUARIO_REGISTRADO);

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
        String scientificName = nombreCientificoUnico();
        String creada = crearFicha(scientificName);
        String id = JsonPath.read(creada, "$.id");
        Map<String, String> datos = formulario(scientificName);
        datos.put("vernacularName", "Azulejo común");
        datos.put("alimentacion", "Principalmente frutos");
        datos.put("estadoConservacion", "NT");

        String editada = editar(tokenAdministrador, id, datos, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.vernacularName").value("Azulejo común"))
                .andExpect(jsonPath("$.alimentacion").value("Principalmente frutos"))
                .andExpect(jsonPath("$.estadoConservacion").value("NT"))
                .andExpect(jsonPath("$.urlFoto").value((String) JsonPath.read(creada, "$.urlFoto")))
                .andExpect(jsonPath("$.fechaCreacion").value((String) JsonPath.read(creada, "$.fechaCreacion")))
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(editada, "$.fechaActualizacion"))
                .isNotEqualTo(JsonPath.read(creada, "$.fechaActualizacion"));

        mockMvc.perform(get(RUTA_FICHAS + "/" + id))
                .andExpect(jsonPath("$.vernacularName").value("Azulejo común"));
    }

    @Test
    @DisplayName("HU-8: la edición solo cambia el texto; un archivo enviado no modifica las fotos")
    void edicionIgnoraFotos() throws Exception {
        String scientificName = nombreCientificoUnico();
        String creada = crearFicha(scientificName);
        MockMultipartFile fotoJpg = new MockMultipartFile("fotos", "nueva.jpg", "image/jpeg", jpeg);

        editar(tokenAdministrador, JsonPath.read(creada, "$.id"), formulario(scientificName), fotoJpg)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlFoto").value((String) JsonPath.read(creada, "$.urlFoto")))
                .andExpect(jsonPath("$.fotos.length()").value(1));
    }

    @Test
    @DisplayName("HU-8: el superadministrador también puede editar (HU-6)")
    void edicionComoSuperadministrador() throws Exception {
        String scientificName = nombreCientificoUnico();
        String id = JsonPath.read(crearFicha(scientificName), "$.id");
        String token = obtenerToken(CORREO_SUPERADMIN, CONTRASENA_SUPERADMIN);

        editar(token, id, formulario(scientificName), null).andExpect(status().isOk());
    }

    @Test
    @DisplayName("HU-8: un campo obligatorio vacío no modifica la ficha")
    void edicionConCampoFaltante() throws Exception {
        String scientificName = nombreCientificoUnico();
        String id = JsonPath.read(crearFicha(scientificName), "$.id");
        Map<String, String> datos = formulario(scientificName);
        datos.put("vernacularName", "Nombre que no debe guardarse");
        datos.remove("family");

        esperarError(editar(tokenAdministrador, id, datos, null), 400, MENSAJE_OBLIGATORIOS);
        mockMvc.perform(get(RUTA_FICHAS + "/" + id))
                .andExpect(jsonPath("$.vernacularName").value("Azulejo"))
                .andExpect(jsonPath("$.family").value("Thraupidae"));
    }

    @Test
    @DisplayName("HU-8: un formulario inválido no modifica la ficha ni su foto")
    void edicionInvalidaConservaFoto() throws Exception {
        String scientificName = nombreCientificoUnico();
        String creada = crearFicha(scientificName);
        String id = JsonPath.read(creada, "$.id");
        Map<String, String> datos = formulario(scientificName);
        datos.remove("genus");

        esperarError(editar(tokenAdministrador, id, datos, null), 400, MENSAJE_OBLIGATORIOS);
        mockMvc.perform(get(RUTA_FICHAS + "/" + id))
                .andExpect(jsonPath("$.urlFoto").value((String) JsonPath.read(creada, "$.urlFoto")));
    }

    @Test
    @DisplayName("HU-8: no se puede usar el nombre científico de otra ficha")
    void edicionConNombreDeOtraFicha() throws Exception {
        String nombreOtra = nombreCientificoUnico();
        crearFicha(nombreOtra);
        String scientificName = nombreCientificoUnico();
        String id = JsonPath.read(crearFicha(scientificName), "$.id");

        esperarError(editar(tokenAdministrador, id, formulario(nombreOtra), null), 409,
                "Ya existe una ficha con el nombre científico '" + nombreOtra + "'.");
    }

    @Test
    @DisplayName("HU-8: la ficha puede conservar su propio nombre científico, incluso cambiando mayúsculas")
    void edicionConservandoSuNombre() throws Exception {
        String scientificName = nombreCientificoUnico();
        String id = JsonPath.read(crearFicha(scientificName), "$.id");

        editar(tokenAdministrador, id, formulario(scientificName.toUpperCase()), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scientificName").value(scientificName.toUpperCase()));
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
        String scientificName = nombreCientificoUnico();
        String id = JsonPath.read(crearFicha(scientificName), "$.id");

        esperarError(editar(tokenDeUsuarioNuevo(Role.USUARIO_REGISTRADO), id, formulario(scientificName), null), 403,
                "No tiene permisos para realizar esta acción.");
        esperarError(editar(null, id, formulario(scientificName), null), 401,
                "Debe iniciar sesión para acceder a este recurso.");
    }

    // HU-5: revocar o inhabilitar tiene efecto inmediato, aunque el token siga vigente

    @Test
    @DisplayName("HU-5: un administrador degradado pierde el permiso con su token actual")
    void administradorDegradadoNoPuedeCrear() throws Exception {
        String correo = crearUsuario(Role.ADMINISTRADOR, "Clave-Admin-9", true);
        String token = obtenerToken(correo, "Clave-Admin-9");
        User usuario = userRepository.findByCorreo(correo).orElseThrow();
        usuario.setRol(Role.USUARIO_REGISTRADO);
        userRepository.save(usuario);

        esperarError(crear(token, formulario(nombreCientificoUnico()), fotoPng()), 403,
                "No tiene permisos para realizar esta acción.");
    }

    @Test
    @DisplayName("HU-5: un administrador inhabilitado queda sin sesión aunque su token no haya vencido")
    void administradorInhabilitadoNoPuedeCrear() throws Exception {
        String correo = crearUsuario(Role.ADMINISTRADOR, "Clave-Admin-10", true);
        String token = obtenerToken(correo, "Clave-Admin-10");
        User usuario = userRepository.findByCorreo(correo).orElseThrow();
        usuario.setHabilitado(false);
        userRepository.save(usuario);

        esperarError(crear(token, formulario(nombreCientificoUnico()), fotoPng()), 401,
                "Debe iniciar sesión para acceder a este recurso.");
    }
    
    // HU-10: listado por categoría

    @Test
    @DisplayName("HU-10 escenario 1: filtrar por categoría devuelve solo fichas de esa categoría, con foto y nombre común")
    void listarPorCategoria() throws Exception {
        String ave = nombreCientificoUnico();
        String planta = nombreCientificoUnico();
        crearFicha(ave);
        Map<String, String> datosPlanta = formulario(planta);
        datosPlanta.put("categoria", "PLANTA");
        crear(tokenAdministrador, datosPlanta, fotoPng()).andExpect(status().isCreated());

        List<Map<String, Object>> aves = listadoCompleto("AVE");
        assertThat(aves).extracting(ficha -> ficha.get("scientificName")).contains(ave).doesNotContain(planta);
        assertThat(aves).extracting(ficha -> ficha.get("categoria")).containsOnly("AVE");
        mockMvc.perform(get(RUTA_FICHAS).param("categoria", "AVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido[0].vernacularName").isNotEmpty())
            .andExpect(jsonPath("$.contenido[0].urlFoto").isNotEmpty());
    }

    @Test
    @DisplayName("HU-10 escenario 2: una categoría sin fichas devuelve una página vacía")
    void listarCategoriaSinFichas() throws Exception {
        crearFicha(nombreCientificoUnico()); // solo hay aves en estas pruebas

        mockMvc.perform(get(RUTA_FICHAS).param("categoria", "INSECTO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido").isArray())
            .andExpect(jsonPath("$.contenido[?(@.categoria != 'INSECTO')]").isEmpty());
    }

    @Test
    @DisplayName("HU-10: sin categoría se listan todas las fichas")
    void listarSinFiltro() throws Exception {
        String scientificName = nombreCientificoUnico();
        crearFicha(scientificName);

        assertThat(listadoCompleto(null)).extracting(ficha -> ficha.get("scientificName")).contains(scientificName);
    }

    @Test
    @DisplayName("HU-10 y HU-21: el listado sigue el orden alfabético español en todas las páginas: sin importar "
            + "mayúsculas ni tildes, y la ñ después de la n")
    void listadoEnOrdenAlfabetico() throws Exception {
        String zorzal = crearConNombreComun("Zorzal");
        String nandu = crearConNombreComun("Ñandú");
        String aguila = crearConNombreComun("águila");
        String nutria = crearConNombreComun("nutria");
        String buho = crearConNombreComun("Búho");

        assertThat(listadoCompleto(null)).extracting(ficha -> ficha.get("id"))
            .containsSubsequence(aguila, buho, nutria, nandu, zorzal);
    }

    @Test
    @DisplayName("HU-10: una categoría que no existe responde 400")
    void listarConCategoriaInvalida() throws Exception {
        esperarError(mockMvc.perform(get(RUTA_FICHAS).param("categoria", "PEZ")), 400,
            "El valor del parámetro 'categoria' no es válido.");
    }
    
    @ParameterizedTest(name = "{0}")
    @EnumSource(TaxonCategory.class)
    @DisplayName("HU-7 criterio 1 y HU-10: la ficha se publica en el listado de su categoría (ave, planta, insecto, mamífero, reptil, anfibio u hongo)")
    void publicarYListarPorCadaCategoria(TaxonCategory categoria) throws Exception {
        String scientificName = nombreCientificoUnico();
        Map<String, String> datos = formulario(scientificName);
        datos.put("categoria", categoria.name());

        crear(tokenAdministrador, datos, fotoPng())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.categoria").value(categoria.name()));

        List<Map<String, Object>> fichas = listadoCompleto(categoria.name());
        assertThat(fichas).extracting(ficha -> ficha.get("scientificName")).contains(scientificName);
        assertThat(fichas).extracting(ficha -> ficha.get("categoria")).containsOnly(categoria.name());
    }

    @Test
    @DisplayName("HU-7: las categorías de la versión 2.0 son exactamente siete")
    void categoriasDisponibles() {
        assertThat(TaxonCategory.values()).extracting(Enum::name)
            .containsExactly("AVE", "PLANTA", "INSECTO", "MAMIFERO", "REPTIL", "ANFIBIO", "HONGO");
    }
    
    // HU-11: generación y acceso al QR

    @Test
    @DisplayName("HU-11: administrador descarga un PNG QR que apunta al identificador de la ficha")
    void descargarQrComoAdministrador() throws Exception {
        String ficha = crearFicha(nombreCientificoUnico());
        String id = JsonPath.read(ficha, "$.id");

        byte[] qr = mockMvc.perform(get(RUTA_FICHAS + "/" + id + "/qr")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdministrador))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=orquidea-ficha-" + id + ".png"))
                .andReturn().getResponse().getContentAsByteArray();

        BufferedImage imagenQr = ImageIO.read(new java.io.ByteArrayInputStream(qr));
        assertThat(imagenQr.getWidth()).isEqualTo(1024);
        assertThat(imagenQr.getHeight()).isEqualTo(1024);
        int[] pixeles = imagenQr.getRGB(0, 0, imagenQr.getWidth(), imagenQr.getHeight(), null, 0,
                imagenQr.getWidth());
        String contenido = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(
                new RGBLuminanceSource(imagenQr.getWidth(), imagenQr.getHeight(), pixeles)))).getText();
        assertThat(contenido).isEqualTo("orquideamobileapp://species/" + id);
    }

    @Test
    @DisplayName("HU-11: OpenAPI describe la descarga QR como PNG binario")
    void openApiDescribeDescargaQrComoPngBinario() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/fichas-taxonomicas/{id}/qr'].get.responses['200'].content['image/png'].schema.type")
                        .value("string"))
                .andExpect(jsonPath("$.paths['/api/fichas-taxonomicas/{id}/qr'].get.responses['200'].content['image/png'].schema.format")
                        .value("binary"));
    }

    @Test
    @DisplayName("HU-11: el superadministrador también puede descargar QR")
    void descargarQrComoSuperadministrador() throws Exception {
        String id = JsonPath.read(crearFicha(nombreCientificoUnico()), "$.id");
        String token = obtenerToken(CORREO_SUPERADMIN, CONTRASENA_SUPERADMIN);

        mockMvc.perform(get(RUTA_FICHAS + "/" + id + "/qr")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"));
    }

    @Test
    @DisplayName("HU-11: usuarios sin rol administrativo no descargan el QR")
    void descargarQrSinPermisos() throws Exception {
        String id = JsonPath.read(crearFicha(nombreCientificoUnico()), "$.id");

        esperarError(mockMvc.perform(get(RUTA_FICHAS + "/" + id + "/qr")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + tokenDeUsuarioNuevo(Role.USUARIO_REGISTRADO))),
                403, "No tiene permisos para realizar esta acción.");
        esperarError(mockMvc.perform(get(RUTA_FICHAS + "/" + id + "/qr")),
                401, "Debe iniciar sesión para acceder a este recurso.");
    }

    @Test
    @DisplayName("HU-11: no se genera QR para una ficha inexistente")
    void descargarQrDeFichaInexistente() throws Exception {
        esperarError(mockMvc.perform(get(RUTA_FICHAS + "/" + UUID.randomUUID() + "/qr")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdministrador)),
                404, "La ficha taxonómica solicitada no existe.");
    }

    // HU-7 (ajuste): varias fotos por ficha

    @Test
    @DisplayName("HU-7 (ajuste) escenario 4: con varias fotos se guardan todas en orden y la primera es la principal")
    void creacionConVariasFotos() throws Exception {
        MockMultipartHttpServletRequestBuilder solicitud = multipart(RUTA_FICHAS);
        solicitud.file(fotoPng());
        solicitud.file(new MockMultipartFile("fotos", "segunda.jpg", "image/jpeg", jpeg));

        String respuesta = enviar(solicitud, tokenAdministrador, formulario(nombreCientificoUnico()), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fotos.length()").value(2))
                .andExpect(jsonPath("$.fotos[0].url", endsWith(".png")))
                .andExpect(jsonPath("$.fotos[1].url", endsWith(".jpg")))
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(respuesta, "$.urlFoto")).isEqualTo(JsonPath.read(respuesta, "$.fotos[0].url"));
    }

    @Test
    @DisplayName("HU-7 (ajuste): no se puede crear una ficha con más de 10 fotos")
    void creacionConDemasiadasFotos() throws Exception {
        MockMultipartHttpServletRequestBuilder solicitud = multipart(RUTA_FICHAS);
        for (int i = 0; i < 11; i++) {
            solicitud.file(fotoPng());
        }

        esperarError(enviar(solicitud, tokenAdministrador, formulario(nombreCientificoUnico()), null), 409,
                "La ficha ya tiene el máximo de 10 fotos.");
    }

    @Test
    @DisplayName("HU-7 (ajuste): una foto agregada queda al final, la principal no cambia y la ficha registra la modificación")
    void agregarFoto() throws Exception {
        String creada = crearFicha(nombreCientificoUnico());
        String id = JsonPath.read(creada, "$.id");

        agregarFoto(tokenAdministrador, id, new MockMultipartFile("foto", "otra.jpg", "image/jpeg", jpeg))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.fotos.length()").value(2))
                .andExpect(jsonPath("$.fotos[1].url", endsWith(".jpg")))
                .andExpect(jsonPath("$.urlFoto").value((String) JsonPath.read(creada, "$.urlFoto")))
                .andExpect(jsonPath("$.fechaActualizacion", not((String) JsonPath.read(creada, "$.fechaActualizacion"))));
    }

    @Test
    @DisplayName("HU-7 (ajuste): agregar una foto valida el archivo")
    void agregarFotoInvalida() throws Exception {
        String id = JsonPath.read(crearFicha(nombreCientificoUnico()), "$.id");

        esperarError(agregarFoto(tokenAdministrador, id, null), 400, MENSAJE_SIN_FOTO);
        esperarError(agregarFoto(tokenAdministrador, id,
                new MockMultipartFile("foto", "foto.gif", "image/gif", imagen("gif"))), 400, MENSAJE_FORMATO);
        esperarError(agregarFoto(tokenAdministrador, UUID.randomUUID().toString(), fotoParaAgregar()), 404,
                "La ficha taxonómica solicitada no existe.");
    }

    @Test
    @DisplayName("HU-7 (ajuste): la foto agregada no puede superar 10 MB y la ficha no cambia")
    void agregarFotoDemasiadoGrande() throws Exception {
        String id = JsonPath.read(crearFicha(nombreCientificoUnico()), "$.id");
        byte[] grande = Arrays.copyOf(png, 10 * 1024 * 1024 + 1);

        esperarError(agregarFoto(tokenAdministrador, id, new MockMultipartFile("foto", "grande.png", "image/png", grande)),
                413, "La imagen supera el tamaño máximo permitido de 10 MB.");
        mockMvc.perform(get(RUTA_FICHAS + "/" + id))
                .andExpect(jsonPath("$.fotos.length()").value(1));
    }

    @Test
    @DisplayName("HU-7 (ajuste): una ficha admite como máximo 10 fotos")
    void agregarFotoSobreElMaximo() throws Exception {
        String id = JsonPath.read(crearFicha(nombreCientificoUnico()), "$.id");
        for (int i = 1; i < 10; i++) {
            agregarFoto(tokenAdministrador, id, fotoParaAgregar()).andExpect(status().isCreated());
        }

        esperarError(agregarFoto(tokenAdministrador, id, fotoParaAgregar()), 409,
                "La ficha ya tiene el máximo de 10 fotos.");
    }

    @Test
    @DisplayName("HU-7 (ajuste): al eliminar la foto principal la siguiente pasa a serlo y la eliminada se borra del almacenamiento")
    void eliminarFotoPrincipal() throws Exception {
        String id = JsonPath.read(crearFicha(nombreCientificoUnico()), "$.id");
        String conDos = agregarFoto(tokenAdministrador, id, fotoParaAgregar())
                .andReturn().getResponse().getContentAsString();
        String idPrincipal = JsonPath.read(conDos, "$.fotos[0].id");
        String urlPrincipal = JsonPath.read(conDos, "$.fotos[0].url");
        String miniaturaPrincipal = JsonPath.read(conDos, "$.fotos[0].urlMiniatura");
        assertThat(estadoHttp(miniaturaPrincipal)).isEqualTo(200);
        String urlSegunda = JsonPath.read(conDos, "$.fotos[1].url");

        eliminarFoto(tokenAdministrador, id, idPrincipal).andExpect(status().isNoContent());

        mockMvc.perform(get(RUTA_FICHAS + "/" + id))
                .andExpect(jsonPath("$.fotos.length()").value(1))
                .andExpect(jsonPath("$.urlFoto").value(urlSegunda))
                .andExpect(jsonPath("$.fechaActualizacion", not((String) JsonPath.read(conDos, "$.fechaActualizacion"))));
        assertThat(estadoHttp(urlPrincipal)).isEqualTo(404);
        assertThat(estadoHttp(miniaturaPrincipal)).isEqualTo(404);
    }

    @Test
    @DisplayName("HU-7 (ajuste): la ficha conserva siempre al menos una foto")
    void eliminarUnicaFoto() throws Exception {
        String creada = crearFicha(nombreCientificoUnico());
        String id = JsonPath.read(creada, "$.id");

        esperarError(eliminarFoto(tokenAdministrador, id, JsonPath.read(creada, "$.fotos[0].id")), 409,
                "La ficha debe conservar al menos una foto.");
        esperarError(eliminarFoto(tokenAdministrador, id, UUID.randomUUID().toString()), 404,
                "La foto solicitada no existe en esta ficha.");
    }

    @Test
    @DisplayName("HU-7 (ajuste): un usuario registrado no puede gestionar las fotos")
    void usuarioRegistradoNoGestionaFotos() throws Exception {
        String creada = crearFicha(nombreCientificoUnico());
        String id = JsonPath.read(creada, "$.id");
        String token = tokenDeUsuarioNuevo(Role.USUARIO_REGISTRADO);

        agregarFoto(token, id, fotoParaAgregar()).andExpect(status().isForbidden());
        eliminarFoto(token, id, JsonPath.read(creada, "$.fotos[0].id")).andExpect(status().isForbidden());
    }

    // HU-21: listado paginado y sincronización para consultar las fichas sin conexión

    @Test
    @DisplayName("HU-21: el listado se pagina y una página no repite fichas de otra")
    void listadoPaginado() throws Exception {
        for (int i = 0; i < 3; i++) {
            crearFicha(nombreCientificoUnico());
        }

        String primera = mockMvc.perform(get(RUTA_FICHAS).param("tamano", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido.length()").value(2))
            .andExpect(jsonPath("$.pagina").value(0))
            .andExpect(jsonPath("$.tamano").value(2))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        int total = JsonPath.<Number>read(primera, "$.totalElementos").intValue();
        assertThat(total).isGreaterThanOrEqualTo(3);
        assertThat((Integer) JsonPath.read(primera, "$.totalPaginas")).isEqualTo((total + 1) / 2);

        String segunda = mockMvc.perform(get(RUTA_FICHAS).param("pagina", "1").param("tamano", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pagina").value(1))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> idsPrimera = JsonPath.read(primera, "$.contenido[*].id");
        List<String> idsSegunda = JsonPath.read(segunda, "$.contenido[*].id");
        assertThat(idsSegunda).isNotEmpty().doesNotContainAnyElementsOf(idsPrimera);
    }

    @Test
    @DisplayName("HU-21: por defecto se pide la página 0 de 20; el tamaño queda entre 1 y 50 y la página no baja de 0")
    void limitesDeLaPaginacion() throws Exception {
        crearFicha(nombreCientificoUnico());

        mockMvc.perform(get(RUTA_FICHAS))
            .andExpect(jsonPath("$.pagina").value(0))
            .andExpect(jsonPath("$.tamano").value(20));
        mockMvc.perform(get(RUTA_FICHAS).param("pagina", "-3").param("tamano", "500"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pagina").value(0))
            .andExpect(jsonPath("$.tamano").value(50));
        mockMvc.perform(get(RUTA_FICHAS).param("tamano", "0"))
            .andExpect(jsonPath("$.tamano").value(1))
            .andExpect(jsonPath("$.contenido.length()").value(1));
        mockMvc.perform(get(RUTA_FICHAS).param("pagina", "100000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.contenido").isEmpty())
            .andExpect(jsonPath("$.totalElementos", greaterThan(0)));
        esperarError(mockMvc.perform(get(RUTA_FICHAS).param("pagina", "primera")), 400,
            "El valor del parámetro 'pagina' no es válido.");
    }

    @Test
    @DisplayName("HU-21: sin fecha, la sincronización trae todas las fichas, iguales a su consulta y con sus fotos")
    void sincronizacionCompleta() throws Exception {
        String id = idDe(crearFicha(nombreCientificoUnico()));
        String detalle = mockMvc.perform(get(RUTA_FICHAS + "/" + id))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        int total = JsonPath.<Number>read(
            mockMvc.perform(get(RUTA_FICHAS)).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
            "$.totalElementos").intValue();

        String respuesta = mockMvc.perform(get(RUTA_SINCRONIZACION))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completa").value(true))
            .andExpect(jsonPath("$.fechaSincronizacion").isNotEmpty())
            .andExpect(jsonPath("$.idsEliminados").isEmpty())
            .andExpect(jsonPath("$.fichas.length()").value(total))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        List<Map<String, Object>> sincronizada = JsonPath.read(respuesta, "$.fichas[?(@.id == '" + id + "')]");
        Map<String, Object> consultada = JsonPath.read(detalle, "$");
        assertThat(sincronizada).singleElement().isEqualTo(consultada);
    }

    @Test
    @DisplayName("HU-21: con la fecha de la sincronización anterior solo llegan las fichas creadas o modificadas "
            + "después, también si cambiaron sus fotos")
    void sincronizacionIncremental() throws Exception {
        String sinCambios = idDe(crearFicha(nombreCientificoUnico()));
        String editada = idDe(crearFicha(nombreCientificoUnico()));
        String conFotoNueva = idDe(crearFicha(nombreCientificoUnico()));
        // Descargadas hace una hora, fuera del margen que repasa la sincronización
        for (String id : List.of(sinCambios, editada, conFotoNueva)) {
            cambiarFechaActualizacion(id, Instant.now().minus(1, ChronoUnit.HOURS));
        }
        String fechaAnterior = JsonPath.read(sincronizar(null), "$.fechaSincronizacion");

        String nueva = idDe(crearFicha(nombreCientificoUnico()));
        editar(tokenAdministrador, editada, formulario(nombreCientificoUnico()), null).andExpect(status().isOk());
        agregarFoto(tokenAdministrador, conFotoNueva, fotoParaAgregar()).andExpect(status().isCreated());

        String cambios = sincronizar(fechaAnterior);
        assertThat((Boolean) JsonPath.read(cambios, "$.completa")).isFalse();
        assertThat(idsSincronizados(cambios)).contains(nueva, editada, conFotoNueva).doesNotContain(sinCambios);
        List<String> fotos = JsonPath.read(cambios, "$.fichas[?(@.id == '" + conFotoNueva + "')].fotos[*].id");
        assertThat(fotos).hasSize(2);
        assertThat(Instant.parse(JsonPath.read(cambios, "$.fechaSincronizacion")))
            .isAfter(Instant.parse(fechaAnterior));
    }

    @Test
    @DisplayName("HU-21: se repasa un margen antes de la fecha enviada, para no perder una edición que se confirmó "
            + "justo después de la sincronización anterior")
    void sincronizacionConMargen() throws Exception {
        String id = idDe(crearFicha(nombreCientificoUnico()));
        Instant fechaAnterior = Instant.now();
        cambiarFechaActualizacion(id, fechaAnterior.minusSeconds(30));

        assertThat(idsSincronizados(sincronizar(fechaAnterior.toString()))).contains(id);
    }

    @Test
    @DisplayName("HU-21: una ficha eliminada, aunque sea directamente en la base de datos, llega en idsEliminados "
            + "y sale de la sincronización completa")
    void sincronizacionConFichaEliminada() throws Exception {
        String eliminada = idDe(crearFicha(nombreCientificoUnico()));
        String fechaAnterior = JsonPath.read(sincronizar(null), "$.fechaSincronizacion");

        eliminarEnBaseDeDatos(eliminada);

        String cambios = sincronizar(fechaAnterior);
        assertThat(idsEliminados(cambios)).contains(eliminada);
        assertThat(idsSincronizados(cambios)).doesNotContain(eliminada);
        String completa = sincronizar(null);
        assertThat(idsSincronizados(completa)).doesNotContain(eliminada);
        assertThat(idsEliminados(completa)).isEmpty();
    }

    @Test
    @DisplayName("HU-21: una eliminación anterior a la fecha enviada no se vuelve a informar")
    void eliminacionAnteriorALaFecha() throws Exception {
        String id = idDe(crearFicha(nombreCientificoUnico()));
        eliminarEnBaseDeDatos(id);
        jdbcTemplate.update("UPDATE fichas_eliminadas SET fecha_eliminacion = ? WHERE id = ?",
            Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS)), UUID.fromString(id));

        assertThat(idsEliminados(sincronizar(Instant.now().minus(10, ChronoUnit.MINUTES).toString())))
            .doesNotContain(id);
    }

    @Test
    @DisplayName("HU-21: una ficha que volvió a existir (por ejemplo, al restaurar un respaldo) no se informa como eliminada")
    void fichaRestauradaNoSeInformaComoEliminada() throws Exception {
        String id = idDe(crearFicha(nombreCientificoUnico()));
        jdbcTemplate.update("INSERT INTO fichas_eliminadas (id, fecha_eliminacion) VALUES (?, now())", UUID.fromString(id));

        String cambios = sincronizar(Instant.now().minus(10, ChronoUnit.MINUTES).toString());
        assertThat(idsEliminados(cambios)).doesNotContain(id);
        assertThat(idsSincronizados(cambios)).contains(id);
    }

    @Test
    @DisplayName("HU-21: una fecha que no es ISO-8601 responde 400")
    void sincronizacionConFechaInvalida() throws Exception {
        esperarError(mockMvc.perform(get(RUTA_SINCRONIZACION).param("modificadoDesde", "ayer")), 400,
            "El valor del parámetro 'modificadoDesde' no es válido.");
    }

    @Test
    @DisplayName("HU-21: una fecha muy antigua o futura no hace fallar la sincronización")
    void sincronizacionConFechaFueraDeRango() throws Exception {
        String id = idDe(crearFicha(nombreCientificoUnico()));

        assertThat(idsSincronizados(sincronizar("-1000000000-01-01T00:00:00Z"))).contains(id);
        assertThat((Boolean) JsonPath.read(sincronizar("+1000000000-01-01T00:00:00Z"), "$.completa")).isFalse();
    }

    // Utilidades

    /** Recorre todas las páginas del listado (de una categoría, si no es nula) y devuelve las fichas en orden. */
    private List<Map<String, Object>> listadoCompleto(String categoria) throws Exception {
        List<Map<String, Object>> fichas = new ArrayList<>();
        int pagina = 0;
        int totalPaginas;
        do {
            var solicitud = get(RUTA_FICHAS).param("pagina", String.valueOf(pagina)).param("tamano", "50");
            if (categoria != null) {
                solicitud.param("categoria", categoria);
            }
            String respuesta = mockMvc.perform(solicitud)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            fichas.addAll(JsonPath.read(respuesta, "$.contenido"));
            totalPaginas = JsonPath.read(respuesta, "$.totalPaginas");
            pagina++;
        } while (pagina < totalPaginas);
        return fichas;
    }

    private String sincronizar(String modificadoDesde) throws Exception {
        var solicitud = get(RUTA_SINCRONIZACION);
        if (modificadoDesde != null) {
            solicitud.param("modificadoDesde", modificadoDesde);
        }
        return mockMvc.perform(solicitud)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static List<String> idsSincronizados(String respuesta) {
        return JsonPath.read(respuesta, "$.fichas[*].id");
    }

    private static List<String> idsEliminados(String respuesta) {
        return JsonPath.read(respuesta, "$.idsEliminados");
    }

    private void cambiarFechaActualizacion(String id, Instant fecha) {
        jdbcTemplate.update("UPDATE fichas_taxonomicas SET fecha_actualizacion = ? WHERE id = ?",
            Timestamp.from(fecha), UUID.fromString(id));
    }

    /** No hay endpoint para borrar fichas: se borra como lo haría alguien desde pgAdmin. */
    private void eliminarEnBaseDeDatos(String id) {
        jdbcTemplate.update("DELETE FROM fichas_taxonomicas WHERE id = ?", UUID.fromString(id));
    }

    private static String idDe(String ficha) {
        return JsonPath.read(ficha, "$.id");
    }

    private ResultActions agregarFoto(String token, String id, MockMultipartFile foto) throws Exception {
        return enviar(multipart(RUTA_FICHAS + "/" + id + "/fotos"), token, Map.of(), foto);
    }

    private ResultActions eliminarFoto(String token, String id, String idFoto) throws Exception {
        var solicitud = delete(RUTA_FICHAS + "/" + id + "/fotos/" + idFoto);
        if (token != null) {
            solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(solicitud);
    }

    private static MockMultipartFile fotoParaAgregar() {
        return new MockMultipartFile("foto", "otra.png", "image/png", png);
    }

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
    private String crearFicha(String scientificName) throws Exception {
        return crear(tokenAdministrador, formulario(scientificName), fotoPng())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String crearConNombreComun(String vernacularName) throws Exception {
        Map<String, String> datos = formulario(nombreCientificoUnico());
        datos.put("vernacularName", vernacularName);
        String respuesta = crear(tokenAdministrador, datos, fotoPng())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.id");
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
    private static Map<String, String> formulario(String scientificName) {
        Map<String, String> datos = new LinkedHashMap<>();
        datos.put("categoria", "AVE");
        datos.put("order", "Passeriformes");
        datos.put("family", "Thraupidae");
        datos.put("genus", "Thraupis");
        datos.put("scientificName", scientificName);
        datos.put("vernacularName", "Azulejo");
        datos.put("alimentacion", "Frutos e insectos");
        datos.put("rolEnHumedal", "Dispersor de semillas");
        datos.put("estadoConservacion", "LC");
        return datos;
    }

    private static MockMultipartFile fotoPng() {
        return new MockMultipartFile("fotos", "azulejo.png", "image/png", png);
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
