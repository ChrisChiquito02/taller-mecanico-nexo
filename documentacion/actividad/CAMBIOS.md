# Cambios locales de la actividad

Archivos fuente/configuración modificados o añadidos (no se incluyeron secretos ni recursos generados en el parche):

- `.dockerignore`
- `.gitignore`
- `README.md`
- `agente-nexo/Abrir NEXO.command`
- `agente-nexo/GUIA_DE_USO.md`
- `agente-nexo/README.md`
- `agente-nexo/escenario-lectura.json`
- `agente-nexo/escenario-permisos.json`
- `agente-nexo/escenario.json`
- `agente-nexo/nexo.py`
- `agente-nexo/panel/app.js`
- `agente-nexo/panel/index.html`
- `agente-nexo/panel/styles.css`
- `agente-nexo/servidor.py`
- `agente-nexo/test_nexo.py`
- `agente-nexo/test_servidor.py`
- `deploy/Dockerfile.api`
- `deploy/backend.env.example`
- `deploy/dns/api.tu-identificador.json.example`
- `deploy/dns/db.tu-identificador.json.example`
- `deploy/dns/tu-identificador.json.example`
- `deploy/mysql/.env.example`
- `deploy/mysql/.gitignore`
- `deploy/mysql/compose.yml`
- `deploy/mysql/generar-tls.py`
- `deploy/mysql/tls.cnf`
- `deploy/start-api.sh`
- `frontend/.env.example`
- `frontend/README.md`
- `frontend/package.json`
- `frontend/scripts/build.mjs`
- `frontend/src/api-base.js`
- `frontend/src/api-client.js`
- `frontend/tests/e2e/environment.js`
- `frontend/tests/separate-origins.mjs`
- `frontend/tests/unit/api-client.test.js`
- `scripts/verificar.sh`
- `seguridad/README.md`
- `seguridad/ejecutar_restler.py`
- `seguridad/instalar-restler.sh`
- `seguridad/probar_tls.py`
- `sistema-m01-m02/pom.xml`
- `sistema-m01-m02/src/main/java/com/sistemam01m02/config/AppSecurityProperties.java`
- `sistema-m01-m02/src/main/java/com/sistemam01m02/identity/api/AuthController.java`
- `sistema-m01-m02/src/main/java/com/sistemam01m02/identity/security/SecurityConfig.java`
- `sistema-m01-m02/src/main/resources/application.yml`
- `sistema-m01-m02/src/test/java/com/sistemam01m02/identity/security/GuardAndConfigurationTest.java`

Los recursos estáticos Spring se regeneraron explícitamente para pruebas integradas. El build independiente está en frontend/dist, y la imagen API los excluye del JAR. V1/V2 coinciden con los hashes de la línea base histórica, según evidencia/migraciones.json.

No se inicializó Git ni se creó commit: el directorio original no era un repositorio Git. Se conservan copias privadas de los archivos modificados para revisión/recuperación.
