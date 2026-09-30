package com.orquidea.api.service;

import com.orquidea.api.dto.LoginResponse;
import com.orquidea.api.dto.RegisterResponse;
import com.orquidea.api.dto.LoginRequest;
import com.orquidea.api.dto.RegisterRequest;
import com.orquidea.api.dto.AuthenticatedUserDto;
import com.orquidea.api.exception.InvalidCredentialsException;
import com.orquidea.api.exception.DuplicateResourceException;
import com.orquidea.api.mapper.UserMapper;
import com.orquidea.api.model.Role;
import com.orquidea.api.model.User;
import com.orquidea.api.repository.UserRepository;
import com.orquidea.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String TIPO_TOKEN = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder codificadorContrasenas;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public LoginResponse iniciarSesion(LoginRequest solicitud) {
        User usuario = userRepository.findByCorreo(normalizarCorreo(solicitud.getCorreo()))
                .filter(User::isHabilitado)
                .filter(u -> codificadorContrasenas.matches(solicitud.getContrasena(), u.getContrasenaHash()))
                .orElseThrow(InvalidCredentialsException::new);

        return LoginResponse.builder()
                .token(jwtService.generarToken(usuario))
                .tipo(TIPO_TOKEN)
                .expiraEnSegundos(jwtService.getExpiracionEnSegundos())
                .usuario(userMapper.aUsuarioAutenticadoDto(usuario))
                .build();
    }

    /**
     * Datos actualizados del usuario dueño del token. Si la cuenta fue eliminada o deshabilitada
     * después de emitir el token, se trata como sesión no válida.
     */
    public AuthenticatedUserDto obtenerUsuarioActual(UUID idUsuario) {
        return userRepository.findById(idUsuario)
                .filter(User::isHabilitado)
                .map(userMapper::aUsuarioAutenticadoDto)
                .orElseThrow(() -> new InsufficientAuthenticationException("Sesión no válida"));
    }

    static String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }


    @Transactional
    public RegisterResponse registrarUsuario(RegisterRequest solicitud) {

        String correo = normalizarCorreo(solicitud.getCorreo());

        if (userRepository.existsByCorreo(correo)) {
            throw new DuplicateResourceException(
                    "El correo electrónico ya está registrado.");
        }

        User usuario = User.builder()
                .nombre(solicitud.getNombre().trim())
                .apellido(solicitud.getApellido().trim())
                .correo(correo)
                .contrasenaHash(
                        codificadorContrasenas.encode(solicitud.getContrasena()))
                .rol(Role.USUARIO_REGISTRADO)
                .habilitado(true)
                .build();

        User usuarioGuardado = userRepository.save(usuario);

        RegisterResponse respuesta = userMapper.aRespuestaRegistro(usuarioGuardado);

        return respuesta;
    }
}
