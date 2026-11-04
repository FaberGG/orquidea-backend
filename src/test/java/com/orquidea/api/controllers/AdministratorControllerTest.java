package com.orquidea.api.controllers;
import com.jayway.jsonpath.JsonPath;
import com.orquidea.api.IntegrationTestBase;
import com.orquidea.api.model.Role;
import com.orquidea.api.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdministratorControllerTest extends IntegrationTestBase {

    private static final String RUTA_ADMINISTRADORES = "/api/administradores";

    @Test
    @DisplayName("El superadministrador lista administradores y superadministradores, sin datos sensibles")
    void listarAdministradores() throws Exception {
        String correoAdmin = crearUsuario(Role.ADMINISTRADOR, "Clave-Admin-1", true);
        String correoInhabilitado = crearUsuario(Role.ADMINISTRADOR, "Clave-Admin-2", false);
        String correoRegistrado = crearUsuario(Role.USUARIO_REGISTRADO, "Clave-Usuario-1", true);
        String token = obtenerToken(CORREO_SUPERADMIN, CONTRASENA_SUPERADMIN);

        mockMvc.perform(get(RUTA_ADMINISTRADORES).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].correo", hasItem(CORREO_SUPERADMIN)))
            .andExpect(jsonPath("$[*].correo", hasItem(correoAdmin)))
            .andExpect(jsonPath("$[*].correo", hasItem(correoInhabilitado)))
            .andExpect(jsonPath("$[*].correo", not(hasItem(correoRegistrado))))
            .andExpect(jsonPath("$[?(@.rol == 'USUARIO_REGISTRADO')]").isEmpty())
            .andExpect(jsonPath("$[0].contrasenaHash").doesNotExist());
    }

    @Test
    @DisplayName("El listado sale en orden alfabético por nombre y apellido, sin importar mayúsculas ni tildes")
    void listadoEnOrdenAlfabetico() throws Exception {
        String zoe = crearAdministrador("Zoe", "Ríos");
        String andresZapata = crearAdministrador("andrés", "Zapata");
        String andresAlvarez = crearAdministrador("andrés", "álvarez");
        String alvaro = crearAdministrador("Álvaro", "Muñoz");
        String token = obtenerToken(CORREO_SUPERADMIN, CONTRASENA_SUPERADMIN);

        String listado = mockMvc.perform(get(RUTA_ADMINISTRADORES).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        List<String> correos = JsonPath.read(listado, "$[*].correo");
        assertThat(correos).containsSubsequence(alvaro, andresAlvarez, andresZapata, zoe);
    }

    @Test
    @DisplayName("Un administrador no puede listar administradores")
    void administradorNoPuedeListar() throws Exception {
        String token = tokenDeUsuarioNuevo(Role.ADMINISTRADOR);

        mockMvc.perform(get(RUTA_ADMINISTRADORES).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.mensaje").value("No tiene permisos para realizar esta acción."));
    }

    @Test
    @DisplayName("Sin sesión no se puede listar administradores")
    void sinSesionNoPuedeListar() throws Exception {
        mockMvc.perform(get(RUTA_ADMINISTRADORES))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.mensaje").value("Debe iniciar sesión para acceder a este recurso."));
    }

    /** Inhabilitado, para no ocupar cupo del límite de administradores habilitados en otras pruebas. */
    private String crearAdministrador(String nombre, String apellido) {
        String correo = "administrador-" + UUID.randomUUID() + "@prueba.local";
        userRepository.save(User.builder()
                .nombre(nombre)
                .apellido(apellido)
                .correo(correo)
                .contrasenaHash(codificadorContrasenas.encode("Clave-Admin-3"))
                .rol(Role.ADMINISTRADOR)
                .habilitado(false)
                .build());
        return correo;
    }
}
