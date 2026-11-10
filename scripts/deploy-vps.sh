#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOST="${VPS_HOST:-82.180.160.106}"
USER="${VPS_USER:-root}"
APP_DIR="${VPS_APP_DIR:-/opt/bank-bookkeeping}"
SSH_KEY="${VPS_SSH_KEY_FILE:-$HOME/.ssh/id_ed25519}"

if [[ ! -f "$SSH_KEY" ]]; then
  echo "SSH key not found: $SSH_KEY" >&2
  echo "Set VPS_SSH_KEY_FILE or create ~/.ssh/id_ed25519" >&2
  exit 1
fi

echo "Deploying $ROOT → ${USER}@${HOST}:${APP_DIR}"
rsync -az --delete \
  -e "ssh -i ${SSH_KEY} -o IdentitiesOnly=yes" \
  --exclude '.git/' \
  --exclude 'target/' \
  --exclude 'var/' \
  --exclude '_temp/' \
  --exclude '.vscode/' \
  --exclude '.DS_Store' \
  "$ROOT/" "${USER}@${HOST}:${APP_DIR}/"

ssh -i "$SSH_KEY" -o IdentitiesOnly=yes "${USER}@${HOST}" \
  "cd '${APP_DIR}' && mkdir -p var && chown -R 10001:10001 var && docker compose up --build -d"

echo "Smoke check login…"
ssh -i "$SSH_KEY" -o IdentitiesOnly=yes "${USER}@${HOST}" \
  "curl -sf -o /dev/null -w 'login=%{http_code}\n' http://127.0.0.1:8086/login"

echo "Done. Public URL: http://${HOST}:8086/login"
