# NEXO-Δ AI · estabilización final

**12 de noviembre de 2026 · entorno aislado de ExamenErik**

| Indicador | Auditoría original | Fase anterior | Estabilización |
|---|---:|---:|---:|
| Implementación | 89,30 % | 96,32 % | **98,00 %** |
| Funcionamiento verificado | 65,21 % | 85,27 % | **94,00 %** |
| Funciones completas | 24 | 33 | **37** |
| Batería de comportamiento | 45/47 | 46/47 | **47/47** |

Se mantuvieron los 41 criterios, los pesos por categoría y la asignación de 0/0,5/1 para implementación. Funcionamiento aporta 1 para un criterio completo o una simulación legítima acreditada. Tres criterios parciales siguen aportando 0 a funcionamiento. [Matriz](matriz-final.csv) · [Cálculo](puntuar.py) · [Indicadores](indicadores.json).

## Criterios examinados

| Pendiente anterior | Resultado | Evidencia |
|---|---|---|
| Premisas falsas y restricciones incompatibles | Java calcula la intersección de límites explícitos sobre una misma salida y pide aclaración si está vacía. Conserva alternativas, citas y unidades distintas fuera de esa regla. El prompt del modelo prioriza el turno actual. | [Fallo reproducido](reproduccion.json) · [Batería final](bateria.jsonl) · [Variantes](casos.json) |
| Explicación de simulación | Ollama elige el foco. Java presenta el puntaje, la alternativa y la métrica calculada. Para lecturas reales explicita que se cuentan refresh vigentes conocidos y que el total de sesiones y JWT de acceso es desconocido. | [Casos](casos.json) · [Chrome](navegador.json) |
| Informe fiel a datos | Ollama elige título y prioridad en JSON con esquema. Java valida la selección y conserva todos los fragmentos literales con su procedencia. Es un informe extractivo. La primera iteración omitió un fallo de prueba y devolvió un plan inválido en otro caso. Los dos pasaron con el contrato final. | [Iteración](casos-iteracion.json) · [Versión final](casos.json) |
| SMTP y recuperación | Mailpit recibió el enlace, el primer uso devolvió 204, la reutilización y la contraseña anterior devolvieron 401 y la nueva contraseña permitió acceder. SMTP externo pendiente. | [Correo](correo.json) |
| TLS, secretos y cookies entre hosts | HTTPS local con certificado autofirmado, validación del certificado con curl y cookies Secure, HttpOnly y SameSite=Strict verificadas en Chrome. Dominio final, certificados públicos, secretos de despliegue y comunicación entre hosts pendientes. | [HTTPS](tls/health.json) · [Cookies](tls/chrome.json) |
| Concurrencia y carga sostenida | Dos réplicas, seis cuentas y 216 solicitudes en 60,73 s. Hubo 203 turnos completos y 13 rechazos esperados por capacidad. Los 406 mensajes de los turnos completos coincidieron con el historial. Mediana 138 ms, p95 309 ms para respuestas cortas completadas. Se acreditó cuota compartida con 62 solicitudes alternadas. | [Carga](carga.json) · [Cancelación cruzada](controles.jsonl) |
| Recuperación desde respaldo de producción | Se restauró un volcado sintético en otro esquema. Coincidieron 952 mensajes y su suma CRC32. Queda pendiente la restauración en la infraestructura de producción. | [Restauración MySQL](mysql-restauracion.log) |

## Separación entre el modelo y la aplicación

La versión anterior retomó Roma ante la contradicción del caso 30 y trató como compatibles tres líneas exactas y un mínimo de cinco. En una muestra directa de Ollama con cuatro solicitudes y tres intentos por solicitud, **1 de 12** cumplió su criterio automático. La muestra incluye una instrucción compatible con dos líneas esperadas. El caso 30 final se resolvió mediante el validador de Java. Otras contradicciones siguen delegadas al modelo y su respuesta puede contener explicaciones incorrectas. En la verificación de la solicitud compatible, la aplicación dejó pasar el turno y el modelo excedió dos palabras. El validador detecta incompatibilidad dentro de su gramática; el cumplimiento de restricciones compatibles sigue siendo una limitación del modelo. [Prueba directa](modelo-directo.json) · [Casos de aplicación](casos.json) · [Contradicciones abiertas](contradiccion-abierta.json).

## Regresión y recuperación

- **60/60 Java**, con MySQL real en la suite de integración. Los dobles de proveedor se usaron para contratos. **20/20 frontend**, **12/12 legado**, **18/18 Chrome**. Los seis controles funcionales de casos adicionales pasaron sus oráculos; el caso compatible acreditó encaminamiento a Ollama. [Java](paquete.log) · [Frontend](frontend.log) · [Legado](legado.log) · [Chrome](navegador.json).
- La inferencia general en Chrome coincidió por SHA-256 con los tokens de Ollama. Los informes y simulaciones usan inferencia estructurada y renderizado posterior. Se verificó truncamiento real a 64 tokens en Chrome. [Huellas](huellas.txt) · [Ollama](ollama.log) · [Truncamiento](truncamiento.json).
- Pasaron los adjuntos adversariales, el aislamiento de cuentas, la cancelación entre réplicas y la revocación de sesión durante inferencia. La caída abrupta de un proceso durante un turno conservó los dos mensajes anteriores y permitió reintentar tras expirar la reserva. Se recuperó TIGRE-593 en 8,67 s desde el primer reintento tras reiniciar. Se verificaron también memoria antigua, lectura del historial y purga. [Controles](controles.jsonl) · [Caída](recuperacion.json) · [Memoria](memoria.log).
- Los ensayos usaron cuentas y bases sintéticas. V5 añade una tabla de cuota por cuenta y deja intactas V1–V4. El límite de generación sigue siendo por proceso. La carga local mide solicitudes cortas y no certifica una capacidad de producción.

## Cambios y restauración

- `OutputConstraints.java` y `NexoService.java` verifican límites explícitos antes de inferencia y separan la respuesta local de la generada.
- `GroundedResponses.java`, `ModelProvider.java` y `OllamaProvider.java` validan el plan y separan inferencia de las cifras y la evidencia literal.
- `TurnReservations.java` y V5 comparten la cuota de solicitudes. Las reservas activas se renuevan y vencen en 20 s si el proceso deja de renovarlas.
- `frontend/src/nexo.js` y sus compilados muestran la procedencia de respuestas y la unidad de tokens adecuada. Las pruebas cubren límites, alternativas, cifras y selección de fragmentos.
- Antes de editar se extrajo el respaldo en otro directorio y se comparó con los originales. Se comprobó la huella. Los servicios de prueba se detuvieron y se conservaron los datos sintéticos.

[Inventario](archivos-cambiados.txt) · [Diseño](diseno.md) · [Respaldo](respaldo.tgz) · [Huella](respaldo.sha256) · [Verificación de restauración](restauracion-fuentes.log) · [Cierre](cierre.log).

**Preparación académica. Apta.** Chat, informes extractivos, simulaciones, referencias y memoria tienen recorridos reproducibles.

**Preparación para producción. Pendiente.** Quedan SMTP externo, TLS y cookies en el dominio real y restauración en la infraestructura de producción. Se desconoce el rendimiento de turnos largos y carga prolongada. La protección de adjuntos separa texto e inferencia; el chat general sigue expuesto a errores de interpretación y a instrucciones adversariales que no se pueden excluir en todos los casos.

Referencias técnicas consultadas para las decisiones de implementación. [Bloqueos transaccionales en MySQL 8.4](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html?ff=nopfpls) · [Salidas estructuradas de Ollama](https://docs.ollama.com/capabilities/structured-outputs).
