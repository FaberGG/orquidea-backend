package com.orquidea.api.dto;

import com.orquidea.api.model.TaxonCategory;
import com.orquidea.api.model.ConservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ficha taxonómica publicada")
public class TaxonDto {

    @Schema(description = "Identificador de la ficha", example = "5b0c7f0e-2f5d-4a8e-9a57-0c7d3c1b6e21")
    private UUID id;

    @Schema(description = "Listado en el que se publica la ficha", example = "AVE")
    private TaxonCategory categoria;

    @Schema(description = "Orden (DwC: order)", example = "Passeriformes")
    private String orden;

    @Schema(description = "Familia (DwC: family)", example = "Thraupidae")
    private String familia;

    @Schema(description = "Género (DwC: genus)", example = "Thraupis")
    private String genero;

    @Schema(description = "Nombre científico (DwC: scientificName)", example = "Thraupis episcopus")
    private String nombreCientifico;

    @Schema(description = "Nombre común (DwC: vernacularName)", example = "Azulejo")
    private String nombreComun;

    @Schema(description = "Alimentación de la especie", example = "Frutos, néctar e insectos pequeños")
    private String alimentacion;

    @Schema(description = "Rol ecológico de la especie en el humedal", example = "Dispersor de semillas")
    private String rolEnHumedal;

    @Schema(description = "Categoría UICN", example = "LC")
    private ConservationStatus estadoConservacion;

    @Schema(description = "URL pública de la foto", example = "http://localhost:9000/orquidea/fichas/5b0c7f0e.jpg")
    private String urlFoto;

    @Schema(description = "Fecha de creación", example = "2026-09-28T15:30:00Z")
    private Instant fechaCreacion;

    @Schema(description = "Fecha de la última modificación", example = "2026-09-28T15:30:00Z")
    private Instant fechaActualizacion;
}
