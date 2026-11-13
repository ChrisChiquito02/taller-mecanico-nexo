# Documentador Backend — ExamenErik

[Orquestador](../../orquestador.md) · [OpenAPI](../../docs/integracion/openapi.json) · [Matriz y evidencia](../auditoria/MATRIZ_INTEGRACION.md).

En las rutas siguientes, `java/` significa `sistema-m01-m02/src/main/java/com/sistemam01m02/` y `resources/` significa `sistema-m01-m02/src/main/resources/`.

## 1. Arquitectura

Monolito modular, capas HTTP/aplicación/dominio/persistencia/adaptadores. Inyección por constructor. `IdentityService` conserva la fachada existente, pero delega en `AccountService`, `SessionService`, `PasswordService` y `UserAdministrationService`. `TokenLookup` unifica bloqueos y `UserViews` representa datos sin secretos. No existe orquestador universal ejecutable.

## 2. Java

Objetivo Java 21 (`release=21`); compilación y ejecución verificadas en Temurin 21 dentro de Docker. El host tiene JDK 26.0.1; no se deduce compatibilidad con otros runtimes por la mera compilación.

## 3. Spring Boot

4.1.1, conservado del proyecto. Entrada y escaneo desde paquete raíz. Se mantienen Spring MVC, JPA y Security; dependencias transitivas efectivas se resuelven con Maven, no con versiones inventadas de Hibernate/Security.

## 4. Maven y dependencias

[Maven POM](../../sistema-m01-m02/pom.xml), Wrapper 3.9.11: webmvc, data-jpa, security, security-oauth2-resource-server, validation, mail, starter-flyway, flyway-mysql, mysql-connector-j runtime, Bouncy Castle 1.84 y Caffeine. Pruebas: starter-test, security-test, Testcontainers 2 administrado por Boot, GreenMail 2.1.3 y ArchUnit 1.4.1. El árbol de dependencias de ejecución se conserva con la evidencia de construcción cuando se genera; no se afirma auditoría CVE exhaustiva de Maven.

## 5. Punto de entrada

[`SistemaApplication.java`](../../sistema-m01-m02/src/main/java/com/sistemam01m02/SistemaApplication.java). `main` llama a `SpringApplication.run`; `@ConfigurationPropertiesScan` registra propiedades. Markdown no es cargado por Spring.

## 6. Configuración

[`application.yml`](../../sistema-m01-m02/src/main/resources/application.yml). DataSource externo, UTC, JPA validate, open-in-view false, Flyway validate/clean-disabled, límites HTTP, SMTP con tiempos acotados, TTL y bloqueo. `AppSecurityProperties` / `RecoveryProperties` validan valores. `SecurityConfig` exige clave Base64 de al menos 32 bytes, duraciones positivas, acceso hasta 15 minutos, orígenes válidos y protección de producción.

## 7. Entidades

[`identity/entity`](../../sistema-m01-m02/src/main/java/com/sistemam01m02/identity/entity): `AppUser`, `Role`, `Permission`, `UserStatus`, `AccountToken`. UUID binario de 16 bytes. Usuario: correo normalizado único, hash, estado, bloqueo, fechas y versión de sesión. `emailVerified` se conserva como dato; no hay flujo de verificación de correo implementado ni afirmado.

## 8. Relaciones JPA

Usuario M:N rol (`user_role`), rol M:N permiso (`role_permission`), token N:1 usuario. Relaciones LAZY. Tabla de uniones con PK compuesta y FK; tokens con hash único e índices por usuario/tipo y vencimiento. Usuario desactivado se conserva; no hay borrado físico de cuentas por API.

## 9. Repositorios y consultas

[`UserRepository`](../../sistema-m01-m02/src/main/java/com/sistemam01m02/identity/repository/UserRepository.java): búsqueda normalizada, bloqueo por correo/ID, grafo de autoridades por ID y carga conjunta por IDs. [`RoleRepository`](../../sistema-m01-m02/src/main/java/com/sistemam01m02/identity/repository/RoleRepository.java): código de rol. [`TokenRepository`](../../sistema-m01-m02/src/main/java/com/sistemam01m02/identity/repository/TokenRepository.java): propietario por proyección, bloqueo hash/tipo, revocación global/por tipo.

