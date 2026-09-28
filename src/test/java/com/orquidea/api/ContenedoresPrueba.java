package com.orquidea.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
class ContenedoresPrueba {

    static final String BUCKET = "orquidea-pruebas";
    private static final int PUERTO_S3 = 9000;
    private static final String ACCESS_KEY = "pruebas";
    private static final String SECRET_KEY = "pruebas-secreto";

    /** Almacenamiento S3 real (el mismo RustFS de docker-compose). */
    static class ContenedorAlmacenamiento extends GenericContainer<ContenedorAlmacenamiento> {

        ContenedorAlmacenamiento() {
            super("rustfs/rustfs:1.0.0");
            withExposedPorts(PUERTO_S3);
            withEnv("RUSTFS_ACCESS_KEY", ACCESS_KEY);
            withEnv("RUSTFS_SECRET_KEY", SECRET_KEY);
            waitingFor(Wait.forHttp("/health").forPort(PUERTO_S3));
        }

        String urlBase() {
            return "http://" + getHost() + ":" + getMappedPort(PUERTO_S3);
        }
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:18-alpine");
    }

    @Bean
    ContenedorAlmacenamiento almacenamiento() {
        return new ContenedorAlmacenamiento();
    }

    @Bean
    DynamicPropertyRegistrar propiedadesAlmacenamiento(ContenedorAlmacenamiento almacenamiento) {
        return registro -> {
            registro.add("app.storage.endpoint", almacenamiento::urlBase);
            registro.add("app.storage.url-publica", () -> almacenamiento.urlBase() + "/" + BUCKET);
            registro.add("app.storage.bucket", () -> BUCKET);
            registro.add("app.storage.access-key", () -> ACCESS_KEY);
            registro.add("app.storage.secret-key", () -> SECRET_KEY);
            registro.add("app.storage.crear-bucket", () -> "true");
        };
    }
}
