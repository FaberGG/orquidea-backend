package com.orquidea.api;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Base de las pruebas de integración: levanta un PostgreSQL real con Testcontainers (requiere Docker).
 * Todas las subclases comparten el mismo contexto de Spring y el mismo contenedor.
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
}