Paginación en dos pasos: página de usuarios y luego fetch de relaciones de esos IDs, sin paginar JOIN de colección. Prueba con estadísticas Hibernate exige como máximo tres consultas para diez usuarios con permisos; no se confunde LAZY con ausencia de N+1.

## 10. Servicios y coordinación

| Servicio real en `java/identity/service` | Casos de uso |
|---|---|
| `IdentityService` | Fachada compatible; delega sin política transaccional propia |
| `AccountService` | Registro y perfil |
| `SessionService` | Login, refresh rotativo, logout |
| `PasswordService` | Solicitud transaccional de reset, consumo y cambio de contraseña |
| `UserAdministrationService` | Página, reemplazo de roles, desactivación |
| `TokenLookup` | Orden de bloqueos compartido |
| `RecoveryRequests` | Admisión de recuperación y ejecución SMTP en segundo plano |
| `RecoveryDelivery` / `SmtpRecoveryDelivery` | Puerto y adaptador de correo |
| `EmailNormalizer` / `UserViews` | Normalización y representación segura |

`java/identity/security/TokenService` emite JWT/tokens opacos; no depende de servicios de aplicación. La fachada mantiene firmas Java anteriores para bootstrap/pruebas, sin crear un ciclo inverso.

## 11. Controladores REST

[`AuthController`](../../sistema-m01-m02/src/main/java/com/sistemam01m02/identity/api/AuthController.java), [`UserController`](../../sistema-m01-m02/src/main/java/com/sistemam01m02/identity/api/UserController.java) y [`HealthController`](../../sistema-m01-m02/src/main/java/com/sistemam01m02/config/HealthController.java). Delegan en servicios; no consultan repositorios. Cookie solo en adaptador HTTP. Doce operaciones en OpenAPI.

## 12. DTO

[`Contracts.java`](../../sistema-m01-m02/src/main/java/com/sistemam01m02/identity/dto/Contracts.java): Register, Login, Forgot, Reset, ChangePassword, Roles, UserView, Access, Message. [`SessionTokens`](../../sistema-m01-m02/src/main/java/com/sistemam01m02/identity/dto/SessionTokens.java) es interno: refresh nunca se serializa como cuerpo público. Fachada conserva su Session interna compatible.

## 13. Validaciones

Email válido ≤254, nombre no vacío ≤120, contraseñas nuevas 12–128, token opaco patrón 43 caracteres URL-safe, códigos de rol y límites de colección, UUID convertido por Spring, página no negativa y tamaño 1–100. Registro normaliza correo y valida allowlist de dominios. Hashes nunca sustituyen una validación de contraseña.

## 14. Transacciones y concurrencia

Servicios de caso de uso tienen `@Transactional`; consultas usan readOnly. Orden único: bloquear usuario antes de token. La primera consulta de token solo proyecta ID propietario y evita entidad precargada obsoleta. Login/reutilización lanzan `LoginRejected` con noRollbackFor para confirmar intentos/revocación; no se amplía esa excepción a todos los errores.

SMTP se ejecuta fuera del hilo HTTP pero **dentro** de la transacción `PasswordService.forgot` del trabajador. Si falla, rollback conserva el RESET anterior y no deja nuevo token activo. Las pruebas esperan trabajadores solo después de recibir la respuesta HTTP para verificar ese efecto; esa espera no existe en la API real.

## 15. Autenticación

[`SecurityConfig`](../../sistema-m01-m02/src/main/java/com/sistemam01m02/identity/security/SecurityConfig.java): Argon2id (19 456 KiB, dos iteraciones, paralelismo 1, sal 16 y hash 32 bytes). Login bloquea fila, normaliza, comprueba hash y estado; inexistente compara hash ficticio. JWT HS256 validado por Nimbus: emisor, firma y tiempo; versión de sesión y estado consultados en MySQL por petición.

## 16. RBAC

