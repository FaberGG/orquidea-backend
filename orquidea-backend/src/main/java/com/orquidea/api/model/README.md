Responsabilidad

La capa "model" define las entidades persistentes del dominio. Deben mapearse con Jakarta Persistence (JPA) y usar nombres y atributos ajustados al estándar Darwin Core (DwC) cuando correspondan.

Qué incluir en el futuro

- Entidades JPA anotadas (@Entity, @Table, @Id).
- Mapas de relaciones y estrategias de carga.
- Comentarios sobre correspondencia con términos DwC (ej.: scientificName, taxonRank, occurrenceID).

Notas

Evitar lógica de negocio en las entidades; mantenerlas como representación del estado.