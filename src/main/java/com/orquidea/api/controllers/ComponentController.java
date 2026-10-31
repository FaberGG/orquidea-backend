package com.orquidea.api.controllers;

import com.orquidea.api.dto.ApiErrorResponse;
import com.orquidea.api.dto.ComponentDto;
import com.orquidea.api.dto.ComponentUpdateRequest;
import com.orquidea.api.dto.ComponentCreateRequest;
import com.orquidea.api.service.ComponentService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/componentes")
@RequiredArgsConstructor
@Tag(name = "Componentes del humedal", description = "Información de los componentes del humedal (HU-14, HU-15)")
public class ComponentController {

    private static final String DESCRIPCION_TIPO = "Código del componente, en mayúsculas (p. ej. AVES, FLORA, COMPONENTE_HIDRICO)";

    private final ComponentService componentService;

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Listar componentes del humedal",
            description = "Público. Devuelve siempre todos los componentes existentes. "
                    + "Los que aún no tienen información traen tieneContenido=false y el cliente muestra: "
                    + "'La información de esta sección estará disponible próximamente.'")
    @ApiResponse(responseCode = "200", description = "Listado de componentes")
    public List<ComponentDto> listar() {
        return componentService.listar();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Crear un componente del humedal",
        description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. Recibe nombre y descripción; el código (tipo) se genera "
            + "a partir del nombre (p. ej. 'Componente hídrico' da COMPONENTE_HIDRICO). Las fotos se agregan después "
            + "con POST /api/componentes/{tipo}/fotos. El componente queda en el listado público.")
    @ApiResponse(responseCode = "201", description = "Componente creado")
    @ApiResponse(responseCode = "400", description = "Debes completar el nombre y la descripción del componente.",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Ya existe un componente con ese nombre.",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ComponentDto> crear(@Valid @RequestBody ComponentCreateRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(componentService.crear(solicitud));
    }
    
    @GetMapping("/{tipo}")
    @SecurityRequirements
    @Operation(summary = "Consultar un componente del humedal",
            description = "Público. Devuelve la descripción y las fotos del componente. Si todavía no tiene información "
                    + "responde 200 con tieneContenido=false, descripcion nula y fotos vacío; el cliente muestra: "
                    + "'La información de esta sección estará disponible próximamente.'")
    @ApiResponse(responseCode = "200", description = "Componente encontrado")
    @ApiResponse(responseCode = "404", description = "El componente no existe",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ComponentDto obtener(@Parameter(description = DESCRIPCION_TIPO, example = "AVES") @PathVariable String tipo) {
        return componentService.obtener(tipo);
    }

    @PutMapping(path = "/{tipo}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Editar el texto de un componente",
            description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. Reemplaza nombre y descripción; las fotos tienen sus "
                    + "propios endpoints. Ante un 200 el cliente muestra: 'El contenido del componente se actualizó correctamente.'")
    @ApiResponse(responseCode = "200", description = "Componente actualizado")
    @ApiResponse(responseCode = "400", description = "Debes completar todos los campos obligatorios.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "El componente no existe",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Ya existe un componente con ese nombre.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ComponentDto actualizar(@Parameter(description = DESCRIPCION_TIPO, example = "AVES") @PathVariable String tipo,
                                   @Valid @RequestBody ComponentUpdateRequest solicitud) {
        return componentService.actualizar(tipo, solicitud);
    }

    @PostMapping(path = "/{tipo}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Agregar una foto a un componente",
            description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. Formulario multipart con el campo 'foto' "
                    + "(jpg o png, máx. 10 MB). Cada componente admite como máximo 10 fotos. "
                    + "Devuelve el componente completo con su lista de fotos actualizada.")
    @ApiResponse(responseCode = "201", description = "Foto agregada")
    @ApiResponse(responseCode = "400", description = "Debes seleccionar una imagen. / El formato de la imagen no es válido.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "El componente no existe",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El componente ya tiene el máximo de fotos",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "413", description = "La imagen supera el tamaño máximo",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<ComponentDto> agregarFoto(
            @Parameter(description = DESCRIPCION_TIPO, example = "AVES") @PathVariable String tipo,
            @Parameter(description = "Foto jpg o png, máximo 10 MB")
            @RequestParam(value = "foto", required = false) MultipartFile foto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(componentService.agregarFoto(tipo, foto));
    }

    @DeleteMapping("/{tipo}/fotos/{idFoto}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar una foto de un componente",
            description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. El id de la foto viene en la lista 'fotos' del componente.")
    @ApiResponse(responseCode = "204", description = "Foto eliminada")
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "El componente o la foto no existen",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public void eliminarFoto(@Parameter(description = DESCRIPCION_TIPO, example = "AVES") @PathVariable String tipo,
                             @Parameter(description = "Identificador de la foto") @PathVariable UUID idFoto) {
        componentService.eliminarFoto(tipo, idFoto);
    }
}
