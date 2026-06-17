#!/bin/bash
set -e

echo "=== Rebuilding frontend Docker container (no cache) ==="
docker compose build --no-cache frontend

echo "=== Restarting frontend ==="
docker compose up -d frontend

echo "=== Done. Checking status ==="
docker compose ps frontend
