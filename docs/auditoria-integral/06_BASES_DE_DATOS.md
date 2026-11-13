# Bases de datos y persistencia

## Motor y configuración declarada
El backend declara MySQL mediante `mysql-connector-j` y `flyway-mysql`. `application.yml` espera una URL JDBC y credenciales por entorno; Hibernate valida el esquema, no lo crea (`ddl-auto: validate`). Las pruebas futuras tienen dependencias Testcontainers MySQL. No hay dependencia MongoDB en el `pom.xml`.

## Esquema inferido del código, no verificado en base real

| Tabla/relación | Evidencia | Campos o relación inferidos |
|---|---|---|
| `app_user` | `@Table` en `AppUser` | `id BINARY(16)` PK, correo normalizado, hash, nombre, estado, banderas/fechas y bloqueo |
| `role` | `@Table` en `Role` | `id BINARY(16)` PK, `code` único, nombre, descripción, activo |
| `permission` | `@Table` en `Permission` | `id BINARY(16)` PK, `code` único, módulo, descripción |
| `user_role` | `@JoinTable` | muchos usuarios a muchos roles (`user_id`, `role_id`) |
| `role_permission` | `@JoinTable` | muchos roles a muchos permisos (`role_id`, `permission_id`) |

No se pueden confirmar tipos reales, PK/FK, índices de uniones, `NOT NULL`, charset, datos semilla ni filas sin migraciones o consultas. No se ejecutaron consultas contra MySQL: la auditoría no obtuvo ni mostró credenciales y no modificó datos.

## Migraciones
Existe `src/main/resources/db/migration/README.md`; establece `baseline-on-migrate=true`, baseline versión 1, y que migraciones futuras deben iniciar en V2. No hay archivos `V*.sql`. Esto es incompatible con poder crear una base nueva sólo desde el proyecto y deja el esquema inicial fuera del control de versiones entregado.

## Servicio observado
El contenedor `sistema-mysql` con imagen `mysql:8.4` estaba en ejecución, saludable y publicado como 3308→3306. Es infraestructura observada, no definición contenida en este proyecto. También hay MongoDB 8.2 en un contenedor y red separados; no hay evidencia de uso por este backend fuente.
