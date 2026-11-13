# Módulos y funcionalidades

## Módulo: núcleo Spring Boot
- **Objetivo:** iniciar la aplicación y descubrir propiedades configurables.
- **Implementado:** clase principal con `@SpringBootApplication` y `@ConfigurationPropertiesScan`.
- **Pendiente:** perfiles, configuración de observabilidad, endpoint de salud público/controlado y verificación reproducible.
- **Archivos:** `SistemaApplication.java`.
- **Dependencias:** Spring Boot Web.
- **Estado:** implementado como punto de arranque; no probado durante la auditoría.

## Módulo: identidad de usuario
- **Objetivo:** representar usuario, estado y controles de bloqueo.
- **Implementado:** entidad `AppUser`, transiciones de login correcto/fallido, vencimiento de bloqueo y cambio de contraseña.
- **Pendiente:** repositorio, servicio transaccional, registro, login, hash Argon2, recuperación, validaciones y API.
- **Integraciones:** requiere JPA/MySQL, seguridad y correo, todos sin cableado fuente.
- **Estado:** parcial. Riesgo: no hay evidencia de que las transiciones se invoquen.

## Módulo: autorización RBAC
- **Objetivo:** modelar roles y permisos por módulo.
- **Implementado:** entidades `Role` y `Permission`, relaciones M:N con usuario y permisos.
- **Pendiente:** creación/asignación, carga de autoridades, autorización de rutas, datos semilla y API administrativa.
- **Estado:** parcial, sólo modelo de datos.

## Módulo: persistencia compartida
- **Objetivo:** emitir UUIDv7 ordenables temporalmente.
- **Implementado:** `UuidV7Generator` con `SecureRandom`, reloj UTC, variante RFC 4122 y secuencia de 12 bits por milisegundo.
- **Pendiente:** pruebas de formato, unicidad/orden y uso efectivo en entidades.
- **Estado:** implementado como componente aislado; no se usa desde el código disponible.

## Módulo: configuración de seguridad y recuperación
- **Objetivo:** externalizar claves, TTL, cookie, CORS, lockout y recuperación.
- **Implementado:** records de propiedades y valores mediante entorno/YAML; política declarativa en `SECURITY.md`.
- **Pendiente:** validación de propiedades y las implementaciones que las consumen.
- **Estado:** parcial.
