# Informe final — Prompt maestro ExamenErik

4 de noviembre de 2026, America/Mexico_City. [Orquestador](../../orquestador.md) · [Matriz](MATRIZ_INTEGRACION.md) · [Evidencia final](evidencia-final.json).

## A. Resumen ejecutivo

La línea base ya tenía identidad/API/SMTP y una interfaz estática, con 27 pruebas backend repetidas satisfactoriamente. Faltaban documentadores especializados, fuentes frontend modulares/pruebas de navegador y coordinación separada por caso de uso. Se corrigieron riesgos de expiración y enumeración encontrados al revisar código, no solo los descritos por auditorías antiguas.

Estado final local: servicios especializados, frontend modular compilado, contratos 0.2.0, seguridad y persistencia integradas; 41 pruebas backend, 16 frontend y ocho E2E. La evidencia exacta de la última secuencia, hashes y contenedores está en JSON. SMTP/HTTPS de producción y CI remoto siguen sin verificarse; no se declara 100 % ni certificación de producción.

## B. Arquitectura implementada

```text
ExamenErik/
├── orquestador.md
├── documentacion/{frontend,backend,auditoria}/
├── frontend/{src,public,tests,scripts}/
├── sistema-m01-m02/
│   ├── src/main/java/com/sistemam01m02/
│   │   ├── config/
│   │   ├── identity/{api,dto,service,security,entity,repository}/
│   │   └── shared/{error,persistence}/
│   ├── src/main/resources/{static,db/migration,application.yml}
│   ├── src/test/
│   └── scripts/ / pom.xml / Dockerfile / compose.yml
├── scripts/verificar.sh
└── docs/                             Antecedentes y OpenAPI vigente
```

Mismo origen para presentación y API. Java 21/Spring Boot 4.1.1/MySQL 8.4 conservados; JavaScript nativo/esbuild en lugar de añadir framework innecesario. Fachada `IdentityService` compatible, lógica distribuida en cuatro servicios transaccionales, repositorios y adaptadores. Cinco reglas ArchUnit y arranque Spring verifican límites y grafo propio.

## C. Orquestador

[`orquestador.md`](../../orquestador.md) es el índice central: tecnologías, diagramas, responsabilidades, dependencias, contratos, secuencias, errores, ejecución, estado y criterios. Enlaza [Frontend](../frontend/documentadorFrontend.md) y [Backend](../backend/documentadorBackend.md). Ningún Markdown ejecuta Spring ni se carga automáticamente por un agente. Entrada ejecutable: `SistemaApplication.main`.

## D. Integración Frontend

Fuentes únicas en `frontend/src` y `public`. Vistas de registro/login/perfil/recuperación/reset/cambio/admin; navegación por hash y permisos independientes de lectura/escritura. API real, timeout de 20 s, validación de estructuras, un solo reintento cuando falla la seguridad Bearer, sin borrar sesión por contraseña actual incorrecta. Estado sensible solo en memoria y cookie HttpOnly. Promesa/Web Locks coordinan renovación. Carga visible, mensajes accesibles, confirmación de contraseña y diseño móvil.

16 tests Node/jsdom y ocho E2E reales. Se verifica navegación/validación, bloqueo, cambio, recuperación SMTP, administración/paginación, XSS tratado como texto, ausencia de tokens en storage y dos pestañas. No se afirma cobertura WCAG completa ni de todos los navegadores. [Captura](interfaz-final.png).

## E. Integración Backend

AccountService, SessionService, PasswordService y UserAdministrationService coordinan casos de uso; TokenLookup mantiene orden de bloqueos, UserViews evita exponer entidades y RecoveryRequests admite trabajo antes de consultar identidad. Repositorios materializan autoridades sin N+1 en páginas. Controladores delegan y aplican permisos; DTO/errores seguros.

