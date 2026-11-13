# Matriz de integración y requisitos verificables

[Orquestador](../../orquestador.md) · [Informe A–L](INFORME_FINAL.md) · [Evidencia](evidencia-final.json).

Catálogo de **32 verificaciones identificadas**: 29 locales y tres operativas externas. Se cuenta un requisito solo si cumple los doce criterios aplicables (existencia, configuración, contrato, comunicación, función, validación, seguridad, persistencia, errores, pruebas, documentación y trazabilidad). «N/A» solo se admite para efectos que el componente no realiza: documentos/estilos no tienen persistencia ni autenticación propia. No se calcula cobertura de líneas.

Estados: **verificado** = evidencia actual; **parcial** = parte implementada o probada; **pendiente** = necesita ejecución/recurso externo; **bloqueado** no se usa para tareas técnicas difíciles.

| ID / origen del prompt | Integración requerida y componentes | Estado | Archivos reales | Prueba/evidencia asociada y resultado | Pendiente |
|---|---|---|---|---|---|
| R01 / §2 | Auditoría recursiva antes de cambios | Verificado | `LINEA_BASE.md`, `linea-base-hashes.json` | Inventario inicial 86 archivos, repetición inicial 27 tests OK | Git no disponible; no se inventó historial |
| R02 / §15 | Conservar código, cambios ajenos y antecedentes | Verificado | `CAMBIOS.md`, docs previos | Comparación SHA-256, sin archivos fuente eliminados; SQL V1/V2 conservado | Sin Git: hashes, no commits |
| R03 / §4–6 | Tres referencias documentales centrales | Verificado | `orquestador.md`, ambos documentadores | `verificar-documentacion.py`: existencia y enlaces | No ejecución automática de Markdown |
| R04 / §4 | Arquitectura, responsabilidades y contratos | Verificado | Orquestador, OpenAPI 0.2.0 | Doce operaciones y referencias internas; pruebas HTTP | No certificación OpenAPI completa |
| R05 / §4.4, §12 | Capas y ausencia de ciclos | Verificado | `ArchitectureTest.java`, constructor injection | Cinco reglas ArchUnit + arranque Spring OK | Se comprueba código propio; composición exceptuada explícitamente |
| R06 / §7.2, §11 | Coordinación por caso de uso | Verificado | `identity/service/{Account,Session,Password,UserAdministration}Service.java`, fachada | Regresión HTTP/E2E OK después de separación | Ninguno local |
| R07 / §7.4 | DTO separados y validación | Verificado | `Contracts.java`, `SessionTokens.java`, formularios | Entradas inválidas, confirmación, UUID/roles, límites; Node/JUnit OK | Ninguno local |
| R08 / §7.5 | Errores controlados 400/401/403/404/409/500 | Verificado | `shared/error/*`, `HealthControllerTest` | HTTP negativos y test 500 sin detalles; salud 503 sin conexión revelada | CORS framework puede emitir 403 no JSON; cliente lo controla |
| R09 / §9 | MySQL nuevo inicializable | Verificado | MySQL 8.4, migraciones V1/V2 | Testcontainers, smoke y E2E arrancan sobre DB vacía | No adopción de DB real |
| R10 / §9 | Flyway compatible con validate | Verificado | `application.yml`, `db/migration/README.md` | Dos SQL aplicados; Hibernate valida; clean/baseline protegidos | Baseline existente exige revisión de copia |
| R11 / §7.1, §9 | Repositorios, FK, unicidad y consultas acotadas | Verificado | `identity/repository/*` | FK huérfana rechazada, unicidad concurrente, página de diez ≤3 consultas | Sin pruebas de carga |
| R12 / §7.2, §13 | Transacciones y concurrencia | Verificado | Servicios, `TokenLookup` | Fallos login conservados, reset/refresh con una victoria, rollback SMTP | Ninguno local |
| R13 / §8 | Argon2id/JWT y claves seguras | Verificado | `SecurityConfig`, `TokenService` | Hash real, firma/expiración/tamper; configuración insegura rechazada | Rotación operativa por definir |
| R14 / §8 | Autorización RBAC efectiva | Verificado | `DatabaseJwtAuthentication`, controladores | USER no lee/escribe admin; rol inactivo no da permisos; E2E asigna/desactiva | Catálogos por migración, no editor |
| R15 / §8 | Bloqueo y usuarios deshabilitados | Verificado | `AppUser`, SessionService/converter | Lockout concurrente, vencimiento, DISABLED preservado, LOCKED sin fecha rechazado | Ninguno local |
| R16 / §8 | Sesiones, expiración y revocación | Verificado | `AccountToken`, `SessionService` | Refresh/replay/expiración/logout; token expirado no revoca sesiones nuevas | Purga de hashes futura |
| R17 / §8, §10 | Recuperación SMTP real y fallo controlado | Verificado | `RecoveryRequests`, `PasswordService`, adaptador | GreenMail, Mailpit, reset único/expirado y E2E; SMTP falla y revierte | Cola en memoria no durable, documentada |
| R18 / §8 | Prevención de confirmación de cuentas | Verificado | AccountService, AuthController, RecoveryRequests | Nuevo/duplicado concurrente 202 idéntico sin ID; admisión acotada antes de identidad | No promesa matemática de tiempos de red |
| R19 / §8 | CORS/CSRF/cookie/cabeceras/tamaño/tasa | Verificado | `identity/security/*` | Origin/JSON obligatorios, CORS real, 429, 413 conocido/chunked | Rate limiter por nodo |
| R20 / §14 | Secretos externos y plantillas seguras | Verificado | `.env.example`, init-local, properties | No sobrescribe entorno; permisos 600; validación prod; sin secretos cliente | Credenciales externas no disponibles |
| R21 / §3, §5 | Frontend único y compilable | Verificado | `frontend/src`, `public`, build/lockfile | esbuild OK; recursos incluidos en JAR | Node solo construcción |
| R22 / §5, §10 | Formularios, navegación, responsive/accesibilidad básica | Verificado | Plantilla/UI/CSS | Validación, etiquetas/foco, historial y móvil 375px sin desborde; Node/E2E OK | Certificación WCAG y lector humano no realizadas |
| R23 / §10 | Cliente HTTP, contratos, carga y errores | Verificado | `api-client.js`, app/ui | Timeout, no JSON, 403, 401 correcto, sin reintento ambiguo; 16 Node OK | Una mutación con timeout requiere verificar resultado |
| R24 / §10 | Sesión, rutas y permisos cliente | Verificado | ui/app/client | Controles read/write independientes, ruta privada, renovación única y dos pestañas E2E | Sin Web Locks puede requerirse nuevo login |
| R25 / §13 | Pruebas Frontend automatizadas | Verificado | `frontend/tests/unit` | 16 tests OK, sin omisiones | No equivalen a cobertura total del navegador |
| R26 / §13 | Pruebas Backend, DB, seguridad y arquitectura | Verificado | `src/test` | 41 tests OK, sin fallos/errores/omisiones | Sin SCA Maven exhaustivo |
| R27 / §10, §13 | E2E navegador → API → DB/SMTP | Verificado | `frontend/tests/e2e` | Ocho recorridos reales, sin mocks, entorno nuevo y limpieza propia | Solo navegador registrado en evidencia |
| R28 / §14, §17 | Construcción/arranque independiente y Docker local | Verificado | Dockerfile, Compose, scripts | smoke: ADMIN y reinicio rechazado; JAR hash coincide con contenedor y salud UP | Producción no desplegada |
| R29 / §18 | Informe A–L, archivos y evidencia trazable | Verificado | INFORME_FINAL, CAMBIOS, evidencia JSON | Checker documental y hashes actuales | Conteo 29/32 explicado |
| R30 / §14–15 | Operación con SMTP externo real | Parcial | Plantilla y adaptador SMTP | Protocolo local y fallo sí comprobados | Falta proveedor/credenciales/DNS; no se enviaron correos externos |
| R31 / §14–15 | Operación HTTPS real de producción | Parcial | Validaciones y plantilla segura | Configuración insegura rechazada; despliegue local HTTP explícito | Falta proxy/dominio/certificado; no autorizado desplegar producción |
| R32 / §13–15 | CI remoto ejecutado | Pendiente | `.github/workflows/verify.yml` | Workflow creado; comandos equivalentes pasan localmente | No hay repositorio remoto/publicación autorizada |

**Evaluación:** 29 verificados / 32 identificados × 100 = **90,625 %**. Dos parciales y uno pendiente no suman como completos. Las tres verificaciones externas se mantienen visibles aunque la funcionalidad local esté comprobada. No se añade MFA/verificación de correo al denominador porque no son requisitos funcionales confirmados.

## Doce criterios por componente

Para UI/cliente, API/servicios, seguridad, persistencia y adaptadores SMTP, R04–R28 cubren los doce criterios mediante contrato, entradas inválidas, permisos, efectos de datos, fallos y tests reales. Documentación y herramientas: persistencia/autenticación del usuario final no aplican; se comprueban rutas, configuración, salida reproducible, ausencia de secretos y ejecución de verificadores. SMTP externo/HTTPS/CI remoto siguen parciales/pendientes pese a existir archivos: no se convierten en «verificados» por estar configurados.
