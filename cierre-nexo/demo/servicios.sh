#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
root=$(cd ../.. && pwd)
case "${1:-}" in
  estado) args=(ps) ;;
  registros) args=(logs --tail=80 backend mysql mailpit) ;;
  reiniciar-java) args=(restart backend) ;;
  reiniciar-mysql) args=(restart mysql) ;;
  detener) args=(stop backend mysql mailpit) ;;
  *) echo 'Uso: servicios.sh estado|registros|reiniciar-java|reiniciar-mysql|detener' >&2; exit 2 ;;
esac
if [[ ! -f .demo.env ]]; then echo 'Primero ejecuta iniciar.sh.' >&2; exit 1; fi
exec docker compose -p examenerik-nexo-demo-cierre --env-file .demo.env \
  -f "$root/deploy/nexo-local.yml" -f "$PWD/compose.override.yml" "${args[@]}"
