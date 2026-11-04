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
@Schema(description = "Foto de una ficha taxonómica")
public class TaxonPhotoDto {

    @Schema(description = "Identificador de la foto; sirve para eliminarla", example = "9d4e2c1a-3b7f-4e0a-8c55-1f2a6b7c8d90")
    private UUID id;

    @Schema(description = "URL pública de la foto", example = "http://localhost:9000/orquidea/fichas/9d4e2c1a.jpg")
    private String url;
}
