# RESTler: fuzzing de API con estado, sin IA generativa

## Elección y funcionamiento

RESTler es una herramienta de Microsoft Research, menos habitual en trabajos de aula que un escáner web general. Que sea poco común entre compañeros debe comprobarlo el equipo; no hay una estadística de uso de la clase. Analiza OpenAPI, compila una gramática de peticiones, combina valores/secuencias y usa respuestas para explorar operaciones posteriores. No llama a un LLM ni requiere una API de IA. En esta ejecución no se usó componente generativo para encontrar errores.

Se distingue de NEXO-Δ: RESTler envía peticiones reales a una aplicación **sintética de pruebas**; NEXO únicamente simula escenarios de autorización y usa IA local para explicar resultados. RESTler no decide ni ejecuta cambios sobre el proyecto publicado.

## Entorno y preparación auténticos

Se inició Docker Desktop (el ejecutable existía, pero el servicio estaba detenido). Se compiló RESTler localmente con .NET SDK 8.0.421 y Python 3.14.7, commit `6d984deedbc54aad957fa3da0c7e9e5df23a2aee`; el binario se identifica por SHA-256 en los resúmenes. El README de RESTler recomienda Python 3.12.8 y .NET 8; macOS tiene soporte experimental. **La ejecución local utilizó un Python más reciente que el recomendado**: no se presenta como una plataforma certificada. Para reproducir con máxima compatibilidad, usar 3.12.8 y .NET 8 en un directorio/entorno separado.

El laboratorio `seguridad/ejecutar_restler.py` crea red Docker interna para MySQL y Mailpit, entrada separada para exponer solo la API en un puerto aleatorio 127.0.0.1, MySQL 8.4 en tmpfs, esquema identity_synthetic, claves y contraseñas efímeras. El backend necesita una segunda red para que Docker publique el puerto loopback; no se afirma aislamiento total de salida de ese contenedor. No se usa `.env.local`, ni puertos de una base de trabajo ni la API anterior en 8090.

Se conserva la configuración defensiva de producción en el código. Solo este laboratorio usa perfil local, HTTP dentro de la red sintética, SMTP Mailpit y límites de intentos elevados. Ningún correo sale del laboratorio. Al terminar, elimina únicamente sus contenedores/redes/imagen temporales, sin borrar contenedores ajenos o volúmenes de desarrollo.

## Derivación desde OpenAPI

Origen: `docs/integracion/openapi.json`, OpenAPI 3.1, 12 rutas/12 operaciones. Se genera una copia 3.0.3 compatible con el compilador, se reemplaza servidor por la API loopback y se añade una restricción explícita de orden **register → login**. No se cambió el contrato fuente ni la API para que una prueba pase.

El diccionario aporta un correo sintético para ese flujo y una cuenta destino precreada prescindible para las mutaciones administrativas. JWT ADMIN sintético se carga mediante el mecanismo Location de RESTler; no se copia a la gramática ni se publica. Los parámetros no fijados reciben valores inválidos, límites y cadenas de fuzzing. La gramática contiene una variable de orden productor/consumidor para register/login; no se debe afirmar que se infirió una cadena de creación/borrado de recursos que el contrato IAM no devuelve. Registro retorna 202 genérico, no ID.

Refresh/logout no se enlazan automáticamente con la cookie del login en la gramática. La renovación autenticada con cookie se probó en preflight y en navegador aparte. Reset con token de correo y cambio de contraseña correcto tampoco quedan cubiertos funcionalmente por todos los recorridos de RESTler. Sus casos correctos se prueban en integración y navegador. No modificar el sistema para devolver IDs/tokens extra solo para ayudar al fuzzer.

## Ejecución reproducible

1. Compilar el commit indicado de RESTler según su README en una carpeta de trabajo, instalar Python 3.12.8 y .NET 8 para una repetición compatible. El script opcional `seguridad/instalar-restler.sh` fija el commit y crea un entorno Python 3.12; no se afirma que ese instalador opcional se ejecutó en esta sesión.
2. Construir JAR Java 21 integrado con el verificador del proyecto. Docker debe estar activo.
3. Ejecutar desde la raíz de ExamenErik, con ruta absoluta al drop local:

