package com.orquidea.api.dto;

import com.orquidea.api.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Administrador de la plataforma")
public class AdministratorDto {

    @Schema(description = "Identificador del usuario", example = "3f6c1a52-7a1e-4c5b-9c1d-2b8f0e4a9d10")
    private UUID id;

    @Schema(description = "Nombre", example = "María")
    private String nombre;

    @Schema(description = "Apellido", example = "Pérez")
    private String apellido;

    @Schema(description = "Correo", example = "admin@orquidea.local")
    private String correo;

    
    @Schema(description = "Rol actual; tras revocar el acceso pasa a USUARIO_REGISTRADO", example = "ADMINISTRADOR")
    private Role rol;

    @Schema(description = "Si la cuenta puede iniciar sesión", example = "true")
    private boolean habilitado;

    @Schema(description = "Fecha de creación de la cuenta", example = "2026-09-28T15:30:00Z")
    private Instant fechaCreacion;
}