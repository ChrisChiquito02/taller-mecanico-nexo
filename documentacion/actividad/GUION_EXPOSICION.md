# Guion detallado de exposición — 20 minutos

## Antes de entrar al aula

Abrir el proyecto correcto ExamenErik y `documentacion/actividad/README.md`. Tener el modelo Ollama local disponible, Docker activo y la evidencia ya generada. No dejar .env, consola con Authorization, panel de secretos ni buzón personal en pantalla. Usar únicamente cuentas sintéticas. Abrir imágenes/JSON públicos; cerrar `seguridad/private`.

Si el despliegue sigue pendiente, decirlo al inicio. Las URLs de ejemplo no se convierten en evidencia por mostrarlas en una diapositiva. No gastar dinero ni desactivar seguridad para resolver una demostración en vivo. Preparar capturas locales de respaldo por arranques en frío o falta de Internet.

## 0:00–2:00 — Qué hace nuestro proyecto

Decir: “Nuestro proyecto se llama ExamenErik. Gestiona identidad y acceso: registro, login, perfil, recuperación por correo, sesiones y administración con roles. El frontend es HTML/CSS/JavaScript modular; el backend usa Java 21 y Spring Boot 4.1.1; la base es MySQL 8.4”.

Mostrar carpetas frontend/src, backend/src/main y migraciones. Explicar que no hay módulos de ventas o predicción. Mostrar una cuenta sintética, su perfil y el enlace administrativo visible solo con permiso. Aclarar estado: “La implementación y pruebas que mostraremos se ejecutaron localmente el 8 de noviembre; la publicación real requiere completar cuentas y DNS gratuitos”, salvo que después exista evidencia auténtica nueva que justifique actualizarlo.

## 2:00–5:00 — Tres proveedores y comunicación

Mostrar el diagrama de PUBLICACION_GRATUITA. Recorrer navegador → frontend Cloudflare → API Render → MySQL en VM Oracle; SMTP auxiliar aparte. Mostrar que el build normal genera frontend/dist y que el JAR api-only excluye static. Así las tres partes no se publican juntas.

Decir: “Buscamos cero mensualidad dentro de cuotas. No usamos la prueba gratis de un servicio como si fuera permanente. Render Free duerme, Oracle exige validación/capacidad y puede recuperar VMs inactivas, y el DNS comunitario debe aprobarse”. Explicar por qué no Aiven gratis: retiró nuevas bases MySQL Free; no cambiar MySQL 8.4 por otro motor para aparentar cumplimiento.

Mostrar ejemplos de dominio padre y API anidada, sin afirmar que están reservados. Explicar origen = esquema/host/puerto y sitio = esquema/dominio registrable: frontend y API pueden tener distintos orígenes y seguir en el mismo sitio. Dos dominios gratuitos de proveedores independientes son sitios diferentes; CORS por sí solo no evita bloqueos de cookies.

## 5:00–8:00 — Seguridad con pruebas, no con adjetivos

Mostrar V1: tablas, correo único, relaciones. V2: session_version, tokens con hash y roles reales USER/ADMIN. Explicar que no se editan migraciones aplicadas ni se ejecuta baseline automático.

Mostrar código de cookie sin su valor: HttpOnly, Secure, Strict, Path /api/auth y ausencia de Domain. Mostrar evidencia origenes-https.json: login 200, refresh 200 después de recargar, cookie de API host-only. Mostrar que el JavaScript no lee refresh y que el JWT de acceso vive en memoria.

Decir: “JWT válido no basta: el backend comprueba estado y versión de sesión en la base. Al asignar roles se revocan sesiones. CORS solo permite el origen exacto, y refresh/logout rechazan Origin ajeno o ausente con 403 y exigen JSON”. Mostrar preflight de RESTler y los estados, no cabeceras con tokens.

Explicar TLS MySQL VERIFY_IDENTITY frente a solo cifrado, firewall por rangos Render y secretos solo en backend. Para SMTP, puerto 2525 con STARTTLS requerido; no decir que Mailpit prueba entrega en Internet.

## 8:00–13:00 — NEXO-Δ en acción

Decir: “No agregamos otro login. Nuestro agente estudia lo que pasaría antes de cambiar roles/permisos: privilegios nuevos, impacto en sesiones, alternativa de menor privilegio y rollback. No ejecuta cambios reales”.

Puedes abrir el panel con doble clic en `agente-nexo/Abrir NEXO.command` y elegir los mismos tres escenarios. También se conserva la demostración por comandos. Mostrar escenario.json: tres usuarios sintéticos y dos roles reales. Ejecutar:

```sh
python3 agente-nexo/nexo.py agente-nexo/escenario.json --ai
```

