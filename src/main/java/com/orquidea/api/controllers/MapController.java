package com.orquidea.api.controllers;

import com.orquidea.api.dto.MapDto;
import com.orquidea.api.service.MapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

@RestController
@RequestMapping("/api/mapa")
@RequiredArgsConstructor
@Tag(name = "Mapa", description = "Mapa interactivo del sendero (HU-12)")
public class MapController {

    private final MapService mapService;

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Consultar el mapa del sendero",
            description = "Público. Un solo endpoint con todo lo que necesita el mapa: la versión, los límites para "
                    + "encuadrar la vista, los tramos del trazado (GeoJSON LineString, coordenadas [longitud, latitud]) "
                    + "y las estaciones con su información y sus fichas. La respuesta trae la cabecera ETag, igual a "
                    + "'version' entre comillas: el cliente puede guardarla y volver a pedirla con If-None-Match; si no "
                    + "cambió responde 304 sin cuerpo. Sin tramos ni estaciones devuelve listas vacías y limites nulo.")
    @ApiResponse(responseCode = "200", description = "Mapa completo, con la cabecera ETag")
    @ApiResponse(responseCode = "304", description = "El mapa no cambió desde la versión enviada en If-None-Match")
    public ResponseEntity<MapDto> obtener(WebRequest solicitud) {
        MapDto mapa = mapService.obtener();
        String etag = "\"" + mapa.getVersion() + "\"";
        if (solicitud.checkNotModified(etag)) {
            return null; // Spring ya respondió 304; devolver null evita escribir un cuerpo
        }
        // no-cache: el navegador puede guardarla, pero debe revalidar con el ETag antes de usarla
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.noCache())
                .body(mapa);
    }
}
