Responsabilidad

La capa "controllers" expone la API HTTP REST del monolito. Contendrá controladores Spring MVC (@RestController) encargados de recibir peticiones, validar entrada y delegar la lógica al servicio correspondiente.

Qué incluir en el futuro

- Endpoints REST organizados por recursos (ej.: TaxonController, TrailInteractionController).
- Validaciones de entrada (DTOs anotados con javax/validation).
- Anotaciones de documentación OpenAPI (@Operation, @Parameter).
- Manejo ligero de errores locales (delegar a exception package para control global).

Notas

Mantener controladores finos: orquestación y conversión DTO ↔ dominio, sin lógica de negocio.

HU-11: `TaxonController` expone `GET /api/fichas-taxonomicas/{id}/qr` para descargar un PNG de 1024 × 1024. La ruta requiere sesión y el método autoriza ADMINISTRADOR/SUPERADMINISTRADOR; verifica que la ficha exista y delega la generación a `TaxonQrService`. Responde `Content-Type: image/png` y `Content-Disposition: attachment; filename=orquidea-ficha-{uuid}.png`. El QR contiene únicamente `orquideamobileapp://species/{uuid}`. La imagen se genera en memoria: no se sube a RustFS, no se persiste y no cambia la ficha. La especificación OpenAPI declara la respuesta 200 como binario PNG y documenta los errores 401, 403, 404 y 500. La consulta pública de ficha sigue siendo `GET /api/fichas-taxonomicas/{id}`.

Para validar manualmente: iniciar sesión con una cuenta `ADMINISTRADOR` o `SUPERADMINISTRADOR`, copiar el UUID de una ficha existente y descargar `GET /api/fichas-taxonomicas/{uuid}/qr` enviando `Authorization: Bearer {token}`. Confirmar que el archivo descargado es PNG y que un lector QR obtiene exactamente `orquideamobileapp://species/{uuid}`. Un usuario público no debe poder descargar el QR; la consulta pública de la ficha permanece disponible sin token.
