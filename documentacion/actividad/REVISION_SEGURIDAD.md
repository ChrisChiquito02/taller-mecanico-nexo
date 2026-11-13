# Revisión del contrato real y comunicación segura

## Repositorio y cambios

Java 21 y Spring Boot 4.1.1 en `sistema-m01-m02/pom.xml`; MySQL 8.4 en Compose y Testcontainers; frontend modular `src/app.js`, `api-client.js`, `ui.js`, CSS y build esbuild 0.28.2. OpenAPI 3.1 está en `docs/integracion/openapi.json`, con 12 rutas y 12 operaciones HTTP.

Cambios: build predeterminado a `frontend/dist`, modo `build:integrated` explícito, validación común de URL API fija y HTTPS, `credentials: include`, cabeceras Pages con CSP; SameSite configurable con Strict por defecto y rechazo de None sin Secure; puerto de plataforma compatible; perfil `api-only` que excluye el frontend del JAR; imagen Java 21 multietapa y truststore de CA pública. Ninguna migración V1/V2 se modificó. NEXO-Δ no agrega tablas ni endpoints al IAM.

## V1 y V2

- V1 crea `app_user`, `role`, `permission`, `user_role`, `role_permission`, UUID BINARY(16), correo normalizado único, FK y tablas InnoDB.
- V2 agrega `app_user.session_version` y `account_token` con hash único, tipo, caducidad y consumo. Siembra USER con `profile:read` y ADMIN con `profile:read`, `users:read`, `users:write`.
- No hay tabla de sesiones por dispositivo ni endpoint para editar permisos del catálogo. Los tokens persistidos no equivalen a personas únicas/dispositivos. Un snapshot NEXO usa contadores sintéticos, no una exportación de secretos.
- Flyway tiene `baseline-on-migrate=false`, `validate-on-migrate=true`, `clean-disabled=true`; JPA `ddl-auto=validate`.
- Base nueva: V1 → V2 → validación JPA. Base existente: copia aislada, comparación completa contra V1 y revisión de V2 antes de baseline manual 1. Nunca activar baseline automático para ocultar una discrepancia.
- No editar migraciones ya aplicadas ni borrar `flyway_schema_history`. Futuras ampliaciones empiezan en V3. No se aplicó baseline ni se alteró una base real en esta actividad.

## Autenticación y sesiones

Contraseñas Argon2id. JWT HS256: clave Base64 de al menos 256 bits, emisor comprobado, acceso máximo 15 minutos. La autorización carga usuario/roles/permisos desde MySQL en cada conversión; no se confía en un rol antiguo dentro del JWT. Se verifica el claim `sv` contra `session_version` y estado de cuenta.

Refresh opaco aleatorio: se guarda hash, no el valor; cookie HttpOnly, Secure en producción, host-only, Path `/api/auth`, SameSite Strict por defecto. La rotación consume el token anterior. Reutilizar un refresh consumido revoca sesiones y tokens de la cuenta. Logout revoca todas las sesiones de esa cuenta, no solo una pestaña. Cambio/restablecimiento de contraseña y asignación de roles también invalidan sesiones.

`assignRoles` incrementa versión y revoca tokens incluso si se asigna el mismo conjunto de roles. Por eso la alternativa NEXO que conserva USER debe decir **no llamar a assignRoles**, para evitar una revocación innecesaria. Desactivar impide la autodesactivación administrativa y revoca tokens. La revocación no puede deshacerse restaurando roles: se requiere un nuevo login.

## CORS y CSRF

CORS permite orígenes exactos, GET/POST/PUT/DELETE/OPTIONS, Authorization y Content-Type, credenciales y preflight 600 segundos. No se usa `*` con cookies.

Spring CSRF genérico está deshabilitado y la sesión HTTP es stateless. Esto no significa ausencia de protección: refresh/logout exigen **Origin exacto permitido**, POST y cuerpo JSON. Falta de Origin o uno ajeno da 403. El navegador no puede enviar JSON con Content-Type application/json desde un origen ajeno sin preflight. SameSite Strict añade una barrera. El resto de operaciones protegidas exigen Bearer en cabecera; la cookie no autoriza administración. Login/registro no aceptan un POST de formulario como JSON. Mantener estas propiedades al separar proveedores.

CORS es una política del navegador, no una autenticación ni un firewall; un cliente ajeno puede falsificar Origin, pero necesitaría el refresh secreto para renovar. HttpOnly dificulta leer la cookie con JavaScript, pero XSS aún puede operar con la sesión del navegador. CSP, renderizado con textContent, revisión de dependencias y permisos mínimos siguen siendo necesarios.

La prueba local `frontend/tests/separate-origins.mjs` levanta dos orígenes HTTPS del mismo sitio, sirve el build independiente y comprueba login, cookie HttpOnly/Secure/Strict host-only, refresh al recargar y ausencia de cookie visible en document.cookie. Acepta certificado autofirmado solo en el contexto de prueba. No modifica la confianza del sistema ni demuestra la configuración de TLS de nube.

## SMTP, límites y privacidad

Producción exige cookie Secure, modo SMTP y STARTTLS requerido. Se agregó también mail.smtp.ssl.checkserveridentity=true para comprobar el nombre del servidor SMTP. Configurar también autenticación SMTP, remitente verificado y proveedor accesible. Mailpit/GreenMail locales son buzones sintéticos; no demostrar una entrega pública con sus resultados.

El guard limita intentos por HMAC de dirección remota, sin fiarse de X-Forwarded-For. Detrás de un proxy pueden compartir un límite varios alumnos; el limitador está en memoria y no se comparte entre réplicas. No presentar esto como defensa distribuida. Las pruebas elevan el límite solo en su entorno desechable.

No mostrar claves JWT, contraseñas, Cookie/Set-Cookie completos, Authorization, enlaces de recuperación con token, correos reales, dumps, historial ni logs privados. El frontend solo contiene la URL pública de API. `seguridad/private` conserva intercambios sintéticos y credenciales efímeras con acceso restringido; queda excluido de entregas y control de versiones. El paquete de exposición contiene solo métricas/capturas de cuentas sintéticas.

NEXO usa JSON de estructura cerrada, identificadores u-1/u-2 y permisos de V2. Rechaza campos personales o permisos desconocidos; únicamente habla con Ollama en loopback y no dispone de credenciales IAM/MySQL. La salida del modelo es una recomendación no confiable, separada del puntaje calculado. No ejecuta recomendaciones.

## Riesgos pendientes

DNS y certificados públicos, TLS MySQL remoto, firewall de Oracle, SMTP desde Render, rendimiento con CPU/cuotas de Render y arranque en frío requieren verificación tras publicar. El laboratorio sí arrancó la imagen final con límite local de 512 MB y verificó TLS de MySQL con CA propia. Los contenedores de pruebas locales no acreditan esos controles de proveedores. Ninguna cantidad de pruebas confirma seguridad absoluta.
