package com.orquidea.api.controllers;

import com.jayway.jsonpath.JsonPath;
import com.orquidea.api.PruebaIntegracionBase;
import com.orquidea.api.model.Rol;
import com.orquidea.api.model.Usuario;
import com.orquidea.api.repository.UsuarioRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AutenticacionControladorTest extends PruebaIntegracionBase {

    private static final String RUTA_INICIAR_SESION = "/api/autenticacion/iniciar-sesion";
    private static final String RUTA_YO = "/api/autenticacion/yo";
    private static final String MENSAJE_CREDENCIALES = "Correo o contraseña incorrectos.";
    private static final String MENSAJE_CAMPOS = "Ambos campos son obligatorios.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private PasswordEncoder codificadorContrasenas;

    // Escenario 1: ingreso exitoso

    @Test
    @DisplayName("Escenario 1: credenciales correctas devuelven token y rol")
    void ingresoExitoso() throws Exception {
        iniciarSesion(CORREO_SUPERADMIN, CONTRASENA_SUPERADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnSegundos").value(3600))
                .andExpect(jsonPath("$.usuario.correo").value(CORREO_SUPERADMIN))
                .andExpect(jsonPath("$.usuario.nombreCompleto").value("Super Pruebas"))
                .andExpect(jsonPath("$.usuario.rol").value("SUPERADMINISTRADOR"))
                .andExpect(jsonPath("$.usuario.contrasenaHash").doesNotExist());
    }

    @Test
    @DisplayName("Escenario 1: el correo no distingue mayúsculas ni espacios alrededor")
    void ingresoExitosoConCorreoEnMayusculas() throws Exception {
        iniciarSesion("  SUPER@Prueba.Local ", CONTRASENA_SUPERADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.correo").value(CORREO_SUPERADMIN));
    }

    @Test
    @DisplayName("Escenario 1: cada usuario recibe su propio rol")
    void ingresoExitosoDeAdministrador() throws Exception {
        String correo = crearUsuario(Rol.ADMINISTRADOR, "Clave-Admin-1", true);

        iniciarSesion(correo, "Clave-Admin-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("ADMINISTRADOR"));
    }

    // Escenario 2: credenciales incorrectas

    @Test
    @DisplayName("Escenario 2: contraseña incorrecta")
    void contrasenaIncorrecta() throws Exception {
        esperarCredencialesIncorrectas(iniciarSesion(CORREO_SUPERADMIN, "otra-clave"));
    }

    @Test
    @DisplayName("Escenario 2: correo no registrado recibe el mismo mensaje")
    void correoNoRegistrado() throws Exception {
        esperarCredencialesIncorrectas(iniciarSesion("nadie@prueba.local", CONTRASENA_SUPERADMIN));
    }

    @Test
    @DisplayName("Escenario 2: cuenta deshabilitada recibe el mismo mensaje")
    void usuarioDeshabilitado() throws Exception {
        String correo = crearUsuario(Rol.USUARIO_REGISTRADO, "Clave-Usuario-1", false);

        esperarCredencialesIncorrectas(iniciarSesion(correo, "Clave-Usuario-1"));
    }

    // Escenario 3: campos obligatorios vacíos

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"correo\":\"\",\"contrasena\":\"\"}",
            "{\"correo\":\"   \",\"contrasena\":\"clave\"}",
            "{\"correo\":\"super@prueba.local\"}",
            "{\"contrasena\":\"clave\"}"
    })
    @DisplayName("Escenario 3: correo y/o contraseña vacíos")
    void camposObligatoriosVacios(String cuerpo) throws Exception {
        mockMvc.perform(post(RUTA_INICIAR_SESION).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_CAMPOS))
                .andExpect(jsonPath("$.ruta").value(RUTA_INICIAR_SESION));
    }

    @Test
    @DisplayName("Cuerpo que no es JSON válido")
    void cuerpoInvalido() throws Exception {
        mockMvc.perform(post(RUTA_INICIAR_SESION).contentType(MediaType.APPLICATION_JSON).content("{no-es-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El cuerpo de la solicitud no es válido."));
    }

    // Uso del token

    @Test
    @DisplayName("El token permite consultar el usuario de la sesión")
    void consultarUsuarioActualConToken() throws Exception {
        String token = obtenerToken(CORREO_SUPERADMIN, CONTRASENA_SUPERADMIN);

        mockMvc.perform(get(RUTA_YO).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(CORREO_SUPERADMIN))
                .andExpect(jsonPath("$.rol").value("SUPERADMINISTRADOR"));
    }

    @Test
    @DisplayName("Sin token, las rutas protegidas responden 401")
    void consultarUsuarioActualSinToken() throws Exception {
        mockMvc.perform(get(RUTA_YO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Debe iniciar sesión para acceder a este recurso."));
    }

    @Test
    @DisplayName("Un token alterado se rechaza")
    void consultarUsuarioActualConTokenAlterado() throws Exception {
        String token = obtenerToken(CORREO_SUPERADMIN, CONTRASENA_SUPERADMIN);
        String alterado = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        mockMvc.perform(get(RUTA_YO).header(HttpHeaders.AUTHORIZATION, "Bearer " + alterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un token de una cuenta deshabilitada después del ingreso deja de servir")
    void consultarUsuarioActualTrasDeshabilitar() throws Exception {
        String correo = crearUsuario(Rol.ADMINISTRADOR, "Clave-Admin-2", true);
        String token = obtenerToken(correo, "Clave-Admin-2");
        Usuario usuario = usuarioRepositorio.findByCorreo(correo).orElseThrow();
        usuario.setHabilitado(false);
        usuarioRepositorio.save(usuario);

        mockMvc.perform(get(RUTA_YO).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    // Utilidades

    private ResultActions iniciarSesion(String correo, String contrasena) throws Exception {
        String cuerpo = "{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena);
        return mockMvc.perform(post(RUTA_INICIAR_SESION).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private void esperarCredencialesIncorrectas(ResultActions resultado) throws Exception {
        resultado.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_CREDENCIALES))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    private String obtenerToken(String correo, String contrasena) throws Exception {
        String respuesta = iniciarSesion(correo, contrasena)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private String crearUsuario(Rol rol, String contrasena, boolean habilitado) {
        String correo = "usuario-" + UUID.randomUUID() + "@prueba.local";
        usuarioRepositorio.save(Usuario.builder()
                .nombreCompleto("Usuario de prueba")
                .correo(correo)
                .contrasenaHash(codificadorContrasenas.encode(contrasena))
                .rol(rol)
                .habilitado(habilitado)
                .build());
        return correo;
    }
}