JWT/Argon2id/RBAC, bloqueo, revocación y expiración comprobados. Logout con refresh expirado ya no invalida sesiones nuevas. LOCKED sin fecha no acepta Bearer. Se suprime el logger JDBC que revelaba valores en errores de unicidad.

**Cambio público deliberado:** registro pasa de 201 UserView/409 a **202 Message uniforme**, nuevo o existente, también bajo carrera. No se devuelve un ID sin autenticación ni se crean IDs ficticios. Cliente actualizado; ID se consulta tras login en `/users/me`. OpenAPI versión 0.2.0. Otros conflictos de negocio conservan 409.

Recuperación responde antes de SMTP mediante dos trabajadores y cola de 64. Saturación 503 sin consultar cuenta. El trabajador conserva transacción/rollback; no es outbox durable, y reinicio puede requerir repetir solicitud. No se promete entrega por un 202.

## F. Base de datos

Se conservaron V1/V2; no se cambiaron sus checksums de fuente ni se añadió DDL innecesario. MySQL nuevo mediante Flyway, Hibernate validate, clean deshabilitado y baseline automático desactivado. Correo único, PK/FK, tokens hash y relaciones LAZY. Se prueban integridad, unicidad concurrente, bloqueo, rollback, consumo único y página de diez con máximo tres consultas. No se adoptó una base existente.

## G. Archivos modificados

La relación exacta creada por comparación de SHA-256 con los 86 archivos iniciales está en [CAMBIOS.md](CAMBIOS.md). Incluye creados/modificados/eliminados y motivo por ruta. No se eliminaron archivos fuente para ocultar fallos. Salidas estáticas se regeneran desde una plantilla/fuentes, no duplican módulos.

Nuevos principales: ambos documentadores, catálogo/matriz/informe/evidencia, frontend y tests, servicios especializados, RecoveryRequests, RegistrationConflict, SessionTokens, ArchitectureTest y tests adicionales de salud/errores/seguridad. Modificados principales: fachada, controladores/entidad/repositorio/converter, YAML/logging, OpenAPI, README/SECURITY, smoke y CI. No se sobrescribió `.env.local` ni se incluyeron sus valores.

No hay `.git` en el proyecto y el comando Git está bloqueado por la licencia Xcode pendiente. No se aceptó esa licencia ni se inventó historial; se usa [inventario de hashes inicial](linea-base-hashes.json). Tampoco se hizo push, publicación ni despliegue de producción.

## H. Evidencia de ejecución

Secuencia reproducible final: `PW_CHANNEL=chrome ./scripts/verificar.sh` en este equipo con Chrome instalado. En otros equipos se instala Chromium y se omite PW_CHANNEL.

| Verificación | Resultado final |
|---|---|
| Línea base `verify-java21.sh` antes de cambios | BUILD SUCCESS, 27 pruebas |
| `npm ci` / `npm run check` | Instalación con lockfile; 16 tests y build esbuild satisfactorios |
| `verify-java21.sh` con Wrapper/Temurin 21 | BUILD SUCCESS, 41 tests, cero fallos/errores/omisiones |
| `smoke-isolated.py` | JAR real, MySQL vacío, ADMIN explícito, UI/API/RBAC/refresh/logout y bootstrap repetido rechazado |
| Playwright navegador real | Ocho pruebas E2E satisfactorias, cero omisiones, sin mocks de API |
| Checker documental | Documentos/rutas locales/OpenAPI y referencias internas resueltas |
| Compose local | Configuración válida, backend con usuario app, salud UP, mismo SHA-256 que JAR probado |
| Auditoría npm | Resultado registrado de dependencias frontend; no sustituye auditoría Maven |

Los reportes JUnit se regeneran en `sistema-m01-m02/target/surefire-reports`; Playwright en `frontend/test-results`, sin trace/video. [Resumen backend](pruebas-backend.txt), [resumen frontend/E2E](pruebas-frontend.txt) y [evidencia estructurada](evidencia-final.json). Esos resúmenes excluyen cuerpos HTTP, credenciales y tokens.

