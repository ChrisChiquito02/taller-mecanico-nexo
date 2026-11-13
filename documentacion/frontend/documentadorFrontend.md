# Documentador Frontend — ExamenErik

Referencia especializada. [Arquitectura central](../../orquestador.md) · [Contrato OpenAPI](../../docs/integracion/openapi.json) · [Informe y evidencia](../auditoria/INFORME_FINAL.md).

## 1. Objetivo

Interfaz real de identidad: registro, acceso, perfil, cambio/recuperación de contraseña, cierre de sesiones y administración según permisos. Cada acción usa la API Spring/MySQL; no hay cuentas, permisos ni resultados simulados.

## 2. Tecnologías y versiones

HTML semántico, CSS responsivo y JavaScript ES2022 nativo. Node.js 24+ solo para herramientas; ejecución comprobada con Node 24.16.0 y npm 11.13.0. Dependencias de desarrollo fijadas en [package.json](../../frontend/package.json) y `package-lock.json`: esbuild 0.28.2, jsdom 30.1.3 y Playwright 1.64.1.

Se eligió JavaScript nativo porque los siete formularios no necesitan un framework de componentes ni un servidor Node en producción. esbuild permite módulos comprobables y un artefacto sin dependencias de ejecución adicionales. La interfaz comparte origen con la API: reduce configuración CORS y evita exponer tokens a otros hosts.

## 3. Estructura y fuente única

```text
frontend/
├── public/index.html                 Plantilla única
├── src/
│   ├── app.js                        Estado, eventos y coordinación visual
│   ├── api-client.js                 HTTP, contrato, timeout y sesión en memoria
│   ├── ui.js                         Permisos, rutas y representación segura
│   └── styles.css                    Diseño responsivo
├── scripts/build.mjs                 Bundle y copia hacia Spring
├── tests/unit/                       Node test + jsdom
├── tests/e2e/                        Playwright + Docker aislado
├── playwright.config.js
├── package.json / package-lock.json
└── .env.example
```

`npm run build` genera `sistema-m01-m02/src/main/resources/static/{app.js,styles.css,index.html,reset-password.html}`. Son salidas de construcción; no editarlas directamente. Los dos HTML públicos son alias generados de una plantilla, no implementaciones separadas. Se conservan las URLs anteriores. No existe un segundo frontend dentro del backend.

## 4. Componentes visuales

Cabecera/estado de servicio, navegación, región de mensajes, siete formularios, perfil con lista de definiciones, tabla administrativa, selección de usuario y controles de paginación. La disponibilidad mostrada se obtiene de `/api/health`; no representa entrega de correo ni observabilidad completa.

Fuentes: [plantilla](../../frontend/public/index.html), [coordinador](../../frontend/src/app.js), [utilidades visuales](../../frontend/src/ui.js), [estilos](../../frontend/src/styles.css).

## 5. Rutas y navegación

| Ruta de navegador | Vista | Condición |
|---|---|---|
| `/#login` | Acceso | Pública |
| `/#register` | Registro | Pública |
| `/#recovery` | Solicitar recuperación | Pública |
| `/reset-password.html#token=...` | Captura de enlace | Lee token en memoria, elimina fragmento y muestra `#reset` |
| `/#reset` | Nueva contraseña | Exige token capturado en esa instancia de página |
| `/#account` | Perfil/cambio | Sesión restaurada/autenticada |
| `/#admin` | Usuarios | `users:read` |

Se usan History API, `hashchange` y `popstate`; retroceder funciona. Al intentar acceder a vista privada, `routeAllowed` redirige a login o perfil según sesión. Un hash desconocido lleva a login. El token de recuperación no se mantiene en la dirección ni en almacenamiento persistente.

## 6. Formularios y contratos

