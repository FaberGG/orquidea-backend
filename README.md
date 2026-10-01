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

Crear el archivo de variables locales (no se sube al repositorio) y ajustar valores si hace falta:

```bash
cp .env.example .env
```

Luego, una de dos opciones:

- **Desarrollo desde el IDE (recomendado):** ejecutar `OrquideaBackendApplication`. Spring Boot levanta automáticamente `db`, `storage` y `pgadmin` con Docker Compose y lee el `.env`.
- **Todo en contenedores:** `docker compose --profile app up -d --build`

`docker compose up -d` sin perfil levanta solo `db`, `storage` y `pgadmin`.

### Servicios y URLs locales esperadas

| Servicio | URL / conexión |
|---|---|
| API | http://localhost:8080 (o el `PUERTO_API` de tu `.env`) |
| Swagger UI (springdoc) | http://localhost:8080/swagger-ui.html |
| pgAdmin | http://localhost:8081 (credenciales: `PGADMIN_CORREO` / `PGADMIN_CONTRASENA` del `.env`) |
| PostgreSQL | `localhost:5432` (credenciales `POSTGRES_*` del `.env`) |
| Almacenamiento S3 (RustFS) | API `http://localhost:9000`, consola http://localhost:9001 (credenciales `STORAGE_ACCESS_KEY` / `STORAGE_SECRET_KEY`) |

Al arrancar se crea el primer **superadministrador** con `SUPERADMIN_CORREO` / `SUPERADMIN_CONTRASENA`, solo si todavía no existe ninguno.

### Almacenamiento de archivos

Las fotos se suben a un almacenamiento compatible con S3. En desarrollo es **RustFS** (contenedor `storage`), que reemplaza a Cloudflare R2; al arrancar, la API crea el bucket con lectura pública. Para usar R2 basta con cambiar las variables `STORAGE_*` del `.env` (ver comentarios en `.env.example`) y poner `STORAGE_CREAR_BUCKET=false`: el código es el mismo. En la base de datos se guarda la clave del objeto, no la URL, así que cambiar de proveedor no requiere migrar datos.

### Base de datos

El esquema se versiona con **Flyway** en `src/main/resources/db/migration` (`V<n>__descripcion.sql`). Hibernate solo valida (`ddl-auto: validate`). Nunca se edita una migración ya subida: los cambios van en una nueva.

### Pruebas

```bash
./mvnw verify
```

Las pruebas de integración usan **Testcontainers** (PostgreSQL y RustFS reales), así que Docker debe estar corriendo.

## Convención de código y commits

### Nombres

- **Clases en inglés**, con el sufijo de su capa:

  | Capa | Sufijo | Ejemplo |
  |---|---|---|
  | `controllers` | `Controller` | `TaxonController` |
  | `service` | `Service` | `AuthService` |
  | `repository` | `Repository` | `UserRepository` |
  | `mapper` | `Mapper` | `TaxonMapper` |
  | `dto` | `Request` / `Response` / `Dto` | `LoginRequest`, `LoginResponse`, `TaxonDto` |
  | `exception` | `Exception` | `ResourceNotFoundException` |
  | `config` | `Config` / `Properties` / `Initializer` | `StorageConfig`, `JwtProperties` |
  | pruebas | `Test` | `AuthControllerTest` |

- Datos biológicos en **Darwin Core** (regla 1): la entidad (`Taxon`) y sus atributos y campos del JSON (`order`, `family`, `genus`, `scientificName`, `vernacularName`). Los atributos biológicos sin término DwC (`alimentacion`, `rolEnHumedal`, `estadoConservacion`) y los propios de la app (`categoria`) van en español.
- El resto de métodos, variables, campos, rutas de la API, campos del JSON y mensajes al usuario siguen en **español**, sin tildes ni ñ en los identificadores (`contrasena`, `/api/fichas-taxonomicas`).
- Tablas y columnas en español, también para los datos biológicos (`orden`, `nombre_cientifico`): se mapean con `@Column(name = ...)`, y así se evitan palabras reservadas de SQL como `order`.
- Archivos en **UTF-8 sin BOM** (lo fija `.editorconfig`); `javac` no compila archivos con BOM.

### Ramas y commits

- Ramas: `feature/hu-<n>-<descripcion>`, `fix/<descripcion>` o `refactor/<descripcion>`, creadas desde `dev`.
- Commits en **inglés** con [Conventional Commits](https://www.conventionalcommits.org/): `<type>(<scope>): <description>`, con el alcance opcional (la HU o el módulo).

| Tipo | Uso |
|---|---|
| `feat` | Funcionalidad nueva |
| `fix` | Corrección de errores |
| `refactor` | Cambio de código sin cambiar el comportamiento |
| `test` | Solo pruebas |
| `docs` | Documentación |
| `chore` | Dependencias, Docker, build, CI |

Ejemplos: `feat(HU-1): add JWT login`, `fix(auth): empty fields message`, `chore: bump springdoc`.
