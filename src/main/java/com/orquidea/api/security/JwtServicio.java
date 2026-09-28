package com.orquidea.api.security;

import com.orquidea.api.model.Rol;
import com.orquidea.api.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtServicio {

    private static final int LONGITUD_MINIMA_SECRETO = 32;
    private static final String CLAIM_CORREO = "correo";
    private static final String CLAIM_ROL = "rol";

    private final SecretKey clave;
    private final Duration expiracion;

    public JwtServicio(PropiedadesJwt propiedades) {
        String secreto = propiedades.secreto();
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < LONGITUD_MINIMA_SECRETO) {
            throw new IllegalStateException(
                    "app.jwt.secreto (variable JWT_SECRETO) debe tener al menos " + LONGITUD_MINIMA_SECRETO + " caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracion = propiedades.expiracion() != null ? propiedades.expiracion() : Duration.ofHours(8);
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim(CLAIM_CORREO, usuario.getCorreo())
                .claim(CLAIM_ROL, usuario.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracion)))
                .signWith(clave)
                .compact();
    }

    /**
     * @return la identidad del token, o vacío si es inválido, está vencido o fue alterado
     */
    public Optional<UsuarioToken> validarToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
            return Optional.of(new UsuarioToken(
                    UUID.fromString(claims.getSubject()),
                    claims.get(CLAIM_CORREO, String.class),
                    Rol.valueOf(claims.get(CLAIM_ROL, String.class))));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    public long getExpiracionEnSegundos() {
        return expiracion.toSeconds();
    }
}
