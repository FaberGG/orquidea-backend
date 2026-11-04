package com.orquidea.api.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tramo del trazado del recorrido")
public class TrailSegmentDto {

    @Schema(description = "Identificador del tramo", example = "0b6f5f0e-6a43-5d52-9d3a-6c0f6f4c8e11")
    private UUID id;

    @Schema(description = "Nombre del tramo", example = "Sendero principal")
    private String nombre;

    @Schema(description = "Posición del tramo dentro del recorrido; se dibujan de menor a mayor", example = "1")
    private int orden;

    /** El texto ya es JSON válido (lo exige la base de datos), así que se escribe como objeto, sin comillas. */
    @JsonRawValue
    @Schema(type = "object", description = "Geometría GeoJSON LineString con coordenadas [longitud, latitud]",
            example = "{\"type\":\"LineString\",\"coordinates\":[[-76.6115,2.4598],[-76.6110,2.4602]]}")
    private String geometria;
}
