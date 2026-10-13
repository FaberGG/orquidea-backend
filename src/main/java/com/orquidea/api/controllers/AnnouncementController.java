package com.orquidea.api.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orquidea.api.dto.AnnouncementRequest;
import com.orquidea.api.dto.AnnouncementResponse;
import com.orquidea.api.dto.ApiErrorResponse;
import com.orquidea.api.service.AnnouncementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    @Operation(summary = "Publicar anuncio",
            description = "Solo ADMINISTRADOR o SUPERADMINISTRADOR. Título y contenido son obligatorios. "
                    + "El anuncio queda visible para todos los usuarios.")
    @ApiResponse(responseCode = "201", description = "Anuncio publicado",
            content = @Content(schema = @Schema(implementation = AnnouncementResponse.class)))
    @ApiResponse(responseCode = "400",
            description = "Debes completar el título del anuncio. / Debes completar el contenido del anuncio.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es administrador o superadministrador",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<AnnouncementResponse> ingresarAnuncio(
            @Valid @RequestBody AnnouncementRequest request) {
        AnnouncementResponse response = announcementService.ingresarAnuncio(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Listar anuncios",
            description = "Público. Devuelve los anuncios ordenados por fecha de publicación, del más reciente al más antiguo. "
                    + "Si no hay anuncios devuelve una lista vacía y el cliente muestra: 'Aún no hay anuncios publicados.'")
    @ApiResponse(responseCode = "200", description = "Listado de anuncios (puede estar vacío)",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AnnouncementResponse.class))))
    public ResponseEntity<List<AnnouncementResponse>> obtenerAnuncios() {
        List<AnnouncementResponse> anuncios = announcementService.obtenerAnuncios();
        return ResponseEntity.ok(anuncios);
    }
}