| Formulario | Endpoint | Éxito / navegación |
|---|---|---|
| `register` | POST `/api/auth/register` | 202 genérico, ir a login; no confirma si el correo ya existía |
| `login` | POST `/api/auth/login` + GET `/api/users/me` | Cuenta y permisos reales, ir a perfil |
| `forgot` | POST `/api/auth/forgot-password` | 202 genérico; entrega en segundo plano |
| `reset` | POST `/api/auth/reset-password` | 204, token descartado, ir a login |
| `change` | POST `/api/auth/change-password` | 204, cerrar estado local y volver a login |
| `roles` | PUT `/api/admin/users/{id}/roles` | Refrescar listado; si afecta al propio administrador, exigir nuevo login |
| `disable` | DELETE `/api/admin/users/{id}` | Confirmación, 204 y listado actualizado |

Logout usa POST `/api/auth/logout` con `{}` y cookie. Listado usa `GET /api/admin/users?page=N&size=10`; el navegador no descarga todos los usuarios. Los botones Seleccionar rellenan IDs reales de las filas.

## 7. Validaciones

`required`, `type=email`, tamaños máximos, contraseña nueva de 12–128 caracteres, confirmación coincidente, patrón UUID y códigos de rol. Confirmación es únicamente local y nunca se envía a la API. Se limpia su error personalizado cuando el usuario edita. El servidor vuelve a validar todo: no se considera la validación HTML una defensa de seguridad.

## 8. Gestión de estado

`app.js` conserva usuario actual, página, restauración y token de reset. `api-client.js` conserva exclusivamente el access token y promesa de renovación en memoria. No se guardan contraseñas, access tokens ni refresh en localStorage/sessionStorage. Las contraseñas se borran del formulario tras éxito. Las páginas privadas empiezan ocultas hasta comprobar identidad.

## 9. Consumo HTTP

`createApiClient` usa fetch, JSON, `credentials=same-origin`, Bearer solo en operaciones protegidas y `AbortController` con 20 segundos. API y recursos deben tener el mismo origen; se rechazan bases externas. Se validan estructuras de Access, UserView y listas antes de utilizarlas. Un 204 no se deserializa. HTML o JSON inválido produce error genérico sin revelar el cuerpo recibido.

No se reintentan fallos de red, timeout ni errores de negocio. Tras timeout se indica verificar el resultado antes de repetir una mutación. Se reintenta una sola vez una operación rechazada por la seguridad Bearer antes de entrar al controlador.

## 10. Autenticación y sesión

Login devuelve `Access` y cookie HttpOnly. La cookie no es legible por JavaScript. Al recargar se solicita `/auth/refresh` y luego perfil. Solo un 401 con código `UNAUTHORIZED` de una operación protegida provoca renovación; un 401 `INVALID_CREDENTIALS` por contraseña actual incorrecta conserva sesión y formulario.

La promesa compartida serializa dentro de pestaña. Web Locks (`examenerik-refresh`) serializa entre pestañas del mismo origen en navegadores compatibles; sin Web Locks queda protección dentro de pestaña y puede requerirse reautenticación ante renovaciones simultáneas. No se comparte JWT por BroadcastChannel. El backend sigue siendo quien detecta reutilización y revoca.

## 11. Protección por permisos

`applyPermissions` distingue `users:read` y `users:write`: poder leer el listado no revela controles de modificación. Una cuenta USER no accede al área administrativa. Estas condiciones son de presentación; el servidor vuelve a comprobar permisos en cada endpoint y versiones/estado en cada Bearer. El cliente nunca asigna privilegios al registrarse.

## 12. Errores

Una región `role=status` / `role=alert` muestra texto seguro. No se usa innerHTML para datos. Un 403 no cierra sesión ni intenta renovar permisos; un Bearer inválido después del único reintento borra estado y va a login. Red/timeout restauran controles. Respuesta 503 de admisión de recuperación informa que debe reintentarse después.

## 13. Carga

Envíos deshabilitan el botón, cambian texto a «Procesando…», aplican `aria-busy` y restauran estado en `finally`. Se bloquea doble envío del mismo formulario. Listado tiene carga y botones de paginación según resultados; los errores se anuncian en la misma región global. No hay animaciones que simulen trabajo.

