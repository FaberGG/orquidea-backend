# Graph Report - .  (2026-10-01)

## Corpus Check
- Corpus is ~14,772 words - fits in a single context window. You may not need a graph.

## Summary
- 465 nodes · 774 edges · 43 communities (20 shown, 23 thin omitted)
- Extraction: 84% EXTRACTED · 15% INFERRED · 0% AMBIGUOUS · INFERRED: 119 edges (avg confidence: 0.8)
- Token cost: 262,413 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Pruebas de fichas taxonomicas|Pruebas de fichas taxonomicas]]
- [[_COMMUNITY_Usuarios y filtro JWT|Usuarios y filtro JWT]]
- [[_COMMUNITY_Flujo de administradores|Flujo de administradores]]
- [[_COMMUNITY_Auth, errores y CORS|Auth, errores y CORS]]
- [[_COMMUNITY_Configuracion e infraestructura|Configuracion e infraestructura]]
- [[_COMMUNITY_Imagenes y almacenamiento S3|Imagenes y almacenamiento S3]]
- [[_COMMUNITY_Pruebas de autenticacion|Pruebas de autenticacion]]
- [[_COMMUNITY_Servicio de fichas taxonomicas|Servicio de fichas taxonomicas]]
- [[_COMMUNITY_Excepciones de dominio|Excepciones de dominio]]
- [[_COMMUNITY_Inicializadores de arranque|Inicializadores de arranque]]
- [[_COMMUNITY_Arquitectura y convenciones|Arquitectura y convenciones]]
- [[_COMMUNITY_Manejador global de errores|Manejador global de errores]]
- [[_COMMUNITY_Testcontainers|Testcontainers]]
- [[_COMMUNITY_TaxonController|TaxonController]]
- [[_COMMUNITY_SecurityConfig|SecurityConfig]]
- [[_COMMUNITY_Interfaz StorageService|Interfaz StorageService]]
- [[_COMMUNITY_AuthController|AuthController]]
- [[_COMMUNITY_Entidad Taxon|Entidad Taxon]]
- [[_COMMUNITY_AdministratorMapper|AdministratorMapper]]
- [[_COMMUNITY_Clase principal|Clase principal]]
- [[_COMMUNITY_OpenApiConfig|OpenApiConfig]]
- [[_COMMUNITY_StorageConfig|StorageConfig]]
- [[_COMMUNITY_Entidad User|Entidad User]]
- [[_COMMUNITY_TaxonRequest|TaxonRequest]]
- [[_COMMUNITY_AdministratorUpdateRequest|AdministratorUpdateRequest]]
- [[_COMMUNITY_ApiErrorResponse|ApiErrorResponse]]
- [[_COMMUNITY_LoginRequest|LoginRequest]]
- [[_COMMUNITY_LoginResponse|LoginResponse]]
- [[_COMMUNITY_TaxonDto|TaxonDto]]
- [[_COMMUNITY_AdministratorDto|AdministratorDto]]
- [[_COMMUNITY_AuthenticatedUserDto|AuthenticatedUserDto]]
- [[_COMMUNITY_RegisterRequest|RegisterRequest]]
- [[_COMMUNITY_RegisterResponse|RegisterResponse]]

## God Nodes (most connected - your core abstractions)
1. `TaxonControllerTest` - 44 edges
2. `AuthControllerTest` - 24 edges
3. `GlobalExceptionHandler` - 18 edges
4. `UserRepository` - 14 edges
5. `GlobalExceptionHandler` - 13 edges
6. `AdministratorService.registrarAdministrador()` - 10 edges
7. `TaxonService` - 9 edges
8. `S3StorageService` - 9 edges
9. `TaxonMapper` - 9 edges
10. `UserRepository` - 8 edges

## Surprising Connections (you probably didn't know these)
- `GlobalExceptionHandler.manejarIntegridad()` --semantically_similar_to--> `DuplicateResourceException`  [INFERRED] [semantically similar]
  src/main/java/com/orquidea/api/exception/GlobalExceptionHandler.java → CLAUDE.md
- `Role Hierarchy SUPERADMINISTRADOR > ADMINISTRADOR` --conceptually_related_to--> `usuarios table (V1)`  [INFERRED]
  CLAUDE.md → src/main/resources/db/migration/V1__crear_tabla_usuarios.sql
- `AdministratorController` --references--> `ApiErrorResponse`  [AMBIGUOUS]
  src/main/java/com/orquidea/api/controllers/AdministratorController.java → CLAUDE.md
