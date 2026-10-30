package com.orquidea.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.orquidea.api.model.Estado;
import com.orquidea.api.model.Prioridad;

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

    @Schema(description = "Prioridad del anuncio", example = "ALTA")
    private Prioridad prioridad;

    @Schema(description = "Estado del anuncio", example = "ACTIVO")
    private Estado estado;

    @Schema(description = "Fecha de expiración en UTC; vacío si no tiene vigencia", example = "2026-12-31T23:59:59Z")
    private Instant fechaExpiracion;

    @Schema(description = "true si la fecha de expiración ya pasó", example = "false")
    private boolean expirado;
    
}