package com.orquidea.api.dto;

import com.orquidea.api.model.TaxonCategory;
import com.orquidea.api.model.ConservationStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Formulario multipart para crear (HU-7) o editar (HU-8) una ficha: un campo de texto por atributo
 * más el campo "fotos", repetido por archivo, al crear. Todos los campos de texto son obligatorios en ambos casos;
 * al editar las fotos se ignoran porque tienen sus propios endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para crear una ficha taxonómica")
public class TaxonRequest {

    public static final String MENSAJE_OBLIGATORIOS = "Debes completar todos los campos obligatorios.";

    @NotNull(message = MENSAJE_OBLIGATORIOS)
    @Schema(description = "Listado en el que se publica la ficha", example = "AVE")
    private TaxonCategory categoria;

    @NotBlank(message = MENSAJE_OBLIGATORIOS)
    @Size(max = 100, message = "El orden no puede superar {max} caracteres.")
    @Schema(description = "Orden (DwC: order)", example = "Passeriformes")
    private String order;

    @NotBlank(message = MENSAJE_OBLIGATORIOS)
    @Size(max = 100, message = "La familia no puede superar {max} caracteres.")
    @Schema(description = "Familia (DwC: family)", example = "Thraupidae")
    private String family;

    @NotBlank(message = MENSAJE_OBLIGATORIOS)
    @Size(max = 100, message = "El género no puede superar {max} caracteres.")
    @Schema(description = "Género (DwC: genus)", example = "Thraupis")
    private String genus;

    @NotBlank(message = MENSAJE_OBLIGATORIOS)
    @Size(max = 200, message = "El nombre científico no puede superar {max} caracteres.")
    @Schema(description = "Nombre científico (DwC: scientificName); único en la plataforma", example = "Thraupis episcopus")
    private String scientificName;

    @NotBlank(message = MENSAJE_OBLIGATORIOS)
    @Size(max = 150, message = "El nombre común no puede superar {max} caracteres.")
    @Schema(description = "Nombre común (DwC: vernacularName)", example = "Azulejo")
    private String vernacularName;

    @NotBlank(message = MENSAJE_OBLIGATORIOS)
    @Size(max = 2000, message = "La alimentación no puede superar {max} caracteres.")
    @Schema(description = "Alimentación de la especie", example = "Frutos, néctar e insectos pequeños")
    private String alimentacion;

    @NotBlank(message = MENSAJE_OBLIGATORIOS)
    @Size(max = 2000, message = "El rol en el humedal no puede superar {max} caracteres.")
    @Schema(description = "Rol ecológico de la especie en el humedal", example = "Dispersor de semillas")
    private String rolEnHumedal;

    @NotNull(message = MENSAJE_OBLIGATORIOS)
    @Schema(description = "Categoría UICN", example = "LC")
    private ConservationStatus estadoConservacion;

    @ArraySchema(arraySchema = @Schema(description = "Fotos de la especie al crear: de 1 a 10, jpg o png, máximo "
            + "10 MB cada una; la primera es la principal. Al editar se ignoran"),
            schema = @Schema(type = "string", format = "binary"))
    private List<MultipartFile> fotos;
}
