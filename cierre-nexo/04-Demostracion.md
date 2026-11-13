# Demostración académica reproducible

El paquete de demostración usa el JAR verificado de esta entrega. Sirve el frontend y la API en `http://localhost:8090/#nexo` y crea un proyecto Compose y un volumen propios. La cuenta es sintética. El puerto solo se publica en loopback.

## Preparación e inicio

1. Abre Docker Desktop. Se usan Docker Compose, MySQL 8.4, Mailpit 1.27 y Eclipse Temurin 21. Java corre dentro del contenedor. El JAR incluye la interfaz y Flyway.
2. Abre Ollama en la Mac. Comprueba `ollama list` y que aparezca `qwen2.5-coder:7b`. Si falta, instálalo antes de la presentación con `ollama pull qwen2.5-coder:7b`.
3. En Terminal, desde `cierre-nexo`, ejecuta `./demo/iniciar.sh`. Verifica el SHA-256, prepara la imagen, aplica migraciones en la base sintética y genera las credenciales locales la primera vez.
4. Abre la dirección indicada. Entra como `demo-admin@example.test` con el valor de `DEMO_PASSWORD` de `demo/.demo.env`. Mantén ese archivo local. Para detener la demo y conservar los datos, ejecuta `./demo/servicios.sh detener`.

## Guion sugerido

| Escena | Entrada o acción | Resultado observable |
|---|---|---|
| Estado y modelo real | Abre NEXO y pulsa «Comprobar modelo». Envía «Multiplica 23 por 17 y explica el resultado». | Aparece Ollama conectado y se genera una respuesta con 391. |
| Límite compatible | «Responde con exactamente 2 palabras. Nombra un animal y un color.» | Si la generación cumple, se muestran dos palabras. Si agota el reintento, se indica el incumplimiento sin guardar el turno. |
| Contradicción | «Escribe al menos 3 líneas y exactamente 4 palabras y como máximo 1 línea. Saluda.» | El validador identifica el conflicto entre líneas y pide elegir. |
| Adjuntos | Adjunta un TXT UTF-8 con `Código de inventario: CIRUELA-761`. Pide extraer el código de inventario. | Se muestra el código literal y la etiqueta de extracción local. |
| Fuente verificable | Pide la arquitectura de ExamenErik. Abre el botón de la referencia. | Se muestra el extracto autorizado del backend modular. |
| Simulación | En el laboratorio selecciona el escenario de ascenso. Pulsa «Calcular simulación» y luego «Explicar con IA». | Se ve el cálculo hipotético y el alcance de los tokens. Ningún permiso se modifica. |
| Memoria y cancelación | Da un nombre ficticio al proyecto, recarga, abre el chat y pregunta el nombre. Pide luego una lista extensa y pulsa «Detener». | Recuperación del dato, confirmación de cancelación y ausencia de turno incompleto en el historial. |

## Recuperación durante la presentación

- **Ollama.** Abre o reinicia Ollama y comprueba `curl -fsS http://127.0.0.1:11434/api/tags`. Pulsa «Comprobar modelo» y reenvía el turno cuando vuelva a estar conectado. La documentación y el cálculo del laboratorio están disponibles con Java y MySQL activos.
- **Java.** Ejecuta `./demo/servicios.sh estado` y `./demo/servicios.sh registros`. Si MySQL está sano, ejecuta `./demo/servicios.sh reiniciar-java`. Consulta `curl -fsS http://localhost:8090/api/health`. Un turno interrumpido puede conservar una reserva hasta 20 segundos.
- **MySQL.** Consulta el estado y registros. Si el problema es un proceso detenido o temporal, ejecuta `./demo/iniciar.sh` o `./demo/servicios.sh reiniciar-mysql` y espera a que recupere salud. Reinicia Java si no recupera la conexión. Conserva el volumen y las credenciales. Si hay corrupción, trabaja con una copia y restaura en un esquema separado antes de reanudar.
- **Puerto ocupado.** Identifica qué servicio usa 8090 y ciérralo desde su propio lanzador si corresponde. El iniciador de la demo aborta cuando el puerto pertenece a otro proyecto.
