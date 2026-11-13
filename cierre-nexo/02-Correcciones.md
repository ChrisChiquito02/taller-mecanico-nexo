# Correcciones finales

La versión anterior devolvió `done` en seis de seis ensayos con respuestas que violaban límites compatibles de palabras o líneas. El fallo quedó reproducido en español e inglés. No se modificaron autenticación, autorización, memoria, adjuntos, esquema de base de datos ni frontend. [Reproducción](evidencias/reproduccion.json).

| Archivo | Cambio |
|---|---|
| `OutputConstraints.java` | Reconoce uno o más límites explícitos de palabras, líneas, frases o párrafos en la apertura de la petición. Comprueba la intersección por unidad y mide la salida real. Distingue conjunciones de alternativas y evita interpretar las citas de los casos ensayados. |
| `NexoService.java` | Pide aclaración antes de inferir ante límites incompatibles. En chat general retiene la salida restringida hasta validarla. Si falla, realiza un solo reintento con la medida observada. Si vuelve a incumplir, emite `OUTPUT_CONSTRAINT_UNMET` y el turno queda sin publicar ni persistir. Los demás turnos conservan su recorrido. |
| `OutputConstraintsTest.java`, `SystemIntegrationTest.java` | Regresión de límites simples y combinados, unidades distintas, primer intento inválido y rechazo después de dos generaciones inválidas. |
| `cierre-nexo/demo/` | JAR compilado y verificado por SHA-256. Inicio y operación de un proyecto Compose independiente con cuentas y datos sintéticos. |

Las restricciones se interpretan según una gramática acotada. Las líneas vacías quedan fuera del conteo de líneas, las palabras usan letras y números Unicode y las frases se segmentan con la biblioteca de Java. No se afirma cumplimiento de otras instrucciones semánticas. Los casos reales siguieron mostrando límites compatibles que Ollama no pudo cumplir aun después del reintento. [Resultados](evidencias/restricciones.json).

Antes de editar se extrajo una copia de `sistema-m01-m02/src` en otro directorio, se comparó con el original y se comprobó SHA-256. [Respaldo](respaldo.tgz) · [Huella](respaldo.sha256).
