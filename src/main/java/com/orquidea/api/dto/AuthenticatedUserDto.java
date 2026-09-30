package com.orquidea.api.dto;

import com.orquidea.api.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos básicos del usuario con sesión iniciada")
public class AuthenticatedUserDto {

    @Schema(description = "Identificador del usuario", example = "3f6c1a52-7a1e-4c5b-9c1d-2b8f0e4a9d10")
    private UUID id;

    @Schema(description = "Nombre", example = "María")
    private String nombre;

    @Schema(description = "Apellido", example = "Pérez")
    private String apellido;

    @Schema(description = "Correo", example = "admin@orquidea.local")
    private String correo;

    @Schema(description = "Rol del usuario; define las opciones habilitadas en la aplicación", example = "ADMINISTRADOR")
    private Role rol;
}
