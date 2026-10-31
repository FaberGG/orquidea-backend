package com.orquidea.api.dto;

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
@Schema(description = "Ficha taxonómica enlazada desde una estación")
public class StationTaxonDto {

    @Schema(description = "Identificador de la ficha", example = "5b0c7f0e-2f5d-4a8e-9a57-0c7d3c1b6e21")
    private UUID id;

    @Schema(description = "Nombre común (DwC: vernacularName)", example = "Garza real")
    private String vernacularName;

    @Schema(description = "URL pública de la foto principal de la ficha", example = "http://localhost:9000/orquidea/fichas/5b0c7f0e.jpg")
    private String urlFoto;
}
