package com.orquidea.api.service;

import com.orquidea.api.model.Role;
import com.orquidea.api.model.User;
import com.orquidea.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder codificadorContrasenas;

    /**
     * Crea el primer SUPERADMINISTRADOR si todavía no existe ninguno.
     * No modifica cuentas existentes, así que es seguro ejecutarlo en cada arranque.
     */
    @Transactional
    public void asegurarSuperadministradorInicial(String nombre, String apellido, String correo, String contrasena) {
        if (userRepository.existsByRol(Role.SUPERADMINISTRADOR)) {
            return;
        }
        String correoNormalizado = AuthService.normalizarCorreo(correo);
        if (userRepository.existsByCorreo(correoNormalizado)) {
            log.warn("No se creó el superadministrador inicial: el correo {} ya pertenece a otro usuario", correoNormalizado);
            return;
        }
        userRepository.save(User.builder()
                .nombre(nombre)
                .apellido(apellido)
                .correo(correoNormalizado)
                .contrasenaHash(codificadorContrasenas.encode(contrasena))
                .rol(Role.SUPERADMINISTRADOR)
                .build());
        log.info("Superadministrador inicial creado: {}", correoNormalizado);
    }
}
