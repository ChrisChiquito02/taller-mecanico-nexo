#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if [ -e .env.local ]; then
  echo '.env.local ya existe; se conserva sin cambios.'
  exit 0
fi
umask 077
# noclobber impide sobrescrituras incluso si hay ejecuciones simultáneas.
set -C
{
  printf 'DB_PASSWORD=%s\n' "$(openssl rand -hex 24)"
  printf 'MYSQL_ROOT_PASSWORD=%s\n' "$(openssl rand -hex 24)"
  printf 'JWT_SIGNING_KEY=%s\n' "$(openssl rand -base64 48)"
  printf 'APP_PORT=8090\nAPP_BOOTSTRAP_ENABLED=false\n'
} > .env.local
echo 'Creado .env.local con secretos locales. No lo compartas ni lo versiones.'
