# Resultados de pruebas

| Suite | Resultado |
|---|---:|
| Java, incluidos tres casos nuevos | 63/63 |
| Frontend unitario | 20/20 |
| Python legado | 12/12 |
| Chrome | 18/18 |
| **Total técnico comparable** | **113/113** |
| Batería de comportamiento con el JAR final | **47/47** |

[Java](evidencias/java-final.log) · [Frontend](evidencias/frontend.log) · [Legado](evidencias/legado.log) · [Chrome](evidencias/final/navegador.json) · [Batería completa](evidencias/final/bateria.jsonl).

- **Restricciones.** En 18 intentos reales, siete generaciones cumplieron, tres conflictos se aclararon y ocho resultados se rechazaron sin publicar ni guardar. Los 18 pasaron el oráculo de seguridad del turno. Las ocho solicitudes rechazadas quedaron sin respuesta que satisficiera sus límites. [Casos](evidencias/restricciones.json).
- **Inferencia auténtica.** Los deltas del chat general y el DOM coincidieron con la huella SHA-256 de la respuesta de `qwen2.5-coder:7b`. Los extractos documentales y de adjuntos se identifican como locales. [Huellas](evidencias/huellas.txt) · [Traza](evidencias/ollama.log).
- **Conversación prolongada.** Se guardaron 40 turnos y 80 mensajes. El último turno recuperó CEDRO-374 del primero. Otra cuenta recibió 404 y el turno 41 recibió 429 `CHAT_FULL`. Se verificaron también la memoria tras reiniciar Java y la purga de chats vencidos. [Conversación](evidencias/conversacion.json) · [Memoria](evidencias/memoria.log).
- **Concurrencia.** La repetición completó 148 turnos, rechazó 59 por capacidad y conservó 296 mensajes, con seis cuentas, dos réplicas y 69,49 s de ejecución. La cuota compartida produjo 30 altas, 30 rechazos por límite de chats y dos por cuota. El primer ensayo registró 31 errores 500 en la fase de altas alternadas. Tras reiniciar la réplica se repitió la prueba y no aparecieron. La causa no quedó determinada. [Incidencia](evidencias/carga-incidencia.json) · [Repetición](evidencias/carga.json).
- **Cancelación, sesiones y adjuntos.** Cancelación cruzada en 0,19 s, sin mensajes guardados. La cuenta ajena recibió 404 y el turno concurrente de la misma cuenta 409. Se verificaron revocación durante inferencia, adjuntos adversariales y ACL de documentos. [Controles](evidencias/controles.jsonl).
- **Paquete de demostración.** Compose arrancó con base sintética nueva, bootstrap único y Java 21. Chrome generó 391 con Ollama y recuperó el resultado al recargar. Se repitió después de reiniciar Java y después de reiniciar MySQL. [Arranque](evidencias/demo.log) · [Navegador](evidencias/demo-chrome.json) · [Reinicio Java](evidencias/demo-reinicio-chrome.json) · [Reinicio MySQL](evidencias/demo-mysql-chrome.json).

## Pruebas no ejecutadas en esta fase

- Dominio, certificados, secretos, CORS y cookies entre hosts de producción.
- Entrega SMTP externa y restauración en la infraestructura objetivo.
- Carga prolongada de producción con turnos largos y objetivos de servicio definidos.
- Apagado forzado de Ollama compartido. Se conservó la evidencia de fallo controlado del proveedor de la fase anterior.
- Nueva prueba específica de truncamiento con límite de 64 tokens. Se conservó la prueba real anterior y se repitieron sus contratos técnicos.
