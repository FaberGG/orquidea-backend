package com.orquidea.api.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "Anuncio publicado en el humedal.")
public class AnnouncementResponse {

    @Schema(description = "Identificador del anuncio", example = "62d62909-5809-460e-a582-78912e5abfe8")
    private UUID id;

    @Schema(description = "Título del anuncio", example = "Día mundial del agua")
    private String titulo;

    @Schema(description = "Contenido del anuncio", example = "Hoy se conmemora el Día Mundial del Agua.")
    private String descripcion;

    @Schema(description = "Fecha de publicación en UTC", example = "2026-10-08T22:32:47.976914Z")
    private Instant fechaCreacion;
}