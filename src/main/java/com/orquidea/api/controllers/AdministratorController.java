package com.orquidea.api.controllers;

import com.orquidea.api.dto.*;
import com.orquidea.api.service.AdministratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/administradores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMINISTRADOR')")
@Tag(name = "Administradores", description = "Gestión de cuentas de administrador, solo SUPERADMINISTRADOR (HU-5)")
public class AdministratorController {

    private final AdministratorService administratorService;

    @GetMapping
    @Operation(summary = "Listar administradores y superadministradores",
        description = "Solo SUPERADMINISTRADOR. Devuelve las cuentas con rol ADMINISTRADOR o SUPERADMINISTRADOR, "
            + "incluidas las inhabilitadas, ordenadas por nombre y apellido.")
    @ApiResponse(responseCode = "200", description = "Listado de administradores (vacío si no hay ninguno)")
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es superadministrador",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public List<AdministratorDto> listar() {
        return administratorService.listar();
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Editar o inhabilitar un administrador",
            description = "Reemplaza nombre, apellido, correo, teléfono y estado (habilitado) de la cuenta. "
                    + "Ante un 200 el cliente muestra: 'Los cambios se guardaron correctamente.'")
    @ApiResponse(responseCode = "200", description = "Administrador actualizado")
    @ApiResponse(responseCode = "400", description = "Debes completar todos los campos obligatorios. / Ingresa un correo electrónico válido.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es superadministrador",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @ApiResponse(responseCode = "404", description = "El administrador no existe",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @ApiResponse(responseCode = "409", description = "Este correo ya está registrado.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public AdministratorDto actualizar(@PathVariable UUID id,
                                       @Valid @RequestBody AdministratorUpdateRequest solicitud) {
        return administratorService.actualizar(id, solicitud);
    }

    @PostMapping("/{id}/revocar-acceso")
    @Operation(summary = "Revocar el acceso de administrador",
            description = "Degrada la cuenta a USUARIO_REGISTRADO y notifica al afectado por correo.")
    @ApiResponse(responseCode = "200", description = "Acceso revocado; la cuenta quedó como usuario registrado")
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es superadministrador",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @ApiResponse(responseCode = "404", description = "El administrador no existe",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @ApiResponse(responseCode = "409", description = "No puedes revocar el único superadministrador de la plataforma.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public AdministratorDto revocarAcceso(@PathVariable UUID id) {
        return administratorService.revocarAcceso(id);
    }

    @PostMapping("/registrarAdmin")
    @Operation(summary = "Registrar un nuevo administrador",
            description = "Crea una nueva cuenta de administrador con los datos proporcionados.")
    @ApiResponse(responseCode = "201", description = "Administrador registrado")
    @ApiResponse(responseCode = "400", description = "Debes completar todos los campos obligatorios. / Ingresa un correo electrónico válido.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es superadministrador",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @ApiResponse(responseCode = "409", description = "Este correo ya está registrado.",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<RegisterResponse> registrar(@Valid @RequestBody RegisterRequest solicitud) {
        RegisterResponse response = administratorService.registrarAdministrador(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
