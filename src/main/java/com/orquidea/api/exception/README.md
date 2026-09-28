Responsabilidad

La capa "exception" centraliza el manejo de errores. Debe incluir un controlador global de excepciones (@ControllerAdvice) que traduzca excepciones a respuestas HTTP coherentes.

Qué incluir en el futuro

- Clases de excepción específicas del dominio.
- Un GlobalExceptionHandler con @ExceptionHandler y mapeo a códigos HTTP.
- Eventos de auditoría o logs centralizados en caso de errores.

Notas

El controlador global es obligatorio para garantizar respuestas uniformes y trazabilidad.