package com.orquidea.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
public class MailConfig {

    /**
     * Cliente de la API de Resend. Los correos se envían dentro de la petición del usuario,
     * así que los tiempos de espera evitan que una caída de Resend deje la petición colgada.
     */
    @Bean
    public RestClient clienteResend(MailProperties propiedades) {
        HttpClient clienteHttp = HttpClient.newBuilder()
                .connectTimeout(propiedades.tiempoEspera())
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(clienteHttp);
        fabrica.setReadTimeout(propiedades.tiempoEspera());
        return RestClient.builder()
                .baseUrl(propiedades.urlApi())
                .requestFactory(fabrica)
                .build();
    }
}
