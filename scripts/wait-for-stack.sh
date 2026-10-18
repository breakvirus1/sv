#!/bin/bash
# Waits until the docker-compose stack is ready for E2E tests:
# frontend serves the SPA, Keycloak issues tokens, gateway routes authenticated API calls.
set -e

BASE_URL="${1:-http://localhost:5174}"
KC_URL="${2:-http://localhost:8080}"
MAX_ATTEMPTS="${3:-60}"

echo "=== Waiting for stack readiness (max ${MAX_ATTEMPTS} x 5s) ==="

for i in $(seq 1 "$MAX_ATTEMPTS"); do
  # 1) frontend SPA
  if curl -sf -o /dev/null "$BASE_URL/"; then
    # 2) Keycloak token
    TOKEN=$(curl -sf -X POST "$KC_URL/realms/print-sv/protocol/openid-connect/token" \
      -H 'Content-Type: application/x-www-form-urlencoded' \
      -d 'grant_type=password' \
      -d 'client_id=frontend' \
      -d 'username=admin' \
      -d 'password=admin' 2>/dev/null \
      | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p' || true)

    if [ -n "$TOKEN" ]; then
      # 3) authenticated API call through the frontend proxy
      CODE=$(curl -s -o /dev/null -w '%{http_code}' \
        -H "Authorization: Bearer $TOKEN" \
        "$BASE_URL/api/v1/orders?page=0&size=1")
      if [ "$CODE" = "200" ]; then
        echo "=== Stack is READY (attempt $i) ==="
        exit 0
      fi
      echo "attempt $i: API returned $CODE"
    else
      echo "attempt $i: no Keycloak token yet"
    fi
  else
    echo "attempt $i: frontend not up yet"
  fi
  sleep 5
done

echo "=== Stack did NOT become ready in time ==="
docker compose ps || true
exit 1
