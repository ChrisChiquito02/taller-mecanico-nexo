#!/bin/sh
set -eu
cd "$(dirname "$0")"
if ! command -v python3 >/dev/null 2>&1; then
 echo 'NEXO necesita Python 3. Consulta GUIA_DE_USO.md.'
 read -r respuesta
 exit 1
fi
if ! python3 launcher.py; then
 echo 'Pulsa Intro para cerrar después de revisar el mensaje.'
 read -r respuesta
 exit 1
fi
