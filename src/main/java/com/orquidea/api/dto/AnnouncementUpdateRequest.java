package com.orquidea.api.dto;

import java.time.Instant;

import com.orquidea.api.model.Prioridad;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnnouncementUpdateRequest {

    @Schema(description = "Nuevo título (opcional). Si no se envía, se conserva el actual.")
    @Pattern(regexp = ".*\\S.*", message = "El título no puede estar vacío.")
    @Size(max = 150, message = "El título no puede superar 150 caracteres.")
    private String titulo;

    @Schema(description = "Nuevo contenido (opcional). Si no se envía, se conserva el actual.")
    @Pattern(regexp = ".*\\S.*", message = "El contenido no puede estar vacío.")
    @Size(max = 1000, message = "El contenido no puede superar 1000 caracteres.")
    private String descripcion;

    @Schema(description = "Nueva prioridad (opcional): ALTA, MEDIA o BAJA.")
    private Prioridad prioridad;

    @Schema(description = "Nueva fecha de expiración (opcional), en formato ISO. Debe ser futura.")
    @Future(message = "La fecha de expiración debe ser posterior a la fecha actual.")
    private Instant fechaExpiracion;
    
}