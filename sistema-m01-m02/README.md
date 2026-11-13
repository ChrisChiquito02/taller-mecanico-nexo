# sistema-m01-m02

[Orquestador](../orquestador.md) · [Documentador Backend](../documentacion/backend/documentadorBackend.md) · [Frontend](../documentacion/frontend/documentadorFrontend.md).

Construir primero las fuentes frontend desde la raíz: `npm --prefix frontend ci` y `npm --prefix frontend run check`. Desde esta carpeta:

```sh
./scripts/verify-java21.sh
./scripts/init-local.sh
docker compose --env-file .env.local up --build -d
```

Alternativa con Java 21+ instalado: `./mvnw -B -ntp verify`. API/UI localhost:8090, Mailpit localhost:8026. No hay administrador predeterminado. Variables en `.env.example`, sin carga automática. La verificación usa MySQL desechable, no `.env.local`.
