#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
command -v docker >/dev/null 2>&1 || { echo "Instale Docker + Compose v2. Consulte README.md."; exit 1; }
docker info >/dev/null 2>&1 || { echo "Docker nao esta em execucao."; exit 1; }
docker compose version >/dev/null
if [ ! -f .env ]; then cp .env.example .env; echo "Criado .env de DEMONSTRACAO LOCAL."; fi
docker compose up --build -d --wait --wait-timeout 180
printf '\nAbra http://localhost:8080 (ou APP_PORT do .env).\n'
