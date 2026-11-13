# Operación local y preparación de publicación

## Requisitos locales

Docker Desktop iniciado, Node 24+, npm, Python 3, conexión para dependencias iniciales y Ollama con `qwen2.5-coder:7b`. Java 21 se usa dentro de Docker; no es necesario cambiar el Java predeterminado de la Mac. El lanzador comprueba comandos, acceso a Docker y puerto. No instala ni arranca por su cuenta servicios de IA ajenos, y no descarga modelos sin indicación.

Desde la raíz de ExamenErik:

```sh
python3 agente-nexo/launcher.py
# Sin abrir navegador, para automatización:
python3 agente-nexo/launcher.py --no-open
# Detener solo este entorno, conservando su base:
python3 agente-nexo/launcher.py --stop
```

En Finder: `agente-nexo/Abrir NEXO.command` y `Detener NEXO.command`. Usa el lanzador del proyecto completo, no la copia extraída del antiguo ZIP de demostración.

Dirección: `http://localhost:8090/#nexo`. La primera vez se muestra login. La cuenta sintética inicial está en `.local/Acceso local.txt`, accesible solo al usuario del equipo. Bootstrap se habilita únicamente cuando la base de este entorno no tiene cuentas. No se modificó la regla de seguridad de bootstrap del IAM.

`deploy/nexo-local.yml` crea exclusivamente el proyecto Compose `examenerik-nexo-dev`: backend, MySQL 8.4 con volumen propio y Mailpit. La base no se publica al host. API/interfaz se publican solo en loopback. Ollama se consulta mediante `host.docker.internal`. Este entorno local usa HTTP y SMTP sin TLS dentro de Docker; no es una configuración de producción.

Una cerradura de archivo impide lanzadores simultáneos. Un hash de fuentes evita recompilar sin cambios. Compose reutiliza servicios y volumen; no duplica instancias. Si 8090 pertenece a otro servicio, se detiene con explicación. Para reiniciar puede cerrarse Terminal: los servicios Docker siguen activos. Detener no elimina el volumen ni otros proyectos Docker. Ollama se conserva por ser un servicio compartido.

El lanzador ejecuta `package -DskipTests` para preparar desarrollo rápido: **no equivale a verificar**. Las pruebas se ejecutan por separado y sus resultados se entregan. Si falta Docker o el modelo, se informa; no se presenta una respuesta simulada.

## Variables de NEXO

| Variable | Predeterminado | Uso |
|---|---|---|
| NEXO_PROVIDER | ollama | ollama o disabled |
| NEXO_BASE_URL | http://127.0.0.1:11434 | Servicio local permitido |
| NEXO_MODEL | qwen2.5-coder:7b | Modelo instalado |
| NEXO_TIMEOUT_SECONDS | 180 | 10–300 por llamada |
| NEXO_MAX_TOKENS | 700 | 64–2048 de salida |
| NEXO_RETENTION_DAYS | 7 | 1–30 días por chat |
| NEXO_REQUESTS_PER_MINUTE | 8 | 1–60 por usuario/instancia; desarrollo usa 20 |
| NEXO_TURNS_PER_DAY | 100 | 1–1000; incluye intentos fallidos |

No hay claves de API de IA externas. Continúan siendo necesarias DB_URL/DB_USERNAME/DB_PASSWORD, JWT_SIGNING_KEY, APP_ALLOWED_ORIGINS, configuración SMTP y URL de recuperación descritas en el proyecto. Ninguna clave debe usarse como variable pública de esbuild.

## Tres proveedores independientes: preparación, no despliegue

| Componente | Proveedor candidato | Condición |
|---|---|---|
| Frontend estático | Cloudflare Pages Free | `frontend/dist`, límite de compilaciones y archivos |
| API Java | Render Free con Docker Java 21 | Recursos limitados y suspensión por inactividad; no alojar el 7B aquí |
| MySQL 8.4 | Oracle Always Free, VM Ampere con MySQL autogestionado | Cuota/elegibilidad/capacidad de región; almacenamiento persistente y TLS configurados por operador |

