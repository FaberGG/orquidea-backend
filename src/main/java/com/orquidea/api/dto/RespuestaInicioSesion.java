package com.orquidea.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resultado de un inicio de sesión exitoso")
public class RespuestaInicioSesion {

    @Schema(description = "JWT a enviar en el encabezado Authorization: Bearer <token>")
    private String token;

    @Schema(description = "Tipo de token", example = "Bearer")
    private String tipo;

    @Schema(description = "Segundos de validez del token", example = "28800")
    private long expiraEnSegundos;

    @Schema(description = "Usuario autenticado")
    private UsuarioAutenticadoDto usuario;
}
