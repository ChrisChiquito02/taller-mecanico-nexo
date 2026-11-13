# Brechas de NEXO-Δ AI

El cálculo usa la [matriz de 41 criterios](../estabilizacion-nexo/matriz-final.csv). Persistencia y seguridad pesa 10 % con cinco filas. Estabilidad pesa 10 % con cinco filas. Cada fila de ambas categorías aporta como máximo **2 puntos porcentuales**. La metodología original concede implementación parcial con 0,5 y exige un criterio completo para acreditar funcionamiento.

| Criterio | Categoría | Peso exacto | Implementación pendiente | Funcionamiento pendiente | Qué falta |
|---|---|---:|---:|---:|---|
| SMTP y recuperación | Persistencia y seguridad | 2 pp | 0 pp | 2 pp | El flujo local con Mailpit ya verificó envío, restablecimiento, uso único e inicio de sesión. En el entorno final, configurar un SMTP autorizado con TLS, remitente y dominio verificados. Confirmar entrega externa, enlace público, expiración, uso único y rechazo seguro con SMTP indisponible. |
| Despliegue TLS/secretos/cookies entre hosts | Persistencia y seguridad | 2 pp | 1 pp | 2 pp | HTTPS local acreditado. Completar configuración del dominio y hosts finales, certificados públicos, inyección de secretos y orígenes autorizados. Verificar en navegador la cookie Secure, HttpOnly y SameSite, su alcance, refresh, logout, CORS y rechazo de un origen ajeno. |
| Recuperación desde backup de producción | Estabilidad | 2 pp | 1 pp | 2 pp | La restauración sintética en otro esquema está acreditada. Falta el mecanismo operativo en la infraestructura objetivo. Verificar copia protegida, restauración aislada, comprobación de datos, arranque y recuperación funcional. Medir pérdida de datos y tiempo de recuperación frente a los objetivos acordados. |
| **Total pendiente** | | **6 pp** | **2 pp** | **6 pp** | |

El defecto reproducido de cumplimiento de límites compatibles pertenecía al chat general. Se daba por completa una salida que violaba un límite explícito. La matriz anterior ya acreditaba razonamiento y detección de contradicciones con sus oráculos acordados. La regresión ampliada documenta el defecto y la limitación residual sin sumar puntos ni cambiar los oráculos históricos.

El modelo puede seguir fallando al interpretar condiciones fuera de la gramática y al producir respuestas que cumplan las condiciones que sí reconoce la aplicación. Los límites admitidos se verifican antes de publicar. La carga de dos réplicas conserva evidencia local bajo el criterio original. La validación operativa requiere carga prolongada con turnos largos, usuarios simultáneos y umbrales acordados sobre la infraestructura final.