- `UserRepository` --conceptually_related_to--> `OperationNotAllowedException`  [INFERRED]
  src/main/java/com/orquidea/api/repository/UserRepository.java → CLAUDE.md
- `Store Object Key (fotoClave), Not URL` --rationale_for--> `fichas_taxonomicas table (V2)`  [INFERRED]
  CLAUDE.md → src/main/resources/db/migration/V2__crear_tabla_fichas_taxonomicas.sql

## Hyperedges (group relationships)
- **JWT Authentication & Authorization Flow** — jwtservice_jwtservice, jwtauthenticationfilter_jwtauthenticationfilter, securityconfig_securityconfig, claude_role_hierarchy, v1_crear_tabla_usuarios_usuarios [INFERRED 0.85]
- **Photo Storage Pipeline (key stored, URL built at mapping)** — storageservice_storageservice, s3storageservice_s3storageservice, imagevalidator_imagevalidator, storageproperties_storageproperties, taxonmapper_taxonmapper, claude_storage_key_not_url, docker_compose_storage_service [INFERRED 0.85]
- **Centralized Error Normalization** — globalexceptionhandler_globalexceptionhandler, apierrorresponse_apierrorresponse, resourcenotfoundexception_resourcenotfoundexception, duplicateresourceexception_duplicateresourceexception, operationnotallowedexception_operationnotallowedexception, securityconfig_securityconfig [EXTRACTED 1.00]
- **Login and session restoration flow (HU-1)** — authcontroller_iniciarsesion, loginrequest_loginrequest, authservice_iniciarsesion, loginresponse_loginresponse, authenticateduserdto_authenticateduserdto, authcontroller_obtenerusuarioactual, jwtprincipal_jwtprincipal [EXTRACTED 1.00]
- **Taxonomic record create/edit/query flow (HU-7, HU-8, HU-10)** — taxoncontroller_taxoncontroller, taxonrequest_taxonrequest, taxonrequest_oncreate, taxonservice_taxonservice, taxondto_taxondto, taxoncategory_taxoncategory, conservationstatus_conservationstatus [EXTRACTED 1.00]
- **ApplicationRunner startup bootstrap driven by configuration properties** — storageinitializer_storageinitializer, storageproperties_storageproperties, storageservice_storageservice, superadmininitializer_superadmininitializer, superadminproperties_superadminproperties, userservice_userservice [INFERRED 0.85]
- **Centralized exception-to-HTTP error translation** — globalexceptionhandler_globalexceptionhandler, globalexceptionhandler_construir, apierrorresponse_apierrorresponse, duplicateresourceexception_duplicateresourceexception, resourcenotfoundexception_resourcenotfoundexception, invalidrequestexception_invalidrequestexception, invalidimageformatexception_invalidimageformatexception, imagetoolargeexception_imagetoolargeexception, invalidcredentialsexception_invalidcredentialsexception, operationnotallowedexception_operationnotallowedexception [EXTRACTED 1.00]
- **Taxon entity mapping and persistence** — taxon_taxon, taxonmapper_taxonmapper, taxonrepository_taxonrepository, taxonrequest_taxonrequest, taxondto_taxondto, taxoncategory_taxoncategory, conservationstatus_conservationstatus [INFERRED 0.85]
- **User/administrator entity mapping and persistence** — user_user, role_role, userrepository_userrepository, usermapper_usermapper, administratormapper_administratormapper [INFERRED 0.85]
- **JWT login and per-request authentication flow** — authservice_iniciarsesion, jwtservice_generartoken, jwtservice_validartoken, jwtauthenticationfilter_dofilterinternal, jwtprincipal_jwtprincipal, securityconfig_cadenafiltrosseguridad, userrepository_userrepository [EXTRACTED 1.00]
- **Transaction-bound taxon photo upload and cleanup** — taxonservice_crear, taxonservice_actualizar, taxonservice_subirfoto, taxonservice_alconfirmar, imagevalidator_validar, imagetype_detectar, storageservice_subir, storageservice_eliminar [EXTRACTED 1.00]
- **Admin lifecycle changes with after-commit email notification** — administratorservice_registraradministrador, administratorservice_revocaracceso, administratorservice_alconfirmar, emailnotificationservice_notificarcreacionadministrador, emailnotificationservice_notificarrevocacionacceso [EXTRACTED 1.00]

## Communities (43 total, 23 thin omitted)

### Community 1 - "Usuarios y filtro JWT"
Cohesion: 0.06
Nodes (10): AdministratorController, UserMapper, OncePerRequestFilter, UserRepository, JwtAuthenticationFilter, JwtService, AdministratorService, AuthService (+2 more)

