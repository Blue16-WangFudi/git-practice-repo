#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="${1:-/mnt/d/学习/sentinel-monitor}"

cd "$PROJECT_DIR"
test -f docker-compose.yml
command -v docker >/dev/null
docker compose config -q

printf 'PROJECT=%s\n' "$PROJECT_DIR"
printf 'JAVA='
java -version 2>&1 | head -n 1 || true
printf 'DOCKER='
docker version --format '{{.Client.Version}}' 2>/dev/null
printf 'COMPOSE_SERVICES\n'
docker compose ps --format '{{.Service}}={{.State}}'