Coste objetivo: 0 dentro de las cuotas aprobadas, sin actualización automática a pago. Oracle puede requerir validación de tarjeta y no garantiza capacidad. Always Free no es lo mismo que el crédito de prueba temporal. Se conserva MySQL 8.4 autogestionado para fijar versión; no se presupone que un servicio gestionado gratuito entregue exactamente esa versión.

Las fuentes oficiales consultadas en esta fase: [Cloudflare Pages](https://developers.cloudflare.com/pages/platform/limits/), [Render Free](https://render.com/docs/free), [Oracle Always Free](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm). Oracle indica hasta 1500 horas OCPU y 9000 GB-h al mes para A1, equivalentes a 2 OCPU/12 GB para uso continuo bajo esas condiciones; comprobar siempre la consola. La capacidad real y la elegibilidad de la cuenta del estudiante no se verificaron.

Para que NEXO funcione públicamente sin la Mac hace falta un host de inferencia controlado y conexión protegida. Un modelo 7B más MySQL en una VM gratuita debe medirse: no se garantiza latencia ni RAM suficiente bajo concurrencia. El adaptador actual admite solo servicio local o alias interno; una topología remota necesita túnel/red privada o un adaptador HTTPS autenticado revisado. Hasta completar esa infraestructura, `NEXO_PROVIDER=disabled` informa estado de IA no disponible. El IAM y el simulador determinista no dependen de la inferencia.

## Secuencia de publicación pendiente de autorización

1. Crear o confirmar el repositorio Git remoto de ExamenErik. No incluir `.env*`, `.local/`, claves TLS privadas, logs privados ni modelos.
2. Preparar Oracle VM dentro de cuota, MySQL 8.4 y persistencia. Usar los archivos `deploy/mysql`; exigir TLS, usuario de aplicación limitado al esquema, firewall restringido y backups privados. Verificar `sslMode=VERIFY_IDENTITY` y CA en Java.
3. Preparar backend con `deploy/Dockerfile.api`, puerto asignado por Render y perfil Maven `api-only`. Verificar que el JAR no contiene `static/**`. Configurar secretos en el proveedor.
4. Solicitar un dominio/subdominio gratuito autorizado que permita `app` y `api` dentro del mismo sitio registrable. El plan previo considera is-a.dev sujeto a revisión; no está concedido. Validar DNS y certificados antes de conectar la UI.
5. Compilar frontend: `APP_API_BASE_PATH=https://api.DOMINIO/api npm --prefix frontend run build`. Publicar `frontend/dist` en Cloudflare. La URL es pública, no un secreto.
6. `APP_ALLOWED_ORIGINS=https://app.DOMINIO`, cookie Secure y Strict, host-only. Usar credenciales en fetch y validar preflight. `pages.dev` y `onrender.com` son sitios distintos: SameSite=None no evita el bloqueo de cookies de terceros. No degradar las validaciones para ocultar ese problema.
7. SMTP externo gratuito con remitente verificado y STARTTLS en un puerto permitido. Render Free bloquea 25/465/587; evaluar 2525. [SMTP2GO](https://www.smtp2go.com/pricing/) ofrece hasta 1000 correos/mes en el plan gratuito; cuenta, remitente y entrega real están pendientes.
8. Aplicar las migraciones pendientes, incluidas V4 y V5, mediante Flyway sobre una copia de seguridad comprobada. V4 añade la tabla de reservas y V5 añade la cuota compartida por cuenta. Mantener intactas las migraciones aplicadas. Un JAR anterior puede requerir restaurar el esquema previo para pasar su validación Flyway.
9. Repetir login, refresh, CORS/CSRF, simulación, retención, TLS y correo contra el despliegue autorizado. Registrar URLs, fechas y resultados reales.

No se crearon dominios, recursos de nube, claves externas ni cargos. “Preparado” no significa “publicado” ni certificado para producción. Render advierte que Free es para pruebas/proyectos personales, no producción crítica.
