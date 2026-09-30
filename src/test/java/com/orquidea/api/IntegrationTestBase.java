package com.orquidea.api;

import com.jayway.jsonpath.JsonPath;
import com.orquidea.api.model.Role;
import com.orquidea.api.model.User;
import com.orquidea.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de las pruebas de integración: levanta PostgreSQL y un almacenamiento S3 reales con Testcontainers
 * (requiere Docker). Todas las subclases comparten el mismo contexto de Spring y los mismos contenedores.
 */
@SpringBootTest(properties = {
        "app.jwt.secreto=secreto-de-pruebas-con-mas-de-32-caracteres",
        "app.jwt.expiracion=1h",
        "app.superadministrador.nombre=Super",
        "app.superadministrador.apellido=Pruebas",
        "app.superadministrador.correo=" + IntegrationTestBase.CORREO_SUPERADMIN,
        "app.superadministrador.contrasena=" + IntegrationTestBase.CONTRASENA_SUPERADMIN
})
@AutoConfigureMockMvc
@Import(TestContainersConfig.class)
public abstract class IntegrationTestBase {

    protected static final String CORREO_SUPERADMIN = "super@prueba.local";
    protected static final String CONTRASENA_SUPERADMIN = "Clave-Prueba-123";
    protected static final String RUTA_INICIAR_SESION = "/api/autenticacion/iniciar-sesion";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder codificadorContrasenas;

    protected ResultActions iniciarSesion(String correo, String contrasena) throws Exception {
        String cuerpo = "{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena);
        return mockMvc.perform(post(RUTA_INICIAR_SESION).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    protected String obtenerToken(String correo, String contrasena) throws Exception {
        String respuesta = iniciarSesion(correo, contrasena)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    /** @return el correo del usuario creado, único por llamada */
    protected String crearUsuario(Role rol, String contrasena, boolean habilitado) {
        String correo = "usuario-" + UUID.randomUUID() + "@prueba.local";
        userRepository.save(User.builder()
                .nombre("Usuario")
                .apellido("De Prueba")
                .correo(correo)
                .contrasenaHash(codificadorContrasenas.encode(contrasena))
                .rol(rol)
                .habilitado(habilitado)
                .build());
        return correo;
    }

    /** Crea un usuario habilitado con ese rol e inicia sesión con él. */
    protected String tokenDeUsuarioNuevo(Role rol) throws Exception {
        String contrasena = "Clave-" + rol.name();
        return obtenerToken(crearUsuario(rol, contrasena, true), contrasena);
    }
}
