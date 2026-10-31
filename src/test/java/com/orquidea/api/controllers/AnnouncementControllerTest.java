package com.orquidea.api.controllers;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.orquidea.api.IntegrationTestBase;
import com.orquidea.api.model.Role;

class AnnouncementControllerTest extends IntegrationTestBase {

    private static final String RUTA_ANUNCIOS = "/api/anuncios";
    private static final String RUTA_ADMINISTRACION = "/api/anuncios/administracion";

    @Autowired
    private JdbcTemplate jdbc;

    /** Todas las pruebas comparten la misma BD: los títulos únicos evitan depender de lo que haya en ella. */
    private static String tituloUnico() {
        return "Anuncio " + UUID.randomUUID();
    }

    private ResultActions publicar(String token, String cuerpoJson) throws Exception {
        var peticion = post(RUTA_ANUNCIOS).contentType(MediaType.APPLICATION_JSON).content(cuerpoJson);
        if (token != null) {
            peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(peticion);
    }

    /** Publica con prioridad MEDIA y sin fecha de expiración. */
    private ResultActions publicar(String token, String titulo, String descripcion) throws Exception {
        return publicar(token, cuerpo(titulo, descripcion, "MEDIA", null));
    }

    /** Arma el JSON del request; prioridad y fecha son opcionales para poder probar su ausencia. */
    private static String cuerpo(String titulo, String descripcion, String prioridad, Instant fechaExpiracion) {
        StringBuilder json = new StringBuilder("{\"titulo\":\"" + titulo + "\",\"descripcion\":\"" + descripcion + "\"");
        if (prioridad != null) {
            json.append(",\"prioridad\":\"").append(prioridad).append("\"");
        }
        if (fechaExpiracion != null) {
            json.append(",\"fechaExpiracion\":\"").append(fechaExpiracion).append("\"");
        }
        return json.append("}").toString();
    }

    /** Simula que la fecha de expiración ya pasó (el request no permite crear anuncios ya vencidos). */
    private void vencer(String titulo) {
        jdbc.update("UPDATE anuncios SET fecha_expiracion = now() - interval '1 hour' WHERE titulo = ?", titulo);
    }

    /** Simula que un administrador bajó el anuncio antes de su fecha final. */
    private void desactivar(String titulo) {
        jdbc.update("UPDATE anuncios SET estado = 'INACTIVO' WHERE titulo = ?", titulo);
    }

    // ---------- HU-16: publicar ----------

    @Test
    @DisplayName("Un administrador publica un anuncio y recibe 201 con id, fecha, prioridad y estado activo")
    void administradorPublicaAnuncio() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);
        String titulo = tituloUnico();

