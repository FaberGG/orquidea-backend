package com.orquidea.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estación del recorrido en el mapa")
public class StationDto {

    @Schema(description = "Identificador de la estación", example = "3c9a7e52-8b1d-4f6a-9e20-5d4c3b2a1f0e")
    private UUID id;

    @Schema(description = "Nombre de la estación", example = "Mirador de la laguna")
    private String nombre;

    @Schema(description = "Descripción de la estación", example = "Desde aquí se observan garzas y patos")
    private String descripcion;

    @Schema(description = "URL pública de la foto", example = "http://localhost:9000/orquidea/estaciones/3c9a7e52.jpg")
    private String urlFoto;

    @Schema(description = "Latitud (DwC: decimalLatitude)", example = "2.459800")
    private BigDecimal decimalLatitude;

    @Schema(description = "Longitud (DwC: decimalLongitude)", example = "-76.611500")
    private BigDecimal decimalLongitude;

    @Schema(description = "Fichas taxonómicas asociadas, por nombre común; lista vacía si no tiene")
    private List<StationTaxonDto> fichas;
}
