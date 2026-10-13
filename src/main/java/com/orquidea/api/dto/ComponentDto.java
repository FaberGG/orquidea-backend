package com.orquidea.api.dto;

import com.orquidea.api.model.ComponentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Componente del humedal con su contenido informativo")
public class ComponentDto {

    @Schema(description = "Componente; identifica el recurso en la ruta", example = "AVES")
    private ComponentType tipo;

    @Schema(description = "Nombre para mostrar", example = "Aves")
    private String nombre;

    @Schema(description = "Descripción; nula si aún no hay información", example = "El humedal alberga especies residentes y migratorias...")
    private String descripcion;

    @Schema(description = "false si no hay descripción ni fotos: el cliente muestra 'La información de esta sección estará disponible próximamente.'",
            example = "true")
    private boolean tieneContenido;

    @Schema(description = "Fotos en el orden en que se subieron (vacío si no hay)")
    private List<ComponentPhotoDto> fotos;

    @Schema(description = "Fecha de la última modificación", example = "2026-09-28T15:30:00Z")
    private Instant fechaActualizacion;
}
