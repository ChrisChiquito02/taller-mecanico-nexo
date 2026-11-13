# Frontend de ExamenErik

Fuentes HTML/CSS/JavaScript modular en `src/` y `public/`, compiladas con esbuild.

```sh
npm ci
npm run check
# Publicación independiente: dist, con API HTTPS fija durante el build
APP_API_BASE_PATH=https://api.example.test/api npm run build
# Desarrollo y E2E anteriores: copia explícita a recursos Spring
npm run build:integrated
```

`APP_API_BASE_PATH` es pública y no se carga automáticamente desde .env. Nunca incluir claves. El cliente mantiene Bearer en memoria, usa `credentials: include` y admite /api o una URL HTTPS validada sin usuario, contraseña, fragmento ni query. El backend debe permitir exactamente el origen del frontend.

`npm run test:e2e` requiere Docker y un JAR integrado construido después de `build:integrated`; crea y elimina MySQL/Mailpit/API sintéticos. `PW_CHANNEL=chrome` usa Chrome instalado. `node tests/separate-origins.mjs` verifica el build independiente en dos orígenes HTTPS locales del mismo sitio; solo su navegador acepta el certificado autofirmado de prueba.

Consultar [publicación gratuita](../documentacion/actividad/PUBLICACION_GRATUITA.md).
