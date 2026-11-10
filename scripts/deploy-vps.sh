#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOST="${VPS_HOST:-82.180.160.106}"
USER="${VPS_USER:-root}"
APP_DIR="${VPS_APP_DIR:-/opt/bank-bookkeeping}"
COMPOSE_FILE="${VPS_COMPOSE_FILE:-docker-compose.yml}"
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
  --exclude '.env' \
  --exclude '_temp/' \
  --exclude '.vscode/' \
  --exclude '.DS_Store' \
  "$ROOT/" "${USER}@${HOST}:${APP_DIR}/"

"${SSH[@]}" "${USER}@${HOST}" \
  "cd '${APP_DIR}' && docker compose -f '${COMPOSE_FILE}' up --build -d"

echo "Waiting for /login…"
"${SSH[@]}" "${USER}@${HOST}" bash -s <<EOF
set -euo pipefail
last_err=
for i in \$(seq 1 7); do
  err_file=\$(mktemp)
  code=\$(curl -sS -o /dev/null -w '%{http_code}' --connect-timeout 5 --max-time 10 http://127.0.0.1:8086/login 2>"\$err_file" || true)
  code=\${code:-000}
  echo "try \$i login=\$code"
  if [ "\$code" = "200" ] || [ "\$code" = "302" ]; then
    rm -f "\$err_file"
    echo "App is up"
    exit 0
  fi
  last_err=\$(cat "\$err_file")
  rm -f "\$err_file"
  if [ "\$i" -lt 7 ]; then
    sleep 2
  fi
done
echo "Failed waiting for /login"
if [ -n "\$last_err" ]; then
  printf '%s\n' "\$last_err"
fi
cd '${APP_DIR}'
docker compose -f '${COMPOSE_FILE}' ps || true
docker compose -f '${COMPOSE_FILE}' logs --tail 80 api || true
exit 1
EOF

echo "Done. Public URL: http://${HOST}:8086/login"
