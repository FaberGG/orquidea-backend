# Matriz de permisos de endpoints

Auditoría de los permisos de los endpoints implementados en la API. Fuentes:

- `security/SecurityConfig.java`: rutas públicas (`authorizeHttpRequests`) y `anyRequest().authenticated()`.
- `@PreAuthorize` en los controladores.
- Jerarquía de roles `SUPERADMINISTRADOR > ADMINISTRADOR` (`SecurityConfig#jerarquiaRoles`).
- Reglas de negocio en los servicios (`AdministratorService`, `AuthService`).

## Roles

| Rol | Descripción |
|---|---|
| Visitante | Petición sin token, o con un token inválido o vencido, o de una cuenta inhabilitada |
| `USUARIO_REGISTRADO` | Usuario autenticado creado con `/registro` o con acceso de administrador revocado |
| `ADMINISTRADOR` | Gestiona las fichas taxonómicas |
| `SUPERADMINISTRADOR` | Hereda todos los permisos de `ADMINISTRADOR` y además gestiona administradores |

## Matriz

Leyenda: ✅ permitido · ❌ 401 sin sesión · ❌ 403 rol insuficiente

| # | Método | Endpoint | Visitante | Usuario registrado | Administrador | Superadmin | Definido en |
|---|---|---|:-:|:-:|:-:|:-:|---|
| 1 | POST | `/api/autenticacion/iniciar-sesion` | ✅ | ✅ | ✅ | ✅ | `SecurityConfig` (permitAll) |
| 2 | POST | `/api/autenticacion/registro` | ✅ | ✅ | ✅ | ✅ | `SecurityConfig` (permitAll) |
| 3 | POST | `/api/autenticacion/recuperar-contrasena` | ✅ | ✅ | ✅ | ✅ | `SecurityConfig` (permitAll) |
| 4 | POST | `/api/autenticacion/restablecer-contrasena` | ✅ | ✅ | ✅ | ✅ | `SecurityConfig` (permitAll) |
| 5 | GET | `/api/autenticacion/yo` | ❌ 401 | ✅ | ✅ | ✅ | `anyRequest().authenticated()` |
| 6 | GET | `/api/fichas-taxonomicas` (`?categoria=`) | ✅ | ✅ | ✅ | ✅ | `SecurityConfig` (permitAll) |
| 7 | GET | `/api/fichas-taxonomicas/{id}` | ✅ | ✅ | ✅ | ✅ | `SecurityConfig` (permitAll) |
| 8 | POST | `/api/fichas-taxonomicas` (multipart) | ❌ 401 | ❌ 403 | ✅ | ✅ (por jerarquía) | `TaxonController` `@PreAuthorize("hasRole('ADMINISTRADOR')")` |
| 9 | PUT | `/api/fichas-taxonomicas/{id}` (multipart) | ❌ 401 | ❌ 403 | ✅ | ✅ (por jerarquía) | `TaxonController` `@PreAuthorize("hasRole('ADMINISTRADOR')")` |
| 10 | PUT | `/api/administradores/{id}` | ❌ 401 | ❌ 403 | ❌ 403 | ✅ | `AdministratorController` `@PreAuthorize("hasRole('SUPERADMINISTRADOR')")` (clase) |
| 11 | POST | `/api/administradores/{id}/revocar-acceso` | ❌ 401 | ❌ 403 | ❌ 403 | ✅ | `AdministratorController` (clase) |
| 12 | POST | `/api/administradores/registrarAdmin` | ❌ 401 | ❌ 403 | ❌ 403 | ✅ | `AdministratorController` (clase) |

Rutas de infraestructura públicas para todos: `OPTIONS /**` (preflight CORS), `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` y `/error`.

## Reglas de negocio adicionales

Estas reglas no dependen del rol sino de la lógica de cada servicio:

- **`PUT /api/administradores/{id}`** solo actúa sobre cuentas con rol `ADMINISTRADOR` (`AdministratorService#actualizar`). Si el id es de un superadministrador o de un usuario registrado responde **404**.
- **`POST /api/administradores/{id}/revocar-acceso`** acepta cuentas `ADMINISTRADOR` o `SUPERADMINISTRADOR` y las degrada a `USUARIO_REGISTRADO`. Responde **409** si se intenta revocar al único superadministrador habilitado.
- **`POST /api/administradores/registrarAdmin`** responde **409** si ya hay tantos administradores habilitados como `ADMINISTRADORES_LIMITE` (3 por defecto).
- **`POST /api/autenticacion/registro`** siempre crea cuentas `USUARIO_REGISTRADO` (`AuthService#registrarUsuario`), así que no permite escalar privilegios.

## Comportamiento transversal

1. **Cuenta inhabilitada = visitante.** `JwtAuthenticationFilter` ignora el token si `habilitado = false`, así que en las rutas protegidas esa cuenta recibe **401**, no 403.
2. **Un token inválido en una ruta pública no se rechaza.** El filtro no corta la cadena y la petición sigue como anónima.
3. **El rol se lee de la base de datos en cada petición.** Revocar o inhabilitar una cuenta tiene efecto inmediato aunque el token siga vigente.
4. **Rutas no mapeadas:** el visitante recibe 401 (por `anyRequest().authenticated()`) y el usuario autenticado recibe 404.

## Hallazgos de la auditoría

| Severidad | Hallazgo |
|---|---|
| Media | **El rol `USUARIO_REGISTRADO` no tiene ningún permiso propio.** Fuera de lo público solo puede usar `GET /yo`, así que en la práctica equivale a un visitante con sesión. |
| Media | **No hay endpoint para consultar administradores** (listar u obtener por id). El superadministrador puede editar o revocar pero no ver el listado. |
| Media | **No hay `DELETE` de fichas taxonómicas**, aunque CORS sí permite el método `DELETE`. |
| Baja | **Inconsistencia entre editar y revocar:** editar solo sirve para `ADMINISTRADOR`, pero revocar también sirve para `SUPERADMINISTRADOR`. |
| Baja | **Un superadministrador puede revocarse a sí mismo** si existe otro superadministrador habilitado. |
| Baja | **La ruta `/registrarAdmin` está en camelCase.** Rompe la convención kebab-case del resto de rutas (`revocar-acceso`, `iniciar-sesion`). |
| Baja (docs) | **Esquema de error mal documentado en Swagger:** `AdministratorController` documenta los errores con `@Schema(implementation = ApiResponse.class)` en lugar de `ApiErrorResponse.class`. Además, `registrarAdmin` no documenta el 409 por límite de administradores. |
| Info | **`/registro` también está abierto a usuarios autenticados.** No es un riesgo porque solo crea `USUARIO_REGISTRADO`. |
