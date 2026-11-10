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

SSH=(ssh -i "$SSH_KEY" -o IdentitiesOnly=yes)

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

"${SSH[@]}" "${USER}@${HOST}" \
  "cd '${APP_DIR}' && mkdir -p var && chown -R 10001:10001 var && docker compose up --build -d"

echo "Waiting for /login (seed can take a minute)…"
"${SSH[@]}" "${USER}@${HOST}" bash -s <<EOF
set -euo pipefail
for i in \$(seq 1 90); do
  code=\$(curl -sS -o /dev/null -w '%{http_code}' http://127.0.0.1:8086/login 2>/dev/null || echo 000)
  echo "try \$i login=\$code"
  if [ "\$code" = "200" ] || [ "\$code" = "302" ]; then
    echo "App is up"
    exit 0
  fi
  sleep 2
done
echo "Timed out waiting for /login"
cd '${APP_DIR}'
docker compose ps || true
docker compose logs --tail 80 api || true
exit 1
EOF

echo "Done. Public URL: http://${HOST}:8086/login"
