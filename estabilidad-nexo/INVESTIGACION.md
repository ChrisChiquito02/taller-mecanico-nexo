# Investigación de estabilidad de ExamenErik

12 de noviembre de 2026. Ensayos sobre cuentas y MySQL de auditoría. El paquete académico previo se conservó byte a byte y el candidato se ejecutó desde copias privadas por réplica.

## Incidente de creación de chats

**Causa histórica no demostrada.** La hipótesis principal es que se reemplazó el JAR que una JVM seguía usando. Hay evidencia cronológica, de un fallo de la misma clase y del punto exacto donde se rompe el experimento, pero no se guardó la excepción anidada de los 31 errores originales.

| Hora local | Observación |
|---|---|
| 18:55:22–18:55:26 | La réplica 8091 arrancó desde `sistema-m01-m02/target/backend-0.1.0-SNAPSHOT.jar`. |
| 18:57:31 | Maven reempaquetó en ese mismo nombre de archivo. |
| 18:57:38–18:57:39 | La réplica registró 31 `ServletException`. El cliente vio 31 respuestas 500 al alternar altas con 8090. |
| 18:58 | Arrancó otra JVM desde el paquete reconstruido. El problema dejó de observarse. |

[Arranque y 500 históricos](../cierre-nexo/evidencias/replica.log) · [Reempaquetado histórico](../cierre-nexo/evidencias/paquete.log) · [Carga histórica](../cierre-nexo/evidencias/carga-incidencia.json).

**Experimento sobre artefacto desechable.** Con rutas precalentadas, reemplazar el archivo por una compilación distinta no alteró 12 comprobaciones HTTP, todas 200. Antes del primer acceso, el mismo cambio produjo reinicio de conexión y JFR registró un encabezado ZIP ausente y `NoClassDefFoundError` de Tomcat. En otra réplica, después de precalentar solo GET y antes de la primera alta, JFR capturó `NoClassDefFoundError` en `JsonBodyLimitFilter$1` y la `ServletException` que lo envolvió. La escritura del error también encontró una clase de Logback ausente. El efecto externo del experimento incluyó solicitudes interrumpidas o sin respuesta dentro del plazo. No se acreditó una repetición exacta de los 31 HTTP 500. [GET y POST precalentados](evidencias/jar-despues.json) · [JFR frío](evidencias/frio-excepciones.txt) · [JFR después de GET](evidencias/templada-parcial.txt).

**Transacciones y dependencias.** El filtro JSON precede al controlador, a la cuota y a `ConversationStore.create`. La cuota se modifica en una transacción sobre su fila por cuenta. La creación bloquea la fila IAM del propietario antes de contar e insertar. Ollama queda fuera de la ruta de alta. La réplica original completó el arranque antes del incidente. En esta ejecución, el muestreo de MySQL registró cero abortos de conexión, cero esperas de bloqueo de fila, cero deadlocks y un máximo de nueve conexiones del servidor. El pool está configurado con máximo 10 por JVM y dos conexiones ociosas mínimas. Estos datos no corresponden al momento de los 500 históricos. En dos JVM con copias privadas, las 62 altas alternadas produjeron 30 éxitos, 30 límites de chats y dos límites de solicitudes. [MySQL](evidencias/mysql-en-carga.tsv) · [Altas alternadas](evidencias/cuota.json).

**Medida aplicada.** El lanzador de auditoría `bin/ejecutar-replica.sh` copia cada JAR a una ruta privada por proceso, valida el ZIP, registra SHA-256 y abre la copia de solo lectura. Compilar o copiar sobre la ruta fuente no cambia esa copia. La demostración académica ya usa un JAR empaquetado en la imagen Docker. No se cambiaron transacciones, dependencias, pool ni migraciones.

## Confiabilidad de respuestas restringidas

De los ocho rechazos anteriores, cinco eran solicitudes válidas que el modelo no consiguió cumplir. Tres pedían saludar con dos palabras y dos pedían nombrar dos planetas en dos líneas y hasta cuatro palabras. Los otros tres solicitaban explicar cómo preparar té con cero palabras, un límite incompatible con una explicación textual.

Se incorporaron límites normalizados e instrucciones de presentación antes de la primera inferencia. El único reintento muestra al modelo el conteo previo sin inyectar el texto fallido. El error final identifica los límites incumplidos y explica cómo reformular. Los intentos inválidos siguen sin emitirse como deltas ni guardarse.

| Muestra real comparable | Anterior | Candidato |
|---|---:|---:|
| Solicitudes compatibles con salida válida | 7/12 | **9/12** |
| Conflictos explícitos aclarados | 3/3 | 3/3 |
| Solicitudes de explicación con cero palabras rechazadas | 3/3 | 3/3 |
| Salidas inválidas publicadas o guardadas | 0 | 0 |

La mejora de esta muestra se concentra en la solicitud de dos líneas y hasta cuatro palabras, que pasó de 1/3 a 3/3. El saludo con exactamente dos palabras sigue en 0/3. Los conteos verifican formato. La calidad semántica depende del modelo. [Antes](../cierre-nexo/evidencias/restricciones.json) · [Después](bin/restricciones.json).

## Regresión

