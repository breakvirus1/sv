#!/bin/bash
set -e

echo "=== Stopping all containers and removing volumes ==="
docker compose down -v

echo "=== Building microservices with Maven ==="
if [[ "$1" == "--skip-tests" ]]; then
  mvn clean install -DskipTests
else
  mvn clean install
fi

echo "=== Rebuilding Docker containers (no cache) ==="
docker compose build --no-cache

echo "=== Starting all services ==="
docker compose up -d

echo "=== Done. Checking status ==="
docker compose ps
