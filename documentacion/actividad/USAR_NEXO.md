> Actualización: el asistente conversacional integrado está documentado en [NEXO-Δ AI](../nexo-ai/README.md). Este documento conserva el alcance de la fase anterior.

# Usar NEXO-Δ antes de publicar ExamenErik

NEXO-Δ ya funciona como un asesor local de simulación. Su pantalla muestra escenarios sintéticos, puntajes calculados, impacto en tokens, alternativas, propuesta de vuelta atrás y una explicación generada por el modelo local. No consulta ni modifica la base de datos real ni se publica como parte del frontend IAM.

## Abrir con doble clic en Mac

1. Abre Ollama. El modelo `qwen2.5-coder:7b` ya estaba instalado en este equipo y se usó en las demostraciones.
2. En la carpeta `agente-nexo`, abre **Abrir NEXO.command** con doble clic.
3. Se abre una ventana de Terminal y el navegador con una dirección local. Mantén esa Terminal abierta mientras usas el panel; Ctrl+C lo cierra.

Si prefieres iniciarlo manualmente desde la raíz del proyecto:

```sh
python3 agente-nexo/servidor.py --open
```

El puerto se elige automáticamente para evitar conflictos. Usa la dirección `http://127.0.0.1:...` que muestra Terminal. El panel escucha exclusivamente en tu computadora; no está diseñado para publicarlo ni exponerlo en la red.

## Demostración para el aula

1. Elige **Un usuario se vuelve administrador**. Conserva el minuto 5 y la explicación con IA activada. Pulsa Analizar escenario.
2. Verás 60/100, un usuario afectado y dos tokens invalidados. La alternativa es conservar USER sin ejecutar una reasignación: riesgo 0/100.
3. Elige **Solo necesita consultar usuarios**. El catálogo actual solo cubre users:read mediante ADMIN, que también concede escritura. La alternativa calculada tiene el mismo riesgo 60/100. Un rol de consulta queda como propuesta futura; conservar USER deja pendiente la tarea solicitada.
4. Elige **Un permiso compartido cambia**. Se simula que USER gana users:write: dos usuarios afectados, riesgo 70/100 y sus tokens siguen vigentes. Es una hipótesis de catálogo, no un endpoint implementado en el IAM.
5. Regresa al primer caso y cambia el momento a 20 minutos. El token de acceso ya habría expirado antes del cambio, por lo que solo se atribuye al cambio la invalidación de un refresh: riesgo 55/100.
6. Puedes desactivar Explicación con IA local para mostrar únicamente los cálculos. La pantalla lo identifica como Motor de simulación y no afirma haber ejecutado IA.
7. Pulsa **Descargar informe JSON** para guardar el resultado que estás viendo. Incluye el hash del escenario y, si hubo IA, modelo y fecha de la inferencia.

## Qué decir al exponer

“NEXO-Δ anticipa las consecuencias de un cambio de autorización. El código calcula permisos, tokens y riesgo; Qwen2.5-Coder explica el resultado mediante Ollama. Comparamos opciones y dejamos la decisión a revisión humana. Aquí trabajamos con datos sintéticos y no ejecutamos cambios reales”.

La puntuación no es una probabilidad de ataque. El texto de IA requiere revisión y está separado de los números. Restaurar roles no revive tokens revocados; cuando una operación real invalida sesiones, se necesita iniciar sesión de nuevo.

## Si algo falla

- Si no hay conexión con la IA, abre Ollama y comprueba que el modelo configurado está disponible. El panel no descarga modelos automáticamente y no reemplaza un fallo por una respuesta de IA prefabricada.
- Solo se permite un análisis a la vez; una segunda solicitud recibe un aviso para esperar.
- Los campos se limitan a los tres escenarios y a un minuto entero entre 0 y 10080. No acepta contraseñas, rutas de archivos, usuarios reales ni texto libre para enviar al modelo.
- Si cierras Terminal, vuelve a abrir el acceso directo: puede mostrar un puerto diferente.

## Verificar el agente y el panel

```sh
python3 -m unittest discover -s agente-nexo -v
```

La suite actual tiene 12 pruebas: las 7 del motor y 5 del panel. Estas últimas comprueban rechazo de origen/Host ajenos, entradas inesperadas y demasiado grandes, preservación del fixture, fallo de IA explícito y exclusión de inferencias simultáneas. Se ejecutaron además tres inferencias reales desde Chrome y comprobaciones de descarga y pantalla móvil; no se confunden esas demostraciones con pruebas unitarias.
