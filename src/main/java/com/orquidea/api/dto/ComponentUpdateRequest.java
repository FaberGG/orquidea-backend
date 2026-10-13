package com.orquidea.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** HU-15: texto editable de un componente. Las fotos se gestionan con sus propios endpoints. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Texto editable de un componente del humedal")
public class ComponentUpdateRequest {

    public static final String MENSAJE_OBLIGATORIOS = "Debes completar todos los campos obligatorios.";

    @NotBlank(message = MENSAJE_OBLIGATORIOS)
    @Size(max = 100, message = "El nombre no puede superar {max} caracteres.")
    @Schema(description = "Nombre para mostrar", example = "Aves")
    private String nombre;

    @NotBlank(message = MENSAJE_OBLIGATORIOS)
    @Size(max = 5000, message = "La descripción no puede superar {max} caracteres.")
    @Schema(description = "Descripción del componente", example = "El humedal alberga especies residentes y migratorias...")
    private String descripcion;
}
