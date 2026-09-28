package com.orquidea.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Credenciales para iniciar sesión")
public class SolicitudInicioSesion {

    // Sin @Email a propósito: un correo mal formado se responde como credenciales incorrectas (HU-1, escenario 2).
    @NotBlank(message = "Ambos campos son obligatorios.")
    @Schema(description = "Correo del usuario", example = "admin@orquidea.local")
    private String correo;

    @NotBlank(message = "Ambos campos son obligatorios.")
    @Schema(description = "Contraseña del usuario", example = "MiClaveSegura123")
    private String contrasena;
}
