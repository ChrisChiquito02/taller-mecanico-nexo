# Auditoría previa al prompt maestro

4 de noviembre de 2026. Inspección recursiva previa a cambios funcionales; 86 archivos fuente/documentales inventariados por SHA-256, excluyendo secretos, dependencias y artefactos. Se conservaron los informes anteriores. No hay `.git` en la solución; `git status` además está impedido por la licencia Xcode pendiente del equipo. No se aceptó esa licencia ni se inicializó un repositorio. El inventario con hashes permite diferenciar cambios de esta intervención.

## Estado comprobable al comenzar

- Java objetivo 21, Spring Boot 4.1.1, Maven Wrapper 3.9.11; JDK del host 26.0.1, Node 24.16.0, npm 11.13.0 y Docker Compose 5.5.1.
- Backend: 29 clases de producción, controladores, DTO, tres repositorios, identidad/RBAC, JWT/Argon2id, correo, dos migraciones MySQL y manejo de errores. Entrada ejecutable: `SistemaApplication.main`.
- Frontend existente: HTML/CSS/JavaScript servido desde `src/main/resources/static`; registro, acceso, perfil, recuperación, cambio de contraseña y administración. No había fuentes/tooling/pruebas separados del navegador.
- El frontend no necesita React/Vite para este alcance. Se conservará JavaScript nativo, modularizado y compilado con esbuild hacia los recursos Spring, con una única fuente por componente.
- Cuatro clases de pruebas backend y 27 casos; se repite la suite como línea base. Los reportes previos no se consideran resultado actual.
- `orquestador.md` y OpenAPI existen. Faltan `documentadorFrontend.md` y `documentadorBackend.md`, matriz de integración detallada y evaluación porcentual trazable.
- Docker local existente se observa en lectura. Las nuevas verificaciones usarán contenedores temporales propios, sin usar sus credenciales/datos.

## Brechas encontradas en código

| Hallazgo | Estado inicial | Corrección prevista |
|---|---|---|
| `IdentityService` coordina todos los casos de uso y tiene diez dependencias | Parcial para modularidad exigida | Separar cuenta, sesiones, contraseñas y administración; conservar fachada compatible |
| Cliente renueva ante cualquier 401, incluso contraseña actual incorrecta | Defecto de sesión/UX | Distinguir fallo Bearer y credencial de negocio; probar sesión conservada |
| Fetch sin timeout ni validación de estructura; errores HTML pueden romper JSON | Parcial | Cliente HTTP con contrato, timeout y errores seguros |
| Visibilidad administrativa solo depende de `users:read` | Parcial | Permisos de lectura y escritura independientes, verificados también en servidor |
| No hay paginador visual, carga uniforme ni pruebas frontend automatizadas | Parcial | UI funcional y pruebas de formulario, red, permisos, navegación y E2E real |
| Dos documentos HTML idénticos como fuentes | Duplicación | Una plantilla; alias de recuperación generado en build |
| Relaciones LAZY pueden producir N+1 al listar | Riesgo | Página seguida de carga conjunta de autoridades, sin paginar colección con JOIN |
| Logout acepta un hash de refresh expirado para revocar sesiones nuevas | Riesgo | Validar expiración del token bajo bloqueo antes de revocar |
| LOCKED sin `lockedUntil` puede aceptar Bearer por condición negativa | Riesgo | Solo permitir LOCKED con fecha explícita vencida |
| Registro confirma existencia mediante conflicto mientras recuperación/login usan mensajes genéricos | Límite de enumeración | Conservar contrato 409 de registro y documentar el riesgo; uniformar login/recuperación y sus pruebas |
| SMTP externo, HTTPS y varios nodos no fueron verificados | Pendiente externo | Mantener plantillas seguras, registrar límites sin inventar evidencia |

Esta auditoría identifica implementación en fuentes, no certifica su ejecución. Los resultados de línea base y finales se registran por separado en el informe final.
