package com.orquidea.api.controllers;

import com.orquidea.api.dto.ApiErrorResponse;
import com.orquidea.api.dto.StationDto;
import com.orquidea.api.dto.StationRequest;
import com.orquidea.api.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/estaciones")
@RequiredArgsConstructor
@Tag(name = "Estaciones", description = "Estaciones del recorrido en el mapa (HU-13)")
public class StationController {

    private static final String DESCRIPCION_FORMULARIO = "Formulario multipart con nombre, descripcion, "
            + "decimalLatitude, decimalLongitude (con punto decimal), foto (jpg o png, máx. 10 MB) y fichaIds "
            + "(opcional, ids separados por comas o en el campo repetido).";

    private final StationService stationService;

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Listar estaciones", description = "Público. Todas las estaciones, sin paginar y ordenadas por nombre.")
    @ApiResponse(responseCode = "200", description = "Listado de estaciones (vacío si no hay ninguna)")
    public List<StationDto> listar() {
        return stationService.listar();
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(summary = "Consultar estación", description = "Público.")
    @ApiResponse(responseCode = "200", description = "Estación encontrada")
    @ApiResponse(responseCode = "404", description = "La estación no existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public StationDto obtenerPorId(@PathVariable UUID id) {
        return stationService.obtenerPorId(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Crear estación",
            description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. " + DESCRIPCION_FORMULARIO + " La foto es obligatoria.")
    @ApiResponse(responseCode = "201", description = "Estación creada y visible en el mapa")
    @ApiResponse(responseCode = "400", description = "Debes completar todos los campos obligatorios. / Las coordenadas "
            + "ingresadas no son válidas. / El formato de la imagen no es válido. / Alguna de las fichas asociadas no existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "413", description = "La imagen supera el tamaño máximo",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<StationDto> crear(@Valid @ModelAttribute StationRequest solicitud) {
        StationDto estacion = stationService.crear(solicitud);
        var ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(estacion.getId()).toUri();
        return ResponseEntity.created(ubicacion).body(estacion);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Editar estación",
            description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. " + DESCRIPCION_FORMULARIO + " La foto es opcional y, "
                    + "si no se envía, se conserva la actual. fichaIds reemplaza las asociaciones: si no llega, la "
                    + "estación queda sin fichas. Ante un 200 el cliente muestra: 'La estación se actualizó correctamente.'")
    @ApiResponse(responseCode = "200", description = "Estación actualizada")
    @ApiResponse(responseCode = "400", description = "Debes completar todos los campos obligatorios. / Las coordenadas "
            + "ingresadas no son válidas. / El formato de la imagen no es válido. / Alguna de las fichas asociadas no existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "La estación no existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "413", description = "La imagen supera el tamaño máximo",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public StationDto actualizar(@PathVariable UUID id, @Valid @ModelAttribute StationRequest solicitud) {
        return stationService.actualizar(id, solicitud);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar estación",
            description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. La estación desaparece del mapa; sus fichas asociadas no cambian.")
    @ApiResponse(responseCode = "204", description = "Estación eliminada")
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "La estación no existe.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public void eliminar(@PathVariable UUID id) {
        stationService.eliminar(id);
    }
}
