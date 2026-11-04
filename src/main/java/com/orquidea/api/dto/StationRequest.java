package com.orquidea.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Formulario multipart para crear o editar una estación (HU-13). La foto es obligatoria al crear y opcional al
 * editar. fichaIds llega en un solo campo separado por comas (la subida nativa de Expo no repite campos) o
 * repetido; al editar reemplaza las asociaciones, así que si no llega la estación queda sin fichas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para crear o editar una estación")
public class StationRequest {

    public static final String MENSAJE_OBLIGATORIOS = "Debes completar todos los campos obligatorios.";
    public static final String MENSAJE_COORDENADAS = "Las coordenadas ingresadas no son válidas.";

    @NotBlank(message = MENSAJE_OBLIGATORIOS)
    @Size(max = 150, message = "El nombre no puede superar {max} caracteres.")
    @Schema(description = "Nombre de la estación", example = "Mirador de la laguna")
    private String nombre;

    @NotBlank(message = MENSAJE_OBLIGATORIOS)
    @Size(max = 2000, message = "La descripción no puede superar {max} caracteres.")
    @Schema(description = "Descripción de la estación", example = "Desde aquí se observan garzas y patos")
    private String descripcion;

    @NotNull(message = MENSAJE_OBLIGATORIOS)
    @DecimalMin(value = "-90", message = MENSAJE_COORDENADAS)
    @DecimalMax(value = "90", message = MENSAJE_COORDENADAS)
    @Schema(description = "Latitud (DwC: decimalLatitude), entre -90 y 90, con punto decimal", example = "2.459800")
    private BigDecimal decimalLatitude;

    @NotNull(message = MENSAJE_OBLIGATORIOS)
    @DecimalMin(value = "-180", message = MENSAJE_COORDENADAS)
    @DecimalMax(value = "180", message = MENSAJE_COORDENADAS)
    @Schema(description = "Longitud (DwC: decimalLongitude), entre -180 y 180, con punto decimal", example = "-76.611500")
    private BigDecimal decimalLongitude;

    @Schema(description = "Foto de la estación, jpg o png, máximo 10 MB. Obligatoria al crear; al editar, si no se "
            + "envía (o llega vacía) se conserva la actual", type = "string", format = "binary")
    private MultipartFile foto;

    @ArraySchema(arraySchema = @Schema(description = "Ids de las fichas asociadas, separados por comas (id1,id2) o en "
            + "el campo repetido. Opcional; al editar reemplaza las asociaciones"))
    private List<UUID> fichaIds;
}
