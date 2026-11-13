# Problemas y pendientes

| Prioridad | Hallazgo e impacto | Recomendación |
|---|---|---|
| 🔴 Crítico | No hay migraciones SQL iniciales, pero Hibernate valida esquema. Una instalación limpia no puede construir el modelo requerido. | Versionar un baseline real o una migración inicial compatible con la política Flyway y probarla en MySQL limpio. |
| 🔴 Crítico | No existe código fuente de API: controladores, servicios, repositorios, DTO, seguridad y JWT no están presentes. No hay flujo usable de identidad. | Recuperar el código faltante si existe fuera del alcance o implementar los flujos mínimos de extremo a extremo. |
| 🔴 Crítico | La infraestructura que ejecuta `sistema-backend` no tiene definición Docker/Compose en el árbol. No es reproducible ni auditable desde el proyecto. | Incorporar archivos declarativos sin secretos y documentar variables/volúmenes/redes. |
| 🟠 Alto | No hay pruebas ni cobertura. Las reglas de bloqueo, entidades y UUIDv7 pueden contener errores sin detección. | Crear pruebas unitarias e integración antes de exponer rutas. |
| 🟠 Alto | `SECURITY.md` promete Argon2id, JWT, tokens opacos, cookies y control de origen, pero no se encontró código de implementación. Riesgo de discrepancia entre diseño y despliegue. | Implementar, probar y trazar cada decisión a clases/configuración concretas. |
| 🟠 Alto | No hay `.git`; se perdió trazabilidad de cambios, revisión y recuperación. | Inicializar/recuperar repositorio y subir historial o documentar su ubicación autorizada. |
| 🟡 Medio | Relaciones `ManyToMany` con `FetchType.EAGER` pueden provocar cargas innecesarias y consultas costosas al escalar. | Evaluar `LAZY`, consultas específicas y DTOs según necesidades reales. |
| 🟡 Medio | Propiedades críticas no tienen `@Validated` ni restricciones; valores mal configurados pueden fallar tarde o reducir seguridad. | Añadir validación a `@ConfigurationProperties` y pruebas de arranque. |
| 🟡 Medio | `UuidV7Generator` existe pero ninguna entidad lo utiliza. | Integrarlo al flujo de alta o eliminarlo si no forma parte del diseño. |
| 🟡 Medio | Los métodos de entidad usan estado `ACTIVE` al desbloquear sin preservar la intención de `PENDING`/`DISABLED` en escenarios no cubiertos. | Definir máquina de estados y probar las transiciones permitidas. |
| 🟢 Bajo | Falta README operativo, wrapper Maven y scripts de desarrollo. | Documentar requisitos, arranque, variables y comandos; considerar Maven Wrapper. |

No se detectaron errores de compilación porque no se ejecutó una compilación, y no se debe inferir que el proyecto compila únicamente por la existencia de una imagen Docker.
