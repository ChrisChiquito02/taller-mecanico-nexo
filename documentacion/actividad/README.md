> Actualización: el asistente conversacional integrado está documentado en [NEXO-Δ AI](../nexo-ai/README.md). Este documento conserva el alcance de la fase anterior.

# Actividad universitaria — ExamenErik

Fecha: 8 de noviembre de 2026, México. Proyecto correcto: **ExamenErik**. Alcance: identidad, autenticación, autorización y sesiones. No contiene ventas, pagos ni predicción de vehículos.

**Se implementó y probó localmente. No se ha publicado en Internet.** Las URLs de los ejemplos son marcadores; no son servicios desplegados. El presupuesto solicitado es cero. No se creó ninguna suscripción, dominio ni cuenta de pago.

| Entrega | Estado y acceso |
|---|---|
| Frontend independiente HTML/CSS/JS + esbuild | `frontend/dist`; build integrado opcional para desarrollo |
| API Java 21/Spring Boot 4.1.1 sin frontend | Perfil Maven `api-only` y `deploy/Dockerfile.api` |
| MySQL 8.4 separado y TLS | Configuración para VM Oracle; pendiente alta real |
| Arquitectura, proveedores y costos | [Publicación gratuita](PUBLICACION_GRATUITA.md) |
| Migraciones, JWT, cookies, CORS/CSRF, SMTP y secretos | [Revisión técnica](REVISION_SEGURIDAD.md) |
| Agente NEXO-Δ con IA local real y panel de demostración | [Diseño, ejecución y límites](NEXO_DELTA.md) · [Abrir y usar](USAR_NEXO.md) |
| RESTler sin IA | [Laboratorio y resultados](RESTLER.md) |
| Exposición paso a paso | [Guion de 20 minutos](GUION_EXPOSICION.md) |
| Pruebas y pendientes auténticos | [Evidencia](EVIDENCIA.md) |

La referencia de 65 pruebas aprobadas del 4/nov se conserva como antecedente. Las verificaciones del 8/nov se reportan por suite, sin reutilizar ese total. Se inició Docker Desktop, que estaba instalado pero detenido; se usaron contenedores nuevos y sintéticos. Los contenedores anteriores no se utilizaron como objetivo del fuzzer.

## Preparación local

```sh
npm --prefix frontend ci
npm --prefix frontend run check
# Solo para pruebas integradas o desarrollo en un mismo origen:
npm --prefix frontend run build:integrated
sistema-m01-m02/scripts/verify-java21.sh
PW_CHANNEL=chrome npm --prefix frontend run test:e2e
node frontend/tests/separate-origins.mjs
docker build -f deploy/Dockerfile.api -t examenerik-actividad-api .
python3 seguridad/probar_tls.py
python3 -m unittest discover -s agente-nexo -v
python3 agente-nexo/nexo.py agente-nexo/escenario.json --ai
```

El último comando usa el modelo local `qwen2.5-coder:7b` disponible en Ollama, sin una clave comercial. No instala ni descarga modelos automáticamente. La prueba de dos orígenes usa certificados autofirmados aceptados únicamente por su navegador de laboratorio; no es evidencia de certificados públicos.

## Qué falta para publicar

Crear/acceder a cuentas gratuitas de Cloudflare, Render y Oracle; comprobar capacidad Always Free y sus condiciones de identificación; aprobar el subdominio gratuito; verificar el remitente SMTP; introducir secretos en los proveedores; aplicar firewall, certificados y migraciones sobre un esquema vacío; desplegar y verificar las URLs reales. Si una cuenta exige pago, detener esa ruta: no está autorizado sustituirla por una de pago.

Render se conectó durante esta sesión y se comprobó acceso de lectura a un espacio. No se desplegó ningún servicio ni se modificaron recursos. Pendiente confirmar espacio/repositorio y acceso a Cloudflare/Oracle.
