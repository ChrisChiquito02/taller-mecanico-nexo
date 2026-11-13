# Motor real, selección y privacidad

## Decisión implementada

Ollama local con `qwen2.5-coder:7b`: ya estaba instalado en la Mac ARM64 con 24 GiB de RAM. El modelo ocupa aproximadamente 4,68 GB en disco, es Q4_K_M y declara 7,6B parámetros. No se descargó otro modelo ni se contrataron APIs. La RAM total no garantiza por sí sola calidad/latencia; las mediciones reales de esta entrega están en los registros de navegador.

`ModelProvider` define `status`, `plan` y `stream`; `OllamaProvider` implementa el protocolo HTTP de Ollama sin SDK externo. Las respuestas llegan desde `/api/chat`; no son texto predefinido. El modo automático usa una generación JSON para elegir una herramienta y otra generación para responder. El estado consulta `/api/tags` y comprueba el modelo exacto. [API de Ollama](https://docs.ollama.com/api/chat), [herramientas](https://docs.ollama.com/capabilities/tool-calling).

Se permite `127.0.0.1`, `localhost`, `host.docker.internal` u `ollama` como destinos locales configurados por el operador. No se aceptan destinos de un prompt, redirecciones, proxy del entorno, credenciales en URL ni query. `host.docker.internal` es necesario para Docker Desktop. No se expone el puerto de Ollama públicamente.

## Comparación investigada

| Alternativa | Privacidad y coste | Calidad/latencia | Estado |
|---|---|---|---|
| Ollama local, Qwen 7B | Sin tarifa de API; consume hardware/electricidad propios. Contexto permanece en servicios locales | Depende del hardware; memoria multitur­no y RAG se prueban en esta Mac | Implementado y sometido a inferencias reales |
| API externa compatible, por ejemplo Groq | Plan gratuito sujeto a cuotas; datos salen al proveedor; requiere credenciales y revisión de tratamiento | Puede ofrecer baja latencia; no se midió aquí | Investigada, no conectada ni habilitada |
| Híbrido con modelo autogestionado remoto | Control del servidor propio; necesita red privada/cifrado, capacidad y operación | CPU ARM gratuita puede ser lenta; no se ha medido | Arquitectura posible, pendiente de infraestructura |

[Compatibilidad de Groq](https://console.groq.com/docs/openai), [cuotas](https://console.groq.com/docs/rate-limits), [tratamiento de datos](https://console.groq.com/docs/your-data). No se añade un fallback externo: un fallo local no debe enviar conversaciones privadas fuera de la Mac ni generar costes inesperados. La interfaz de proveedor permite desarrollar un adaptador sin cambiar la aplicación; **esta entrega incluye solo el adaptador Ollama**, y el modo `disabled` explícito.

## Contexto y RAG

Cinco documentos curados incluidos en el JAR, con título, fuente y permiso. Recuperación léxica por palabras normalizadas, máximo tres documentos. No se usa una base vectorial: el corpus pequeño no la requiere, evita nuevas dependencias y permite inspeccionar exactamente qué documentos recibe el modelo. El documento operativo requiere `users:write`; los demás `profile:read`. El endpoint que abre una referencia vuelve a comprobar la ACL.

Los archivos del usuario son solo texto adjunto al turno (txt, md, json, log, java, js, sql), hasta 6000 caracteres. No se leen rutas arbitrarias ni se ejecuta el contenido. No se indexan globalmente. Preguntas hasta 4000 caracteres; cuerpo total sujeto al límite HTTP de 16 KiB. El usuario puede compartir fragmentos de errores o pruebas después de eliminar datos privados. Historial visible de hasta 40 turnos; el modelo utiliza como máximo doce mensajes recientes, acotados por tamaño. No tiene memoria ilimitada.

Los datos recuperados se marcan como datos, separados de instrucciones. Las referencias se adjuntan como metadatos provenientes del servidor, no de URLs inventadas por el modelo. La respuesta puede omitir o citar mal un documento: la UI permite abrir las fuentes efectivamente recuperadas para comprobarlo. No se afirma verificación semántica automática.

## Límites de generación

700 tokens por respuesta, 180 para el plan, contexto Ollama de 8192 tokens, 180 segundos por llamada por defecto; configurable dentro de límites. Streaming de hasta 14 000 caracteres y máximo 300 000 caracteres de transporte. La lectura tiene cierre por plazo incluso cuando el proveedor se estanca. Un turno automático puede consumir dos llamadas. Hay un límite de dos inferencias concurrentes y 100 turnos iniciados por usuario/día. El navegador no reintenta automáticamente un stream parcial.

Se da al modelo una muestra de hasta cinco afectados, conteo total, puntajes, alternativas y límites. El informe completo determinista permanece separado en la interfaz; informes demasiado grandes para almacenarse en un chat se rechazan antes de generar. El presupuesto por tamaño es aproximado en caracteres, no un contador exacto de tokens del modelo.

## Qué significa “IA propia”

La personalización es el dominio, políticas de herramientas, recuperación, interfaz, persistencia, metodología y pruebas. No es un modelo fundacional creado desde cero ni una promesa de no repetición con compañeros. La novedad defendible es unir simulación contrafactual determinista de permisos y sesiones con explicación por IA, alternativas de menor privilegio y rollback propuesto, dentro del IAM real y sin mutarlo.

## Revisión de calidad observada

Las ejecuciones reales conservadas muestran respuestas útiles y también errores: en una explicación el modelo confundió conservar ADMIN con la alternativa calculada USER; en otra expresó incorrectamente la dirección del puntaje. Mejorar el prompt no eliminó de forma fiable estos errores. Se mantienen las respuestas auténticas en evidencia, sin reemplazarlas por texto redactado manualmente.

La interfaz etiqueta la explicación como no verificada y presenta por separado los hechos del laboratorio. Una comprobación heurística puede advertir si una respuesta en español propone mantener roles actuales cuando la alternativa calculada exige cambiarlos. Esa regla no detecta toda contradicción y jamás etiqueta una respuesta como certificada. La revisión humana y el informe determinista siguen siendo necesarios. Este modelo local no ha superado una evaluación exhaustiva de exactitud semántica.
