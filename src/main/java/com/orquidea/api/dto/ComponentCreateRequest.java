package com.orquidea.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** HU-25: datos de un componente nuevo. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para crear un componente del humedal")
public class ComponentCreateRequest {

    public static final String MENSAJE_OBLIGATORIOS = "Debes completar el nombre y la descripción del componente.";

    @NotBlank(message = MENSAJE_OBLIGATORIOS)
    @Size(max = 100, message = "El nombre no puede superar {max} caracteres.")
    @Schema(description = "Nombre para mostrar; a partir de él se genera el código del componente",
        example = "Componente hídrico")
    private String nombre;

    @NotBlank(message = MENSAJE_OBLIGATORIOS)
    @Size(max = 5000, message = "La descripción no puede superar {max} caracteres.")
    @Schema(description = "Descripción del componente", example = "El humedal se alimenta de nacimientos de agua y escorrentía...")
    private String descripcion;
}
