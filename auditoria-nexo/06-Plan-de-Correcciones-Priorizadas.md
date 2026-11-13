# Plan de correcciones priorizadas

| Prioridad | Trabajo | Criterio verificable |
|---|---|---|
| P0 | Encapsular datos no confiables de adjuntos y asegurar la ejecución del agente. H01. | Repetir caso 40 con variaciones, enlaces y datos extraídos. Acreditar que las entradas no cambian la salida solicitada ni habilitan herramientas o nuevos argumentos. |
| P0 | Verificar fidelidad de documentación y citas. H02. | Responder a preguntas sobre el repositorio con IDs autorizados y afirmaciones respaldadas por el fragmento. Comparar respuesta con fuentes y detectar citas inventadas. |
| P1 | Corregir análisis de texto adjunto. H03. | Probar desde Chrome todos los tipos declarados, contenido válido, límites, caracteres no ASCII y recuperación del turno. |
| P1 | Gestionar cancelación y truncamiento. H04–H05. | Simular caída, abortar antes y después del primer token, confirmar liberación y consultar MySQL al terminar. Una respuesta con razón `length` se presenta como limitada. |
| P1 | Mejorar alcance de memoria y probar revocación. H06, H08. | Recuperar un dato sembrado fuera de ventana tras reiniciar. Ensayar concurrencia entre dos instancias y revocación con cuentas sintéticas. |
| P1 | Verificar despliegue y operación. | Validar TLS, correo real, restauración de copia, CORS y cookies del dominio final. Medir latencia y capacidad con carga sostenida. |
| P2 | Alinear catálogo y mejorar identificación del historial. H09–H10. | El selector refleja las capacidades de la API y permite distinguir conversaciones creadas en el mismo minuto. |

Para una demostración académica, ensayar el recorrido de chat general, historial y laboratorio antes de presentarlo. Mostrar la tabla determinista al explicar permisos y preparar un caso que reproduzca la limitación de adjuntos o de citas.
