# Auditoría técnica de ExamenErik y NEXO-Δ AI

**12 de noviembre de 2026 · Entorno local de pruebas · Ciudad de México**

NEXO-Δ AI usa un modelo auténtico y tiene chat, historial persistente y un laboratorio operativo. La salida de Ollama coincidió con el texto recibido por Chrome. Las limitaciones observadas afectan seguridad de adjuntos, fidelidad documental, memoria de largo alcance, cancelación y señalización de respuestas truncadas. La evaluación distingue **89,30 % de implementación** y **65,21 % de funcionamiento verificado** sobre 41 criterios ponderados.

## Arquitectura observada

| Componente | Ubicación | Responsabilidad |
|---|---|---|
| Orquestación y documentación | `orquestador.md`, `README.md`, `documentacion/nexo-ai/`, `documentacion/backend/`, `documentacion/frontend/`, `docs/` | Diseño, operación y evidencias anteriores. El orquestador existe. |
| Interfaz web | `frontend/src/`, `frontend/public/`, `frontend/dist/` | JS, esbuild, vistas por fragmento y lector NDJSON. Spring puede servir la versión integrada desde `resources/static/`. |
| Backend principal | `sistema-m01-m02/src/main/java/` | Una aplicación Spring Boot 4.1.1 con módulos de identidad y NEXO. Controladores, servicios de dominio, JPA para identidad y JDBC para conversaciones y snapshots. |
| Identidad | `identity/api`, `identity/security`, `identity/service` | Registro, login, refresh/logout, recuperación, perfil, roles y administración. Argon2, JWT, cookie de refresh, estado y versión de sesión, permisos consultados en DB, CORS y guardas de origen. |
| Orquestación IA | `nexo/api/NexoController.java`, `nexo/service/NexoService.java`, `nexo/model/OllamaProvider.java` | Autoriza, aplica cuotas, carga historial, invoca plan y generación y transmite NDJSON. El proveedor HTTP privado restringe hosts y no sigue redirecciones. |
| Fuentes y herramientas | `nexo/service/KnowledgeBase.java`, `AccessLab.java`, `resources/nexo/` | Búsqueda léxica en cinco documentos empaquetados, con ACL y máximo de tres coincidencias. Laboratorio determinista con fixtures o snapshot IAM. Las herramientas de error, pruebas e informe son asesoría generativa. |
| Persistencia | `resources/db/migration/`, `nexo/repository/` | MySQL con Flyway V1–V3. Conversación ligada a propietario y huella de autoridades, mensajes y auditoría. Caducidad predeterminada de siete días. |
| Infraestructura | `deploy/nexo-local.yml`, `sistema-m01-m02/compose.yml`, Dockerfiles y `deploy/mysql/` | Compose local, MySQL, SMTP y backend. Configuración separada de publicación API, TLS y DNS. |
| Laboratorio legado | `agente-nexo/` | Motor Python, panel y servidor anteriores. El lanzador del mismo directorio inicia la aplicación Java integrada. |
| Calidad y seguridad | `frontend/tests/`, `src/test/`, `seguridad/`, `.github/workflows/` | Casos unitarios, integración, Playwright, RESTler y verificación. Las capturas históricas quedaron fuera de las mediciones. |

La ruta principal usa `/api/auth/*`, `/api/users/me` y `/api/nexo/*`. NEXO expone estado, catálogo, documentos, listado/alta/lectura/borrado de chats, envío de turno y simulación. El laboratorio lee el IAM autorizado y registra auditoría. El informe de riesgo es calculado por reglas con escenarios hipotéticos y sus puntuaciones no constituyen cambios de permisos ni predicciones de incidentes reales.

## Criterios y cálculo reproducible

