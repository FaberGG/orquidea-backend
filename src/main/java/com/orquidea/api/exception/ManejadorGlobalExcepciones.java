package com.orquidea.api.exception;

import com.orquidea.api.dto.RespuestaError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    @ExceptionHandler(CredencialesInvalidasExcepcion.class)
    public ResponseEntity<RespuestaError> manejarCredencialesInvalidas(
            CredencialesInvalidasExcepcion ex, HttpServletRequest request) {
        return construir(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> manejarValidacion(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("La solicitud contiene datos inválidos.");
        return construir(HttpStatus.BAD_REQUEST, mensaje, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> manejarCuerpoIlegible(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no es válido.", request);
    }

    /** Petición sin token, o con token inválido o vencido, a una ruta protegida. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RespuestaError> manejarNoAutenticado(
            AuthenticationException ex, HttpServletRequest request) {
        return construir(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión para acceder a este recurso.", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespuestaError> manejarAccesoDenegado(
            AccessDeniedException ex, HttpServletRequest request) {
        return construir(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta acción.", request);
    }

    /**
     * Resto de errores. Las excepciones propias de Spring MVC (404, 405, 415...) traen su código HTTP;
     * cualquier otra se considera un error interno.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> manejarGeneral(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode estado = errorResponse.getStatusCode();
            return construir(estado, mensajePorEstado(estado), request);
        }
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intente de nuevo más tarde.", request);
    }

    private static String mensajePorEstado(HttpStatusCode estado) {
        return switch (estado.value()) {
            case 404 -> "El recurso solicitado no existe.";
            case 405 -> "Método HTTP no permitido para este recurso.";
            case 415 -> "Tipo de contenido no soportado.";
            default -> estado.is4xxClientError()
                    ? "La solicitud no es válida."
                    : "Ocurrió un error inesperado. Intente de nuevo más tarde.";
        };
    }

    private static ResponseEntity<RespuestaError> construir(
            HttpStatusCode estado, String mensaje, HttpServletRequest request) {
        HttpStatus estadoHttp = HttpStatus.resolve(estado.value());
        RespuestaError cuerpo = RespuestaError.builder()
                .fecha(Instant.now())
                .estado(estado.value())
                .error(estadoHttp != null ? estadoHttp.getReasonPhrase() : String.valueOf(estado.value()))
                .mensaje(mensaje)
                .ruta(request.getRequestURI())
                .build();
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
