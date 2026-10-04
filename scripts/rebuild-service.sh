#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib.sh"

require_docker
kill_java

SERVICE="${1:-}"

if [ -z "$SERVICE" ]; then
    echo "Usage: $0 <service-name>"
    echo "Examples:"
    echo "  $0 api-gateway"
    echo "  $0 discovery-server"
    echo "  $0 frontend"
    exit 1
fi

case "$SERVICE" in
    frontend)
        echo "=== Rebuilding frontend Docker container (no cache) ==="
        docker compose build --no-cache frontend

        echo "=== Restarting frontend ==="
        docker compose up -d --force-recreate --no-deps frontend

        echo "=== Done. Checking status ==="
        docker compose ps frontend
        ;;
    *)
        echo "=== Building $SERVICE with Maven ==="
        mvn clean install -pl "back/$SERVICE" -am -DskipTests

        echo "=== Rebuilding $SERVICE Docker container (no cache) ==="
        docker compose build --no-cache "$SERVICE"

        echo "=== Restarting $SERVICE ==="
        docker compose up -d --force-recreate --no-deps "$SERVICE"

        echo "=== Done. Checking status ==="
        docker compose ps "$SERVICE"
        ;;
esac
