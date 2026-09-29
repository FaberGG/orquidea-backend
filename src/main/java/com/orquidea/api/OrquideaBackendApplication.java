package com.orquidea.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Sin UserDetailsServiceAutoConfiguration: la autenticación es por JWT y no queremos el usuario en memoria por defecto.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class OrquideaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrquideaBackendApplication.class, args);
    }

}