Cada fila de la [matriz](02-Matriz-Funcionalidades.csv) es un criterio. Se fijó el mismo peso para las filas dentro de una categoría y se conservaron los pesos solicitados entre categorías. **Implementación** asigna 1 a mecanismo presente de alcance suficiente, 0,5 a mecanismo incompleto y 0 a ausencia. Esta inspección puede acreditar implementación aunque la prueba funcional falle. **Funcionamiento** asigna 1 solo a un criterio completado mediante prueba real, incluida la simulación legítima identificada. Asigna 0 a resultados parciales, fallidos y pendientes, conservando cada estado separado. Los pendientes permanecen en el denominador para medir cobertura del alcance. El indicador es un mínimo acreditado.

`Indicador = suma(peso porcentual de categoría × suma de valores de filas / número de filas de categoría)`.

| Categoría | Peso | Criterios | Aprobados completos | Fallidos o parciales | Pendientes | Implementación | Verificado |
|---|---:|---:|---:|---:|---:|---:|---:|
| LLM | 20 % | 4 | 4 | 0 | 0 | 100.0 % | 100.0 % |
| Coherencia | 20 % | 5 | 3 | 2 | 0 | 90.0 % | 60.0 % |
| Memoria | 15 % | 5 | 3 | 1 | 1 | 90.0 % | 60.0 % |
| Agente | 15 % | 11 | 7 | 4 | 0 | 90.9 % | 63.6 % |
| Frontend/backend | 10 % | 6 | 4 | 2 | 0 | 91.7 % | 66.7 % |
| Persistencia/seguridad | 10 % | 5 | 2 | 1 | 2 | 90.0 % | 40.0 % |
| Estabilidad | 10 % | 5 | 2 | 1 | 2 | 60.0 % | 40.0 % |

## Evidencia y confianza por categoría

| Categoría | Pruebas y confianza |
|---|---|
| LLM | Alta para la instalación local. Inventario, llamada de planificación, streaming, igualdad de huellas y fallo controlado. |
| Coherencia | Media. 30 casos A/B/C/E, 29 aprobados; la prueba documental de navegador detectó una arquitectura inventada. La muestra es pequeña y dependiente de la generación. |
| Memoria | Alta para persistencia y aislamiento en las cuentas probadas, media para calidad de recuerdo. API y Chrome tras reinicio, dato cercano recuperado y antiguo inventado. |
| Agente | Media. Chrome ejecutó laboratorio, adjunto, referencia, explicación y exportación. Herramientas de error, pruebas e informe se ejercitaron por API real. |
| Frontend/backend | Alta para los flujos ensayados, media para compatibilidad general. Chrome local, sesión, historial, estados, acciones y vista de 375 px. |
| Persistencia/seguridad | Media. Casos 41–45 y SEC validan barreras concretas. Revocación concurrente y topología TLS siguen pendientes. |
| Estabilidad | Media para arranque y recuperación local, baja para carga de producción. Arranque compilado, Flyway, reinicio, caída del proveedor y salida truncada. |

La [batería de 47 casos](03-Pruebas-Coherencia.md) mide respuestas o guardas individuales. La matriz mide capacidades agregadas con criterios más amplios. UI05 aprobó el recorrido técnico de inferencia y falló la evaluación de fidelidad documental. Los casos parcialmente satisfechos requieren completar la prueba para acreditar la fila.

## Límites de la auditoría

Se probó una instancia aislada y un navegador en escritorio y a 375 px. Quedaron sin verificación de extremo a extremo la limpieza por caducidad, la revocación concurrente durante una respuesta, la concurrencia distribuida, restauración de backups y despliegue TLS con SMTP externo. Se revisaron configuraciones de límites y consultas; no se realizó certificación de dependencias ni un pentest de infraestructura. La recomendación académica se aplica al entorno local evaluado.

[Evidencias técnicas](05-Evidencias-Tecnicas.md) · [Hallazgos](04-Errores-y-Funciones-No-Operativas.md) · [Plan](06-Plan-de-Correcciones-Priorizadas.md) · [Diagnóstico](07-Diagnostico-Final.md).
