package com.orquidea.api.controllers;

import com.orquidea.api.IntegrationTestBase;
import com.orquidea.api.model.Role;
import com.orquidea.api.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

class AuthControllerTest extends IntegrationTestBase {

    private static final String RUTA_YO = "/api/autenticacion/yo";
    private static final String MENSAJE_CREDENCIALES = "Correo o contraseña incorrectos.";
    private static final String MENSAJE_CAMPOS = "Ambos campos son obligatorios.";
    private static final String RUTA_REGISTRO = "/api/autenticacion/registro";

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
                .andExpect(jsonPath("$.usuario.nombre").value("Super"))
                .andExpect(jsonPath("$.usuario.apellido").value("Pruebas"))
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
        String correo = crearUsuario(Role.ADMINISTRADOR, "Clave-Admin-1", true);

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
        String correo = crearUsuario(Role.USUARIO_REGISTRADO, "Clave-Usuario-1", false);

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
        String correo = crearUsuario(Role.ADMINISTRADOR, "Clave-Admin-2", true);
        String token = obtenerToken(correo, "Clave-Admin-2");
        User usuario = userRepository.findByCorreo(correo).orElseThrow();
        usuario.setHabilitado(false);
        userRepository.save(usuario);

        mockMvc.perform(get(RUTA_YO).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    // Utilidades

    private void esperarCredencialesIncorrectas(ResultActions resultado) throws Exception {
        resultado.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_CREDENCIALES))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    private ResultActions registrar(String nombre, String apellido, String correo, String contrasena) throws Exception {
    String cuerpo = """
            {"nombre":"%s","apellido":"%s","correo":"%s","contrasena":"%s"}
            """.formatted(nombre, apellido, correo, contrasena);
    return mockMvc.perform(post(RUTA_REGISTRO).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
}

    // HU-2: registro de usuario

    @Test
    @DisplayName("Escenario 1: registro exitoso crea el usuario con rol USUARIO_REGISTRADO")
    void registroExitoso() throws Exception {
        String correo = "nuevo-" + UUID.randomUUID() + "@prueba.local";

        registrar("Ana", "Gómez", correo, "Clave-Nueva-123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(emptyOrNullString())))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.apellido").value("Gómez"))
                .andExpect(jsonPath("$.correo").value(correo))
                .andExpect(jsonPath("$.rol").value("USUARIO_REGISTRADO"))
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());
    }

    @Test
    @DisplayName("Un usuario recién registrado puede iniciar sesión con las mismas credenciales")
    void registroExitosoPermiteIniciarSesionDespues() throws Exception {
        String correo = "nuevo-" + UUID.randomUUID() + "@prueba.local";
        registrar("Luis", "Ramírez", correo, "Clave-Nueva-123")
                .andExpect(status().isCreated());

        iniciarSesion(correo, "Clave-Nueva-123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("USUARIO_REGISTRADO"));
    }

    @Test
    @DisplayName("Escenario 2: el correo ya registrado responde 409")
    void correoYaRegistrado() throws Exception {
        String correo = crearUsuario(Role.USUARIO_REGISTRADO, "Clave-Existente", true);

        registrar("Otra", "Persona", correo, "Otra-Clave-123")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje").value("El correo electrónico ya está registrado."))
                .andExpect(jsonPath("$.ruta").value(RUTA_REGISTRO));
    }

    // Escenario 3: campos obligatorios incompletos

    @Test
    @DisplayName("Escenario 3: nombre ausente")
    void nombreObligatorio() throws Exception {
        String cuerpo = """
            {"apellido":"Pérez","correo":"valido1-%s@prueba.local","contrasena":"Clave-123"}
            """.formatted(UUID.randomUUID());
        mockMvc.perform(post(RUTA_REGISTRO).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El nombre es obligatorio."));
    }

    @Test
    @DisplayName("Escenario 3: apellido ausente")
    void apellidoObligatorio() throws Exception {
        String cuerpo = """
            {"nombre":"Juan","correo":"valido2-%s@prueba.local","contrasena":"Clave-123"}
            """.formatted(UUID.randomUUID());
        mockMvc.perform(post(RUTA_REGISTRO).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El apellido es obligatorio."));
    }

    @Test
    @DisplayName("Escenario 3: correo ausente")
    void correoObligatorio() throws Exception {
        String cuerpo = """
            {"nombre":"Juan","apellido":"Pérez","contrasena":"Clave-123"}
            """;
        mockMvc.perform(post(RUTA_REGISTRO).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El correo electrónico es obligatorio."));
    }

    @Test
    @DisplayName("Escenario 3: contraseña vacía")
    void contrasenaObligatoria() throws Exception {
        registrar("Juan", "Pérez", "valido3-" + UUID.randomUUID() + "@prueba.local", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La contraseña es obligatoria."));
    }

    @Test
    @DisplayName("Escenario 4: correo con formato inválido")
    void correoConFormatoInvalido() throws Exception {
        registrar("Juan", "Pérez", "no-es-un-correo", "Clave-123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Ingresa un correo electrónico válido."));
    }
    
}
