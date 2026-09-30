package com.orquidea.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Formato uniforme de las respuestas de error")
public class ApiErrorResponse {

    @Schema(description = "Momento del error", example = "2026-09-28T15:30:00Z")
    private Instant fecha;

    @Schema(description = "Código HTTP", example = "401")
    private int estado;

    @Schema(description = "Nombre del código HTTP", example = "Unauthorized")
    private String error;

    @Schema(description = "Mensaje para mostrar al usuario", example = "Correo o contraseña incorrectos.")
    private String mensaje;

    @Schema(description = "Ruta solicitada", example = "/api/autenticacion/iniciar-sesion")
    private String ruta;
}
