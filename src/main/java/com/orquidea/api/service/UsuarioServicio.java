package com.orquidea.api.service;

import com.orquidea.api.model.Rol;
import com.orquidea.api.model.Usuario;
import com.orquidea.api.repository.UsuarioRepositorio;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioServicio {

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder codificadorContrasenas;

    /**
     * Crea el primer SUPERADMINISTRADOR si todavía no existe ninguno.
     * No modifica cuentas existentes, así que es seguro ejecutarlo en cada arranque.
     */
    @Transactional
    public void asegurarSuperadministradorInicial(String nombreCompleto, String correo, String contrasena) {
        if (usuarioRepositorio.existsByRol(Rol.SUPERADMINISTRADOR)) {
            return;
        }
        String correoNormalizado = AutenticacionServicio.normalizarCorreo(correo);
        if (usuarioRepositorio.existsByCorreo(correoNormalizado)) {
            log.warn("No se creó el superadministrador inicial: el correo {} ya pertenece a otro usuario", correoNormalizado);
            return;
        }
        usuarioRepositorio.save(Usuario.builder()
                .nombreCompleto(nombreCompleto)
                .correo(correoNormalizado)
                .contrasenaHash(codificadorContrasenas.encode(contrasena))
                .rol(Rol.SUPERADMINISTRADOR)
                .build());
        log.info("Superadministrador inicial creado: {}", correoNormalizado);
    }
}
