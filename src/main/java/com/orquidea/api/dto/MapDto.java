package com.orquidea.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Todo lo que necesita el mapa del sendero en una sola respuesta, versionada para guardarla sin conexión")
public class MapDto {

    @Schema(description = "Huella del contenido: cambia cuando cambia cualquier dato del mapa (tramos, estaciones, "
            + "sus fotos o sus fichas). Coincide con la cabecera ETag", example = "9f2c4a7e1b3d5f60")
    private String version;

    @Schema(description = "Rectángulo que contiene los tramos y las estaciones, como [minLon, minLat, maxLon, maxLat]; "
            + "nulo si todavía no hay tramos ni estaciones", example = "[-76.6121, 2.459, -76.6102, 2.4611]")
    private List<Double> limites;

    @Schema(description = "Trazado del recorrido, ordenado por 'orden'")
    private List<TrailSegmentDto> tramos;

    @Schema(description = "Estaciones del recorrido, ordenadas por nombre, con su información y sus fichas")
    private List<StationDto> estaciones;
}
