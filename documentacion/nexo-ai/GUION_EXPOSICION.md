# Guion de exposición universitaria — 18 a 22 minutos

## Preparación previa (fuera de la exposición)

Abrir Docker y Ollama. Ejecutar el lanzador integrado; comprobar `http://localhost:8090`. Tener la cuenta de demostración a mano sin proyectar su contraseña. Preparar las evidencias públicas y verificar que no se muestra `.local`, `.env`, cookies, cabeceras Authorization ni registros privados de RESTler. El guion usa exclusivamente cuentas sintéticas. No iniciar fuzzing contra una URL de Internet.

## 0:00–2:00 · El proyecto

“Este proyecto se llama ExamenErik. Es una aplicación de gestión de identidad: registra cuentas, autentica, permite recuperar contraseñas y administra roles. Usa frontend modular, Java 21, Spring Boot y MySQL. No es un sistema de ventas ni de predicción de automóviles.”

Mostrar pantalla inicial y menú de cuenta. Explicar que la autenticación ya existía y se reutilizó; NEXO no es otro sistema de login.

## 2:00–4:00 · Acceso e integración

Iniciar sesión sin exponer credenciales. Entrar a **NEXO-Δ AI** desde la navegación. Mostrar el nombre del modelo y su estado. Explicar:

“El navegador habla con el backend autenticado. Solo el backend puede invocar el modelo. Mi navegador no tiene claves de IA ni acceso directo a MySQL. Si Ollama no está disponible, se informa: no se sustituye por respuestas pregrabadas.”

Mostrar que la interfaz conserva la identidad visual del sitio y añade un espacio oscuro, historial y herramientas.

## 4:00–6:00 · Conversación real y memoria

Crear una conversación. Seleccionar “Conversación general”. Preguntar:

1. “La palabra de nuestro ejercicio es albahaca. Explica qué es una base de datos relacional en dos frases.”
2. “¿Qué palabra te dije en el mensaje anterior?”

Observar la respuesta progresiva. Explicar que el modelo recibe mensajes anteriores limitados; el historial completo se guarda de forma privada, pero el contexto del modelo no es ilimitado. No prometer que todas las respuestas serán idénticas a las capturas.

## 6:00–8:00 · Contexto y herramientas

Elegir automática: “Explica los roles y la autenticación de ExamenErik. Cita documentos autorizados.”

Mostrar herramienta seleccionada, respuesta y botones de referencias. Abrir “Identidad, roles y sesiones”. Señalar USER/ADMIN y los tres permisos iniciales.

“El modelo propone la herramienta, pero Java decide si puede ejecutarse. Los documentos tienen permisos. No indexamos todo el disco ni enviamos secretos al modelo. Este RAG es léxico sobre un corpus pequeño revisado; no necesita una base vectorial para funcionar.”

Opcional: adjuntar un archivo `.txt` con un error sintético, sin contraseñas, y elegir “Analizar error”. Explicar que interpreta el texto, no ejecuta código ni diagnostica un servidor que no observó.

## 8:00–12:00 · Predictive Access Lab

Escenario sintético → “Convertir USER en ADMIN” → minuto 5 → **Calcular simulación**.

“Los números vienen de reglas verificables. Este escenario obtiene 60/100: 15 por lectura administrativa, 35 por escritura y 10 por dos tokens sintéticos invalidados. La tarea solo requiere perfil propio, así que conservar USER satisface la necesidad con puntaje 0.”

Mostrar tabla antes/después y alternativa. Cambiar el minuto a 20: el access sintético ya venció antes del cambio; el puntaje es 55 porque solo el refresh conocido se invalida.

Cambiar a “Acceso de consulta”: “El catálogo inicial no tiene un rol de solo lectura administrativa. ADMIN concede escritura adicional. No inventamos un rol lector ni afirmamos que USER ya puede listar usuarios. Diseñar un nuevo rol sería un cambio futuro separado.”

Pulsar **Explicar con IA**. Contrastar la explicación con la tabla. “El modelo puede equivocarse; manda el cálculo. En pruebas detectamos una explicación que confundió la alternativa y conservamos esa evidencia.”

## 12:00–14:00 · Datos reales autorizados y no modificación

