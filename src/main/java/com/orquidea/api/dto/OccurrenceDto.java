package com.orquidea.api.dto;

import com.orquidea.api.model.Kingdom;
import com.orquidea.api.model.ReviewStatus;
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
@Schema(description = "Reporte ciudadano de avistamiento")
public class OccurrenceDto {

    @Schema(description = "Identificador del reporte", example = "0f8e6a1c-3b2d-4c5e-9f10-7a6b5c4d3e2f")
    private UUID id;

    @Schema(description = "Descripción del avistamiento (DwC: occurrenceRemarks)", example = "Vi un ave azul comiendo frutos en el borde del humedal")
    private String occurrenceRemarks;

    @Schema(description = "Reino (DwC: kingdom)", example = "ANIMALIA")
    private Kingdom kingdom;

    @Schema(description = "Clasificación específica (DwC: vernacularName)", example = "Azulejo")
    private String vernacularName;

    @Schema(description = "Lugar del avistamiento (DwC: locality)", example = "Cerca al puente de la estación 3", nullable = true)
    private String locality;

    @Schema(description = "Nombre y apellido del autor (DwC: recordedBy)", example = "Ana Gómez")
    private String recordedBy;

    @Schema(description = "URL pública de la foto, o null si el reporte no tiene", nullable = true,
            example = "http://localhost:9000/orquidea/reportes/0f8e6a1c.jpg")
    private String urlFoto;

    @Schema(description = "Estado de revisión", example = "PENDIENTE")
    private ReviewStatus estado;

    @Schema(description = "Fecha de envío", example = "2026-10-08T15:30:00Z")
    private Instant fechaCreacion;

    @Schema(description = "Fecha de aprobación o rechazo; null mientras está pendiente", nullable = true,
            example = "2026-10-09T10:00:00Z")
    private Instant fechaRevision;
}
