package com.orquidea.api.service;

import com.orquidea.api.config.MailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private static final String RUTA_ENVIO = "/emails";

    private final RestClient clienteResend;
    private final MailProperties propiedades;

    /** HU-5, escenario 2. Un fallo al enviar se registra, pero no deshace la revocación. */
    public void notificarRevocacionAcceso(String destinatario, String nombre) {
        enviar(destinatario,
                "Cambio en tu acceso a la plataforma del Humedal La Orquídea",
                "Hola " + nombre + ",\n\n"
                        + "Tu acceso como administrador fue revocado. Tu cuenta sigue activa como usuario registrado.\n\n"
                        + "Humedal La Orquídea");
    }

    /** HU-4 al crear un administrador se le envía un correo de notificación */
    public void notificarCreacionAdministrador(String destinatario, String nombre) {
        enviar(destinatario,
                "Bienvenido al sistema del Humedal La Orquídea",
                "Hola " + nombre + ",\n\n"
                        + "Has sido registrado como administrador del sistema.\n\n"
                        + "Humedal La Orquídea");
    }

    /**
     * Recuperación de contraseña. Es asíncrono para que la solicitud tarde lo mismo exista o no la cuenta:
     * si la respuesta esperara a Resend, solo tardaría más cuando el correo está registrado.
     */
    @Async
    public void enviarCodigoRecuperacion(String destinatario, String nombre, String codigo, Duration vigencia) {
        enviar(destinatario,
                "Código para restablecer tu contraseña",
                "Hola " + nombre + ",\n\n"
                        + "Tu código para restablecer la contraseña es: " + codigo + "\n\n"
                        + "Vence en " + vigencia.toMinutes() + " minutos. Si no lo solicitaste, ignora este correo;"
                        + " tu contraseña no cambiará.\n\n"
                        + "Humedal La Orquídea");
    }

    /**
     * Los errores de Resend (red, API key inválida, remitente sin verificar...) se registran y no se propagan:
     * el correo es un aviso y no debe hacer fallar la operación que lo originó.
     */
    private void enviar(String destinatario, String asunto, String texto) {
        if (propiedades.claveApi() == null || propiedades.claveApi().isBlank()) {
            log.info("RESEND_API_KEY no está configurada; no se envía el correo '{}' a {}", asunto, destinatario);
            return;
        }
        try {
            clienteResend.post()
                    .uri(RUTA_ENVIO)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + propiedades.claveApi())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SolicitudCorreo(propiedades.remitente(), List.of(destinatario), asunto, texto))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("No se pudo enviar el correo '{}' a {}", asunto, destinatario, e);
        }
    }

    /** Cuerpo de POST /emails; los nombres de los campos los define la API de Resend. */
    record SolicitudCorreo(String from, List<String> to, String subject, String text) {
    }
}
