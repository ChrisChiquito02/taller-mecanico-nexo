# Evidencia auténtica — 8 de noviembre de 2026

La evidencia local del 8/nov no reemplaza ni refecha el informe histórico del 4/nov. No se han desplegado servicios públicos. Las marcas UTC en los archivos corresponden a México (UTC−06:00) el mismo día.

| Verificación realizada | Resultado | Evidencia |
|---|---|---|
| Frontend: Node unitarias y esbuild | 17 pasaron; 0 fallos | [frontend.txt](evidencia/frontend.txt) |
| Backend con Java 21, MySQL Testcontainers, SMTP GreenMail, arquitectura | 41 ejecutadas; 0 fallos, errores u omisiones | [backend.txt](evidencia/backend.txt) |
| Navegador integrado, Chrome | 8 pasaron; registro/login/RBAC/SMTP/sesiones/interfaz | [e2e.txt](evidencia/e2e.txt) |
| NEXO: semántica, menor privilegio, caducidad y privacidad | 7 pasaron | [nexo-tests.txt](evidencia/nexo-tests.txt) |
| NEXO inferencia local real | Ollama qwen2.5-coder:7b; ai.status=executed | [nexo-ia.json](evidencia/nexo-ia.json) |
| NEXO primera salida revisada | Confusión detectada; conservada, no usada como resultado final | [primera inferencia](evidencia/nexo-ia-primera-revision.json) |
| Dos orígenes HTTPS del mismo sitio | Login/refresh tras recargar; cookie HttpOnly/Secure/Strict host-only | [JSON](evidencia/origenes-https.json), [captura](evidencia/origenes-https.png), [salida](evidencia/origenes-https.txt) |
| Imagen API sin frontend | Java 21, usuario app, cero entradas static, V1/V2 presentes | [empaquetado-api.json](evidencia/empaquetado-api.json) |
| API final con límite 512 MB y MySQL TLS | Arranque 200; MySQL 8.4.11, TLS_AES_128_GCM_SHA256; V1/V2 aplicadas; rechaza hostname/CA ajenos y conexión sin TLS | [mysql-tls.json](evidencia/mysql-tls.json) |
| RESTler último laboratorio | compile/test/fuzz código 0; sin bug buckets; analizador Fuzz incompleto | [resumen](evidencia/examenerik-fuzz-cea23c80/resumen.json), [salida test](evidencia/examenerik-fuzz-cea23c80/test.txt), [salida fuzz](evidencia/examenerik-fuzz-cea23c80/fuzz.txt) |
| Respuestas HTTP contadas desde logs | Estadística independiente; no son pruebas funcionales únicas | [conteo test](evidencia/examenerik-fuzz-cea23c80/Test-http-counts.json), [conteo fuzz](evidencia/examenerik-fuzz-cea23c80/Fuzz-http-counts.json) |
| Referencias de documentación y contratos | Validador local del repositorio | [documentacion.txt](evidencia/documentacion.txt) |

Los 41 tests backend incluyen comprobaciones de arquitectura y la integración; no se suman otra vez. Los números de peticiones RESTler no se agregan al total de tests. La verificación de dos orígenes es un script adicional con varias aserciones, no una suite inventada de cierto número de casos.

[Hashes de migraciones sin cambios](evidencia/migraciones.json) · [Cálculo de sitio para DNS anidado](evidencia/dns-sitio.json) · [Revisión de privacidad](evidencia/privacidad.json) · [Inventario de cambios](CAMBIOS.md).

[Hashes de migraciones sin cambios](evidencia/migraciones.json) · [Cálculo de sitio para DNS anidado](evidencia/dns-sitio.json) · [Revisión de privacidad](evidencia/privacidad.json) · [Inventario de cambios](CAMBIOS.md).

[Hashes de migraciones sin cambios](evidencia/migraciones.json) · [Cálculo de sitio para DNS anidado](evidencia/dns-sitio.json) · [Revisión de privacidad](evidencia/privacidad.json) · [Inventario de cambios](CAMBIOS.md).

[Hashes de migraciones sin cambios](evidencia/migraciones.json) · [Cálculo de sitio para DNS anidado](evidencia/dns-sitio.json) · [Revisión de privacidad](evidencia/privacidad.json) · [Inventario de cambios](CAMBIOS.md).

## Evidencia fallida conservada

El intento RESTler 376842b7 no pudo publicar el puerto de la red interna y limpió sus recursos. Se corrigió la preparación antes de repetir. El intento 51f477e5 ejecutó fuzzing pero su analizador falló; el último intento cea23c80 vuelve a indicar `analysis_complete=false`. No se oculta esa limitación por el exit code 0 del lanzador.

