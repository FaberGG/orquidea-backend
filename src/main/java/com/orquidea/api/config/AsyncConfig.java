package com.orquidea.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Habilita @Async; las tareas usan el ejecutor que configura Spring Boot (applicationTaskExecutor). */
@Configuration
@EnableAsync
public class AsyncConfig {
}
