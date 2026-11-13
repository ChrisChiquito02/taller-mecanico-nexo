#!/bin/zsh
set -eu
export DB_URL='jdbc:mysql://127.0.0.1:43306/nexo_audit?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC'
export DB_USERNAME=nexo_audit DB_PASSWORD=OnlyTestMysql-91
export JWT_SIGNING_KEY=$(printf "audit-only-%.0s" {1..6} | base64)
export SMTP_HOST=127.0.0.1 SMTP_PORT=41025 SMTP_AUTH=false SMTP_STARTTLS=false SMTP_FROM=noreply@example.test
export APP_ALLOWED_ORIGINS=https://localhost:8443 APP_COOKIE_SECURE=true APP_RECOVERY_MODE=development APP_RECOVERY_BASE_URL=https://localhost:8443/reset-password.html
export ALLOWED_EMAIL_DOMAINS=example.test APP_BOOTSTRAP_EMAIL=audit-admin@example.test APP_BOOTSTRAP_PASSWORD='AuditOnly-19!Test'
export NEXO_REQUESTS_PER_MINUTE=60 AUTH_REQUESTS_PER_MINUTE=120 NEXO_BASE_URL=${AUDIT_OLLAMA:-http://127.0.0.1:11434}
exec java -jar "${AUDIT_JAR:-sistema-m01-m02/target/backend-0.1.0-SNAPSHOT.jar}" --spring.profiles.active=local --server.address=127.0.0.1 --server.port=${AUDIT_PORT:-8090} --server.ssl.enabled=true --server.ssl.key-store=file:estabilizacion-nexo/tls/qa.p12 --server.ssl.key-store-password=local-test-only --server.ssl.key-store-type=PKCS12 --app.bootstrap.enabled="${1:-false}"
