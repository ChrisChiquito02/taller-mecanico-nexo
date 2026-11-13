> Actualización: el asistente conversacional integrado está documentado en [NEXO-Δ AI](../nexo-ai/README.md). Este documento conserva el alcance de la fase anterior.

# NEXO-Δ — Navegador de Efectos Contrafactuales de Autorización

## Problema e innovación concreta

ExamenErik permite cambiar roles y revocar sesiones. Antes de hacerlo, un equipo necesita saber qué privilegios aparecerían, qué sesiones dejarían de funcionar y si basta una alternativa menos invasiva. **NEXO-Δ analiza cambios futuros, no ejecuta la administración de cuentas.** Su unidad de trabajo es un delta hipotético de autorizaciones en un horizonte temporal, no un chatbot de login ni un detector de contraseñas.

Personalización: semántica de `session_version` y refresh consumido de ExamenErik; catálogo real de V2; diferencia entre asignar roles y modificar hipotéticamente permisos compartidos; comparación con la alternativa de no cambiar nada; rollback que no revive tokens. Esta combinación hace específica la propuesta. No es posible garantizar que nadie haya presentado una idea parecida ni que no coincida con otro equipo: registrar nombre, escenarios y demostración propios y consultarlo con el docente.

## Qué está implementado

`agente-nexo/nexo.py` es un agente híbrido local: valida snapshot sintético, ejecuta simulaciones, enumera conjuntos de roles que cumplen la necesidad, evalúa la alternativa con menos exceso de permisos y entrega los resultados al modelo local para explicar la decisión. La IA generativa es **Ollama + qwen2.5-coder:7b**; no se entrenó un modelo nuevo. Los cálculos son deterministas, separados del texto de IA. No se presenta el motor matemático por sí solo como IA.

Secuencia: entrada → validación → mundo antes/después → análisis de sesiones al horizonte → puntuación → búsqueda de alternativas → simulación alternativa → inferencia local → salida JSON y revisión humana. No existe herramienta apply, endpoint de administración, cliente SQL, importación de .env ni conexión al backend dentro del agente. El único HTTP permitido por su implementación es la URL literal loopback de Ollama, sin proxies ni redirecciones. Esta es una separación de capacidades en código, no una garantía contra modificación maliciosa del proceso por otro programa con permisos del usuario.

## Entrada y supuestos

- `synthetic: true`, de 1 a 100 usuarios u-1/u-2, de 1 a 8 roles activos de catálogo y hasta 20 tokens sintéticos por usuario.
- Solo `profile:read`, `users:read`, `users:write`. Campos cerrados: rechaza email, hashes, JWT, contraseñas o propiedades extra.
- `change.assignments` expresa el futuro equivalente a assignRoles. Exige al menos un rol conocido por usuario.
- `change.permission_patch` expresa un catálogo futuro hipotético. El IAM no tiene ese endpoint; su implementación requeriría una ampliación/migración revisada. No se generó V3 ni se aplicó SQL.
- `required_permissions` declara la tarea necesaria; no se inventa desde texto libre del usuario.
- `at_minute` de 0 a 10,080: evalúa validez de los tokens en ese momento y aplica el cambio hipotético. Tokens ya expirados no se atribuyen al daño del cambio.
- Estado no activo se considera sin sesiones válidas. No se modelan expiración de bloqueo, carreras, latencia, rotación concurrente o ataque de reutilización de refresh.

Los permisos del JWT se recalculan desde BD en el sistema real. Por ello, un cambio hipotético de permisos compartidos puede afectar sesiones que siguen vigentes, sin cambiar `sv`. Una asignación por el servicio real, incluso idéntica, revoca accesos por incremento de versión y consume/revoca refresh.

## Rúbrica auditable

```text
G = suma por usuario de pesos de cada permiso nuevo
    profile:read = 2, users:read = 15, users:write = 35
S = mínimo(20, 5 × tokens válidos que el cambio invalidaría)
A = 40 si no queda ninguna cuenta ACTIVE con users:write; si queda, 0
riesgo = mínimo(100, G + S + A)
0–24 bajo; 25–59 medio; 60–100 alto
```

Los pesos son una decisión educativa explícita, no un modelo entrenado ni probabilidad de ataque. El indicador no penaliza todo posible daño: por ejemplo, una pérdida de lectura puede ser grave para una tarea aunque no agregue puntos G. Mostrar siempre permisos perdidos y sesiones además del número. `remaining_active_writers` cuenta capacidad de escritura, no solo el nombre ADMIN.