        publicar(token, cuerpo(titulo, "Contenido del anuncio", "ALTA", null))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andExpect(jsonPath("$.titulo").value(titulo))
            .andExpect(jsonPath("$.descripcion").value("Contenido del anuncio"))
            .andExpect(jsonPath("$.fechaCreacion").value(notNullValue()))
            .andExpect(jsonPath("$.prioridad").value("ALTA"))
            .andExpect(jsonPath("$.estado").value("ACTIVO"))
            .andExpect(jsonPath("$.fechaExpiracion").value(nullValue()))
            .andExpect(jsonPath("$.expirado").value(false));
    }

    @Test
    @DisplayName("El superadministrador también puede publicar anuncios")
    void superadministradorPublicaAnuncio() throws Exception {
        String token = obtenerToken(CORREO_SUPERADMIN, CONTRASENA_SUPERADMIN);

        publicar(token, tituloUnico(), "Publicado por el superadministrador")
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("El cliente no puede imponer el id, la fecha de creación ni el estado del anuncio")
    void idFechaYEstadoLosAsignaElSistema() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);
        UUID idImpuesto = UUID.randomUUID();

        publicar(token, """
                {"id":"%s","titulo":"%s","descripcion":"Contenido","prioridad":"MEDIA",
                 "estado":"INACTIVO","fechaCreacion":"2000-01-01T00:00:00Z"}
                """.formatted(idImpuesto, tituloUnico()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(not(idImpuesto.toString())))
            .andExpect(jsonPath("$.fechaCreacion").value(not("2000-01-01T00:00:00Z")))
            .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    @DisplayName("Sin título no se publica el anuncio")
    void tituloObligatorio() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);

        publicar(token, "{\"descripcion\":\"Solo contenido\",\"prioridad\":\"MEDIA\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("Debes completar el título del anuncio."));
    }

    @Test
    @DisplayName("Un título con solo espacios no se acepta")
    void tituloEnBlancoNoSeAcepta() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);

        publicar(token, "   ", "Contenido")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("Debes completar el título del anuncio."));
    }

    @Test
    @DisplayName("Sin contenido no se publica el anuncio")
    void contenidoObligatorio() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);

        publicar(token, "{\"titulo\":\"Solo título\",\"prioridad\":\"MEDIA\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("Debes completar el contenido del anuncio."));
    }

    @Test
    @DisplayName("Un título de más de 150 caracteres se rechaza")
    void tituloDemasiadoLargo() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);

        publicar(token, "T".repeat(151), "Contenido")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("El título no puede superar 150 caracteres."));
    }

    @Test
    @DisplayName("Un contenido de más de 1000 caracteres se rechaza")
    void contenidoDemasiadoLargo() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);

        publicar(token, tituloUnico(), "C".repeat(1001))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("El contenido no puede superar 1000 caracteres."));
    }

    @Test
    @DisplayName("Sin prioridad no se publica el anuncio")
    void prioridadObligatoria() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);

        publicar(token, cuerpo(tituloUnico(), "Contenido", null, null))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensaje").value("Debes seleccionar la prioridad del anuncio."));
    }

    @Test
    @DisplayName("Una prioridad que no existe se rechaza con 400")
    void prioridadInvalida() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);

        publicar(token, cuerpo(tituloUnico(), "Contenido", "URGENTE", null))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Una fecha de expiración pasada se rechaza")
    void fechaExpiracionPasadaSeRechaza() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);

        publicar(token, cuerpo(tituloUnico(), "Contenido", "MEDIA", Instant.now().minus(2, ChronoUnit.DAYS)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Una fecha de expiración futura se acepta y el anuncio no aparece como expirado")
    void fechaExpiracionFuturaSeAcepta() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);

        publicar(token, cuerpo(tituloUnico(), "Contenido", "BAJA", Instant.now().plus(7, ChronoUnit.DAYS)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.fechaExpiracion").value(notNullValue()))
            .andExpect(jsonPath("$.prioridad").value("BAJA"))
            .andExpect(jsonPath("$.expirado").value(false));
    }

    @Test
    @DisplayName("Un usuario registrado no puede publicar anuncios")
    void usuarioRegistradoNoPuedePublicar() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.USUARIO_REGISTRADO);

        publicar(token, tituloUnico(), "Contenido")
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.mensaje").value("No tiene permisos para realizar esta acción."));
    }

    @Test
    @DisplayName("Sin sesión no se pueden publicar anuncios")
    void sinSesionNoPuedePublicar() throws Exception {
        publicar(null, tituloUnico(), "Contenido")
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.mensaje").value("Debe iniciar sesión para acceder a este recurso."));
    }

    // ---------- HU-17: consultar (público) ----------

    @Test
    @DisplayName("Cualquier visitante sin sesión consulta el listado y recibe 200")
    void visitanteConsultaAnuncios() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);
        String titulo = tituloUnico();
        publicar(token, titulo, "Visible para todos").andExpect(status().isCreated());

        mockMvc.perform(get(RUTA_ANUNCIOS))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(instanceOf(List.class)))
            .andExpect(jsonPath("$[*].titulo", hasItem(titulo)));
    }

    @Test
    @DisplayName("El listado se ordena por fecha de publicación, del más reciente al más antiguo")
    void anunciosOrdenadosPorFechaDescendente() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);
        String primero = tituloUnico();
        String segundo = tituloUnico();
        publicar(token, primero, "Publicado primero").andExpect(status().isCreated());
        Thread.sleep(10); // garantiza fechas distintas
        publicar(token, segundo, "Publicado después").andExpect(status().isCreated());

        String respuesta = mockMvc.perform(get(RUTA_ANUNCIOS))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        List<String> titulos = JsonPath.read(respuesta, "$[*].titulo");

        org.assertj.core.api.Assertions.assertThat(titulos).contains(primero, segundo);
        org.assertj.core.api.Assertions.assertThat(titulos.indexOf(segundo))
            .as("el anuncio más reciente debe aparecer antes")
            .isLessThan(titulos.indexOf(primero));
    }

    @Test
    @DisplayName("Un anuncio cuya fecha de expiración ya pasó no aparece en el listado público")
    void anuncioExpiradoNoApareceEnListadoPublico() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);
        String titulo = tituloUnico();
        publicar(token, cuerpo(titulo, "Se vence pronto", "MEDIA", Instant.now().plus(7, ChronoUnit.DAYS)))
            .andExpect(status().isCreated());
        vencer(titulo);

        mockMvc.perform(get(RUTA_ANUNCIOS))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].titulo", not(hasItem(titulo))));
    }

    @Test
    @DisplayName("Un anuncio desactivado por un administrador no aparece en el listado público")
    void anuncioInactivoNoApareceEnListadoPublico() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);
        String titulo = tituloUnico();
        publicar(token, titulo, "Será desactivado").andExpect(status().isCreated());
        desactivar(titulo);

        mockMvc.perform(get(RUTA_ANUNCIOS))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].titulo", not(hasItem(titulo))));
    }

    // ---------- Listado de administración ----------

    @Test
    @DisplayName("El administrador ve en su listado los anuncios expirados marcados como expirado")
    void administradorVeAnunciosExpirados() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);
        String titulo = tituloUnico();
        publicar(token, cuerpo(titulo, "Se vence pronto", "MEDIA", Instant.now().plus(7, ChronoUnit.DAYS)))
            .andExpect(status().isCreated());
        vencer(titulo);

        mockMvc.perform(get(RUTA_ADMINISTRACION).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.titulo=='" + titulo + "')].expirado", hasItem(true)));
    }

    @Test
    @DisplayName("El administrador ve en su listado los anuncios inactivos")
    void administradorVeAnunciosInactivos() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);
        String titulo = tituloUnico();
        publicar(token, titulo, "Será desactivado").andExpect(status().isCreated());
        desactivar(titulo);

        mockMvc.perform(get(RUTA_ADMINISTRACION).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.titulo=='" + titulo + "')].estado", hasItem("INACTIVO")));
    }

    @Test
    @DisplayName("Un usuario registrado no puede ver el listado de administración")
    void usuarioRegistradoNoVeListadoAdministracion() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.USUARIO_REGISTRADO);

        mockMvc.perform(get(RUTA_ADMINISTRACION).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sin sesión no se puede ver el listado de administración")
    void sinSesionNoVeListadoAdministracion() throws Exception {
        mockMvc.perform(get(RUTA_ADMINISTRACION))
            .andExpect(status().isUnauthorized());
    }
}