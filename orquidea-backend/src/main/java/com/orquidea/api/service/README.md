Responsabilidad

La capa "service" contiene la lógica de negocio del monolito por capas. Aquí se implementan casos de uso, reglas de negocio y orquestación entre repositorios, mappers y almacenamiento.

Qué incluir en el futuro

- Clases de servicio (@Service) para operaciones transaccionales.
- Interfaces que definan contratos y sus implementaciones.
- Uso de DTO y entidades (nombres según Darwin Core cuando representen datos biológicos).
- Manejo de transacciones JPA y llamadas al storage para subida a R2/S3.

Notas

No colocar lógica de infraestructura aquí; delegar BD a repository y subida a storage.