### Community 2 - "Flujo de administradores"
Cohesion: 0.1
Nodes (39): AdministratorController.actualizar(), AdministratorController.registrar(), AdministratorController.revocarAcceso(), AdministratorDto, AdministratorMapper.actualizar(), AdministratorMapper, AdministratorProperties, AdministratorService.actualizar() (+31 more)

### Community 3 - "Auth, errores y CORS"
Cohesion: 0.07
Nodes (35): AdministratorController, AdministratorService, ApiErrorResponse, app.cors config, Multipart upload limits (10MB image / 12MB request), AuthController, AuthControllerTest, AuthService (+27 more)

### Community 4 - "Configuracion e infraestructura"
Cohesion: 0.08
Nodes (34): app.correo.remitente config, app.jwt config, app.storage config, app.superadministrador config, spring.datasource / jpa config, spring.mail config, Startup Bootstrap (superadmin + bucket creation), Env Vars -> application.yaml app.* -> @ConfigurationProperties records (+26 more)

### Community 5 - "Imagenes y almacenamiento S3"
Cohesion: 0.11
Nodes (27): Store Object Key (fotoClave), Not URL, ConservationStatus, ImageType, ImageValidator.validar(), ImageValidator.ValidatedImage, StorageConfig.clienteS3(), StorageConfig, StorageService.eliminar() (+19 more)

### Community 6 - "Pruebas de autenticacion"
Cohesion: 0.1
Nodes (3): OrquideaBackendApplicationTests, AuthControllerTest, IntegrationTestBase

### Community 7 - "Servicio de fichas taxonomicas"
Cohesion: 0.11
Nodes (7): TaxonMapper, TaxonRepository, TaxonService, detectar(), extension(), tipoContenido(), ImageValidator

### Community 8 - "Excepciones de dominio"
Cohesion: 0.09
Nodes (8): DuplicateResourceException, ImageTooLargeException, InvalidCredentialsException, InvalidImageFormatException, InvalidRequestException, OperationNotAllowedException, ResourceNotFoundException, RuntimeException

### Community 9 - "Inicializadores de arranque"
Cohesion: 0.12
Nodes (6): ApplicationRunner, StorageInitializer, SuperAdminInitializer, estaConfigurado(), S3StorageService, StorageService

### Community 10 - "Arquitectura y convenciones"
Cohesion: 0.17
Nodes (19): Strict Layered Monolith (controllers -> service -> repository/storage), Naming Conventions (English classes, Spanish identifiers), Normalized Error Responses via GlobalExceptionHandler, Config Layer, Controllers Layer, DTO Layer, Exception Layer, Mapper Layer (MapStruct) (+11 more)

## Ambiguous Edges - Review These
- `ApiErrorResponse` → `AdministratorController`  [AMBIGUOUS]
  src/main/java/com/orquidea/api/controllers/AdministratorController.java · relation: references
- `S3StorageService` → `ImageValidator`  [AMBIGUOUS]
  CLAUDE.md · relation: calls
- `InvalidCredentialsException` → `UserRepository`  [AMBIGUOUS]
  src/main/java/com/orquidea/api/repository/UserRepository.java · relation: conceptually_related_to

## Knowledge Gaps
- **29 isolated node(s):** `AdministratorDto`, `AdministratorUpdateRequest`, `ApiErrorResponse`, `AuthenticatedUserDto`, `LoginRequest` (+24 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **23 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `ApiErrorResponse` and `AdministratorController`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `S3StorageService` and `ImageValidator`?**
  _Edge tagged AMBIGUOUS (relation: calls) - confidence is low._
- **What is the exact relationship between `InvalidCredentialsException` and `UserRepository`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `UserRepository` connect `Usuarios y filtro JWT` to `Pruebas de fichas taxonomicas`?**
  _High betweenness centrality (0.043) - this node is a cross-community bridge._
- **Why does `TaxonService` connect `Servicio de fichas taxonomicas` to `Inicializadores de arranque`?**
  _High betweenness centrality (0.037) - this node is a cross-community bridge._
- **Are the 2 inferred relationships involving `UserRepository` (e.g. with `DuplicateResourceException` and `OperationNotAllowedException`) actually correct?**
  _`UserRepository` has 2 INFERRED edges - model-reasoned connections that need verification._
- **Are the 2 inferred relationships involving `GlobalExceptionHandler` (e.g. with `ApiErrorResponse` and `SecurityConfig.cadenaFiltrosSeguridad()`) actually correct?**
  _`GlobalExceptionHandler` has 2 INFERRED edges - model-reasoned connections that need verification._