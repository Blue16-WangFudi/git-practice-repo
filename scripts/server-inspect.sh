#!/usr/bin/env bash
set -euo pipefail

SSH_HOST="${1:?用法：server-inspect.sh user@host [identity-file]}"
IDENTITY_FILE="${2:-}"
args=()
if [[ -n "$IDENTITY_FILE" ]]; then
  args+=(-i "$IDENTITY_FILE")
fi
ssh "${args[@]}" "$SSH_HOST" 'set -eu
echo "=== 时间 ==="; date
echo "=== CPU / Load ==="; top -b -n 1 | head -n 5 || true
echo "=== 内存 ==="; free -h || true
echo "=== 磁盘 ==="; df -h /
echo "=== Docker 容器 ==="; docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" || true
echo "=== Docker 资源 ==="; docker stats --no-stream || true'