Cambiar a lectura autorizada. Dejar objetivo vacío para analizar la cuenta propia. En la cuenta ADMIN de prueba, simular USER con necesidad `profile:read` y minuto 0.

“El número real de JWT no puede contarse porque no se almacenan. Solo se conocen refresh vigentes. El puntaje real varía con las sesiones de prueba y se etiqueta como parcial; no debe memorizarse un número fijo. Si esta es la última cuenta escritora, la rúbrica añade la penalización correspondiente.”

Mostrar que la cuenta conserva sus roles al volver a “Mi cuenta”. Complementar con la evidencia automatizada que compara cuentas, roles, permisos y tokens completos antes/después. No usar solo una captura como prueba de no modificación.

“El rollback es una propuesta: restaurar permisos con revisión y volver a iniciar sesión. Nunca se restauran tokens consumidos ni se reduce la versión de sesión. NEXO no tiene funciones para aplicar este cambio.”

## 14:00–16:00 · Seguridad sin IA

Abrir el informe RESTler. Explicar:

“RESTler consume OpenAPI, infiere dependencias y genera secuencias de llamadas, como crear un recurso y consultarlo. Se ejecutó localmente con una base sintética, Mailpit y API propia. El modelo estaba deshabilitado durante fuzzing. No escaneamos sistemas externos.”

Mostrar identificador, commit, resumen de ejecución, cobertura y códigos HTTP de la campaña final. Leer exactamente los resultados documentados, incluyendo fallos/timeouts. “Cero buckets de errores no demuestra que la aplicación sea segura. Cobertura de operaciones no es porcentaje de requisitos validados. El análisis de resultados tiene limitaciones en este entorno y se declara incompleto.”

Distinguir tests Java con modelo simulado de la prueba Chrome con inferencia real. Mostrar la evidencia de sesión revocada durante una respuesta y la del intento de acceso a chat ajeno.

## 16:00–18:00 · Infraestructura distribuida

Mostrar el diagrama de arquitectura. Explicar Cloudflare frontend, Render API, Oracle MySQL 8.4, como **preparación aún no desplegada**. Mostrar `frontend/dist` y el Dockerfile de API sin recursos estáticos.

“La gratuidad tiene cuotas. Un subdominio gratuito requiere aprobación; los dominios independientes de proveedores pueden bloquear cookies. Necesitamos HTTPS, orígenes explícitos, cookies Secure y TLS con identidad verificada para MySQL. Render Free bloquea puertos SMTP comunes y no puede alojar este modelo 7B.”

“La IA local funciona con esta Mac. Para publicarla sin depender de mi equipo falta un host de inferencia privado con capacidad medida. No voy a presentar una simulación local como despliegue real.”

## 18:00–20:00 · Evidencia y límites

Exportar informe y recargar la página para mostrar persistencia y refresh. Mostrar historial privado, retención y borrado. Explicar que archivos adjuntos no se comparten con otros usuarios.

Cerrar con: “La contribución personalizada es una IA contextual con herramientas limitadas y un laboratorio contrafactual verificable. El modelo redacta; las reglas calculan; la autenticación autoriza. No certifica seguridad, no garantiza predicciones y no toma decisiones administrativas por nosotros.”

## Preguntas frecuentes

**¿Entrenaron su propia IA?** No. Integramos un modelo preentrenado local y desarrollamos la personalización, el control de herramientas, contexto, interfaz y laboratorio.

**¿Es imposible atacar el sistema?** No. Las pruebas aportan evidencia limitada. No hay certificación y se documentan pendientes.

**¿Por qué no es solamente un chatbot?** Consulta contexto permitido, selecciona herramientas acotadas, mantiene historial por usuario y puede invocar cálculos deterministas sobre un snapshot autorizado.

**¿Qué pasa con una instrucción que pide ejecutar SQL?** No existe herramienta que lo permita. Los nombres y argumentos se validan, independientemente de lo que diga el modelo.

**¿Cómo es gratis?** La inferencia actual usa hardware local sin tarifa de API. Los proveedores propuestos tienen cuotas y condiciones; la publicación aún requiere acceso, aprobación y verificación. No se prometen recursos ilimitados.
