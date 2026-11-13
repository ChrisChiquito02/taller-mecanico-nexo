# Seguridad vigente — ExamenErik

[Arquitectura y límites](../orquestador.md) · [Backend](../documentacion/backend/documentadorBackend.md).

- Argon2id: memoria 19 456 KiB, dos iteraciones, paralelismo 1, sal 16 bytes y hash 32 bytes. Contraseñas nuevas de 12–128 caracteres, nunca normalizadas ni registradas.
- JWT HS256 con clave Base64 ≥256 bits, emisor y tiempo validados. Access TTL máximo 15 minutos; cada petición consulta estado, versión y autoridades actuales. Cambios de contraseña/roles/estado y logout revocan accesos.
- Refresh/RESET aleatorios de 256 bits, solo SHA-256 en DB. Rotación de un uso, replay vigente revoca sesiones. Tokens expirados no pueden revocar accesos nuevos, tampoco mediante logout.
- Cookie HttpOnly, SameSite=Strict, Path=/api/auth y Secure obligatorio fuera de local/test. El cliente mantiene acceso en memoria, sin almacenamiento persistente, y serializa renovaciones por promesa/Web Locks.
- CSRF desactivado bajo diseño Bearer. Refresh/logout por cookie exigen JSON y Origin exacto permitido; no se acepta ausencia de Origin ni wildcard con credenciales.
- RBAC por permisos de roles activos. USER no modifica usuarios. Bootstrap ADMIN explícito solo con base sin usuarios, sin contraseña predeterminada. LOCKED sin fecha no acepta Bearer; recuperar/cambiar no reactiva DISABLED.
- Registro público: 202 genérico y sin ID para cuenta nueva/existente, incluido conflicto concurrente de unicidad. Login uniforme. Recuperación: cola acotada antes de consultar identidad, 202 sin esperar SMTP; 503 por saturación independiente del correo.
- La recuperación se ejecuta en trabajador y transacción; fallo SMTP revierte el RESET nuevo y conserva el anterior. La cola es en memoria, sin garantía de entrega tras reinicio. Solicitar otro enlace si no llega; no se almacenan enlaces en claro en una outbox.
- SMTP con tiempos de 5 s y STARTTLS obligatorio fuera de local/test. Desarrollo también entrega por SMTP (Mailpit), nunca por logs. Enlace con fragmento eliminado al abrirlo.
- Rate limiting HMAC por IP remota, caché acotada y por instancia; no se confía en X-Forwarded-For recibido. JSON ≤16 KiB también chunked. CSP, no-store y errores sin SQL/stacktrace/secretos. Se suprime el logger JDBC que podría revelar valores en errores de unicidad.
- Tests con MySQL y SMTP nuevos, sin bases/correo reales. E2E sin trace/video para no conservar tokens HTTP.

## Límites operativos

Producción requiere HTTPS, secretos propios, SMTP verificado y revisión de proxy/red. No hay certificación de seguridad, auditoría persistente, MFA, verificación de correo, purga automática, rotación automática de claves ni rate limiter distribuido. No se promete indistinguibilidad matemática de tiempos de red. El SMTP externo y HTTPS real no fueron verificados en este entorno.

No adjuntar `.env`, Authorization, cookies, contraseñas ni correos con tokens a reportes de incidentes. No desactivar validaciones para conseguir que un despliegue arranque.
