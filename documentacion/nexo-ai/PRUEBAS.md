# Pruebas y evidencia auténtica

## Resultados

| Comprobación | Resultado | Alcance |
|---|---|---|
| Línea base Java | 41 aprobadas | Java 21, MySQL sintético, SMTP de pruebas |
| Línea base frontend | 17 aprobadas | Node, contratos y DOM |
| Línea base Python | 12 aprobadas | Motor y límites de la demo anterior |
| Java tras integración | 54 aprobadas, sin fallos/errores/omitidas | Incluye 13 verificaciones adicionales; las de conversación usan proveedor simulado |
| Frontend tras integración | 19 aprobadas | Incluye render inerte y stream incompleto sin reintento |
| Regresión Chrome IAM | 8 aprobadas | Registro, bloqueo, SMTP, roles, desactivación, refresh, rutas y móvil |
| Orígenes HTTPS distintos del mismo sitio | Aprobada | Login, cookie HttpOnly/Secure/Strict y refresh al recargar |
| Motor/panel Python conservado | 12 aprobadas | No sumar otra vez a las 12 de línea base |
| Chrome + Ollama real | Aprobada técnicamente | Tres turnos de chat más explicación real de simulación, memoria, RAG, exportación, historial, móvil |
| TLS MySQL + API-only | Aprobada | Java 21, contenedor de 512 MB, MySQL 8.4.11, Flyway 1/2/3, CA/hostname/plaintext negativos |
| RESTler ampliado | Ejecución completada, análisis parcial | Compile/Test/Fuzz 0; 19 rutas y 21 operaciones en el contrato |

No se suman suites repetidas ni se cuenta cada token del modelo como una prueba. La inferencia se ejecuta en la Mac, no dentro del contenedor Java de 512 MB. La comprobación de memoria del contenedor no es una prueba de carga ni de capacidad en Render.

## Archivos y comandos reproducibles

Desde la raíz del proyecto:

```sh
npm --prefix frontend run check
npm --prefix frontend run build:integrated
sistema-m01-m02/scripts/verify-java21.sh
PW_CHANNEL=chrome npm --prefix frontend run test:e2e
node frontend/tests/separate-origins.mjs
python3 -m unittest discover -s agente-nexo -v
python3 agente-nexo/launcher.py --no-open
node frontend/tests/nexo-live.mjs
python3 scripts/verificar-documentacion.py
```

La prueba HTTPS utiliza certificados efímeros autofirmados e `ignoreHTTPSErrors` **solo en ese navegador de prueba**; no representa certificados públicos válidos. El build de esa prueba contiene orígenes locales transitorios: ejecutar `npm --prefix frontend run build` antes de empaquetar o configurar el origen de publicación real.

Para TLS:

```sh
docker build -f deploy/Dockerfile.api -t examenerik-actividad-api .
python3 seguridad/probar_tls.py --output documentacion/nexo-ai/evidencia/mysql-tls.json
```

Para RESTler, compilar primero el commit fijado siguiendo `seguridad/instalar-restler.sh` (Python 3.12 recomendado por el proyecto; esta sesión usó el Python 3.14.7 disponible y .NET 8). Después:

```sh
python3 seguridad/ejecutar_restler.py \
  --restler-drop /RUTA/AL/restler-bin \
  --source-commit 6d984deedbc54aad957fa3da0c7e9e5df23a2aee \
  --minutes 1
```

La ruta es local a binarios compilados, no un destino de red. El script genera exclusivamente un entorno desechable propio. Debe haberse construido el JAR actual. Las credenciales efímeras y logs de red completos se guardan privados, no en la entrega.

## Verificaciones de seguridad añadidas

- JWT obligatorio y `profile:read`; chat ajeno responde 404 en lectura, escritura y borrado.
- ACL de documentación antes de recuperar y antes de abrir una referencia.
- Simulación de terceros o catálogo denegada a USER, antes de leer el snapshot.
- Comparación exacta de cuentas, roles, permisos y tokens antes/después; no solo una captura.
- Snapshot parcial no afirma desaparición del último administrador. JWT reales desconocidos se declaran como tales.
- Roles idénticos revocan; tokens vencidos no suman disrupción; pérdida del último escritor suma penalización; no se inventa rol lector.
- Secretos comunes, archivos no permitidos, herramienta arbitraria, origen ajeno y cuerpo excesivo rechazados.
- Rate limit antes de invocar al modelo, retención vencida e historial ligado al alcance de permisos.
- Revocación durante generación detiene persistencia del turno.
- Proveedor simulado con streaming HTTP real comprueba protocolo; no se presenta como inferencia real.
- Redirecciones y destinos externos de modelo rechazados. Fallo del modelo no crea respuesta falsa.
- HTML generado y bloques de código se tratan como texto inerte.

