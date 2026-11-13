# Estado actual

| Módulo | Implementado | Probado | Integrado | Pendientes | Estado |
|---|---|---|---|---|---|
| Arranque Spring | Sí, fuente | No hay evidencia | Parcial: configuración | ejecución reproducible | Base |
| Configuración | Sí, fuente | No | No hay consumidores | validación y perfiles | Parcial |
| Entidades identidad | Sí, fuente | No | No: sin repositorios/migración | persistencia y flujos | Parcial |
| RBAC | Sí, modelo | No | No | autoridades, API y datos | Parcial |
| UUIDv7 | Sí, componente | No | No se usa | pruebas e integración | Aislado |
| API REST | No | No | No | implementación completa | Pendiente |
| Autenticación/recuperación | Sólo diseño/properties | No | No | implementación completa | Pendiente |
| Frontend | No | No | No | implementación completa | Ausente |
| Migraciones | No (sólo README) | No | No | esquema versionado | Pendiente |
| Docker declarativo | No | No | hay contenedores externos | archivos de definición | Ausente |

No se proporciona porcentaje de avance: no existe alcance funcional, backlog, pruebas ni historial contra el cual calcularlo sin inventar datos.

## Qué funciona y qué no puede afirmarse
- **Verificado en código:** el arranque está definido; el modelo JPA y sus métodos existen; UUIDv7 se puede instanciar como componente; se declaran propiedades y dependencias.
- **Parcial:** identidad/RBAC son modelos sin capa de aplicación o base reproducible.
- **Desarrollado pero no probado:** todas las clases Java localizadas.
- **No implementado en el árbol:** API, seguridad efectiva, correo, repositorios, migraciones, frontend, pruebas, despliegue reproducible.
- **Listo para demostración:** no hay flujo de negocio demostrable desde las fuentes. El backend desplegado responde 401, pero sus capacidades internas no fueron atribuidas ni verificadas.

Para considerar finalizado al menos un módulo de identidad se requiere migración, repositorio/servicio, endpoints, autenticación/authorization, pruebas y documentación de arranque.
