package com.orquidea.api.security;

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
 * Si el token falta o es inválido no corta la cadena: las rutas protegidas responderán 401.
 * No es un @Component para que Spring Boot no lo registre también como filtro de servlet.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String encabezado = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (encabezado != null && encabezado.startsWith(PREFIJO_BEARER)) {
            jwtService.validarToken(encabezado.substring(PREFIJO_BEARER.length()).trim())
                    .ifPresent(usuario -> {
                        var autenticacion = new UsernamePasswordAuthenticationToken(
                                usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol().name())));
                        SecurityContextHolder.getContext().setAuthentication(autenticacion);
                    });
        }
        chain.doFilter(request, response);
    }
}
