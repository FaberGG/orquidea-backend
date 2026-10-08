package com.orquidea.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos necesarios para publicar un anuncio.")
public class AnnouncementRequest {
    public static final String MENSAJE_OBLIGATORIOS_T = "Debes completar el título del anuncio.";
    public static final String MENSAJE_OBLIGATORIOS_C = "Debes completar el contenido del anuncio.";

    @NotBlank(message = MENSAJE_OBLIGATORIOS_T)
    @Size(max = 150, message = "El título no puede superar {max} caracteres.")
    @Schema(description = "Título del anuncio", example = "Protejamos el humedal")
    private String titulo;

    @NotBlank(message = MENSAJE_OBLIGATORIOS_C)
    @Size(max = 1000, message = "El contenido no puede superar {max} caracteres.")
    @Schema(description = "Contenido del anuncio", example = "El humedal es fundamental para la biodiversidad.")
    private String descripcion;
    
}
