package com.orquidea.api.controllers;

import com.orquidea.api.dto.TaxonDto;
import com.orquidea.api.dto.ApiErrorResponse;
import com.orquidea.api.dto.TaxonRequest;
import com.orquidea.api.model.TaxonCategory;
import com.orquidea.api.service.TaxonService;
import com.orquidea.api.service.TaxonQrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fichas-taxonomicas")
@RequiredArgsConstructor
@Tag(name = "Fichas taxonómicas", description = "Fichas de especies del humedal (HU-7, HU-8)")
public class TaxonController {

    private final TaxonService taxonService;
    private final TaxonQrService taxonQrService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Crear ficha taxonómica",
            description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. Formulario multipart con los datos y de 1 a 10 fotos "
                    + "en el campo 'fotos', repetido por archivo (jpg o png, máx. 10 MB cada una). Las fotos quedan en el "
                    + "orden de envío y la primera es la principal del listado (urlFoto).")
    @ApiResponse(responseCode = "201", description = "Ficha creada y publicada")
    @ApiResponse(responseCode = "400", description = "Debes completar todos los campos obligatorios. / Debes seleccionar una imagen. "
            + "/ El formato de la imagen no es válido.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Ya existe una ficha con ese nombre científico / Más de 10 fotos",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "413", description = "La imagen supera el tamaño máximo",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<TaxonDto> crear(@Valid @ModelAttribute TaxonRequest solicitud) {
        TaxonDto ficha = taxonService.crear(solicitud);
        var ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(ficha.getId()).toUri();
        return ResponseEntity.created(ubicacion).body(ficha);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Editar ficha taxonómica",
            description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. Reemplaza todos los datos de la ficha con el mismo "
                    + "formulario de la creación, solo con texto: las fotos no cambian y se gestionan con "
                    + "/{id}/fotos. Ante un 200 el cliente muestra: 'La ficha se actualizó correctamente.'")
    @ApiResponse(responseCode = "200", description = "Ficha actualizada")
    @ApiResponse(responseCode = "400", description = "Debes completar todos los campos obligatorios.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "La ficha no existe",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Otra ficha ya usa ese nombre científico",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public TaxonDto actualizar(@PathVariable UUID id, @Valid @ModelAttribute TaxonRequest solicitud) {
        return taxonService.actualizar(id, solicitud);
    }

    @PostMapping(path = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Agregar una foto a una ficha",
            description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. Formulario multipart con el campo 'foto' "
                    + "(jpg o png, máx. 10 MB). Cada ficha admite como máximo 10 fotos y la nueva queda al final. "
                    + "Devuelve la ficha completa con su lista de fotos actualizada.")
    @ApiResponse(responseCode = "201", description = "Foto agregada")
    @ApiResponse(responseCode = "400", description = "Debes seleccionar una imagen. / El formato de la imagen no es válido.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "La ficha no existe",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La ficha ya tiene el máximo de fotos",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "413", description = "La imagen supera el tamaño máximo",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<TaxonDto> agregarFoto(
            @PathVariable UUID id,
            @Parameter(description = "Foto jpg o png, máximo 10 MB")
            @RequestParam(value = "foto", required = false) MultipartFile foto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxonService.agregarFoto(id, foto));
    }

    @DeleteMapping("/{id}/fotos/{idFoto}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar una foto de una ficha",
            description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. El id de la foto viene en la lista 'fotos' de la ficha. "
                    + "Si era la principal, la siguiente pasa a serlo. La ficha conserva siempre al menos una foto.")
    @ApiResponse(responseCode = "204", description = "Foto eliminada")
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "La ficha o la foto no existen",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Es la única foto de la ficha",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public void eliminarFoto(@PathVariable UUID id,
                             @Parameter(description = "Identificador de la foto") @PathVariable UUID idFoto) {
        taxonService.eliminarFoto(id, idFoto);
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(summary = "Consultar ficha taxonómica", description = "Público.")
    @ApiResponse(responseCode = "200", description = "Ficha encontrada")
    @ApiResponse(responseCode = "404", description = "La ficha no existe",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public TaxonDto obtenerPorId(@PathVariable UUID id) {
        return taxonService.obtenerPorId(id);
    }

    @GetMapping(value = "/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Descargar el código QR de una ficha taxonómica",
            description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. El QR abre la ficha en la app Orquídea.")
    @ApiResponse(responseCode = "200", description = "Imagen PNG del código QR",
            content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE,
                    schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "La ficha no existe",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "No fue posible generar el código QR",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<byte[]> descargarQr(@PathVariable UUID id) {
        taxonService.obtenerPorId(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("Content-Disposition", "attachment; filename=orquidea-ficha-" + id + ".png")
                .body(taxonQrService.generar(id));
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Listar fichas taxonómicas",
        description = "Público. Ordenadas por nombre común. Con el parámetro categoria (AVE, PLANTA, INSECTO, MAMIFERO, REPTIL, ANFIBIO o HONGO) "
            + "devuelve solo las de ese listado; sin él, todas. Si la categoría no tiene fichas responde una "
            + "lista vacía y el cliente muestra: 'Aún no hay especies registradas en esta categoría.'")
    @ApiResponse(responseCode = "200", description = "Listado de fichas (vacío si no hay ninguna)")
    @ApiResponse(responseCode = "400", description = "El valor del parámetro 'categoria' no es válido.",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public List<TaxonDto> listar(
        @Parameter(description = "Categoría a consultar; si se omite, se listan todas", example = "AVE")
        @RequestParam(required = false) TaxonCategory categoria) {
        return taxonService.listar(categoria);
    }
}
