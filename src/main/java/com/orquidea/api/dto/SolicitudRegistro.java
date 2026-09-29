package com.orquidea.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Datos necesarios para registrar un nuevo usuario.")
public class SolicitudRegistro {

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(min = 2, message = "El nombre debe tener al menos 2 caracteres.")
    @Schema(
            description = "Nombre del usuario.",
            example = "Santiago",
            minLength = 2
    )
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio.")
    @Size(min = 2, message = "El apellido debe tener al menos 2 caracteres.")
    @Schema(
            description = "Apellido del usuario.",
            example = "Bastidas",
            minLength = 2
    )
    private String apellido;

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "Ingresa un correo electrónico válido.")
    @Schema(
            description = "Correo electrónico que identificará al usuario y será utilizado para iniciar sesión.",
            example = "santiago.bastidas@gmail.com"
    )
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria.")
    @Schema(
            description = "Contraseña que utilizará el usuario para iniciar sesión.",
            example = "123456"
    )
    private String contrasena;
}