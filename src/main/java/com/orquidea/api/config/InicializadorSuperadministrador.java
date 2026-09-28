package com.orquidea.api.config;

import com.orquidea.api.service.UsuarioServicio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InicializadorSuperadministrador implements ApplicationRunner {

    private static final String NOMBRE_POR_DEFECTO = "Superadministrador";

    private final PropiedadesSuperadministrador propiedades;
    private final UsuarioServicio usuarioServicio;

    @Override
    public void run(ApplicationArguments args) {
        if (!propiedades.estaConfigurado()) {
            log.warn("SUPERADMIN_CORREO / SUPERADMIN_CONTRASENA no configurados: no se creará el superadministrador inicial");
            return;
        }
        String nombre = propiedades.nombre() == null || propiedades.nombre().isBlank()
                ? NOMBRE_POR_DEFECTO
                : propiedades.nombre();
        usuarioServicio.asegurarSuperadministradorInicial(nombre, propiedades.correo(), propiedades.contrasena());
    }
}
