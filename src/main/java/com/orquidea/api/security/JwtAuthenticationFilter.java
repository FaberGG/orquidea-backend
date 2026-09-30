package com.orquidea.api.security;

import com.orquidea.api.model.User;
import com.orquidea.api.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Autentica la petición si trae un "Authorization: Bearer <token>" válido.
 * El token solo identifica al usuario: el rol y si está habilitado se leen de la base en cada petición,
 * para que revocar o inhabilitar una cuenta (HU-5) tenga efecto de inmediato y no cuando venza el token.
 * Si el token falta, es inválido o la cuenta ya no está habilitada, no corta la cadena: las rutas protegidas
 * responderán 401.
 * No es un @Component para que Spring Boot no lo registre también como filtro de servlet.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String encabezado = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (encabezado != null && encabezado.startsWith(PREFIJO_BEARER)) {
            jwtService.validarToken(encabezado.substring(PREFIJO_BEARER.length()).trim())
                    .flatMap(token -> userRepository.findById(token.id()))
                    .filter(User::isHabilitado)
                    .map(actual -> new JwtPrincipal(actual.getId(), actual.getCorreo(), actual.getRol()))
                    .ifPresent(usuario -> {
                        var autenticacion = new UsernamePasswordAuthenticationToken(
                                usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name())));
                        SecurityContextHolder.getContext().setAuthentication(autenticacion);
                    });
        }
        chain.doFilter(request, response);
    }
}
