# Migraciones

V1 crea el modelo original en una base vacía, sin borrar datos. V2 agrega sesiones/tokens y semillas RBAC. Hibernate valida el esquema después de Flyway.

`baseline-on-migrate=false` impide adoptar por accidente una base existente; `clean-disabled=true` impide limpieza Flyway. Las migraciones desplegadas son inmutables; nuevas modificaciones comienzan en V3.

La política anterior suponía una base inicial externa no entregada. Para adoptarla debe compararse primero una copia aislada con V1 (incluidos índices únicos, claves y tipos), revisar V2 y aprobar un baseline explícito de versión 1. El proyecto no realiza ese paso automáticamente y esta implementación no tocó ninguna base existente.

Véase [orquestador.md](../../../../../../orquestador.md) para integración y operación.
