package com.orquidea.api.controllers;

import com.orquidea.api.dto.RespuestaError;
import com.orquidea.api.dto.RespuestaInicioSesion;
import com.orquidea.api.dto.SolicitudInicioSesion;
import com.orquidea.api.dto.UsuarioAutenticadoDto;
import com.orquidea.api.security.UsuarioToken;
import com.orquidea.api.service.AutenticacionServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/autenticacion")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio de sesión y datos de la sesión actual (HU-1)")
public class AutenticacionControlador {

    private final AutenticacionServicio autenticacionServicio;

    @PostMapping("/iniciar-sesion")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión",
            description = "Valida correo y contraseña y devuelve un JWT junto con el rol del usuario.")
    @ApiResponse(responseCode = "200", description = "Ingreso exitoso")
    @ApiResponse(responseCode = "400", description = "Ambos campos son obligatorios.",
            content = @Content(schema = @Schema(implementation = RespuestaError.class)))
    @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos.",
            content = @Content(schema = @Schema(implementation = RespuestaError.class)))
    public RespuestaInicioSesion iniciarSesion(@Valid @RequestBody SolicitudInicioSesion solicitud) {
        return autenticacionServicio.iniciarSesion(solicitud);
    }

    @GetMapping("/yo")
    @Operation(summary = "Usuario de la sesión actual",
            description = "Devuelve los datos y el rol del dueño del token; útil para restaurar la sesión en el cliente.")
    @ApiResponse(responseCode = "200", description = "Usuario autenticado")
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido",
            content = @Content(schema = @Schema(implementation = RespuestaError.class)))
    public UsuarioAutenticadoDto obtenerUsuarioActual(@AuthenticationPrincipal UsuarioToken usuarioToken) {
        return autenticacionServicio.obtenerUsuarioActual(usuarioToken.id());
    }
}
