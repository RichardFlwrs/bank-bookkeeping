#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8086}"
OUT_DIR="$ROOT/postman"
TEMP_DIR="$ROOT/_temp"
OPENAPI="$TEMP_DIR/openapi.json"
COLLECTION="$OUT_DIR/Bank.postman_collection.json"
ENVIRONMENT="$OUT_DIR/local.postman_environment.json"

COLLECTION_NAME="Spring Bank Bookkeeping"
COLLECTION_DESCRIPTION="$(cat <<'EOF'
Ledger and purchase-walk API for Orquídea Bank (Spring Boot).

**Setup**
1. Import this collection and `local.postman_environment.json`.
2. Select environment **Orquídea Bank — Local**.
3. Demo card token: `card_ada_orquidea` (Ada, deposit `2100`). Merchant settlement always credits Café Orquídea (`2200`). Fee is `1.50` when amount ≥ `40.00`, else `0.00`.
4. Send a unique `Idempotency-Key` per purchase; the same key returns the original trail with no second walk.

Suggested first call: **POST /api/purchases** with body `{"cardToken":"{{cardToken}}","amount":"{{amount}}"}` and header `Idempotency-Key: {{idempotencyKey}}`.
EOF
)"

mkdir -p "$OUT_DIR" "$TEMP_DIR"

echo "Fetching OpenAPI from $BASE_URL/v3/api-docs ..."
curl -sf "$BASE_URL/v3/api-docs" -o "$OPENAPI"

echo "Converting OpenAPI → Postman collection ..."
npx --yes openapi-to-postmanv2 \
  -s "$OPENAPI" \
  -o "$COLLECTION" \
  -p

echo "Setting collection name, description, and env placeholders ..."
COLLECTION_NAME="$COLLECTION_NAME" \
COLLECTION_DESCRIPTION="$COLLECTION_DESCRIPTION" \
python3 - "$COLLECTION" "$ENVIRONMENT" <<'PY'
import json, os, re, sys

collection_path, environment_path = sys.argv[1], sys.argv[2]

with open(collection_path, encoding="utf-8") as f:
    data = json.load(f)

with open(environment_path, encoding="utf-8") as f:
    environment = json.load(f)

env_keys = {item["key"] for item in environment.get("values", []) if item.get("key")}

# Header names that do not match env keys 1:1
HEADER_TO_ENV = {
    "Idempotency-Key": "idempotencyKey",
}

PLACEHOLDER = "<string>"
BODY_FIELD_RE = re.compile(
    r'("(?P<field>[^"]+)"\s*:\s*)"' + re.escape(PLACEHOLDER) + r'"'
)


def env_ref(key):
    return "{{" + key + "}}"


def map_header_key(header_key):
    if header_key in HEADER_TO_ENV:
        mapped = HEADER_TO_ENV[header_key]
        return mapped if mapped in env_keys else None
    return header_key if header_key in env_keys else None


def fill_headers(headers):
    if not isinstance(headers, list):
        return
    for header in headers:
        if not isinstance(header, dict):
            continue
        if header.get("value") != PLACEHOLDER:
            continue
        mapped = map_header_key(header.get("key", ""))
        if mapped:
            header["value"] = env_ref(mapped)


def fill_named_params(params):
    if not isinstance(params, list):
        return
    for param in params:
        if not isinstance(param, dict):
            continue
        if param.get("value") != PLACEHOLDER:
            continue
        key = param.get("key")
        if key in env_keys:
            param["value"] = env_ref(key)


def fill_body(body):
    if not isinstance(body, dict):
        return
    raw = body.get("raw")
    if not isinstance(raw, str) or PLACEHOLDER not in raw:
        return

    def repl(match):
        field = match.group("field")
        if field in env_keys:
            return f'{match.group(1)}"{env_ref(field)}"'
        return match.group(0)

    body["raw"] = BODY_FIELD_RE.sub(repl, raw)


def fill_request(request):
    if not isinstance(request, dict):
        return
    fill_headers(request.get("header"))
    fill_body(request.get("body"))
    url = request.get("url")
    if isinstance(url, dict):
        fill_named_params(url.get("query"))
        fill_named_params(url.get("variable"))


def walk(node):
    if isinstance(node, list):
        for item in node:
            walk(item)
        return
    if not isinstance(node, dict):
        return

    if "request" in node:
        fill_request(node["request"])
    if "originalRequest" in node:
        fill_request(node["originalRequest"])

    for value in node.values():
        if isinstance(value, (dict, list)):
            walk(value)


info = data.setdefault("info", {})
info["name"] = os.environ["COLLECTION_NAME"]
info["description"] = os.environ["COLLECTION_DESCRIPTION"]
walk(data)

with open(collection_path, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)
    f.write("\n")
PY

echo "Wrote $COLLECTION"
echo "Import postman/Bank.postman_collection.json and postman/local.postman_environment.json in Postman."
