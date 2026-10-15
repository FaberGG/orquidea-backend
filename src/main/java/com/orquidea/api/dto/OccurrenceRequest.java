package com.orquidea.api.dto;

import com.orquidea.api.model.Kingdom;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Formulario multipart para crear un reporte de avistamiento (HU-18). Solo la descripción es obligatoria;
 * el lugar, si llega, son coordenadas (latitud y longitud juntas).
 * El autor no se recibe: se toma del usuario autenticado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para crear un reporte ciudadano de avistamiento")
public class OccurrenceRequest {

    public static final String MENSAJE_COORDENADAS = "Las coordenadas ingresadas no son válidas.";

    @Schema(description = "Id generado por el cliente (opcional). Reenviar el mismo id no duplica el reporte, "
            + "lo que permite reintentar o sincronizar reportes creados sin conexión", example = "0f8e6a1c-3b2d-4c5e-9f10-7a6b5c4d3e2f")
    private UUID id;

    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 2000, message = "La descripción no puede superar {max} caracteres.")
    @Schema(description = "Descripción del avistamiento (DwC: occurrenceRemarks)", example = "Vi un ave azul comiendo frutos en el borde del humedal")
    private String occurrenceRemarks;

    @Schema(description = "Reino (DwC: kingdom). Si se omite queda DESCONOCIDO", example = "ANIMALIA")
    private Kingdom kingdom;

    @Size(max = 150, message = "La clasificación no puede superar {max} caracteres.")
    @Schema(description = "Clasificación específica (DwC: vernacularName). Si se omite queda 'Desconocido'", example = "Azulejo")
    private String vernacularName;

    @DecimalMin(value = "-90", message = MENSAJE_COORDENADAS)
    @DecimalMax(value = "90", message = MENSAJE_COORDENADAS)
    @Schema(description = "Latitud del lugar del avistamiento (DwC: decimalLatitude). Opcional, pero va junto con la longitud",
            example = "2.459800")
    private BigDecimal decimalLatitude;

    @DecimalMin(value = "-180", message = MENSAJE_COORDENADAS)
    @DecimalMax(value = "180", message = MENSAJE_COORDENADAS)
    @Schema(description = "Longitud del lugar del avistamiento (DwC: decimalLongitude). Opcional, pero va junto con la latitud",
            example = "-76.611500")
    private BigDecimal decimalLongitude;

    @Schema(description = "Foto opcional, jpg o png, máximo 10 MB", type = "string", format = "binary")
    private MultipartFile foto;
}
