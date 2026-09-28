Responsabilidad

La capa "config" contiene la configuración de la aplicación: beans, datasources, OpenAPI/Swagger, y configuración de integración con Cloudflare R2 (credenciales y clients).

Qué incluir en el futuro

- Configuraciones de DataSource/JPA, beans de mapeo, configuración de OpenAPI (springdoc).
- Properties y manejo seguro de secretos (no hardcodear). Integración con storage en storage package.

Notas

No implementar lógica de negocio aquí; solo configuraciones y factories.