Explicar sus pasos: validar entrada, simular USER → ADMIN, comprobar tokens en minuto 5, calcular riesgo, comparar alternativas y pedir una explicación al modelo local. Señalar ai.status=executed y nombre del modelo. El puntaje es calculado por código; la redacción es IA. Si Ollama falla, mostrar el error y la evidencia previa fechada, sin presentarla como inferencia nueva.

Desglosar 60: users:read=15 + users:write=35 + dos tokens invalidados×5=10. Mostrar alternativa: conservar USER, cero cambios, riesgo 0. No “reasignar USER”, porque incluso la asignación idéntica revoca sesiones.

Ejecutar escenario-lectura.json sin --ai y decir expresamente que esa ejecución usa solo el motor de simulación. Mostrar que necesita users:read pero ADMIN incluye escritura. El agente no inventa un rol ni altera V2. Proponer un rol de lectura como ampliación futura revisada.

Mostrar escenario-permisos.json: si USER ganara users:write, dos usuarios ganarían escritura y sus sesiones continuarían válidas, porque la autorización consulta permisos en BD y ese cambio de catálogo hipotético no pasa por assignRoles. Riesgo 70. Es una hipótesis: no existe endpoint para modificar el catálogo.

Explicar rollback: restaurar roles requiere revisión y nuevo login; nunca reducir session_version ni revivir cookies. Mostrar que el primer texto de IA tuvo una confusión que se detectó, se conservó y se corrigió. Eso justifica que el modelo no tenga permisos de escritura. No asegurar exclusividad mundial de la idea.

## 13:00–17:00 — RESTler sin IA

Presentar autor Microsoft Research y su repositorio oficial. Decir: “RESTler compila OpenAPI en peticiones y explora secuencias con estado; no usa un modelo de lenguaje en esta prueba”. Mostrar copia de contrato, diccionario conceptual sin credenciales y restricción register → login.

Explicar el aislamiento: MySQL temporal, SMTP capturado, API loopback, claves sintéticas, limpieza de recursos propios. Mostrar resumen del intento cea23c80: commit, hash, compile/test/fuzz código 0, duración y cleanup_success.

Mostrar testing_summary y conteo HTTP independiente. Decir: “La herramienta reportó cobertura 12/12 y no registró bug buckets. Sin embargo, su analizador Fuzz falló al leer algunas peticiones/respuestas malformadas; analysis_complete es false. Esto no es una certificación. Refresh con cookie y recuperación correcta se verificaron aparte”.

No ejecutar la campaña sobre el servicio público en aula. Si se repite el laboratorio, mostrar su carpeta nueva y fecha nueva. Un minuto es presupuesto de fuzzing, no tiempo total de la demostración. No contar cada petición como una prueba funcional aprobada.

## 17:00–19:00 — Evidencia y límites

Abrir EVIDENCIA.md: 41 backend, 17 frontend unitarias, 8 navegador, 7 NEXO y prueba adicional de orígenes HTTPS, con archivos de salida reales. Distinguir del antecedente histórico de 65 del 4/nov. Mostrar captura HTTPS local y qué afirma su certificado autofirmado.

Explicar qué falta de nube: cuentas, DNS, certificados públicos, firewall real, prueba TLS remota, entrega SMTP desde Render y rendimiento bajo cuota. Si el profesor pregunta “¿cómo sabes que es segura?”, responder: “Tenemos controles concretos y verificaciones reproducibles para ciertos escenarios; también registramos los límites. No afirmamos seguridad absoluta”.

## 19:00–20:00 — Cierre y preguntas

Decir: “Entregamos separación de componentes, un agente específico que anticipa efectos de autorización y una herramienta independiente para buscar fallos. El objetivo es decidir con evidencia antes de modificar permisos”.

Preguntas previstas:

- ¿NEXO repite el IAM? No crea usuarios ni sesiones; compara estados hipotéticos y explica el impacto.
- ¿Es IA solo por tener nombre? Hay inferencia local real documentada; cálculo y validación siguen siendo deterministas.
- ¿El riesgo 60 significa 60%? No; es una rúbrica explícita de 0 a 100.
- ¿Por qué no usar cualquier base gratuita? Debe ser MySQL 8.4, con TLS y acceso adecuado; compatibilidad parcial o una prueba temporal no acreditan el requisito.
- ¿Sin hallazgos equivale a seguro? No; campaña breve, cobertura limitada y analizador incompleto.
- ¿Rollback recupera las sesiones? No; exige autenticarse de nuevo.
- ¿Todo está publicado? Responder el estado que indique evidencia, nunca el que se desearía tener.
