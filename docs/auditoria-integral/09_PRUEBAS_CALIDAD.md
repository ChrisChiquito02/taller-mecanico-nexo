# Pruebas y calidad

## Inventario
No existe `src/test`, archivos de pruebas, reportes Surefire/Failsafe, cobertura, colecciones API, pruebas frontend ni pruebas de seguridad. Cantidad de pruebas localizadas: **0**.

| Conjunto | Herramienta/dependencia | Objetivo previsto | Resultado documentado | Ejecución de auditoría |
|---|---|---|---|---|
| Unitarias/integración backend | `spring-boot-starter-test`, `spring-security-test` | no hay clases que especifiquen objetivo | ninguno | no ejecutada |
| Integración MySQL | Testcontainers MySQL/JUnit Jupiter | no hay clases que especifiquen objetivo | ninguno | no ejecutada |
| Frontend/API/seguridad | no encontrado | no definido | ninguno | no ejecutada |

## Validación realizada
No se ejecutó Maven ni pruebas porque la instrucción autoriza escritura sólo en `docs/auditoria-integral/`; una compilación crearía `target/`. Sí se hicieron comprobaciones no persistentes: inventario de fuentes, búsqueda de marcadores incompletos y HTTP sin credenciales. No se encontraron `TODO`, `FIXME`, `XXX` ni `throw new` en los archivos auditados. Esto no sustituye pruebas.

Dos GET sin autenticación al backend desplegado devolvieron `401`; no son pruebas funcionales ni de salud. No hay cobertura disponible.

## Recomendación
Añadir pruebas unitarias para `AppUser` y `UuidV7Generator`, integración de migraciones/repositorios con Testcontainers y pruebas de seguridad/endpoints una vez implementados. Ejecutarlas en un entorno aislado que respete las variables de configuración.
