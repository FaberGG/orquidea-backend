package com.orquidea.api.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orquidea.api.dto.AnnouncementRequest;
import com.orquidea.api.dto.AnnouncementResponse;
import com.orquidea.api.dto.AnnouncementUpdateRequest;
import com.orquidea.api.dto.ApiErrorResponse;
import com.orquidea.api.service.AnnouncementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;



@RestController
@RequestMapping("/api/anuncios")
@RequiredArgsConstructor
@Tag(name = "Anuncios", description = "Gestión y consulta de anuncios del humedal (HU-16, HU-17)")
public class AnnouncementController {

        private final AnnouncementService announcementService;

        @PostMapping
        @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR', 'ADMINISTRADOR')")
        @Operation(summary = "Publicar anuncio", description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. Título y contenido son obligatorios. "
                        + "El anuncio queda visible para todos los usuarios.")
        @ApiResponse(responseCode = "201", description = "Anuncio publicado", content = @Content(schema = @Schema(implementation = AnnouncementResponse.class)))
        @ApiResponse(responseCode = "400", description = "Debes completar el título del anuncio. / Debes completar el contenido del anuncio.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        @ApiResponse(responseCode = "401", description = "Sin sesión iniciada", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        @ApiResponse(responseCode = "403", description = "El usuario no es administrador o superadministrador", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))

        public ResponseEntity<AnnouncementResponse> ingresarAnuncio(
                        @Valid @RequestBody AnnouncementRequest request) {
                AnnouncementResponse response = announcementService.ingresarAnuncio(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping
        @SecurityRequirements
        @Operation(summary = "Listar anuncios", description = "Público. Devuelve los anuncios ordenados por fecha de publicación, del más reciente al más antiguo. "
                        + "Si no hay anuncios devuelve una lista vacía y el cliente muestra: 'Aún no hay anuncios publicados.'")
        @ApiResponse(responseCode = "200", description = "Listado de anuncios (puede estar vacío)", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AnnouncementResponse.class))))
        public ResponseEntity<List<AnnouncementResponse>> obtenerAnuncios() {
                List<AnnouncementResponse> anuncios = announcementService.obtenerAnuncios();
                return ResponseEntity.ok(anuncios);
        }

        @GetMapping("/administracion")
        @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR', 'ADMINISTRADOR')")
        @Operation(summary = "Listar anuncios para administración", description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. Devuelve todos los anuncios, incluidos los inactivos "
                        + "y los expirados, del más reciente al más antiguo. El campo 'expirado' indica si la fecha de "
                        + "expiración ya se cumplió y 'estado' si el administrador lo mantiene ACTIVO o INACTIVO.")
        @ApiResponse(responseCode = "200", description = "Listado completo de anuncios (puede estar vacío)", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AnnouncementResponse.class))))
        @ApiResponse(responseCode = "401", description = "Sin sesión iniciada", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        @ApiResponse(responseCode = "403", description = "El usuario no es administrador o superadministrador", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
        public ResponseEntity<List<AnnouncementResponse>> obtenerAnunciosAdministracion() {
                return ResponseEntity.ok(announcementService.obtenerAnunciosAdministracion());
        }

        @PatchMapping("/{id}/ocultar")
        @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR')")
        @Operation(summary = "Ocultar un anuncio", description = "Deja de mostrarse a los visitantes y queda marcado como inactivo en la vista del administrador.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Anuncio oculto"),
                        @ApiResponse(responseCode = "401", description = "Sin sesión"),
                        @ApiResponse(responseCode = "403", description = "Sin permisos"),
                        @ApiResponse(responseCode = "404", description = "El anuncio no existe")
        })
        public AnnouncementResponse ocultar(@PathVariable UUID id) {
                return announcementService.ocultarAnuncio(id);
        }

        @PatchMapping("/{id}/mostrar")
        @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR')")
        @Operation(summary = "Mostrar un anuncio oculto", description = "Vuelve a mostrarse en la sección de anuncios. No se puede mostrar uno que ya expiró.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Anuncio visible"),
                        @ApiResponse(responseCode = "401", description = "Sin sesión"),
                        @ApiResponse(responseCode = "403", description = "Sin permisos"),
                        @ApiResponse(responseCode = "404", description = "El anuncio no existe"),
                        @ApiResponse(responseCode = "409", description = "El anuncio ya expiró")
        })
        public AnnouncementResponse mostrar(@PathVariable UUID id) {
                return announcementService.mostrarAnuncio(id);
        }

        @PatchMapping("/{id}/actualizar")
        @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR','ADMINISTRADOR')")
        @Operation(summary = "Actualizar un anuncio", description = "Actualiza solo los campos enviados; los que no se envían conservan su valor. "
                        + "El estado se cambia con los endpoints de ocultar y mostrar.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Anuncio actualizado"),
                        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
                        @ApiResponse(responseCode = "401", description = "Sin sesión"),
                        @ApiResponse(responseCode = "403", description = "Sin permisos"),
                        @ApiResponse(responseCode = "404", description = "El anuncio no existe")
        })
        public AnnouncementResponse actualizar(@PathVariable UUID id,
                        @Valid @RequestBody AnnouncementUpdateRequest request) {
                return announcementService.actualizarAnuncio(id, request);
        }
    
}