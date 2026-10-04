#!/usr/bin/env bash
set -euo pipefail

SSH_HOST="${1:?用法：ssh-tunnel.sh user@host [identity-file] [local-port] [remote-port]}"
IDENTITY_FILE="${2:-}"
LOCAL_PORT="${3:-8088}"
REMOTE_PORT="${4:-8088}"

args=(-N -L "${LOCAL_PORT}:127.0.0.1:${REMOTE_PORT}")
if [[ -n "$IDENTITY_FILE" ]]; then
  args+=(-i "$IDENTITY_FILE")
fi
args+=("$SSH_HOST")
echo "建立 SSH Tunnel：本机 ${LOCAL_PORT} -> 服务器 127.0.0.1:${REMOTE_PORT}"
exec ssh "${args[@]}"
