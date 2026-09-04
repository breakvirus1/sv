#!/bin/bash
set -e

SERVICE="${1:-all}"

if [ "$SERVICE" = "all" ]; then
    echo "=== Building all services with Maven ==="
    mvn clean install -DskipTests

    echo "=== Rebuilding all Docker containers (no cache) ==="
    docker compose build --no-cache

    echo "=== Recreating all services ==="
    docker compose up -d --force-recreate

    echo "=== Done. Checking status ==="
    docker compose ps
else
    echo "=== Building $SERVICE with Maven ==="
    mvn clean install -pl back/$SERVICE -am -DskipTests

    echo "=== Rebuilding $SERVICE Docker container (no cache) ==="
    docker compose build --no-cache $SERVICE

    echo "=== Restarting $SERVICE ==="
    docker compose up -d --force-recreate --no-deps $SERVICE

    echo "=== Done. Checking status ==="
    docker compose ps $SERVICE
fi
