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

    private static final String NOMBRE_POR_DEFECTO = "Super";
    private static final String APELLIDO_POR_DEFECTO = "Administrador";

    private final PropiedadesSuperadministrador propiedades;
    private final UsuarioServicio usuarioServicio;

    @Override
    public void run(ApplicationArguments args) {
        if (!propiedades.estaConfigurado()) {
            log.warn("SUPERADMIN_CORREO / SUPERADMIN_CONTRASENA no configurados: no se creará el superadministrador inicial");
            return;
        }
        usuarioServicio.asegurarSuperadministradorInicial(
                valorODefecto(propiedades.nombre(), NOMBRE_POR_DEFECTO),
                valorODefecto(propiedades.apellido(), APELLIDO_POR_DEFECTO),
                propiedades.correo(),
                propiedades.contrasena());
    }

    private static String valorODefecto(String valor, String porDefecto) {
        return valor == null || valor.isBlank() ? porDefecto : valor.trim();
    }
}