[`DatabaseJwtAuthentication`](../../sistema-m01-m02/src/main/java/com/sistemam01m02/identity/security/DatabaseJwtAuthentication.java) obtiene autoridades de roles activos. Método protegido con `@PreAuthorize`: `profile:read`, `users:read`, `users:write`. Cambios de roles revocan sesión del destino; un JWT viejo no conserva autoridad. LOCKED sin vencimiento se rechaza; solo bloqueo temporal con fecha vencida permite continuidad Bearer.

## 17. Usuarios y prevención de enumeración

Registro público responde **202 Message uniforme**, tanto nuevo como existente, y no devuelve ID. Se realiza hash antes de comprobar duplicado para mantener coste equivalente y se maneja carrera de unicidad exclusivamente del correo. El ID real se obtiene después de autenticarse en `/users/me`. No se generan usuarios/IDs ficticios. Este cambio reemplaza el contrato anterior 201 UserView / 409 por exigencia de privacidad.

Fallos de login usan el mismo mensaje para inexistente, contraseña incorrecta y cuenta bloqueada/deshabilitada. El bloqueo por cuenta persiste bajo concurrencia. Rate limiting por fingerprint HMAC de IP real del socket es acotado y por nodo; no se confía en X-Forwarded-For del cliente.

## 18. Roles y permisos

Semillas USER y ADMIN por migración. USER: profile:read; ADMIN: profile:read/users:read/users:write. El registro asigna USER, ignorando cualquier intento del cliente de declarar privilegios. API permite asignar roles existentes/activos, no editar catálogos. Primer ADMIN requiere bootstrap explícito con credenciales propias y base sin usuarios; se deshabilita después del primer arranque. No hay contraseña ADMIN predeterminada.

## 19. Recuperación y sesiones

Refresh/RESET aleatorios de 256 bits, solo SHA-256 persistido. Refresh rota y consume atómicamente; reutilización vigente revoca todo. Expirados se rechazan sin revocar accesos nuevos. Logout comprueba también expiración bajo bloqueo antes de revocar. Reset único, expirado o reutilizado da 401; cambiar contraseña revoca versión/tokens y no reactiva DISABLED.

`RecoveryRequests`: dos trabajadores y cola en memoria de 64, antes de consultar cuenta. HTTP responde 202 igual exista o no y sin esperar SMTP; saturación da 503 uniforme. SMTP falla con log seguro y rollback. No hay cola durable: reinicio puede perder solicitudes admitidas y el usuario debe repetir. No se promete entrega por responder 202. Enlace `#token=...` evita token en petición inicial del sitio.

## 20. MySQL

MySQL 8.4, InnoDB, URL/usuario/password por entorno. Sin `ddl-auto=update/create`. En Compose, servicio propio sin puerto publicado al host; credenciales aleatorias en archivo ignorado con permiso 600. Pruebas nunca usan ese volumen ni una DB real del usuario.

## 21. Flyway

[`V1`](../../sistema-m01-m02/src/main/resources/db/migration/V1__identity_baseline.sql) crea identidad; [`V2`](../../sistema-m01-m02/src/main/resources/db/migration/V2__sessions_and_rbac.sql) agrega sesiones/tokens/RBAC. No se modificaron migraciones ya aplicadas en esta intervención. Base nueva verificada; base existente sin historial rechazada. Baseline explícito solo tras revisar copia aislada contra V1; no se ejecutó adopción de bases anteriores.

## 22. Errores globales

[`shared/error`](../../sistema-m01-m02/src/main/java/com/sistemam01m02/shared/error): ApiException, LoginRejected, RegistrationConflict interno, ApiErrors y GlobalExceptionHandler. 400 validación, 401 autenticación/credencial inválida, 403 permiso/origen, 404 inexistente, 409 conflicto de negocio, 405 método, 413 tamaño, 415 formato, 429 tasa, 503 admisión SMTP llena, 500 imprevisto. JSON Problem tiene status/code/title sin SQL/stacktrace. CORS de framework puede producir 403 sin JSON; el cliente lo trata con error seguro.

## 23. Logging y observabilidad

