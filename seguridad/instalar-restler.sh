#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
# Uso opcional; no instala Python/.NET globalmente ni sobrescribe directorios.
command -v python3.12 >/dev/null || { echo "Instala Python 3.12 según documentación oficial y vuelve a ejecutar"; exit 1; }
command -v dotnet >/dev/null || { echo "Se requiere .NET SDK 8"; exit 1; }
test ! -e work/restler-fuzzer || { echo "work/restler-fuzzer ya existe; conservar o mover explícitamente antes de repetir"; exit 1; }
mkdir -p work
python3.12 -m venv work/restler-venv
. work/restler-venv/bin/activate
git clone --no-checkout https://github.com/microsoft/restler-fuzzer.git work/restler-fuzzer
git -C work/restler-fuzzer checkout --detach 6d984deedbc54aad957fa3da0c7e9e5df23a2aee
python work/restler-fuzzer/build-restler.py --dest_dir "$PWD/work/restler-bin"
echo "Compilado. Activa work/restler-venv antes de ejecutar el laboratorio para que RESTler encuentre Python 3.12."
