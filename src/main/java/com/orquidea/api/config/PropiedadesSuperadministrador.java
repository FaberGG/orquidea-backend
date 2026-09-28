package com.orquidea.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Datos del primer superadministrador (variables SUPERADMIN_*). Si faltan correo o contraseña, no se crea ninguno.
 */
@ConfigurationProperties(prefix = "app.superadministrador")
public record PropiedadesSuperadministrador(String nombre, String apellido, String correo, String contrasena) {

    public boolean estaConfigurado() {
        return correo != null && !correo.isBlank() && contrasena != null && !contrasena.isBlank();
    }
}
