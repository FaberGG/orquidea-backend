package com.orquidea.api.controllers;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.orquidea.api.IntegrationTestBase;
import com.orquidea.api.model.Role;

class AnnouncementControllerTest extends IntegrationTestBase {

    private static final String RUTA_ANUNCIOS = "/api/anuncios";

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

    private ResultActions publicar(String token, String titulo, String descripcion) throws Exception {
        return publicar(token, """
                {"titulo":"%s","descripcion":"%s"}
                """.formatted(titulo, descripcion));
    }

    

    @Test
    @DisplayName("Un administrador publica un anuncio y recibe 201 con id y fecha")
    void administradorPublicaAnuncio() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);
        String titulo = tituloUnico();

        publicar(token, titulo, "Contenido del anuncio")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andExpect(jsonPath("$.titulo").value(titulo))
            .andExpect(jsonPath("$.descripcion").value("Contenido del anuncio"))
            .andExpect(jsonPath("$.fechaCreacion").value(notNullValue()));
    }

    @Test
    @DisplayName("El superadministrador también puede publicar anuncios")
    void superadministradorPublicaAnuncio() throws Exception {
        String token = obtenerToken(CORREO_SUPERADMIN, CONTRASENA_SUPERADMIN);

        publicar(token, tituloUnico(), "Publicado por el superadministrador")
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("El cliente no puede imponer el id ni la fecha del anuncio")
    void idYFechaLosAsignaElSistema() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);
        UUID idImpuesto = UUID.randomUUID();

        publicar(token, """
                {"id":"%s","titulo":"%s","descripcion":"Contenido","fechaCreacion":"2000-01-01T00:00:00Z"}
                """.formatted(idImpuesto, tituloUnico()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(idImpuesto.toString())))
            .andExpect(jsonPath("$.fechaCreacion").value(org.hamcrest.Matchers.not("2000-01-01T00:00:00Z")));
    }

    @Test
    @DisplayName("Sin título no se publica el anuncio")
    void tituloObligatorio() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);

        publicar(token, "{\"descripcion\":\"Solo contenido\"}")
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

        publicar(token, "{\"titulo\":\"Solo título\"}")
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

    // ---------- HU-17: consultar ----------

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
}