```sh
python3 seguridad/ejecutar_restler.py \
  --restler-drop /RUTA/PRIVADA/restler-bin \
  --source-commit 6d984deedbc54aad957fa3da0c7e9e5df23a2aee \
  --minutes 1
```

El ejecutor no acepta una URL arbitraria ni contraseñas externas. Ejecuta `compile`, `test` y `fuzz` con presupuesto de 1 minuto para la fase fuzz; limita longitud de secuencia a 3. El tiempo total incluye preparación, test y análisis y es mayor que un minuto. Salida pública: `documentacion/actividad/evidencia/examenerik-fuzz-<id>`. Intercambios completos/diccionarios/env/token: `seguridad/private`, excluido de publicación.

## Resultados y límites

Se conservan tres intentos:

- `376842b7`: preparación fallida porque la primera red interna impedía publicar la API; se limpió el entorno y se corrigió con una red de entrada independiente. No fue una campaña ejecutada.
- `51f477e5`: compile/test/fuzz ejecutados, motor reportó 12/12 y cero bug buckets. El analizador de la fase Fuzz falló con respuestas de peticiones malformadas. Se preserva como primer resultado, no como análisis completo.
- `cea23c80`: se separó el usuario del flujo registro/login del destino de administración y se redujeron combinaciones. Compile/test/fuzz finalizaron con código 0. El analizador Fuzz volvió a fallar, registrado explícitamente en `analysis_complete: false`. No se ocultó el error por el código 0 del lanzador.

En el último intento, el conteo independiente de líneas HTTP recibidas registra 200=713, 202=986, 204=615, 400=4831, 401=550, 403=2160 y 404=3; no hay líneas 5xx detectadas por ese contador. Incluye reejecuciones y checkers, no son casos de prueba únicos. Hay 14,737 líneas de envío: su diferencia con respuestas contadas no debe presentarse como éxito ni ignorarse; los logs incluyen peticiones malformadas, respuestas no parseables y errores de transporte.

**12/12 es la cobertura declarada por RESTler, no demuestra 12 flujos de negocio completos exitosos.** El recuento por operación y por estado HTTP se conserva adicionalmente; hay recorridos negativos y campos no enlazados. `bug_buckets: {}` significa que no se registraron hallazgos con esa configuración/duración. No demuestra ausencia de vulnerabilidades ni un análisis de respuestas completo. No hay un bug confirmado que pueda inventarse para la exposición.

Preflight real del laboratorio: perfil ADMIN 200; refresh sin Origin 403; Origin ajeno 403; refresh con Origin autorizado y cookie válida 200; OPTIONS autorizado 200. Las pruebas de backend/navegador complementan estos resultados. No se ejecutó el fuzzer contra proveedores o datos públicos.

El analizador registró `Unexpected response without prior request` al procesar intercambios malformados. Es una limitación de esta ejecución de herramienta, no evidencia por sí sola de fallo del backend. Para investigación futura: repetir en Linux/Python 3.12.8, reducir a fuzz-lean, acotar checkers y analizar replay. No declarar un resultado corregido sin ejecutar esa repetición.

Fuentes primarias: [repositorio/instalación](https://github.com/microsoft/restler-fuzzer), [Quick Start](https://github.com/microsoft/restler-fuzzer/blob/main/docs/user-guide/QuickStart.md), [fuzzing y checkers](https://github.com/microsoft/restler-fuzzer/blob/main/docs/user-guide/Fuzzing.md), [anotaciones de dependencias](https://github.com/microsoft/restler-fuzzer/blob/main/docs/user-guide/Annotations.md), [autenticación](https://github.com/microsoft/restler-fuzzer/blob/main/docs/user-guide/Authentication.md).
