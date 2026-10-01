package com.orquidea.api.service;

import com.orquidea.api.config.MailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Prueba el envío contra un servidor simulado de Resend, sin contexto de Spring. */
class EmailNotificationServiceTest {

    private static final String URL_API = "https://api.resend.com";
    private static final String REMITENTE = "Humedal <no-responder@orquidea.test>";
    private static final String DESTINATARIO = "ana@prueba.local";

    private RestClient clienteResend;
    private MockRestServiceServer resend;

    @BeforeEach
    void preparar() {
        RestClient.Builder constructor = RestClient.builder().baseUrl(URL_API);
        resend = MockRestServiceServer.bindTo(constructor).build();
        clienteResend = constructor.build();
    }

    private EmailNotificationService servicioConClave(String claveApi) {
        return new EmailNotificationService(clienteResend,
                new MailProperties(REMITENTE, claveApi, URL_API, Duration.ofSeconds(5)));
    }

    @Test
    void enviaElCorreoALaApiDeResend() {
        resend.expect(requestTo(URL_API + "/emails"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer re_prueba"))
                .andExpect(jsonPath("$.from").value(REMITENTE))
                .andExpect(jsonPath("$.to[0]").value(DESTINATARIO))
                .andExpect(jsonPath("$.subject").value("Bienvenido al sistema del Humedal La Orquídea"))
                .andExpect(jsonPath("$.text").value(containsString("Hola Ana")))
                .andRespond(withSuccess("{\"id\":\"correo-1\"}", MediaType.APPLICATION_JSON));

        servicioConClave("re_prueba").notificarCreacionAdministrador(DESTINATARIO, "Ana");

        resend.verify();
    }

    @Test
    void unErrorDeResendNoSePropaga() {
        resend.expect(requestTo(URL_API + "/emails"))
                .andRespond(withStatus(HttpStatusCode.valueOf(422))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"name\":\"validation_error\",\"message\":\"The domain is not verified.\"}"));

        assertThatNoException().isThrownBy(() ->
                servicioConClave("re_prueba").notificarRevocacionAcceso(DESTINATARIO, "Ana"));

        resend.verify();
    }

    @Test
    void sinClaveApiNoLlamaAResend() {
        // Sin expectativas registradas, cualquier petición haría fallar la prueba
        servicioConClave("").notificarCreacionAdministrador(DESTINATARIO, "Ana");

        resend.verify();
    }
}
