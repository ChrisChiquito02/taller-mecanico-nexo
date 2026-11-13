# Implementación y verificación de ExamenErik

Fecha: 4 de noviembre de 2026, America/Mexico_City. Entrada principal: [orquestador.md](../../orquestador.md).

## Resultado funcional

Se integraron 12 operaciones HTTP de salud, registro, autenticación JWT, renovación rotativa, cierre global de sesiones, consulta propia, cambio de contraseña, recuperación SMTP, listado administrativo, asignación de roles y desactivación lógica. Hay interfaz HTML/JavaScript del mismo origen y contratos OpenAPI.

Se conservó Spring Boot 4.1.1, el objetivo Java 21 y el modelo original. Se corrigieron los nombres de starters para Boot 4, módulos Testcontainers 2 y versión de Bouncy Castle; se añadió el starter de Flyway necesario para su autoconfiguración, Caffeine y GreenMail de pruebas.

La documentación anterior en `docs/auditoria-integral` se conserva como evidencia histórica, no como estado vigente.

## Archivos modificados y creados

Rutas relativas a la raíz de ExamenErik. El [inventario completo](INVENTARIO.md) enumera los archivos vigentes excluyendo artefactos, secretos y metadatos ajenos al cambio.

| Archivos / área | Cambio |
|---|---|
| `orquestador.md`, `README.md`, `sistema-m01-m02/README.md` | Entrada documental, arquitectura, contratos, arranque, reglas de integración y límites |
| `docs/integracion/*` | OpenAPI, informe, inventario y evidencia verificable |
| `.github/workflows/verify.yml` | Verificación Java 21 y smoke test para CI; no ejecutada remotamente |
| `.idea/misc.xml` | JDK local instalado 26.0.1 y nivel de lenguaje 21; ejecución Java 21 verificada por Docker |
| `pom.xml` | Dependencias compatibles y bibliotecas de integración/prueba |
| `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` | Maven Wrapper 3.9.11, distribución oficial |
| `Dockerfile`, `.dockerignore`, `compose.yml` | Empaquetado del JAR verificado, usuario sin privilegios, MySQL/Mailpit en proyecto separado, puertos solo loopback |
| `scripts/init-local.sh` | Secretos locales aleatorios, permiso 600, sin sobrescribir entorno existente |
| `scripts/verify-java21.sh` | Construcción y suite con Java 21 mediante Docker y Wrapper |
| `scripts/smoke-isolated.py` | Arranque independiente del JAR y bootstrap, sobre contenedores nuevos que se eliminan al terminar; copia temporal del JAR |
| `.env.example`, `application.yml` | Variables de MySQL/JWT/SMTP, validación del esquema, Flyway seguro y bootstrap optativo |
| `config/AppSecurityProperties.java`, `RecoveryProperties.java` | Validación de tiempos, límites y entradas de configuración |
| `config/AdminBootstrap.java`, `HealthController.java` | Alta ADMIN inicial explícita sobre base sin usuarios y salud MySQL |
| `identity/entity/*` | Conservación del modelo, relaciones LAZY, versión de sesión, estado seguro y nuevo AccountToken |
| `identity/repository/*` | Acceso JPA y consultas con bloqueos coherentes de usuario/token |
| `identity/dto/Contracts.java` | DTO validados y vistas sin secretos |
| `identity/service/*` | Casos de uso transaccionales y puerto/adaptador SMTP |
| `identity/api/*` | Controladores de autenticación, cuenta y administración |
| `identity/security/*` | JWT HS256, Argon2id, autoridades actuales, CORS, origen, limitación por fingerprint y tamaño JSON |
| `shared/persistence/UuidV7Generator.java` | Uso efectivo, orden local incluso con retroceso/agotamiento de secuencia |
| `shared/error/*` | Errores HTTP seguros y excepción de rechazo que conserva contadores/revocaciones |
| `db/migration/V1__identity_baseline.sql`, `V2__sessions_and_rbac.sql`, `README.md` | Esquema reproducible y estrategia explícita para bases existentes |
| `static/index.html`, `reset-password.html`, `app.js`, `styles.css` | Interfaz de registro/acceso/recuperación/cuenta/administración; sin token en localStorage |
| `src/test/java/*`, `src/test/resources/*` | Pruebas unitarias, integración MySQL/SMTP y seguridad |
| `SECURITY.md` | Política vinculada a las implementaciones y límites actuales |

Las rutas desde `config`, `identity`, `shared`, `db/migration` y `static` en la tabla son relativas a `sistema-m01-m02/src/main/java/com/sistemam01m02` o `src/main/resources`, según corresponda. El entrypoint `SistemaApplication.main` se conserva; se normalizó formato Java de las clases existentes.

`.env.local` se generó exclusivamente para el entorno local nuevo y está ignorado. No se adjuntan sus contenidos ni los tokens/contraseñas usados en pruebas.

## Pruebas automatizadas ejecutadas

Comando final: `cd sistema-m01-m02 && ./scripts/verify-java21.sh`.

