package com.orquidea.api.controllers;

import com.orquidea.api.dto.ApiErrorResponse;
import com.orquidea.api.dto.OccurrenceDto;
import com.orquidea.api.dto.OccurrenceRequest;
import com.orquidea.api.dto.PageResponse;
import com.orquidea.api.security.JwtPrincipal;
import com.orquidea.api.service.OccurrenceService;
import com.orquidea.api.service.OccurrenceService.CreationResult;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/reportes-avistamiento")
@RequiredArgsConstructor
@Tag(name = "Reportes de avistamiento", description = "Reportes ciudadanos: creación (HU-18), revisión (HU-19) y foro público (HU-20)")
public class OccurrenceController {

    private static final String TAMANO_POR_DEFECTO = "20";

    private final OccurrenceService occurrenceService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USUARIO_REGISTRADO')")
    @Operation(summary = "Crear reporte de avistamiento",
            description = "Solo USUARIO_REGISTRADO. Formulario multipart: la descripción es obligatoria; reino y "
                    + "clasificación sin valor quedan como desconocidos; la foto es opcional (jpg o png, máx. 10 MB). "
                    + "El autor es el usuario autenticado. Ante un 201 el cliente muestra: "
                    + "'Tu reporte fue enviado y será revisado por un administrador.'")
    @ApiResponse(responseCode = "201", description = "Reporte creado en estado PENDIENTE")
    @ApiResponse(responseCode = "200", description = "Ya existía un reporte propio con ese id; se devuelve sin duplicarlo")
    @ApiResponse(responseCode = "400", description = "La descripción es obligatoria. / El formato de la imagen no es válido.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es USUARIO_REGISTRADO",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Otro usuario ya registró un reporte con ese id",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "413", description = "La imagen supera el tamaño máximo",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<OccurrenceDto> crear(@Valid @ModelAttribute OccurrenceRequest solicitud,
                                               @AuthenticationPrincipal JwtPrincipal usuario) {
        CreationResult resultado = occurrenceService.crear(solicitud, usuario.id());
        if (!resultado.nuevo()) {
            return ResponseEntity.ok(resultado.reporte());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado.reporte());
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Foro de avistamientos",
            description = "Público. Solo reportes APROBADOS, los más recientes primero. Si no hay ninguno responde "
                    + "una página vacía y el cliente muestra: 'Aún no hay avistamientos publicados.'")
    @ApiResponse(responseCode = "200", description = "Página de reportes aprobados")
    public PageResponse<OccurrenceDto> listarAprobados(
            @Parameter(description = "Número de página, desde 0", example = "0") @RequestParam(defaultValue = "0") int pagina,
            @Parameter(description = "Tamaño de página, máximo 50", example = "20") @RequestParam(defaultValue = TAMANO_POR_DEFECTO) int tamano) {
        return occurrenceService.listarAprobados(pagina, tamano);
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Cola de revisión",
            description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. Reportes PENDIENTES, los más antiguos primero.")
    @ApiResponse(responseCode = "200", description = "Página de reportes pendientes")
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public PageResponse<OccurrenceDto> listarPendientes(
            @Parameter(description = "Número de página, desde 0", example = "0") @RequestParam(defaultValue = "0") int pagina,
            @Parameter(description = "Tamaño de página, máximo 50", example = "20") @RequestParam(defaultValue = TAMANO_POR_DEFECTO) int tamano) {
        return occurrenceService.listarPendientes(pagina, tamano);
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Aprobar reporte", description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. Publica el reporte en el foro.")
    @ApiResponse(responseCode = "200", description = "Reporte APROBADO")
    @ApiResponse(responseCode = "404", description = "El reporte no existe",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El reporte ya fue revisado.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public OccurrenceDto aprobar(@PathVariable UUID id, @AuthenticationPrincipal JwtPrincipal usuario) {
        return occurrenceService.aprobar(id, usuario.id());
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Rechazar reporte",
            description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. El reporte sale de la cola y no se publica.")
    @ApiResponse(responseCode = "200", description = "Reporte RECHAZADO")
    @ApiResponse(responseCode = "404", description = "El reporte no existe",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El reporte ya fue revisado.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public OccurrenceDto rechazar(@PathVariable UUID id, @AuthenticationPrincipal JwtPrincipal usuario) {
        return occurrenceService.rechazar(id, usuario.id());
    }
}
