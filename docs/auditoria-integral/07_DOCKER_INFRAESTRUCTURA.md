# Docker e infraestructura

## Archivos declarativos
No se encontraron `Dockerfile`, `docker-compose.yml`, Compose alternativo, manifiestos Kubernetes, scripts de despliegue ni CI/CD en el proyecto. Por ello no se puede documentar de forma reproducible imágenes, redes, volúmenes, health checks, órdenes de arranque o secretos de la infraestructura.

## Inspección de solo lectura del 26-10-2026

| Contenedor | Imagen | Estado | Puertos publicados | Red | Volúmenes |
|---|---|---|---|---|---|
| `sistema-backend` | `sistema-m01-m02-backend` | running | 8080→8080 | `sistema-m01-m02_default` | tres volúmenes de imágenes (`/app/storage/...`) |
| `sistema-mysql` | `mysql:8.4` | running, healthy | 3308→3306 | `sistema-m01-m02_default` | `/var/lib/mysql` |
| `mongodb` | `mongo:8.2` | running | 27017→27017 | `mongodb_nube-academica` | `/data/db`, `/data/configdb` |

La imagen backend fue creada el 2026-10-21T05:16:33 UTC, ejecuta `java -XX:MaxRAMPercentage=75 -jar /app/app.jar` y declara variables de DB/JWT/SMTP/notificaciones/almacenamiento. Se registraron sólo los nombres; los valores se redactaron. Estos nombres sugieren funciones no presentes en la fuente auditada, por lo que no se atribuyen al proyecto sin el código o configuración correspondiente.

## Accesibilidad
Las solicitudes no autenticadas a `http://localhost:8080/` y `http://localhost:8080/actuator/health` devolvieron `401`. Esto confirma conectividad TCP/HTTP y una política de autenticación en esas rutas, pero no confirma salud de aplicación ni disponibilidad de endpoints de negocio.
