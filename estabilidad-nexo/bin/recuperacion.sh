#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
# Only the explicitly named synthetic audit container is restarted.
docker restart examenerik-audit-mysql
for ((i=0;i<25;i++)); do
  a=$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 http://localhost:8090/api/health || true)
  b=$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 http://localhost:8091/api/health || true)
  printf '%s 8090=%s 8091=%s\n' "$(date '+%T')" "$a" "$b"
  if [[ "$a" = 200 && "$b" = 200 ]]; then exit 0; fi
  sleep 1
done
exit 1
