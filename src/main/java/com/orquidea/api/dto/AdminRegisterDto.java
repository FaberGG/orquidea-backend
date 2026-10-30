package com.orquidea.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@lombok.Getter
@lombok.Setter

@Schema (description = "Datos necesarios para registrar un nuevo usuario administrador.")
public class AdminRegisterDto {

    public static final String MENSAJE_CONTRASENA_INSEGURA =
            "La contraseña debe tener mínimo 8 caracteres e incluir al menos una mayúscula, una minúscula y un número.";
    
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

    @NotNull(message = "La contraseña es obligatoria.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", message = MENSAJE_CONTRASENA_INSEGURA)
    @Schema(description = "Mínimo 8 caracteres, con al menos una mayúscula, una minúscula y un número.",
            example = "Clave2026")
    private String contrasena;
    

}
