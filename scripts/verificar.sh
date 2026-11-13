#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
# Ejecutar secuencialmente: todas las verificaciones usan el mismo target de construcción.
npm --prefix frontend ci
npm --prefix frontend run check
npm --prefix frontend run build:integrated
sistema-m01-m02/scripts/verify-java21.sh
python3 sistema-m01-m02/scripts/smoke-isolated.py
npm --prefix frontend run test:e2e
python3 scripts/verificar-documentacion.py
