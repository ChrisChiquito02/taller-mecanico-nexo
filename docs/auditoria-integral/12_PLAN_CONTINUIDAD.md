# Plan de continuidad

## Fase 1. Correcciones urgentes
| Actividad | Prioridad | Archivos involucrados | Dependencias | Resultado/aceptación |
|---|---|---|---|---|
| Recuperar o definir el alcance fuente real | Crítica | raíz, Git, infraestructura autorizada | acceso al origen legítimo | árbol completo y trazable; no asumir funciones del contenedor |
| Crear esquema versionado | Crítica | `src/main/resources/db/migration/` | modelo JPA, MySQL | base nueva migra y Hibernate valida sin cambios manuales |
| Incorporar Docker/Compose reproducible | Crítica | nuevos archivos infra/documentación | variables de entorno | `docker compose config` válido sin secretos |

## Fase 2. Finalización
| Actividad | Prioridad | Archivos involucrados | Dependencias | Resultado/aceptación |
|---|---|---|---|---|
| Implementar persistencia y servicios de identidad | Alta | nuevos repositorios/servicios/DTO | migración | altas, consultas y lockout transaccionales |
| Implementar API REST | Alta | controladores/validación/errores | servicios | contratos documentados y respuestas seguras |
| Implementar JWT, refresh y recuperación | Alta | seguridad, mail, propiedades | secretos/configuración | flujos autenticados y revocación/expiración probados |
| Definir semillas/administración RBAC | Alta | migraciones y servicios | roles/permisos | permisos aplicados a endpoints |

## Fase 3. Integración
| Actividad | Prioridad | Archivos involucrados | Dependencias | Resultado/aceptación |
|---|---|---|---|---|
| Construir frontend Vite/React | Media | nuevo frontend | contratos API | login y pantallas prioritarias consumen API |
| Configurar CORS/cookies/errores | Media | seguridad/frontend | autenticación | integración navegador validada en entorno local |

## Fase 4. Calidad
| Actividad | Prioridad | Archivos involucrados | Dependencias | Resultado/aceptación |
|---|---|---|---|---|
| Pruebas unitarias e integración | Alta | `src/test` | módulos anteriores, Testcontainers | suite reproducible y sin tocar datos de producción |
| Revisar seguridad y rendimiento | Alta | seguridad/JPA/infra | implementación real | lista de amenazas mitigadas y pruebas de autorización |

## Fase 5. Preparación final
| Actividad | Prioridad | Archivos involucrados | Dependencias | Resultado/aceptación |
|---|---|---|---|---|
| Documentar operación y API | Media | README, OpenAPI, runbooks | sistema estable | otra persona puede instalar, configurar y demostrar |
| Preparar demostración y despliegue | Media | Compose/CI/configuración | pruebas aprobadas | entorno repetible, variables seguras y checklist firmado |
