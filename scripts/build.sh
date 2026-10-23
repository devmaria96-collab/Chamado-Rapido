#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
ROOT=$(pwd)
cd frontend
if [ -f package-lock.json ]; then npm ci; else npm install; fi
npm run build
cd "$ROOT"
rm -rf backend/src/main/resources/static
mkdir -p backend/src/main/resources/static
cp -R frontend/dist/chamado-rapido/browser/. backend/src/main/resources/static/
cd backend
mvn clean verify
cd "$ROOT"
mkdir -p executavel
cp backend/target/chamado-rapido.jar executavel/chamado-rapido.jar
printf '\nGerado executavel/chamado-rapido.jar. Compilacao e testes Maven concluidos.\n'
