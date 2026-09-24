Responsabilidad

La capa "controllers" expone la API HTTP REST del monolito. Contendrá controladores Spring MVC (@RestController) encargados de recibir peticiones, validar entrada y delegar la lógica al servicio correspondiente.

Qué incluir en el futuro

- Endpoints REST organizados por recursos (ej.: TaxonController, TrailInteractionController).
- Validaciones de entrada (DTOs anotados con javax/validation).
- Anotaciones de documentación OpenAPI (@Operation, @Parameter).
- Manejo ligero de errores locales (delegar a exception package para control global).

Notas

Mantener controladores finos: orquestación y conversión DTO ↔ dominio, sin lógica de negocio.