package com.orquidea.api.controllers;

import com.orquidea.api.dto.FichaTaxonomicaDto;
import com.orquidea.api.dto.RespuestaError;
import com.orquidea.api.dto.SolicitudFichaTaxonomica;
import com.orquidea.api.service.FichaTaxonomicaServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fichas-taxonomicas")
@RequiredArgsConstructor
@Tag(name = "Fichas taxonómicas", description = "Fichas de especies del humedal (HU-7, HU-8)")
public class FichaTaxonomicaControlador {

    private final FichaTaxonomicaServicio fichaServicio;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Crear ficha taxonómica",
            description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. Formulario multipart con los datos y la foto (jpg o png, máx. 10 MB).")
    @ApiResponse(responseCode = "201", description = "Ficha creada y publicada")
    @ApiResponse(responseCode = "400", description = "Debes completar todos los campos obligatorios. / El formato de la imagen no es válido.",
            content = @Content(schema = @Schema(implementation = RespuestaError.class)))
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = RespuestaError.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
            content = @Content(schema = @Schema(implementation = RespuestaError.class)))
    @ApiResponse(responseCode = "409", description = "Ya existe una ficha con ese nombre científico",
            content = @Content(schema = @Schema(implementation = RespuestaError.class)))
    @ApiResponse(responseCode = "413", description = "La imagen supera el tamaño máximo",
            content = @Content(schema = @Schema(implementation = RespuestaError.class)))
    public ResponseEntity<FichaTaxonomicaDto> crear(
            @Validated({Default.class, SolicitudFichaTaxonomica.Creacion.class}) @ModelAttribute SolicitudFichaTaxonomica solicitud) {
        FichaTaxonomicaDto ficha = fichaServicio.crear(solicitud);
        var ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(ficha.getId()).toUri();
        return ResponseEntity.created(ubicacion).body(ficha);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Editar ficha taxonómica",
            description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. Reemplaza todos los datos de la ficha con el mismo "
                    + "formulario de la creación; la foto es opcional y, si no se envía, se conserva la actual. "
                    + "Ante un 200 el cliente muestra: 'La ficha se actualizó correctamente.'")
    @ApiResponse(responseCode = "200", description = "Ficha actualizada")
    @ApiResponse(responseCode = "400", description = "Debes completar todos los campos obligatorios. / El formato de la imagen no es válido.",
            content = @Content(schema = @Schema(implementation = RespuestaError.class)))
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = RespuestaError.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
            content = @Content(schema = @Schema(implementation = RespuestaError.class)))
    @ApiResponse(responseCode = "404", description = "La ficha no existe",
            content = @Content(schema = @Schema(implementation = RespuestaError.class)))
    @ApiResponse(responseCode = "409", description = "Otra ficha ya usa ese nombre científico",
            content = @Content(schema = @Schema(implementation = RespuestaError.class)))
    @ApiResponse(responseCode = "413", description = "La imagen supera el tamaño máximo",
            content = @Content(schema = @Schema(implementation = RespuestaError.class)))
    public FichaTaxonomicaDto actualizar(@PathVariable UUID id, @Valid @ModelAttribute SolicitudFichaTaxonomica solicitud) {
        return fichaServicio.actualizar(id, solicitud);
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(summary = "Consultar ficha taxonómica", description = "Público.")
    @ApiResponse(responseCode = "200", description = "Ficha encontrada")
    @ApiResponse(responseCode = "404", description = "La ficha no existe",
            content = @Content(schema = @Schema(implementation = RespuestaError.class)))
    public FichaTaxonomicaDto obtenerPorId(@PathVariable UUID id) {
        return fichaServicio.obtenerPorId(id);
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Listar fichas taxonómicas",
            description = "Público. Todas las fichas ordenadas por nombre común; el filtrado por categoría llega con la HU-10.")
    @ApiResponse(responseCode = "200", description = "Listado de fichas")
    public List<FichaTaxonomicaDto> listar() {
        return fichaServicio.listar();
    }
}
