# Análisis del backend

## Arquitectura y paquetes
Backend monolítico Spring Boot, artefacto Maven `com.sistemam01m02:backend:0.1.0-SNAPSHOT`. La separación actual es por dominio/configuración:

| Paquete | Elementos | Estado |
|---|---|---|
| raíz | `SistemaApplication` | Implementado: arranque y `@ConfigurationPropertiesScan` |
| `config` | `AppSecurityProperties`, `RecoveryProperties` | Implementado: binding tipado, sin validación Bean Validation |
| `identity.entity` | `AppUser`, `Role`, `Permission`, `UserStatus` | Implementado: mapeos y parte de lógica de cuenta |
| `shared.persistence` | `UuidV7Generator` | Implementado: componente UUIDv7 sincronizado |

## Modelo de identidad y autorización
- **AppUser:** entidad `app_user`, UUID binario de 16 bytes, correo normalizado, hash de contraseña, nombre, estado, marcas temporales, contador de fallos y bloqueo. Tiene relación `ManyToMany` eager a `Role` mediante `user_role`.
- **Role:** entidad `role` con código único, nombre, descripción, activo y relación `ManyToMany` eager a `Permission` por `role_permission`.
- **Permission:** entidad `permission` con código único, módulo y descripción.
- **UserStatus:** `PENDING`, `ACTIVE`, `LOCKED`, `DISABLED`.
- **Reglas reales en `AppUser`:** un login exitoso restablece contador y desbloquea únicamente estado `LOCKED`; un fallo incrementa y bloquea cuando alcanza el máximo recibido; el bloqueo se considera temporal según `lockedUntil`; al expirar se restaura `ACTIVE`; cambio de contraseña limpia bloqueo y contador. No hay métodos para desactivar, verificar correo ni asignar roles salvo `addRole`.

## Endpoints, servicios e integración
No se encontraron `@RestController`, `@Controller`, `@Service`, `JpaRepository`, DTO, filtros, `SecurityFilterChain`, `PasswordEncoder`, emisor/verificador JWT, manejador de excepciones ni cliente de correo. En consecuencia:

| Módulo | Funcionalidad | Archivos | Endpoint | Seguridad | Estado | Evidencia |
|---|---|---|---|---|---|---|
| Arranque | inicia Spring Boot | `SistemaApplication.java` | N/A | N/A | Implementado | `@SpringBootApplication` |
| Identidad | modelo de usuario/bloqueo | `identity/entity/AppUser.java` | Ninguno | no conectada | Parcial | entidad y métodos |
| RBAC | modelo roles/permisos | `Role.java`, `Permission.java` | Ninguno | no conectada | Parcial | relaciones JPA |
| IDs | genera UUID versión 7 | `UuidV7Generator.java` | N/A | N/A | Implementado | `@Component`, `generate()` |
| JWT/recuperación | parámetros declarados | `AppSecurityProperties.java`, `RecoveryProperties.java` | Ninguno | configuración solamente | Pendiente | records y YAML |

## Persistencia y configuración
`application.yml` define DataSource mediante `DB_URL`, usuario y contraseña de entorno, Hibernate con `ddl-auto: validate`, `open-in-view: false`, UTC y Flyway habilitado. Al no haber SQL, no existe esquema reproducible en el árbol. Las columnas declaradas permiten inferir las tablas, no confirmar su existencia ni restricciones físicas.

## Seguridad
El manifiesto incluye Spring Security, Resource Server OAuth2, Bouncy Castle y Mail. `SECURITY.md` declara Argon2id, JWT, tokens opacos, cookie HttpOnly/SameSite y controles de origen, pero falta código que lo implemente. Las propiedades `app.security` poseen TTL, allowlist, bloqueo y dominios de correo permitidos, sin `@Validated` ni restricciones que garanticen valores correctos.
