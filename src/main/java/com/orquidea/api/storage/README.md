Responsabilidad

La capa "storage" gestiona la persistencia de objetos a servicios de almacenamiento como Cloudflare R2 (S3-compatible). Implementa la lógica de subida/descarga, firma de URLs y manejo de buckets.

Qué incluir en el futuro

- Clientes S3/R2 y servicios para subir/descargar archivos.
- Gestión de nombres de objetos, políticas de expiración y metadatos.
- Integración transaccional cuando sea necesario (coherencia eventual explicada).

Notas

La configuración (credenciales, endpoints) debe residir en config; la lógica de subida en storage.