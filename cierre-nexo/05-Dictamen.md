# Dictamen independiente

## Uso académico

**Apto para una demostración local con alcance definido.** La batería de 47 casos, el chat con inferencia real, las referencias, los adjuntos, la simulación, la cancelación y la memoria tienen evidencia. Las solicitudes con restricciones verificadas pueden recibir una respuesta de incumplimiento si Ollama agota el reintento. El guion incluye ese resultado como parte observable de la demostración. [Inicio, guion y recuperación](04-Demostracion.md).

## Producción

**Pendiente de aceptación operativa.** Los resultados locales acreditan **98 % de implementación y 94 % de funcionamiento verificado** sobre el alcance original. Quedan tres criterios parciales de 2 pp cada uno. Requieren dominio y topología definitivos, certificados y secretos de despliegue, SMTP externo autorizado y restauración de una copia bajo la infraestructura objetivo. También falta una campaña de carga prolongada con turnos largos y objetivos de servicio acordados. [Brechas y aceptación](01-Brechas.md).

El ensayo de carga inicial observó errores 500 al crear chats en una réplica. El problema dejó de observarse tras reiniciar con el paquete final y repetir la carga. No se determinó la causa raíz. La prueba local de dos réplicas acredita el escenario repetido y no descarta una recurrencia. [Incidencia](evidencias/carga-incidencia.json) · [Repetición](evidencias/carga.json).

Los servicios de infraestructura final y los datos de producción quedaron fuera de esta ejecución.
