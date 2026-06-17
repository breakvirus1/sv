#!/bin/bash
set -e

SERVICE="material-service"

echo "=== Building $SERVICE with Maven ==="
mvn clean install -pl back/$SERVICE -am -DskipTests

echo "=== Rebuilding $SERVICE Docker container (no cache) ==="
docker compose build --no-cache $SERVICE

echo "=== Restarting $SERVICE ==="
docker compose up -d $SERVICE

echo "=== Done. Checking status ==="
docker compose ps $SERVICE
