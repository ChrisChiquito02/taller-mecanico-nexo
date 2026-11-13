# Evidencias técnicas de la ejecución

## Entorno y reproducibilidad

- Directorio observado en la Mac `/Users/andrickeliasleonchavez/Downloads/ExamenErik`. Existe `orquestador.md`. La copia carece de metadatos Git en la raíz y en el backend.
- Al comienzo, 8090 no escuchaba y Docker Desktop estaba cerrado. Se abrió Docker Desktop y se usaron las imágenes ya instaladas de MySQL 8.4 y Mailpit. Docker inició también contenedores preexistentes según su configuración. Esos contenedores y sus datos quedaron fuera de las pruebas.
- Se crearon únicamente cuentas de ejemplo en `nexo_audit`. Los contenedores propios se llaman `examenerik-audit-mysql` y `examenerik-audit-mail`. Los puertos 8090, 43306, 41025, 48025 y 41434 se vincularon a loopback. Se usó perfil local, límites temporales de 60 solicitudes NEXO y 120 IAM por minuto y una clave JWT exclusivamente sintética. Véase [arranque](evidencias/entorno.sh).
- Se ensayó primero el JAR disponible y luego se repitió con un JAR compilado fuera del árbol de fuentes, con Maven local en modo offline y JDK 26, objetivo Java 21. La comparación detectó siete clases Java distintas byte a byte y recursos estáticos, migraciones y bibliotecas idénticos. El resumen usa el JAR recién compilado. La compilación independiente del frontend en memoria coincide con HTML, CSS y JS integrados. [Compilación](evidencias/build-fuente.log) · [Comparación de JAR](evidencias/comparacion-artefactos.txt) · [Comparación frontend](evidencias/frontend-comparacion.txt).
- El alias de Docker `host.docker.internal:11434` respondió desde un contenedor desechable. El recorrido principal de auditoría ejecutó Java en el host con MySQL en Docker. El lanzador original se revisó por código y quedó pendiente de una ejecución integral sobre un volumen nuevo.
- Node, Maven, Chrome, Docker, Ollama y sus dependencias ya estaban disponibles. La auditoría no modificó código fuente, datos reales ni permisos reales. No se leyeron secretos de `.env.local` ni artefactos privados previos.

## Traza de inferencia

1. Chrome abrió `http://localhost:8090/#nexo` y autenticó una cuenta sintética.
2. La UI envió un turno autenticado a `POST /api/nexo/conversations/{id}/messages`.
3. Spring envió dos solicitudes a `/api/chat` de Ollama en el modo automático, una para seleccionar herramienta y una para generar. El relé local retransmitió las respuestas de Ollama, con registro de roles, tiempo, tokens, finalización y SHA-256. El relé no generó respuestas.
4. El navegador recibió NDJSON con `status → context → delta → done` y se comparó la concatenación de deltas con Ollama y el DOM. Las tres respuestas observadas coincidieron. El turno automático de documentación generó 451 tokens y se cerró por `stop`. La explicación de simulación llegó a 700 tokens con `length` y se marcó completa.
5. Para probar caída se interrumpió únicamente el relé de auditoría. Ollama compartido siguió activo. El estado indicó desconectado y la UI mostró `MODEL_UNAVAILABLE` sin emitir deltas ni un sustituto. La API transmite el error como evento dentro de una respuesta HTTP 200 ya iniciada.

[Trazas](evidencias/ollama.log) · [Comparación de huellas](evidencias/traza-verificada.txt) · [Modelos](evidencias/modelos.json) · [Navegador](evidencias/navegador.json) · [Captura móvil](evidencias/nexo-movil.png) · [Captura de fallo](evidencias/nexo-modelo-no-disponible.png).

## Persistencia y seguridad

- Flyway aplicó V1–V3 sobre MySQL 8.4. El historial del chat medido devolvió 34 mensajes antes y después de reiniciar MySQL y Spring, con SHA-256 idéntico. Chrome recuperó un turno anterior. El dato ZAFIRO-81 fuera de la ventana reciente quedó en el historial, pero el modelo respondió «ExamenErik» al pedirlo de nuevo.
- Registro sintético, sesión, expiración de acceso configurada, logout y recuperación vía Mailpit tuvieron pruebas locales. La entrega SMTP se observó en la bandeja de Mailpit. La suite del backend cubre además flujos de IAM y MySQL.
- Chrome registró 401 para sesión ausente, 409 tras cancelación y una solicitud abortada por el cliente. Los eventos detallados se conservan en JSON. No se ensayó desconexión del servicio Ollama compartido.
- La primera preparación del JAR previo incluyó tres oráculos incorrectos para ACL. Dos solicitudes omitían `minute` y una usó un documento público. Los ensayos corregidos con `minute:0` y el documento `operacion` confirmaron 403, 403 y 404. Las salidas originales se conservaron en `jar-previo`.

[MySQL](evidencias/mysql-fuente.log) · [API](evidencias/api-extra.jsonl) · [Controles](evidencias/controles.jsonl) · [Navegador tras reinicio](evidencias/navegador-reinicio.json) · [Navegador sin proveedor](evidencias/navegador-offline.json).

## Pruebas existentes

Se ejecutaron 54 casos Java, 19 unitarios de frontend y 12 del laboratorio Python legado, todos aprobados. Los casos Java incluyen integración real con MySQL mediante Testcontainers y pruebas de proveedor con servidor controlado. Los resultados con dobles sirven para comprobar contratos, no se contaron como inferencias. La suite E2E original escribe capturas en documentación del proyecto. Para conservar esa evidencia histórica se usaron guiones Chrome independientes dentro de la carpeta de auditoría. [Java](evidencias/unit-backend.log) · [Frontend](evidencias/unit-frontend.log) · [Legado](evidencias/unit-legado.log).

## Cierre del entorno

Se detuvieron Spring, el relé y los dos contenedores de auditoría. Se conservaron los contenedores y sus datos sintéticos, los archivos de prueba y el JAR temporal. Docker Desktop quedó abierto. Ollama y los contenedores preexistentes quedaron fuera del cierre. Una consulta SQL posterior al reinicio tampoco encontró mensajes para el turno abortado, aunque se mantiene el hallazgo de liberación tardía. [Consulta](evidencias/cancelacion-sql.txt) · [Cierre](evidencias/cierre.log).
