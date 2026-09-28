# Orquídea Backend

Backend para la **Plataforma de Educación Ambiental e Interactividad del Humedal La Orquídea**: un monolito Spring Boot por capas que gestiona fichas taxonómicas (según Darwin Core), la interactividad del sendero y el almacenamiento de activos en Cloudflare R2 (S3-compatible).

## Contexto del proyecto

El Humedal La Orquídea, en el barrio La María Occidente de Popayán, es un ecosistema en recuperación que el comité ambiental local, junto con voluntarios de la Universidad del Cauca, ha caracterizado biológicamente durante años (aves, plantas, mamíferos e insectos). El problema es que toda esa información permanece dispersa y es de difícil acceso tanto para la comunidad como para los visitantes del sendero.

Este backend es el soporte de la solución propuesta: una plataforma digital que centraliza y divulga públicamente la información biológica del humedal a través de fichas de especies, un mapa interactivo georreferenciado, códigos QR en las estaciones físicas del sendero y un módulo de reporte ciudadano de avistamientos, con soporte de funcionamiento sin conexión a internet. El objetivo es que el comité ambiental pueda administrar este contenido de forma autónoma, sin requerir conocimientos de programación.

## Tecnologías

- Spring Boot
- Java 21
- Maven
- PostgreSQL
- Docker & Docker Compose
- OpenAPI/Swagger (springdoc)
- Lombok

## Reglas estrictas de desarrollo (OBLIGATORIO)

1. **Estándar Darwin Core (DwC)**
   Todos los modelos de dominio y DTOs que representen información biológica deben nombrar entidades y atributos según DwC (ej.: `occurrenceID`, `scientificName`, `taxonRank`, `eventDate`).

2. **Persistencia**
   Uso exclusivo de Jakarta Persistence (JPA) y Spring Data donde proceda. Todas las entidades deben estar en el paquete `model` y los repositorios en `repository`.

3. **Arquitectura**
   Monolito estricto por capas. Separación clara entre `controllers`, `service`, `repository`, `model`, `dto`, `mapper`, `storage`, `config`, `security` y `exception`.
   - La lógica de subida a R2/S3 reside exclusivamente en `storage`; la configuración y beans asociados en `config`.
   - La seguridad (JWT, filtros, providers) reside únicamente en `security`.

4. **Manejo de errores**
   Uso obligatorio de un controlador global de excepciones (`@ControllerAdvice`) ubicado en `exception` para normalizar respuestas de error.

5. **Documentación de API**
   Uso obligatorio de Swagger/OpenAPI (springdoc). Todos los endpoints y DTOs deben documentarse con `@Operation`, `@ApiResponse`, `@Schema`, etc.

6. **Limpieza de código**
   Uso de Lombok (`@Data`, `@Builder`, `@Getter`/`@Setter`, `@RequiredArgsConstructor`) para reducir boilerplate.

7. **Seguridad de secretos**
   No hardcodear credenciales en el repositorio. Usar variables de entorno o mecanismos seguros.

## Guía de colaboración y ejecución

Clonar el repositorio:

```bash
git clone https://github.com/FaberGG/orquidea-backend
```

Levantar el entorno con Docker Compose:

```bash
docker compose up -d
```

### Servicios y URLs locales esperadas

| Servicio | URL / conexión |
|---|---|
| API | http://localhost:8080 |
| Swagger UI (springdoc) | http://localhost:8080/swagger-ui.html (o `/swagger-ui/index.html`) |
| pgAdmin | http://localhost:8081 (credenciales: `admin@orquidea.local` / `admin`) |
| PostgreSQL | `localhost:5432` (user: `orquidea_user`, password: `orquidea_pass`, db: `orquidea_db`) |

## Notas finales

- Esta estructura inicial es solo esqueleto y documentación; **no incluye código Java por ahora**.
- Próximos pasos recomendados:
   - Añadir `pom.xml` con dependencias (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `springdoc-openapi`, `lombok`, driver de Postgres).
   - Implementar entidades conforme a DwC.
   - Implementar el `GlobalExceptionHandler`.