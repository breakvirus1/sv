#!/bin/bash
# Waits until the docker-compose stack is ready for E2E tests:
# frontend serves the SPA, Keycloak discovery responds through the frontend proxy,
# and the API gateway actuator reports UP.
set -e

BASE_URL="${1:-http://localhost:5174}"
GATEWAY_URL="${2:-http://localhost:8085}"
MAX_ATTEMPTS="${3:-120}"

echo "=== Waiting for stack readiness (max ${MAX_ATTEMPTS} x 5s) ==="

for i in $(seq 1 "$MAX_ATTEMPTS"); do
  # 1) frontend SPA
  if ! curl -sf -o /dev/null "$BASE_URL/"; then
    echo "attempt $i: frontend not up yet"
    sleep 5
    continue
  fi

  # 2) Keycloak OIDC discovery through the frontend reverse proxy
  if ! curl -sf -o /dev/null "$BASE_URL/realms/print-sv/.well-known/openid-configuration"; then
    echo "attempt $i: Keycloak discovery not ready"
    sleep 5
    continue
  fi

  # 3) gateway health
  HEALTH=$(curl -sf "$GATEWAY_URL/actuator/health" 2>/dev/null || true)
  if echo "$HEALTH" | grep -q '"status":"UP"'; then
    echo "=== Stack is READY (attempt $i) ==="
    exit 0
  fi

  echo "attempt $i: gateway not ready"
  sleep 5
done

echo "=== Stack did NOT become ready in time ==="
docker compose ps || true
exit 1
