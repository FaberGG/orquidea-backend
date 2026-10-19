package com.orquidea.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados con un formato estable. Se usa en lugar de serializar Page de Spring Data,
 * cuyo JSON depende de detalles internos y cambia entre versiones.
 */
@Schema(description = "Página de resultados")
public record PageResponse<T>(
        @Schema(description = "Elementos de la página")
        List<T> contenido,
        @Schema(description = "Número de página, empezando en 0", example = "0")
        int pagina,
        @Schema(description = "Tamaño de página solicitado", example = "20")
        int tamano,
        @Schema(description = "Total de elementos en todas las páginas", example = "42")
        long totalElementos,
        @Schema(description = "Total de páginas", example = "3")
        int totalPaginas) {

    public static <E, T> PageResponse<T> de(Page<E> pagina, Function<E, T> conversion) {
        return new PageResponse<>(pagina.map(conversion).getContent(), pagina.getNumber(), pagina.getSize(),
                pagina.getTotalElements(), pagina.getTotalPages());
    }
}
