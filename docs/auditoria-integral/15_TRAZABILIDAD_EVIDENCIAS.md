# Trazabilidad de evidencias

| Afirmación | Tipo de evidencia | Ubicación/comprobación | Límite |
|---|---|---|---|
| Usa Java 21 y Spring Boot 4.1.1 | manifiesto | `sistema-m01-m02/pom.xml` | no prueba build exitoso |
| Es backend sin frontend entregado | inventario recursivo | ausencia de `package.json`, JS/TS, HTML, CSS | no descarta frontend fuera del alcance |
| Existen usuario, rol y permiso JPA | código | `identity/entity/*.java` | no confirma tablas reales |
| Existen relaciones M:N | anotaciones | `AppUser.java`, `Role.java` | no confirma FKs físicas |
| Hay bloqueo temporal en el modelo | código | métodos de `AppUser.java` | no prueba que un servicio lo invoque |
| Hay generador UUIDv7 | código | `shared/persistence/UuidV7Generator.java` | no prueba uso ni pruebas |
| MySQL/Flyway están configurados | YAML y Maven | `application.yml`, `pom.xml` | no hay migraciones SQL |
| No hay migraciones implementadas | inventario | sólo `db/migration/README.md` | no inspecciona fuentes externas |
| No hay API/seguridad efectiva fuente | búsqueda/inventario | ausencia de controllers/services/repositories/security config | imagen desplegada puede diferir |
| No hay pruebas locales | inventario | ausencia de `src/test`/reportes | dependencias de test sí existen |
| No hay historial Git | filesystem/Git | no hay `.git`; comandos Git fallaron | historial puede existir fuera del alcance |
| Hay tres contenedores ejecutándose | Docker lectura | `docker ps` e `inspect` 26-10-2026 | no prueba origen de su configuración |
| Backend responde 401 | HTTP sin credenciales | GET `/` y `/actuator/health` | no prueba funcionalidad ni salud |

Los valores de variables, contraseñas, tokens y credenciales no fueron incorporados a los informes. Toda afirmación de estado se clasifica como código disponible, configuración declarada o infraestructura observada para evitar convertir diseño o inferencias en hechos funcionales.
