package com.orquidea.api.dto;

import java.time.Instant;

import com.orquidea.api.model.Prioridad;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    public static final String MENSAJE_OBLIGATORIOS_P = "Debes seleccionar la prioridad del anuncio.";
    public static final String MENSAJE_FECHA_INVALIDA = "La fecha de finalización debe ser posterior a la fecha actual.";

    @NotBlank(message = MENSAJE_OBLIGATORIOS_T)
    @Size(max = 150, message = "El título no puede superar {max} caracteres.")
    @Schema(description = "Título del anuncio", example = "Protejamos el humedal")
    private String titulo;

    @NotBlank(message = MENSAJE_OBLIGATORIOS_C)
    @Size(max = 1000, message = "El contenido no puede superar {max} caracteres.")
    @Schema(description = "Contenido del anuncio", example = "El humedal es fundamental para la biodiversidad.")
    private String descripcion;

    @NotNull(message = MENSAJE_OBLIGATORIOS_P)
    @Schema(description = "Prioridad del anuncio", example = "ALTA")
    private Prioridad prioridad;
    
    @Future(message = MENSAJE_FECHA_INVALIDA)
    @Schema(description = "Fecha de finalización del anuncio en UTC", example = "2023-12-31T23:59:59Z")
    private Instant fechaExpiracion;
    
}
