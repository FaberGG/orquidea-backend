package com.orquidea.api.service;

import com.orquidea.api.config.MailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender enviadorCorreo;
    private final MailProperties propiedades;

    /** HU-5, escenario 2. Un fallo al enviar se registra, pero no deshace la revocación. */
    public void notificarRevocacionAcceso(String destinatario, String nombre) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        if (propiedades.remitente() != null && !propiedades.remitente().isBlank()) {
            mensaje.setFrom(propiedades.remitente());
        }
        mensaje.setTo(destinatario);
        mensaje.setSubject("Cambio en tu acceso a la plataforma del Humedal La Orquídea");
        mensaje.setText("Hola " + nombre + ",\n\n"
                + "Tu acceso como administrador fue revocado. Tu cuenta sigue activa como usuario registrado.\n\n"
                + "Humedal La Orquídea");
        try {
            enviadorCorreo.send(mensaje);
        } catch (MailException e) {
            log.warn("No se pudo notificar la revocación de acceso a {}", destinatario, e);
        }
    }


    /** HU-4 al crear un administrador se le envía un correo de notificación */
    public void notificarCreacionAdministrador(String destinatario, String nombre) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        if (propiedades.remitente() != null && !propiedades.remitente().isBlank()) {
            mensaje.setFrom(propiedades.remitente());
        }
        mensaje.setTo(destinatario);
        mensaje.setSubject("Bienvenido al sistema del Humedal La Orquídea");
        mensaje.setText("Hola " + nombre + ",\n\n"
                + "Has sido registrado como administrador del sistema.\n\n"
                + "Humedal La Orquídea");
        try {
            enviadorCorreo.send(mensaje);
        } catch (MailException e) {
            log.warn("No se pudo notificar la creación de administrador a {}", destinatario, e);
        }
    }



}