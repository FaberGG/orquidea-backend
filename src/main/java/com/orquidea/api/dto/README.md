Responsabilidad

La capa "dto" contiene objetos de transferencia entre API y la capa interna. Los DTOs deben reflejar los atributos relevantes para cada endpoint y usar nomenclatura DwC para datos biológicos.

Qué incluir en el futuro

- DTOs de petición y respuesta anotados con Swagger/OpenAPI (@Schema) y validación.
- Conversores entre DTOs y entidades (pueden apoyarse en mappers).
- Uso de Lombok (@Data, @Builder) para reducir boilerplate.

Notas

No incluir lógica de negocio; solo representación y validación de datos.