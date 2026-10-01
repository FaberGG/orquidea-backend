package com.orquidea.api.controllers;

import com.orquidea.api.dto.ApiErrorResponse;
import com.orquidea.api.dto.LoginResponse;
import com.orquidea.api.dto.RegisterResponse;
import com.orquidea.api.dto.LoginRequest;
import com.orquidea.api.dto.RegisterRequest;
import com.orquidea.api.dto.AuthenticatedUserDto;
import com.orquidea.api.dto.PasswordRecoveryRequest;
import com.orquidea.api.dto.PasswordResetRequest;
import com.orquidea.api.security.JwtPrincipal;
import com.orquidea.api.service.AuthService;
import com.orquidea.api.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/autenticacion")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio de sesión y datos de la sesión actual (HU-1)")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/iniciar-sesion")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión",
            description = "Valida correo y contraseña y devuelve un JWT junto con el rol del usuario.")
    @ApiResponse(responseCode = "200", description = "Ingreso exitoso")
    @ApiResponse(responseCode = "400", description = "Ambos campos son obligatorios.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public LoginResponse iniciarSesion(@Valid @RequestBody LoginRequest solicitud) {
        return authService.iniciarSesion(solicitud);
    }

    @GetMapping("/yo")
    @Operation(summary = "Usuario de la sesión actual",
            description = "Devuelve los datos y el rol del dueño del token; útil para restaurar la sesión en el cliente.")
    @ApiResponse(responseCode = "200", description = "Usuario autenticado")
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public AuthenticatedUserDto obtenerUsuarioActual(@AuthenticationPrincipal JwtPrincipal usuarioToken) {
        return authService.obtenerUsuarioActual(usuarioToken.id());
    }

    @PostMapping("/registro")
    @SecurityRequirements
    @Operation(summary = "Registrar nuevo usuario", description = "Registra un nuevo usuario en el sistema y asigna el rol de usuario registrado.")
    @ApiResponse(responseCode = "201", description = "Registro exitoso creado")
    @ApiResponse(responseCode = "400", description = "Los datos enviados no son válidos.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El correo electrónico ya está registrado.", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<RegisterResponse> registrarUsuario(@Valid @RequestBody RegisterRequest solicitud) {
            RegisterResponse respuesta = authService.registrarUsuario(solicitud);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PostMapping("/recuperar-contrasena")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Solicitar código de recuperación",
            description = "Envía un código de 6 dígitos al correo si pertenece a una cuenta habilitada. "
                    + "Responde igual aunque el correo no esté registrado, para no revelar qué cuentas existen.")
    @ApiResponse(responseCode = "204", description = "Solicitud recibida")
    @ApiResponse(responseCode = "400", description = "El correo es obligatorio y debe ser válido.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public void solicitarCodigoRecuperacion(@Valid @RequestBody PasswordRecoveryRequest solicitud) {
        passwordResetService.solicitarCodigo(solicitud);
    }

    @PostMapping("/restablecer-contrasena")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Restablecer contraseña",
            description = "Cambia la contraseña con el código recibido por correo. El código es de un solo uso "
                    + "y se invalida al vencer o tras varios intentos fallidos.")
    @ApiResponse(responseCode = "204", description = "Contraseña actualizada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, o código incorrecto o vencido.",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public void restablecerContrasena(@Valid @RequestBody PasswordResetRequest solicitud) {
        passwordResetService.restablecerContrasena(solicitud);
    }

}
