#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib.sh"

require_docker
kill_java

echo "=== Stopping all containers and removing volumes ==="
docker compose down

echo "=== Building microservices with Maven ==="
mvn clean install -DskipTests

echo "=== Rebuilding Docker containers (no cache) ==="
docker compose build --no-cache

echo "=== Starting all services ==="
docker compose up -d

echo "=== Done. Checking status ==="
docker compose ps
