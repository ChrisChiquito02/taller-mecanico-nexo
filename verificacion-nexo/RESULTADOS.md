# ExamenErik y NEXO-Δ AI · correcciones y verificación

12 de noviembre de 2026. El alcance se midió contra los siete informes de `auditoria-nexo`. El código, el JAR y el frontend compilado quedaron actualizados. La base de pruebas conservó sus conversaciones anteriores y recibió la migración aditiva V4. Los datos y permisos reales quedaron fuera de las pruebas.

## Comparación antes y después

| Indicador | Auditoría original | Versión corregida |
|---|---:|---:|
| Implementación ponderada | 89,30 % | **96,32 %** |
| Funcionamiento real verificado | 65,21 % | **85,27 %** |
| Batería completa de coherencia, memoria y seguridad | 45/47 | **46/47** |
| Coherencia A/B/C/E | 29/30 | **29/30** |
| Funciones completas | 24 | 33 |
| Simulación legítima comprobada | 1 | 1 |
| Funciones parciales | 6 | 6 |
| Criterios clasificados como no funcionales | 5 | 0 |
| Criterios sin verificar | 5 | 1 |

La fórmula original se conserva. Cada categoría usa su peso anterior, multiplicado por el promedio de sus criterios. Implementación asigna 1, 0,5 o 0 según el alcance entregado. Funcionamiento asigna 1 al criterio acreditado mediante ejecución real. Una funcionalidad parcial o pendiente aporta 0 a este último indicador. El denominador conserva los 41 criterios. Las cifras son específicas de esta auditoría y entorno.

[Matriz](matriz-antes-despues.csv) · [Cálculo](puntuar.py) · [Indicadores](indicadores.json).

## Cambios y justificación por archivo

| Cambio | Archivos principales | Evidencia y alcance |
|---|---|---|
| Aislamiento de adjuntos y extracción local | `AttachmentReader.java`, `NexoService.java`, `ConversationContext.java`, `frontend/src/nexo.js` | El adjunto queda fuera del planificador y de la inferencia. La extracción valida una línea de código de inventario o devuelve un extracto literal acotado. Se rechazan archivos no válidos y se excluyen extractos y adjuntos históricos del contexto de inferencia. Los cinco casos adversariales y la comprobación de historial pasaron. El extracto general puede contener texto controlado por el archivo y se muestra como datos inertes. |
| Truncamiento | `ModelProvider.java`, `OllamaProvider.java`, `NexoService.java`, `frontend/src/api-client.js`, `frontend/src/nexo.js` | El proveedor propaga una terminación distinta de `stop` y el chat emite `truncated`. Chrome mostró la advertencia, sin `done`, y MySQL registró cero mensajes del turno. La ejecución inicial con 700 tokens acabó por `stop`; para reproducir el límite se usó una segunda instancia de prueba configurada con 64 tokens. |
| Cancelación y concurrencia | `NexoController.java`, `TurnReservations.java`, `ConversationStore.java`, `V4__nexo_turn_reservation.sql`, `NexoService.java`, `frontend/src/nexo.js` | Cancelación autenticada por ID de turno. MySQL coordina las dos instancias y bloquea el guardado cancelado. El servidor que ejecuta la generación interrumpe su solicitud a Ollama y cierra el cuerpo de respuesta. El flujo web de cancelación y borrado pasó, igual que cancelación remota, rechazo ajeno y revocación de sesión durante inferencia. El tiempo observado de cancelación entre instancias fue 0,19 s. La liberación interna de recursos de Ollama no se midió. |
| Fidelidad documental | `KnowledgeBase.java`, `NexoService.java`, `resources/nexo/arquitectura.md` | Fuentes locales autorizadas y citas asignadas por el backend. La premisa falsa sobre microservicios se contestó con la descripción verificable de un backend Spring Boot modular. La interfaz identifica la procedencia. El chat general conserva inferencia real. |
| Memoria | `ConversationContext.java`, `ConversationStore.java` | Contexto reciente y búsqueda léxica acotada de hasta tres fragmentos de turnos anteriores de la misma conversación. Recuperó DIAMANTE-742 después de ocho turnos intermedios y de reiniciar Java y MySQL. La huella del historial antes y después coincidió. Se verificaron caducidad y limpieza efectiva. |
| Informes, selector e historial | `NexoService.java`, `frontend/src/nexo.js`, `frontend/public/index.html` | El generador de informes inventó resultados en una prueba intermedia. Se sustituyó por una plantilla local que preserva el texto aportado y marca los datos como pendientes de corroboración. La redacción flexible sigue parcial. El selector consume el catálogo y el historial muestra un identificador breve. |
| Regresión y empaquetado | `OllamaProviderTest.java`, `SystemIntegrationTest.java`, `frontend/tests/unit/nexo.test.js`, `frontend/dist/`, `resources/static/`, `documentacion/nexo-ai/` | Contratos de truncamiento, aislamiento y migración. Se regeneraron las versiones integrada y separada del frontend y se documentaron compatibilidad y operación. |

