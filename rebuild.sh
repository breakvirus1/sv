#!/bin/bash
set -e

echo "=== Stopping all containers and removing volumes ==="
docker compose down

echo "=== Building microservices with Maven ==="
mvn clean install

echo "=== Rebuilding Docker containers (no cache) ==="
docker compose build --no-cache

echo "=== Starting all services ==="
docker compose up -d

echo "=== Done. Checking status ==="
docker compose ps
