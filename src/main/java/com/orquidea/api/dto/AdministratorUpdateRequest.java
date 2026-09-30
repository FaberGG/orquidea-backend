package com.orquidea.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** HU-5, escenario 1: reemplaza los datos editables del administrador. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos editables de un administrador")
public class AdministratorUpdateRequest {

    public static final String MENSAJE_OBLIGATORIOS = "Debes completar todos los campos obligatorios.";

    @NotBlank(message = MENSAJE_OBLIGATORIOS)
    @Size(max = 100, message = "El nombre no puede superar {max} caracteres.")
    @Schema(description = "Nombre", example = "María")
    private String nombre;

    @NotBlank(message = MENSAJE_OBLIGATORIOS)
    @Size(max = 100, message = "El apellido no puede superar {max} caracteres.")
    @Schema(description = "Apellido", example = "Pérez")
    private String apellido;

    @NotBlank(message = MENSAJE_OBLIGATORIOS)
    @Email(message = "Ingresa un correo electrónico válido.")
    @Size(max = 254, message = "El correo no puede superar {max} caracteres.")
    @Schema(description = "Correo; único en la plataforma", example = "admin@orquidea.local")
    private String correo;

    @Size(max = 20, message = "El teléfono no puede superar {max} caracteres.")
    @Schema(description = "Teléfono de contacto (opcional)", example = "3001234567", nullable = true)
    private String telefono;

    @NotNull(message = MENSAJE_OBLIGATORIOS)
    @Schema(description = "false inhabilita la cuenta: no podrá iniciar sesión", example = "true")
    private Boolean habilitado;
}