- Java **63/63**, frontend unitario **20/20**, legado Python **12/12** y Chrome **18/18**. Total técnico **113/113**. [Java](evidencias/java.log) · [Frontend](evidencias/frontend.log) · [Legado](evidencias/legado.log) · [Chrome](evidencias/browser/navegador.json).
- Batería con oráculo original **46/47** frente a 47/47. El caso 17 pidió más detalles ante una instrucción ambigua, pero no nombró el objeto o elemento como exigía el oráculo. Se conserva como fallo. La corrección de restricciones se aplica a otro recorrido y el ensayo no establece la causa de la variación. [Batería](bin/bateria.jsonl).
- La huella del texto de una respuesta real de Ollama coincidió con el texto visible en Chrome. Las pruebas de adjuntos, autorización y revocación conservaron sus resultados. [Huellas](evidencias/huellas.txt) · [Controles](bin/controles.jsonl).

Spring Boot documenta que su cargador ubica entradas de JAR anidados mediante desplazamientos físicos en el archivo exterior. Esto explica por qué el resultado depende de qué clases ya se cargaron y de cómo se reemplazó el archivo. [Spring Boot NestedJarFile](https://docs.spring.io/spring-boot/specification/executable-jar/jarfile-class.html).

## Recomendación de liberación

Conservar el paquete académico probado como versión de referencia y mantener el nuevo JAR como candidato de evaluación local. El resultado del oráculo de comportamiento bajó un caso y el historial del incidente no permite cerrar su causa. Los despliegues deben ejecutar un archivo inmutable por proceso o una imagen por versión. Para una nueva ocurrencia de 500, capturar clases y marcos de pila del error anidado, hora, réplica, huella del artefacto y estado de pool, sin incluir cuerpos ni credenciales. Sigue pendiente la validación de infraestructura final y capacidad de producción.

### Reproducción operativa

Los guiones de `bin/` usan exclusivamente los contenedores sintéticos `examenerik-audit-*` y puertos locales 8090/8091. Se requiere el modelo local `qwen2.5-coder:7b`, el relé de trazas en 41434 y el JAR candidato. Inicia una copia por puerto con `bin/ejecutar-replica.sh`. Los guiones de altas, resistencia y cancelaciones ejecutan operaciones autenticadas reales. Las trazas del fallo por sobrescritura se obtuvieron en una tercera instancia descartable, fuera de ese recorrido. El ensayo frío y el posterior a GET no deben repetirse sobre archivos que estén sirviendo una demostración o un entorno compartido.

## Carga mixta y recuperación

Se repitió la prueba con seis cuentas, dos réplicas y 180 segundos de emisión, 187,42 segundos hasta que terminó el último turno. El primer intento con un cliente configurado a 50 s abortó con `TimeoutError` y carece de agregado completo. La repetición ajustó el plazo del cliente a 215 s para observar el resultado del backend. El cambio fue solo del guion de prueba.

| Tipo | Enviadas | Completas | Truncadas | Capacidad 429 | Mediana de completas | p95 de completas |
|---|---:|---:|---:|---:|---:|---:|
| Cortas | 242 | 51 | 0 | 191 | 0,259 s | 26,739 s |
| Largas | 58 | 2 | 10 | 46 | 34,869 s | 53,386 s |

El p95 largo se calcula con dos respuestas completas y tiene poca representatividad. No se observaron 500 en la repetición. Las seis cuentas recuperaron 106 mensajes, exactamente dos por cada uno de los 53 turnos completos. Las dos réplicas tuvieron 27 y 26 respuestas completas. El 79 % de las solicitudes recibió rechazo de capacidad. Se observó saturación al compartir Ollama entre las dos JVM. La prueba anterior con 148/207 completos usó turnos cortos y una duración diferente, por lo que las tasas no son directamente comparables. [Repetición](evidencias/resistencia.json) · [Tiempo de espera inicial](evidencias/resistencia-primer-intento.log) · [Prueba previa](../cierre-nexo/evidencias/carga.json).

RSS muestreado de Java entre 126 y 441 MiB en 8090 y entre 135 y 255 MiB en 8091. MySQL se mantuvo alrededor de 463 MiB. Ollama informó 5,15 GB de residencia del modelo en el momento observado. El muestreo breve no permite descartar fugas. No se midió energía. [Muestras](evidencias/muestras.json) · [Residencia](evidencias/ollama-residencia.json).

Tras reiniciar el contenedor MySQL sintético, ambos servicios volvieron a devolver 200. Coincidió la huella del historial y se recuperó el dato antiguo en una sesión nueva. [Recuperación MySQL](evidencias/reinicio-mysql.log) · [Memoria](evidencias/memoria.log).

Dos cancelaciones simultáneas cruzaron de réplica en 0,366 y 0,373 s. Ambas devolvieron 204 y dejaron cero mensajes. Al interrumpir el relé de prueba, se obtuvo `MODEL_UNAVAILABLE` sin guardar el turno. Al restablecerlo, Ollama volvió a calcular 391. La aplicación Ollama compartida permaneció disponible. [Cancelaciones](evidencias/cancelaciones.json) · [Recuperación del proveedor](evidencias/ollama-recuperacion.json).
