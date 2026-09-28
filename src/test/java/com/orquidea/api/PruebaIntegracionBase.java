package com.orquidea.api;

import com.jayway.jsonpath.JsonPath;
import com.orquidea.api.model.Rol;
import com.orquidea.api.model.Usuario;
import com.orquidea.api.repository.UsuarioRepositorio;
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
        "app.superadministrador.nombre=Super Pruebas",
        "app.superadministrador.correo=" + PruebaIntegracionBase.CORREO_SUPERADMIN,
        "app.superadministrador.contrasena=" + PruebaIntegracionBase.CONTRASENA_SUPERADMIN
})
@AutoConfigureMockMvc
@Import(ContenedoresPrueba.class)
public abstract class PruebaIntegracionBase {

    protected static final String CORREO_SUPERADMIN = "super@prueba.local";
    protected static final String CONTRASENA_SUPERADMIN = "Clave-Prueba-123";
    protected static final String RUTA_INICIAR_SESION = "/api/autenticacion/iniciar-sesion";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UsuarioRepositorio usuarioRepositorio;

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
    protected String crearUsuario(Rol rol, String contrasena, boolean habilitado) {
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

    /** Crea un usuario habilitado con ese rol e inicia sesión con él. */
    protected String tokenDeUsuarioNuevo(Rol rol) throws Exception {
        String contrasena = "Clave-" + rol.name();
        return obtenerToken(crearUsuario(rol, contrasena, true), contrasena);
    }
}