La primera prueba HTTPS falló por consultar cookies solo para la ruta / en lugar de /api/auth/refresh; el filtro de Playwright no devolvía esa cookie por su Path. Se corrigió la consulta de prueba, se repitió y pasó. [Registro del primer intento](evidencia/origenes-https-intento1.txt). No se debilitó la cookie ni el backend para superar la aserción.

La primera inferencia de IA confundió el estado original de USER con una retirada de ADMIN; se conservó, se aportaron roles antes/propuestos y se evaluó correctamente la opción de no hacer nada. La inferencia posterior respeta los datos principales. Ninguna salida del modelo modificó el IAM.

## Reproducir TLS local

```sh
docker build -f deploy/Dockerfile.api -t examenerik-actividad-api .
python3 seguridad/probar_tls.py
```

Genera CA/certificados temporales, MySQL y API propios, aplica Flyway sobre un esquema sintético, limita API a 512 MB, comprueba conexión VERIFY_IDENTITY y prueba tres rechazos. No envía correos; `smtp_tested=false`. La raíz / devuelve 404 en la API final porque no lleva frontend. Se limpian sus recursos al terminar. No demuestra firewall o DNS públicos ni rendimiento con 0.1 CPU de Render.

## Privacidad y procedencia

Los resúmenes públicos no contienen contraseñas, JWT, refresh, claves privadas ni enlaces de recuperación. Los logs HTTP completos y los diccionarios con credenciales temporales permanecen en `seguridad/private`, excluidos del paquete. Las capturas muestran una cuenta sintética del laboratorio.

`sha256.json` registra hashes de los archivos de entrega al cierre. Los hashes comprueban cambios posteriores, no constituyen firma externa ni certificación de seguridad. El commit de RESTler y el hash de su binario están en cada resumen. El paquete de cambios corresponde a las fuentes implementadas; las migraciones V1/V2 permanecen iguales.

## Pendientes reales

- Acceso confirmado a Render; pendiente confirmar espacio/repositorio. Acceso a Cloudflare/Oracle y capacidad Oracle y aceptación de sus condiciones sin convertir a pago.
- Aprobación del nombre gratuito y de sus subdominios anidados; HTTPS público válido.
- Publicación real, firewall remoto, comprobación TLS desde Render y captura de Flyway/versión en Oracle.
- Alta de correo gratuita autorizada, remitente verificado y entrega de recuperación desde Render por STARTTLS 2525.
- Pruebas sobre URLs reales con datos sintéticos; rendimiento y arranque en frío bajo las cuotas gratuitas.
- Repetición compatible de RESTler si se requiere análisis completo de respuestas. No existe evidencia de que esa repetición ya pasó.


## Ampliación: panel local NEXO-Δ

Se añadió un panel usable con doble clic y se probó desde Chrome. La suite actual de NEXO tiene **12 pruebas en total**: las 7 del motor que ya se reportaron y 5 nuevas del panel. No sumar 12 a las 7 anteriores como si fueran distintas. [Salida actual](evidencia/nexo-panel-tests.txt).

La ejecución final `nexo-panel-3ce1e9f9` comprobó los tres escenarios con inferencias reales de qwen2.5-coder:7b, resultados de riesgo 60/60/70 y alternativas 0/60/0, informe descargable idéntico al resultado, minuto 20 sin IA explícitamente declarado, pantalla móvil sin desbordamiento, ausencia de errores JavaScript y fixtures sin modificaciones. [Resumen](evidencia/nexo-panel-3ce1e9f9/resumen.json) · [Escritorio](evidencia/nexo-panel-3ce1e9f9/panel-escritorio.png) · [Móvil](evidencia/nexo-panel-3ce1e9f9/panel-movil.png).

Los intentos `nexo-panel-83be08cc` y `nexo-panel-a5525720` se conservaron durante la revisión del aviso de catálogo y de la fidelidad del texto generativo. La revisión detectó omisiones o confusión antes/después en algunas inferencias; el puntaje y las alternativas de la pantalla provienen siempre del motor. La última ejecución corresponde al hash de fuente anotado en su resumen. Los resultados de IA son recomendaciones para revisión, no decisiones automáticas.

El panel es local, acepta únicamente los tres escenarios sintéticos y comprueba Host, Origin y tamaño/campos de las solicitudes. No tiene credenciales ni herramientas para modificar el IAM. No se publicó en Internet ni se cambió el alojamiento del proyecto.
