# Inventario de archivos relevantes

| Ruta | Propósito | Relaciones | Estado |
|---|---|---|---|
| `sistema-m01-m02/pom.xml` | versión Java y dependencias Maven | habilita Spring/JPA/MySQL/seguridad/pruebas | declarado |
| `sistema-m01-m02/.env.example` | plantilla de variables | consumida por YAML en ejecución | declarada; secretos de ejemplo no reales |
| `sistema-m01-m02/.gitignore` | evita versionar secretos/artefactos | Git, IDE, frontend | implementado |
| `sistema-m01-m02/SECURITY.md` | decisiones de diseño | debería corresponder a seguridad faltante | documentación, no implementación |
| `src/main/java/.../SistemaApplication.java` | bootstrap | escanea propiedades y componentes | implementado |
| `src/main/java/.../config/AppSecurityProperties.java` | propiedades `app.security` | YAML y futura seguridad | implementado, no validado/consumido |
| `src/main/java/.../config/RecoveryProperties.java` | propiedades `app.recovery` | YAML y futura recuperación | implementado, no consumido |
| `src/main/java/.../identity/entity/AppUser.java` | entidad/lógica usuario | `Role`, MySQL/Flyway | parcial |
| `src/main/java/.../identity/entity/Role.java` | entidad RBAC | `Permission`, `AppUser` | parcial |
| `src/main/java/.../identity/entity/Permission.java` | entidad permiso | `Role` | parcial |
| `src/main/java/.../identity/entity/UserStatus.java` | estados de usuario | `AppUser` | implementado |
| `src/main/java/.../shared/persistence/UuidV7Generator.java` | UUIDv7 | futuro alta de entidades | aislado |
| `src/main/resources/application.yml` | Spring/DataSource/Flyway/HTTP | properties y entorno | declarado |
| `src/main/resources/db/migration/README.md` | política migraciones | Flyway | sin SQL implementado |

No hay otros archivos fuente, scripts, documentación funcional, assets, pruebas ni manifiestos de frontend detectados antes de la creación de esta auditoría.
