package com.orquidea.api.service;

import com.orquidea.api.dto.RespuestaInicioSesion;
import com.orquidea.api.dto.SolicitudInicioSesion;
import com.orquidea.api.dto.UsuarioAutenticadoDto;
import com.orquidea.api.exception.CredencialesInvalidasExcepcion;
import com.orquidea.api.mapper.UsuarioMapper;
import com.orquidea.api.model.Usuario;
import com.orquidea.api.repository.UsuarioRepositorio;
import com.orquidea.api.security.JwtServicio;
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
public class AutenticacionServicio {

    private static final String TIPO_TOKEN = "Bearer";

    private final UsuarioRepositorio usuarioRepositorio;
    private final PasswordEncoder codificadorContrasenas;
    private final JwtServicio jwtServicio;
    private final UsuarioMapper usuarioMapper;

    public RespuestaInicioSesion iniciarSesion(SolicitudInicioSesion solicitud) {
        Usuario usuario = usuarioRepositorio.findByCorreo(normalizarCorreo(solicitud.getCorreo()))
                .filter(Usuario::isHabilitado)
                .filter(u -> codificadorContrasenas.matches(solicitud.getContrasena(), u.getContrasenaHash()))
                .orElseThrow(CredencialesInvalidasExcepcion::new);

        return RespuestaInicioSesion.builder()
                .token(jwtServicio.generarToken(usuario))
                .tipo(TIPO_TOKEN)
                .expiraEnSegundos(jwtServicio.getExpiracionEnSegundos())
                .usuario(usuarioMapper.aUsuarioAutenticadoDto(usuario))
                .build();
    }

    /**
     * Datos actualizados del usuario dueño del token. Si la cuenta fue eliminada o deshabilitada
     * después de emitir el token, se trata como sesión no válida.
     */
    public UsuarioAutenticadoDto obtenerUsuarioActual(UUID idUsuario) {
        return usuarioRepositorio.findById(idUsuario)
                .filter(Usuario::isHabilitado)
                .map(usuarioMapper::aUsuarioAutenticadoDto)
                .orElseThrow(() -> new InsufficientAuthenticationException("Sesión no válida"));
    }

    static String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
