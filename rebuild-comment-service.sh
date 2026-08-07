#!/bin/bash
set -e

echo "=== Building comment-service with Maven ==="
mvn clean package -pl back/comment-service -am -DskipTests

echo "=== Rebuilding comment-service Docker container (no cache) ==="
docker compose build comment-service

echo "=== Restarting comment-service ==="
docker compose up -d comment-service

echo "=== Done. Checking status ==="
docker compose ps
