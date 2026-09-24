Responsabilidad

La capa "security" agrupa la seguridad de la aplicación: JWT, filtros de seguridad, proveedores de autenticación y configuración de WebSecurity.

Qué incluir en el futuro

- Filtros de JWT, proveedores de UserDetails, utilidades de generación/validación de tokens.
- Configuración de acceso por roles y reglas de autorización.
- Exposición mínima de detalles sensibles y uso de buenas prácticas.

Notas

Toda la seguridad (autenticación y autorización) debe residir exclusivamente en este paquete.