Alternativas: enumera los subconjuntos del catálogo (máximo 256), filtra los que contienen los permisos necesarios y ordena por peso de permisos sobrantes, número de roles y nombre. Evalúa la primera opción y conserva los roles actuales cuando coinciden para evitar llamar a assignRoles inútilmente. No crea roles desconocidos para mejorar artificialmente su resultado.

## Tres demostraciones reproducibles

```sh
python3 agente-nexo/nexo.py agente-nexo/escenario.json --ai
python3 agente-nexo/nexo.py agente-nexo/escenario-lectura.json
python3 agente-nexo/nexo.py agente-nexo/escenario-permisos.json
python3 -m unittest discover -s agente-nexo -v
```

| Escenario | Resultado calculado |
|---|---|
| u-2 USER → ADMIN, solo necesita perfil, minuto 5 | G=50, S=10, A=0: 60/100; dos tokens invalidados. Alternativa: conservar USER sin llamada administrativa: 0/100 |
| u-2 necesita users:read | USER no basta; el único catálogo que cumple es ADMIN, que también concede users:write. Alternativa disponible: 60/100; no hay mejora real en V2 |
| Agregar users:write al permiso compartido de USER | Dos usuarios ganan escritura, G=70, S=0: 70/100; sus tokens continúan válidos. Alternativa: no tocar catálogo: 0/100 |

Para la segunda situación, proponer como **trabajo futuro** un rol de consulta con solo profile:read/users:read, sujeto a pruebas, migración y revisión humana. No decir que ya existe ni que fue aplicado.

## Rollback y revisión de IA

El reporte incluye asignaciones y permisos anteriores, más la advertencia de iniciar sesión de nuevo. Restaurar roles en el sistema real requiere otra operación autorizada; no se debe reducir `session_version`, reactivar tokens consumidos ni reproducir cookies antiguas. El rollback no deshace una divulgación de datos ocurrida durante un permiso excesivo.

Una inferencia inicial describió incorrectamente la alternativa como retirar ADMIN, aunque el estado original era USER. Se conservó en `nexo-ia-primera-revision.json`, se añadió contexto antes/propuesto y se corrigió la alternativa a **no realizar cambios**. La inferencia posterior está en `nexo-ia.json` y concuerda con los números principales. Esto demuestra la necesidad de revisión: la estructura JSON válida no garantiza exactitud semántica. El texto del modelo nunca modifica el resultado numérico ni autoriza una operación.

Ollama ya tenía el modelo instalado; se usó sin descargar modelos ni pagar una API. Sin `--ai`, la salida dice `not_requested`; con fallo del modelo, el comando termina con error, no sustituye silenciosamente la IA por un texto prefabricado. Se guarda hash del snapshot, nombre del modelo, fecha UTC y cantidad de tokens generados. Ningún dato real del IAM fue enviado al modelo.

Fuentes: [Ollama Chat API](https://docs.ollama.com/api/chat), [salida JSON estructurada](https://docs.ollama.com/capabilities/structured-outputs). Semántica IAM comprobada en los servicios locales `UserAdministrationService`, `SessionService` y `DatabaseJwtAuthentication`.


## Panel de demostración implementado

Se añadió una pantalla local con los tres escenarios, control del horizonte, explicación de IA opcional e informe descargable. Se inicia con doble clic en `agente-nexo/Abrir NEXO.command` o `python3 agente-nexo/servidor.py --open`. La [guía de uso](USAR_NEXO.md) detalla la demostración.

El servidor escucha solo en 127.0.0.1, comprueba Host y Origin, rechaza cuerpos inesperados o de más de 2 KiB y limita la ejecución a un análisis a la vez. Solo carga tres fixtures conocidos; el navegador no puede elegir rutas de archivos, proveedor, modelo, prompts libres ni credenciales. El modelo se sigue consultando en loopback. El texto generado se renderiza como texto, sin interpretar HTML.

En la revisión del primer panel se corrigió un aviso de catálogo que se mostraba también en el caso de perfil propio: ahora depende del primer candidato, no de todos los candidatos enumerados. Se añadió la necesidad requerida al contexto del modelo para distinguir entre “conservar el estado satisface la tarea” y “posponer el cambio deja la consulta pendiente”. Los primeros informes permanecen como evidencia histórica; la nueva demostración registra sus propias inferencias y fuente.

En las nuevas pruebas se conservaron también inferencias que omitían detalles o confundían permisos originales e hipotéticos. Se simplificó el contexto a hechos explícitos antes/después y se repitieron los tres escenarios. El texto sigue requiriendo revisión y puede ser genérico; la pantalla muestra por separado los efectos, riesgos y alternativas calculados. No se usa el texto de IA para aprobar ni aplicar cambios.
