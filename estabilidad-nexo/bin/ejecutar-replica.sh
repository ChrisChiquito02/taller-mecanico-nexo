#!/usr/bin/env bash
# Uses only the synthetic audit database configured by the existing runner.
set -euo pipefail
cd "$(dirname "$0")/../.."
port="${1:?Puerto de prueba}"
source_jar="${2:?JAR de origen}"
case "$port" in 8090|8091) ;; *) exit 2 ;; esac
# Each process opens its own snapshot. A rebuild can replace the source without touching it.
snapshot=$(mktemp -d "${TMPDIR:-/tmp}/nexo-replica.XXXXXX")
cp "$source_jar" "$snapshot/backend.jar"
chmod 400 "$snapshot/backend.jar"
unzip -tq "$snapshot/backend.jar" >/dev/null
shasum -a 256 "$snapshot/backend.jar"
export AUDIT_PORT="$port" AUDIT_JAR="$snapshot/backend.jar"
export AUDIT_OLLAMA="${AUDIT_OLLAMA:-http://127.0.0.1:41434}"
exec zsh verificacion-nexo/entorno.sh false
