#!/usr/bin/env bash
# Export only exception type and stack locations for operational sharing.
set -euo pipefail
java_home=$(/usr/libexec/java_home)
"$java_home/bin/jfr" print --events jdk.JavaExceptionThrow --stack-depth 12 "$1" |
  sed -E 's/^  message = .*$/  message = [omitido]/'
