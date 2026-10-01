package com.orquidea.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Código recibido por correo y nueva contraseña")
public class PasswordResetRequest {

    // El código no es único entre usuarios, por eso se pide también el correo.
    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Schema(description = "Correo de la cuenta a recuperar", example = "admin@orquidea.local")
    private String correo;

    @NotBlank(message = "El código es obligatorio.")
    @Pattern(regexp = "\\d{6}", message = "El código debe tener 6 dígitos.")
    @Schema(description = "Código de 6 dígitos enviado al correo", example = "482913")
    private String codigo;

    @NotBlank(message = "La nueva contraseña es obligatoria.")
    @Schema(description = "Nueva contraseña", example = "MiClaveNueva123")
    private String contrasena;
}