## RESTler: resultados y limitaciones

Primera campaña de esta evolución: `examenerik-fuzz-d536d808`. Compiló, el motor Test produjo un resumen parcial (15/21), pero el proceso no terminó dentro de 480 segundos. No se ejecutó Fuzz en ese intento. El error y limpieza quedan registrados; no se cuenta como campaña satisfactoria.

Campaña completada: **`examenerik-fuzz-bb7c7b03`**. Se acotó Test a un minuto, `max_combinations=1`, y se deshabilitó explícitamente ResultsAnalyzer con la opción soportada `--no_results_analyzer`. El script tiene plazo por fase y termina su grupo de procesos si vence. Compile, Test y Fuzz devolvieron 0. El analizador auxiliar no se ejecutó; `analysis_complete=false` es intencional.

- Test: cobertura informada 12/21; 18/21 solicitudes renderizadas.
- Fuzz: cobertura informada 11/21; 17/21 renderizadas; un objeto dinámico creado según el motor.
- No se registraron bug buckets en esos resúmenes.
- Conteo independiente Fuzz: 70 respuestas 200, 1224 respuestas 403, 2019 respuestas 400, 119 respuestas 202, 122 respuestas 401, 5 respuestas 204, 2 respuestas 404 y 12 respuestas 429. No aparecen 5xx en ese conteo.
- 5608 líneas de envío; los conteos de envío/respuesta y resumen del motor no coinciden porque incluyen checkers, renderizados y registros parciales. No son 5608 pruebas funcionales aprobadas.
- Los límites de solicitudes, las dependencias incompletas y el tiempo corto reducen cobertura. El contrato tiene 21 operaciones, no 21 flujos funcionales demostrados.
- El modelo está deshabilitado durante fuzzing. No se evalúa robustez semántica de IA ni coste bajo prompts adversarios con RESTler.
- Los endpoints cookie refresh/logout tienen pruebas específicas, porque no se encadena automáticamente toda la rotación de cookies con RESTler.

El informe completo público está en `documentacion/actividad/evidencia/examenerik-fuzz-bb7c7b03`. Se recomienda repetir con el Python recomendado, ampliar presupuesto/secuencias y completar el análisis auxiliar antes de utilizar estos resultados como parte de una evaluación formal.

## Intentos fallidos y calidad del modelo

Se conservan: prueba antigua que esperaba dos migraciones; rechazo de herramienta devuelto inicialmente como evento en stream (corregido para denegarlo antes de iniciar respuesta); reinicio con bootstrap activo (corregido sin cambiar la regla IAM); colisión de lanzador; selector HTML defectuoso; campañas y respuestas de IA anteriores.

Las comprobaciones Chrome etiquetadas `passed` verifican integración técnica, memoria del ejercicio, fuentes, persistencia, exportación y no modificación IAM. **No validan automáticamente la veracidad de todo el texto del modelo.** La revisión manual detectó errores narrativos sobre alternativas y puntajes que se documentan en MOTOR_IA.md. Las explicaciones se marcan orientativas y se mantienen separadas de datos deterministas.

Los registros `backend-entrega.txt`, `frontend-definitivo.txt`, `live-entrega.txt`, `regresion-e2e.txt`, `origenes-https.txt`, `mysql-tls.json` y los directorios `live-*` permiten revisar resultados. Las fechas UTC pueden corresponder al 9 de noviembre mientras localmente aún es el 8.

## Revisión visual final

La inspección manual detectó recorte interno en móvil que no aparecía en `document.scrollWidth`: el grid medía 696 px dentro de un panel de 356 px. Se corrigió el mínimo de columnas y del sidebar. `frontend/tests/nexo-visual.mjs` comprueba ahora el ancho interno del layout y captura viewport reales. Resultado final: viewport 390 px, documento 390 px, layout 356 px sin desbordamiento interno; el historial tiene scroll horizontal intencional. Las capturas móviles finales válidas están en `evidencia/visual-viewport/`. Las antiguas capturas fullPage repetían franjas y se conservan como evidencia del intento, no como presentación final.

El lanzador pasó inicio idempotente sin contenedores duplicados, parada controlada y reinicio con volumen persistente; otros servicios se conservaron. Evidencia: `evidencia/launcher-validation.json`.