Defectos resueltos y comprobados: 401 de negocio no renueva sesión, payload no JSON/timeout seguros, permisos UI separados, consultas paginadas acotadas, logout expirado y LOCKED sin fecha, confirmación de existencia por registro y dependencia temporal del correo en recuperación. Los antecedentes no se usan como resultado de la nueva ejecución.

## I. Matriz de integración

[Matriz completa R01–R32](MATRIZ_INTEGRACION.md): componentes, estado, archivos, pruebas, resultados y pendientes. Los 29 requisitos locales se verifican con documentación y ejecución. SMTP externo/HTTPS producción son parciales y CI remoto está pendiente. No hay componente local marcado completo solo por existir.

## J. Pendientes reales

1. Proveedor SMTP real, credenciales/DNS y entrega externa: protocolo local sí probado, proveedor externo no.
2. HTTPS de producción con dominio/proxy/certificado: controles de configuración sí probados; despliegue no autorizado ni efectuado.
3. Workflow remoto: creado, sin repositorio remoto ni ejecución; comandos equivalentes sí pasaron localmente.
4. Antes de escalar: rate limiter compartido, rotación de claves, auditoría persistente, purga de hashes y política de entrega durable si se exige. No se añadió infraestructura distribuida sin necesidad local.
5. No se hicieron pruebas de carga, certificación WCAG, pruebas en varios motores ni auditoría CVE exhaustiva Maven. No se afirma que npm audit cubra Java.

MFA, verificación de correo y edición de catálogos de roles no forman parte del alcance confirmado. El sistema no tiene fallos críticos locales conocidos ocultados; los límites operativos están explícitos.

## K. Instrucciones exactas

Requisitos: Node 24+/npm, Docker/Compose, Python 3, shell/OpenSSL y navegador de pruebas. Java/Maven se ejecutan en Docker mediante script.

```sh
npm --prefix frontend ci
npm --prefix frontend run check
sistema-m01-m02/scripts/verify-java21.sh
python3 sistema-m01-m02/scripts/smoke-isolated.py
cd frontend
npx playwright install chromium
npm run test:e2e
cd ..
python3 scripts/verificar-documentacion.py
```

Secuencia unificada después de instalar navegador: `./scripts/verificar.sh`; con Chrome instalado: `PW_CHANNEL=chrome ./scripts/verificar.sh`. Ejecutar desde la raíz; no lanzar builds paralelos sobre el mismo `target`.

Arranque local, desde `sistema-m01-m02`:

```sh
./scripts/init-local.sh
docker compose --env-file .env.local up --build -d
```

Interfaz/API <http://localhost:8090>, SMTP capturado en <http://localhost:8026>. Para detener conservando datos: `docker compose --env-file .env.local stop`. No borrar volúmenes como parte de estas instrucciones.

Para primer ADMIN, antes de registrar usuarios, establecer APP_BOOTSTRAP_ENABLED/EMAIL/PASSWORD explícitos; arrancar, confirmar acceso, desactivar bootstrap y retirar su contraseña de configuración. No hay credenciales predeterminadas. No reutilizar credenciales de pruebas. Para ejecución directa con Java: variables exportadas desde plantilla segura y `java -jar target/backend-0.1.0-SNAPSHOT.jar`.

## L. Evaluación final

Catálogo explícito de 32 verificaciones trazadas a secciones del prompt: **29 verificados / 32 × 100 = 90,625 %**. Dos parciales y un pendiente externo no cuentan como completos. Las tres verificaciones operativas externas se incluyen de forma conservadora para que no desaparezcan detrás del éxito local.

Este porcentaje mide requisitos/verificaciones de la matriz, **no cobertura de líneas ni seguridad absoluta**. Las 65 pruebas automatizadas (41 + 16 + 8), más smoke, build y comprobaciones documentales, prueban el alcance local registrado. No se presenta una evaluación de 100 % ni se confunde plantilla de producción con operación verificada.
