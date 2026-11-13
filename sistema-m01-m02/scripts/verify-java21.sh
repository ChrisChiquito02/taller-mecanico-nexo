#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
# MySQL Testcontainers usa puertos aleatorios del host Docker; nunca la base configurada en .env.
docker run --rm \
  -v "$PWD:/workspace" \
  -v "${HOME}/.m2:/root/.m2" \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
  --add-host=host.docker.internal:host-gateway \
  -w /workspace eclipse-temurin:21-jdk ./mvnw -B -ntp clean verify
