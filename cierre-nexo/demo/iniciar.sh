#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
root=$(cd ../.. && pwd)
shasum -a 256 -c nexo-demo.sha256
envfile="$PWD/.demo.env"
if [[ ! -f "$envfile" ]]; then
  (
    umask 077
    set -o noclobber
    {
      printf 'DB_PASSWORD=%s\n' "$(openssl rand -hex 24)"
      printf 'MYSQL_ROOT_PASSWORD=%s\n' "$(openssl rand -hex 24)"
      printf 'JWT_SIGNING_KEY=%s\n' "$(openssl rand -base64 48)"
      printf 'DEMO_PASSWORD=%s\n' "$(openssl rand -hex 16)"
    } > "$envfile"
  )
fi
compose=(docker compose -p examenerik-nexo-demo-cierre --env-file "$envfile" -f "$root/deploy/nexo-local.yml" -f "$PWD/compose.override.yml")
if lsof -nP -iTCP:8090 -sTCP:LISTEN >/dev/null 2>&1 && [[ -z "$("${compose[@]}" ps -q backend)" ]]; then
  echo 'El puerto 8090 está ocupado. Libéralo antes de iniciar la demostración.' >&2
  exit 1
fi
if ! curl -fsS --max-time 4 http://127.0.0.1:11434/api/tags | grep -q '"qwen2.5-coder:7b"'; then
  echo 'Abre Ollama y comprueba que qwen2.5-coder:7b esté instalado.' >&2
  exit 1
fi
docker build -t examenerik-nexo-demo-cierre:local -f - . <<'DOCKER'
FROM eclipse-temurin:21-jdk
RUN groupadd --system app && useradd --system --gid app --home-dir /app app
WORKDIR /app
COPY --chown=app:app nexo-demo.jar app.jar
USER app
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
DOCKER
"${compose[@]}" up -d --wait mysql mailpit
mysql() {
  "${compose[@]}" exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot -NB -e "$1"' sh "$1"
}
export NEXO_BOOTSTRAP_ENABLED=false
exists=$(mysql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='nexo_local' AND table_name='app_user'")
if [[ "$exists" == 0 ]] || [[ "$(mysql 'SELECT COUNT(*) FROM nexo_local.app_user')" == 0 ]]; then
  export NEXO_BOOTSTRAP_ENABLED=true
fi
"${compose[@]}" up -d backend
for ((i=0; i<60; i++)); do
  if curl -fsS http://localhost:8090/api/health >/dev/null 2>&1; then
    if [[ "$NEXO_BOOTSTRAP_ENABLED" == true ]]; then
      # Wait for the first account before confirming that the environment is ready.
      if [[ ! "$(mysql 'SELECT COUNT(*) FROM nexo_local.app_user' 2>/dev/null || true)" =~ ^[1-9][0-9]*$ ]]; then sleep 1; continue; fi
      export NEXO_BOOTSTRAP_ENABLED=false
      "${compose[@]}" up -d backend
      sleep 1
      continue
    fi
    echo 'NEXO listo en http://localhost:8090/#nexo'
    echo 'Cuenta: demo-admin@example.test. Consulta DEMO_PASSWORD en demo/.demo.env.'
    exit 0
  fi
  sleep 1
done
"${compose[@]}" logs --tail=50 backend mysql >&2
exit 1
