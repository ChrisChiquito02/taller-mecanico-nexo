# Diagnóstico final

| Pregunta | Respuesta sustentada |
|---|---|
| ¿Está terminado al 100 %? | **No.** Cinco criterios son defectuosos, seis parciales y cinco pendientes de verificación. |
| ¿La IA funciona realmente? | **Sí.** Ollama 0.40.16 generó con `qwen2.5-coder:7b`. Coincidieron las huellas de las respuestas de Ollama y los deltas del navegador y coincidió el texto visible. |
| ¿Comprende y responde coherentemente? | En 29 de 30 casos de conversación, razonamiento, lenguaje y contradicción. Falló una corrección de premisa y produjo información documental incorrecta en la prueba real del proyecto. |
| ¿Tiene memoria funcional? | **Persistencia confirmada.** MySQL conservó los mismos 34 mensajes medidos tras reiniciar. Hay memoria reciente. Al pedir el dato ZAFIRO-81 fuera de la ventana, el modelo contestó «ExamenErik». |
| ¿Puede realizar todas las funciones anunciadas? | **No.** Chat, historial, simulaciones y exportación dieron resultados. El análisis de TXT falló en la tarea de extracción y la seguridad de adjuntos falló en el caso de inyección. |
| ¿Qué necesita mejorar? | Resistencia a inyección, fidelidad y citas, recuperación de contexto antiguo, cancelación, truncamiento y pruebas operativas. |
| ¿Qué tan estable es? | Arrancó con MySQL real, conservó los datos después del reinicio y mostró el fallo de proveedor sin inventar respuestas. La cancelación dejó el backend ocupado y una salida truncada se presentó como completa. Carga y alta disponibilidad están pendientes. |
| ¿Está listo para una demostración académica? | **Sí, con alcance acotado.** Chat general, historial y simulación con resultados deterministas son demostrables. Preparar los ejemplos y describir los límites de adjuntos, referencias y memoria. |
| ¿Está listo para producción? | **No.** Fallos reproducibles en el manejo de adjuntos y la confiabilidad de la salida, más verificaciones operativas pendientes. |

| Indicador | Resultado |
|---|---|
| Implementación de NEXO-Δ AI | **89,30 %** |
| Funcionamiento real verificado | **65,21 %** |
| Integración con Ollama | Verificada de extremo a extremo |
| Coherencia conversacional | **96,67 %** (29 de 30 casos A/B/C/E) |
| Memoria persistente | Verificada, con recuerdo limitado a contexto reciente |
| Funciones completas | 24 |
| Funciones parciales | 6 |
| Funciones simuladas | 1, simulación legítima identificada |
| Funciones defectuosas | 5 |
| Funciones no verificadas | 5 |
| Pruebas aprobadas | 45 de 47 en la batería principal |
| Pruebas fallidas | 2 de 47 |
| Pruebas bloqueadas | 0 de 47 |
| Preparación académica | Apta con alcance acotado |
| Preparación para producción | No apta |

[Informe y fórmula](01-Informe-Auditoria-NEXO.md) · [Matriz](02-Matriz-Funcionalidades.csv) · [Resultados por caso](03-Pruebas-Coherencia.md).
