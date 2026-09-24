Responsabilidad

La capa "repository" gestiona la persistencia usando Jakarta Persistence (JPA). Aquí irán los repositorios de Spring Data JPA o implementaciones personalizadas de acceso a datos.

Qué incluir en el futuro

- Interfaces Spring Data JPA (extends JpaRepository/CrudRepository).
- Consultas personalizadas (JPQL, @Query) y repositorios personalizados si es necesario.
- Mapeo de transacciones y optimizaciones de consultas.

Notas

No usar consultas SQL ad-hoc fuera de repositorios; mantener la persistencia encapsulada.