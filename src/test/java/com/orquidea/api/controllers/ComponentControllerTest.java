package com.orquidea.api.controllers;

import com.jayway.jsonpath.JsonPath;
import com.orquidea.api.IntegrationTestBase;
import com.orquidea.api.model.Role;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ComponentControllerTest extends IntegrationTestBase {

    private static final String RUTA = "/api/componentes";
    private static final String MENSAJE_OBLIGATORIOS = "Debes completar todos los campos obligatorios.";
    private static final String MENSAJE_SIN_PERMISOS = "No tiene permisos para realizar esta acción.";
    private static final String MENSAJE_SIN_SESION = "Debe iniciar sesión para acceder a este recurso.";
    private static final int FOTOS_MAXIMAS = 10;

    @Autowired
    private JdbcTemplate jdbc;

    private static byte[] png;
    private String tokenAdministrador;

    @BeforeAll
    static void generarImagen() throws Exception {
        png = imagen("png");
    }

    /** Los componentes son filas fijas compartidas por todas las pruebas: cada una parte de cero. */
    @BeforeEach
    void prepararComponentes() throws Exception {
        jdbc.update("DELETE FROM fotos_componente");
        jdbc.update("UPDATE componentes_humedal SET descripcion = NULL");
        tokenAdministrador = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);
    }

    // HU-14

    @Test
    @DisplayName("HU-14: el listado público trae los cuatro componentes en orden")
    void listarComponentes() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].tipo", contains("INSECTOS", "AVES", "FLORA", "OTROS")))
                .andExpect(jsonPath("$[0].nombre").value("Insectos"));
    }

    @Test
    @DisplayName("HU-14 escenario 2: un componente sin información responde 200 con tieneContenido=false")
    void componenteSinContenido() throws Exception {
        mockMvc.perform(get(RUTA + "/AVES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tieneContenido").value(false))
                .andExpect(jsonPath("$.descripcion").doesNotExist())
                .andExpect(jsonPath("$.fotos").isEmpty());
    }

    @Test
    @DisplayName("HU-14: un componente que no existe responde 404")
    void componenteInexistente() throws Exception {
        esperarError(mockMvc.perform(get(RUTA + "/PECES")), 404, "El recurso solicitado no existe.");
    }

    // HU-15

    @Test
    @DisplayName("HU-15 escenario 1: el administrador actualiza el texto y queda visible para todos")
    void actualizacionExitosa() throws Exception {
        editar(tokenAdministrador, "AVES", cuerpo("Aves del humedal", "Especies residentes y migratorias."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Aves del humedal"))
                .andExpect(jsonPath("$.tieneContenido").value(true));

        mockMvc.perform(get(RUTA + "/AVES"))
                .andExpect(jsonPath("$.descripcion").value("Especies residentes y migratorias."))
                .andExpect(jsonPath("$.tieneContenido").value(true));
    }

    @Test
    @DisplayName("HU-15: el superadministrador también puede editar (HU-6)")
    void actualizacionComoSuperadministrador() throws Exception {
        String token = obtenerToken(CORREO_SUPERADMIN, CONTRASENA_SUPERADMIN);

        editar(token, "FLORA", cuerpo("Flora", "Plantas acuáticas y de ribera.")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("HU-15: nombre o descripción vacíos no modifican el componente")
    void actualizacionConCamposVacios() throws Exception {
        esperarError(editar(tokenAdministrador, "AVES", cuerpo("Aves", "   ")), 400, MENSAJE_OBLIGATORIOS);
        esperarError(editar(tokenAdministrador, "AVES", cuerpo("", "Texto")), 400, MENSAJE_OBLIGATORIOS);

        mockMvc.perform(get(RUTA + "/AVES")).andExpect(jsonPath("$.tieneContenido").value(false));
    }

    @Test
    @DisplayName("HU-15: solo administradores pueden editar")
    void edicionSinPermisos() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.USUARIO_REGISTRADO);

        esperarError(editar(token, "AVES", cuerpo("Aves", "Texto")), 403, MENSAJE_SIN_PERMISOS);
        esperarError(editar(null, "AVES", cuerpo("Aves", "Texto")), 401, MENSAJE_SIN_SESION);
    }

    @Test
    @DisplayName("HU-15: agregar una foto la publica en el componente y eliminarla la quita")
    void agregarYEliminarFoto() throws Exception {
        String respuesta = agregarFoto(tokenAdministrador, "INSECTOS", fotoPng())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tieneContenido").value(true))
                .andExpect(jsonPath("$.fotos[0].url", endsWith(".png")))
                .andReturn().getResponse().getContentAsString();
        String idFoto = JsonPath.read(respuesta, "$.fotos[0].id");

        mockMvc.perform(get(RUTA + "/INSECTOS")).andExpect(jsonPath("$.fotos[0].id").value(idFoto));

        mockMvc.perform(delete(RUTA + "/INSECTOS/fotos/" + idFoto)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdministrador))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(RUTA + "/INSECTOS"))
                .andExpect(jsonPath("$.fotos").isEmpty())
                .andExpect(jsonPath("$.tieneContenido").value(false));
    }

    @Test
    @DisplayName("HU-15: un formato de imagen no permitido se rechaza")
    void fotoConFormatoInvalido() throws Exception {
        MockMultipartFile gif = new MockMultipartFile("foto", "foto.gif", "image/gif", imagen("gif"));

        esperarError(agregarFoto(tokenAdministrador, "AVES", gif), 400, "El formato de la imagen no es válido.");
    }

    @Test
    @DisplayName("HU-15: sin archivo se pide seleccionar una imagen")
    void fotoFaltante() throws Exception {
        esperarError(agregarFoto(tokenAdministrador, "AVES", null), 400, "Debes seleccionar una imagen.");
    }

    @Test
    @DisplayName("HU-15: un componente admite como máximo 10 fotos")
    void limiteDeFotos() throws Exception {
        for (int i = 0; i < FOTOS_MAXIMAS; i++) {
            agregarFoto(tokenAdministrador, "FLORA", fotoPng()).andExpect(status().isCreated());
        }

        esperarError(agregarFoto(tokenAdministrador, "FLORA", fotoPng()), 409,
                "El componente ya tiene el máximo de " + FOTOS_MAXIMAS + " fotos.");
    }

    @Test
    @DisplayName("HU-15: eliminar una foto que no existe responde 404")
    void eliminarFotoInexistente() throws Exception {
        esperarError(mockMvc.perform(delete(RUTA + "/AVES/fotos/" + UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdministrador)), 404,
                "La foto solicitada no existe en este componente.");
    }

    @Test
    @DisplayName("HU-15: solo administradores pueden agregar fotos")
    void fotoSinPermisos() throws Exception {
        esperarError(agregarFoto(tokenDeUsuarioNuevo(Role.USUARIO_REGISTRADO), "AVES", fotoPng()), 403, MENSAJE_SIN_PERMISOS);
        esperarError(agregarFoto(null, "AVES", fotoPng()), 401, MENSAJE_SIN_SESION);
    }

    // Utilidades

    private ResultActions editar(String token, String tipo, String cuerpo) throws Exception {
        var solicitud = put(RUTA + "/" + tipo).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        if (token != null) {
            solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(solicitud);
    }

    private ResultActions agregarFoto(String token, String tipo, MockMultipartFile foto) throws Exception {
        var solicitud = multipart(RUTA + "/" + tipo + "/fotos");
        if (foto != null) {
            solicitud.file(foto);
        }
        if (token != null) {
            solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(solicitud);
    }

    private static String cuerpo(String nombre, String descripcion) {
        return "{\"nombre\":\"%s\",\"descripcion\":\"%s\"}".formatted(nombre, descripcion);
    }

    private static void esperarError(ResultActions resultado, int estado, String mensaje) throws Exception {
        resultado.andExpect(status().is(estado))
                .andExpect(jsonPath("$.estado").value(estado))
                .andExpect(jsonPath("$.mensaje").value(mensaje));
    }

    private static MockMultipartFile fotoPng() {
        return new MockMultipartFile("foto", "foto.png", "image/png", png);
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
