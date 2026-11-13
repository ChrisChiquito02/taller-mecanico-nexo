# ExamenErik

Identidad integrada con Java 21, Spring Boot 4.1.1, MySQL, JWT, Argon2id, RBAC y recuperación SMTP. Frontend HTML/CSS/JavaScript modular con build independiente; modo integrado opcional para desarrollo.

- [NEXO-Δ AI integrado: manual, arquitectura, pruebas y exposición](documentacion/nexo-ai/README.md)
- [Actividad universitaria: publicación gratuita, NEXO-Δ, RESTler y exposición](documentacion/actividad/README.md)
- [Orquestador principal](orquestador.md)
- [Documentador Frontend](documentacion/frontend/documentadorFrontend.md)
- [Documentador Backend](documentacion/backend/documentadorBackend.md)
- [Informe final A–L](documentacion/auditoria/INFORME_FINAL.md), [matriz](documentacion/auditoria/MATRIZ_INTEGRACION.md) y [OpenAPI](docs/integracion/openapi.json)

Desde la raíz, con Node 24+, Docker/Compose, Python 3 y OpenSSL:

```sh
npm --prefix frontend ci
npm --prefix frontend run check
npm --prefix frontend run build:integrated
sistema-m01-m02/scripts/verify-java21.sh
cd sistema-m01-m02
./scripts/init-local.sh
docker compose --env-file .env.local up --build -d
```

Interfaz/API: http://localhost:8090 · Buzón local: http://localhost:8026.

Pruebas completas, tras instalar Chromium con `cd frontend && npx playwright install chromium`: `./scripts/verificar.sh`. Con Chrome instalado puede usarse `PW_CHANNEL=chrome ./scripts/verificar.sh`.

Registro público responde 202 genérico para proteger existencia de cuentas. El administrador inicial se crea únicamente mediante bootstrap explícito y sin credenciales predeterminadas; consulta el orquestador antes de registrar usuarios.

No usar datos reales en pruebas ni editar recursos estáticos generados. Los informes anteriores de `docs/` se conservan como antecedentes.