## 14. Accesibilidad

Idioma español, etiquetas asociadas, autocomplete apropiado, encabezados, navegación con nombre, enlace para saltar al contenido, foco visible, foco de título tras navegación, mensajes vivos, cabeceras de tabla y caption. Los cambios DOM usan APIs de texto. Se probaron estructura y navegación con teclado/semántica del navegador; no se ha certificado WCAG ni realizado revisión con lector de pantalla humano.

## 15. Diseño responsivo

Grid de dos columnas que pasa a una por debajo de 680 px; tabla con desplazamiento interno; anchos mínimos cero y textos largos con ajuste. E2E comprueba que la página no desborda horizontalmente a 375 px. Se conserva la identidad visual verde existente, con controles coherentes y estados legibles.

## 16. Entornos

[`.env.example`](../../frontend/.env.example): `APP_API_BASE_PATH=/api` es una variable de construcción, exportada antes del build, no se carga automáticamente. Cambiarla exige un proxy del mismo origen que mapee esa ruta a `/api`; la API Spring conserva sus rutas. No existe ninguna variable de secreto en el cliente. En local Compose sirve frontend/API juntos; en producción se requiere HTTPS.

## 17. Pruebas

- `npm test`: 16 pruebas automatizadas de cliente HTTP, renovación/reintentos, 403, timeout, errores no JSON, contratos, red, locks, permisos, rutas, XSS por representación, confirmación y estructura accesible.
- `npm run test:e2e`: 8 recorridos de navegador real con MySQL y Mailpit nuevos: registro/validación/login, bloqueo, cambio de contraseña, recuperación SMTP, administración/paginación, móvil/navegación, dos pestañas y evidencia visual.
- No se usan mocks en E2E. Los mocks de fetch solo corresponden a pruebas unitarias de errores controlados.
- Playwright desactiva trace/video para no guardar cuerpos HTTP ni tokens. Los reportes de pruebas son locales; la evidencia compartida contiene solo conteos y metadatos.

Ver resultados ejecutados en [informe final](../auditoria/INFORME_FINAL.md). Las versiones de navegador/plataforma efectivamente ejecutadas se registran allí; otras combinaciones no se atribuyen como verificadas.

## 18. Construcción y ejecución

Desde la raíz de ExamenErik:

```sh
npm --prefix frontend ci
npm --prefix frontend run check
sistema-m01-m02/scripts/verify-java21.sh
```

Después, desde `sistema-m01-m02`: `./scripts/init-local.sh` y `docker compose --env-file .env.local up --build -d`. Abrir `http://localhost:8090`.

Para E2E, tras construir JAR: `cd frontend && npx playwright install chromium && npm run test:e2e`. En un equipo con Chrome instalado: `PW_CHANNEL=chrome npm run test:e2e`. No se reutiliza el backend local ni sus datos; globalSetup crea y limpia su propio entorno. Java/Maven no requieren instalación en host al usar el script Docker; Node sí se necesita para construir/verificar las fuentes frontend.

## 19. Dependencias Backend

Las doce operaciones están descritas en OpenAPI. CORS y cookies los controla Spring; el navegador envía Origin automáticamente en POST del mismo origen. Se necesitan salud y migraciones correctas para operar; SMTP afecta entrega, no la admisión HTTP de recuperación. El backend conserva la autoridad sobre estados, normalización, dominios y permisos.

## 20. Estado de integración

Fuentes, construcción, contratos y tests están presentes. Solo se marca «verificado» en la [matriz central](../auditoria/MATRIZ_INTEGRACION.md) cuando se ejecutó su prueba. Pendientes operativos: navegador sin Web Locks, revisión WCAG completa, SMTP/HTTPS de producción y pruebas en navegadores adicionales. No hay framework/frontend alternativo sin conectar ni pantallas de negocio fuera del alcance de identidad.