[Inventario exacto](archivos-cambiados.txt) · [Impacto previo a la implementación](diseno.md).

## Verificación

- **56/56 Java**. Pruebas unitarias y de integración con MySQL 8.4. Los dobles del proveedor verifican contratos. [Registro final](paquete-final.log).
- **20/20 frontend** y **12/12 Python legado**. [Frontend](unit-frontend-final.log) · [Legado](unit-legado.log).
- **18/18 Chrome** con el JAR final, más la prueba específica de truncamiento con Ollama real. La prueba de multiplicación y la explicación de simulación coincidieron con sus huellas SHA-256 de Ollama en la corrida instrumentada. Los resultados documentales y del adjunto fueron locales y se mostraron con su procedencia. [Chrome final](navegador.json) · [Huellas](huellas.txt) · [Truncamiento](truncamiento.json) · [SQL](truncamiento-sql.log).
- **46/47 batería original**. Pasaron aislamiento, adjuntos, memoria y corrección de la premisa sobre Canadá. Falló el caso 30, que pedía detectar que dos y cuatro palabras exactas eran requisitos incompatibles. El modelo repitió el tema anterior. [Resultados por caso](bateria.jsonl).
- **Dos instancias y MySQL real**. Un turno concurrente de la misma cuenta recibió 409, una cancelación ajena 404 y la cancelación autorizada 204. La revocación durante inferencia devolvió `UNAUTHORIZED`. La caída controlada del relé de auditoría devolvió `MODEL_UNAVAILABLE` sin guardar mensajes. [Controles](controles.jsonl).
- **Restauración local**. Se restauró una copia sintética en un esquema separado. Coincidieron el total de 360 mensajes y la suma de sus CRC32. [Registro de restauración](restauracion.log).

## Respaldo y límites operativos

Se creó un archivo de fuentes previo a las modificaciones y se verificó SHA-256. Incluye las fuentes, pruebas, frontend compilado y documentación modificada. Excluye configuración privada y datos reales. El inventario permite restaurar archivos concretos. V4 se probó en un esquema sintético. Un rollback de aplicación a un JAR anterior a V4 requiere coordinar su validación Flyway con el esquema previo. Los servicios iniciados para las pruebas se detuvieron y se conservaron los datos sintéticos.

[Respaldo](respaldo-fuentes.tgz) · [Huella](respaldo.sha256) · [Inventario](respaldo-inventario.txt) · [Artefactos](artefactos.sha256).

## Preparación

**Académica. Apta con alcance acotado.** El chat general, la documentación, la extracción TXT, el historial y el laboratorio tienen recorridos reproducibles.

**Producción. Pendiente.** La prosa sobre simulaciones todavía puede confundir refresh conocidos con todas las sesiones. La plantilla de informes tiene alcance limitado. La memoria léxica depende de coincidencias con lo que el usuario escribió y del presupuesto de contexto. Las cuotas por minuto y el límite de generación siguen siendo por instancia. Quedan por verificar carga sostenida, TLS y cookies en el dominio final, SMTP externo y restauración en la topología de producción. El filtro de secretos sigue siendo heurístico y el chat general conserva el riesgo inherente de errores del modelo.
