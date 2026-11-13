#!/bin/sh
set -eu
if [ -n "${DB_CA_CERT_BASE64:-}" ]; then
  umask 077
  printf '%s' "$DB_CA_CERT_BASE64" | base64 -d > /tmp/db-ca.pem
  cp "$JAVA_HOME/lib/security/cacerts" /tmp/identity-cacerts
  keytool -importcert -noprompt -alias examenerik-db -file /tmp/db-ca.pem \
    -keystore /tmp/identity-cacerts -storepass changeit >/dev/null 2>&1
  # changeit protege un almacén de certificados públicos; no es una credencial de BD.
  exec java -XX:MaxRAMPercentage=65 -Djavax.net.ssl.trustStore=/tmp/identity-cacerts \
    -Djavax.net.ssl.trustStorePassword=changeit -jar /app/app.jar
fi
exec java -XX:MaxRAMPercentage=65 -jar /app/app.jar
