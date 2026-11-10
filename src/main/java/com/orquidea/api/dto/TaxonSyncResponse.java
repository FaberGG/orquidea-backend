package com.orquidea.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * HU-21. Lo que la app necesita para crear o poner al día su copia sin conexión de las fichas. Las fichas usan el
 * mismo TaxonDto de la consulta, así que cualquier campo nuevo de la ficha llega también a la copia local.
 */
@Schema(description = "Cambios de las fichas taxonómicas para la copia sin conexión de la app")
public record TaxonSyncResponse(
        @Schema(description = "Se guarda y se envía tal cual en 'modificadoDesde' en la próxima sincronización",
                example = "2026-11-10T15:30:00.123456Z")
        Instant fechaSincronizacion,
        @Schema(description = "true si no se envió 'modificadoDesde': 'fichas' trae todas las fichas y la copia local "
                + "se reemplaza entera, borrando las que no vengan", example = "false")
        boolean completa,
        @Schema(description = "Fichas creadas o modificadas desde 'modificadoDesde', también cuando cambian sus fotos; "
                + "todas si la sincronización es completa. Puede repetir fichas ya recibidas: se guardan por id")
        List<TaxonDto> fichas,
        @Schema(description = "Ids de las fichas eliminadas desde 'modificadoDesde'; vacía si la sincronización es "
                + "completa. Puede traer ids que el dispositivo nunca descargó, y nunca repite un id de 'fichas'",
                example = "[\"5b0c7f0e-2f5d-4a8e-9a57-0c7d3c1b6e21\"]")
        List<UUID> idsEliminados) {
}