Ejecuta Docker con `eclipse-temurin:21-jdk`, Maven Wrapper 3.9.11 y `clean verify`. Resultado: **BUILD SUCCESS; 27 pruebas, 0 fallos, 0 errores, 0 omitidas**. Compilación con `release=21` y JAR ejecutable Spring Boot generado.

| Clase | Pruebas | Cobertura comprobada |
|---|---:|---|
| `AppUserTest` | 2 | Bloqueo/vencimiento, conservación de DISABLED y reinicio de contador |
| `UuidV7GeneratorTest` | 3 | Versión/variante, 12 000 IDs con reloj fijo, 8 000 concurrentes, reloj en retroceso |
| `GuardAndConfigurationTest` | 4 | TTL inválidos, clave débil y producción insegura, fingerprint, 429 sin confiar en IP reenviada |
| `SystemIntegrationTest` | 18 | Arranque HTTP real, migraciones MySQL, Argon2id, perfil/RBAC/roles inactivos, errores/validación, bloqueos persistidos y concurrentes, refresh/expiración/reutilización concurrente, cookies/origen/JSON/CORS, logout, reset SMTP único y expirado, rollback por fallo SMTP, permisos administrativos/desactivación, cambio de contraseña, JWT manipulado/expirado, límites JSON conocidos/chunked e interfaz estática |

MySQL 8.4 se crea con Testcontainers y puertos aleatorios; GreenMail es un servidor SMTP local real. Las propiedades dinámicas sustituyen base y SMTP sin cargar `.env.local`. Los contenedores de prueba son eliminados al finalizar. Los reportes íntegros regenerables están en `sistema-m01-m02/target/surefire-reports`.

El caso adicional de refresh expirado comprueba que un token antiguo ya consumido no pueda invalidar indefinidamente sesiones nuevas.

## Verificación independiente y despliegue local

- `python3 scripts/smoke-isolated.py`: ejecuta el JAR en un proceso Java 21 separado y MySQL vacío, crea ADMIN por bootstrap, comprueba UI, registro, login, asignación RBAC, perfil, renovación, logout y rechazo de sesión revocada. También reinicia para comprobar que bootstrap habilitado rechaza usuarios existentes. Solo elimina recursos creados por ese mismo script.
- `docker compose --env-file .env.local config --quiet`: configuración válida sin imprimir secretos.
- `docker compose --env-file .env.local up --build -d`: empaqueta el artefacto verificado y utiliza un proyecto/red/volumen propios; no modifica los servicios anteriores.
- Navegador en `http://localhost:8090`: registro de una cuenta sintética, login, perfil USER, solicitud de recuperación y logout. Mailpit capturó un correo de recuperación. No se enviaron correos por Internet. Al terminar se retiró exclusivamente la cuenta sintética creada para esta comprobación y sus relaciones/tokens; el MySQL local nuevo queda sin usuarios, listo para bootstrap explícito.
- `node --check src/main/resources/static/app.js` y `sh -n` sobre scripts/Wrapper: sin errores.
- OpenAPI: JSON válido, 12 operaciones y referencias internas resueltas. No se ejecutó un validador completo de la especificación OpenAPI.

La evidencia final de artefacto, pruebas, usuario del contenedor y estado de servicios se guarda en [evidencia.json](evidencia.json). Se incluye [captura del navegador](interfaz-verificada.png), sin credenciales.

## Correcciones derivadas de verificaciones

1. Una petición OPTIONS sin encabezados de preflight no comprobaba CORS; se corrigió la prueba y esta detectó que la configuración no estaba conectada explícitamente a SecurityFilterChain. Se corrigió la conexión y se verificó el encabezado de origen permitido.
2. La consulta del propietario de un token usa proyección del ID antes de bloquear; evita precargar estado obsoleto en renovaciones concurrentes.
3. Refresh vencido se rechaza antes de evaluar reutilización con revocación, evitando invalidación indefinida de sesiones.
4. Una ejecución de smoke coincidió con `clean verify` y perdió el JAR montado al reiniciar. El script ahora monta una copia temporal y las verificaciones finales se realizan secuencialmente. La repetición final fue satisfactoria y consta en evidencia.json.
5. La imagen Docker empaqueta el JAR comprobado; no mantiene una segunda compilación que omita pruebas. Las descargas redundantes de imágenes de construcción se cancelaron.

## Pendientes reales y límites

- Configurar y probar SMTP de producción, DNS/certificados, proxy HTTPS y secretos propios. Solo se comprobó entrega SMTP local, no recepción externa.
- Validar una copia de cualquier base preexistente antes de adoptar baseline. No se ejecutó ninguna migración sobre datos anteriores.
- Definir retención/purga de tokens y auditoría persistente según operación. No hay limpieza automática de hashes.
- Para múltiples instancias: rate limiter compartido, rotación de claves y coordinación de renovaciones entre pestañas. El control actual es por nodo.
- MFA, verificación de correo, edición de catálogos RBAC, pruebas de carga y análisis exhaustivo de vulnerabilidades no están incluidos ni se atribuyen como realizados.
- El workflow CI está creado, pero no se ejecutó en un proveedor remoto. No se inicializó Git ni se publicó código.

Las pruebas acreditan los flujos descritos en el entorno aislado; no constituyen una certificación de seguridad de producción.