`/api/health` ejecuta SELECT 1: 200 UP o 503 DOWN, sin detalles DB. No certifica SMTP ni bootstrap concluido; el smoke verifica login administrativo. Logs propios incluyen clases de error/evento SMTP, nunca cuerpos/contraseñas/tokens. Logger de error JDBC Hibernate se desactiva para evitar que un duplicado revele correo o valores SQL. Estadísticas se habilitan solo en pruebas. No hay auditoría persistente ni métricas distribuidas.

## 24. Servicios externos y límites

SMTP mediante puerto `RecoveryDelivery`, JavaMailSender y tiempos de conexión/lectura/escritura de 5 segundos. Producción exige STARTTLS y URL HTTPS; desarrollo usa Mailpit. Protocolo real comprobado con GreenMail/Mailpit. No se verificó un proveedor externo de correo ni certificados/DNS reales.

## 25. Variables de entorno

Fuente: [plantilla](../../sistema-m01-m02/.env.example) y `application.yml`. Requeridas: DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SIGNING_KEY, APP_RECOVERY_BASE_URL, SMTP_HOST, SMTP_FROM. SMTP autenticado: SMTP_USERNAME/PASSWORD. Opcionales: SERVER_PORT, DB_POOL_*, SMTP_PORT/AUTH/STARTTLS, APP_ALLOWED_ORIGINS, APP_COOKIE_SECURE, APP_RECOVERY_MODE, ACCESS_TOKEN_TTL, REFRESH_TOKEN_TTL, PASSWORD_RESET_TTL, MAX_FAILED_ATTEMPTS, LOCK_DURATION, ALLOWED_EMAIL_DOMAINS, AUTH_REQUESTS_PER_MINUTE. Bootstrap: APP_BOOTSTRAP_ENABLED/EMAIL/PASSWORD. `.env.example` no se carga automáticamente.

## 26. Construcción

Desde raíz: `npm --prefix frontend ci`, `npm --prefix frontend run check`, `sistema-m01-m02/scripts/verify-java21.sh`. Alternativa con JDK 21+ instalado: `cd sistema-m01-m02 && ./mvnw -B -ntp verify`. Construir frontend antes del JAR. Docker empaqueta ese JAR, no ejecuta otra compilación omitiendo pruebas.

## 27. Ejecución

Desde `sistema-m01-m02`: `./scripts/init-local.sh`, `docker compose --env-file .env.local up --build -d`. Interfaz/API localhost:8090, Mailpit localhost:8026. Ejecución directa: exportar variables seguras y `java -jar target/backend-0.1.0-SNAPSHOT.jar`. Producción requiere infraestructura HTTPS propia; no se desplegó.

## 28. Pruebas

`src/test/java/com/sistemam01m02`: AppUserTest, UuidV7GeneratorTest, GuardAndConfigurationTest, ArchitectureTest y SystemIntegrationTest. Cubren arquitectura sin ciclos por paquete de identidad y límites de capas, DB vacía/migraciones, FK/unicidad/rollback, concurrencia, permisos negativos, expiración/reutilización, SMTP fallido y contratos. Scripts smoke/E2E ejecutan el JAR separado, no solo contexto de prueba. Conteos/resultados finales en informe, sin inventar cobertura porcentual de líneas.

## 29. Contratos de integración

OpenAPI 0.2.0 define doce operaciones. Los controladores reciben DTO validados y devuelven vistas/mensajes; fachada y SessionTokens son contratos Java internos. Recuperación usa submit(Forgot) → PasswordService.forgot → RecoveryDelivery.send(recipient,url), no se exporta token por API. JPA/SQL son internos, nunca se consumen desde frontend. SMTP y JDBC tienen tiempos configurados; HTTP del navegador tiene timeout independiente y no duplica mutaciones ante fallo de red.

## 30. Estado real

Cada componente tiene estado/prueba en la [matriz central](../auditoria/MATRIZ_INTEGRACION.md). El alcance local es verificable. Pendientes externos/operativos: SMTP real, HTTPS, CI remoto, claves rotadas, purga de hashes, auditoría persistente y control distribuido. MFA y verificación de correo no forman parte de los flujos confirmados y no se presentan como implementados. No hay repositorio remoto publicado ni migraciones sobre datos preexistentes.
