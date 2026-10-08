#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib.sh"

require_docker
kill_java

SERVER_IP="${1:-${SERVER_IP:-192.168.1.40}}"
ENV="${2:-production}"

echo "=== Starting deployment for SERVER_IP=$SERVER_IP ENV=$ENV ==="

echo "=== Building backend with Maven ==="
mvn clean install -DskipTests

echo "=== Building frontend ==="
cd "$(dirname "$SCRIPT_DIR")/front"
npm install
npm run build
cd "$SCRIPT_DIR/.."

echo "=== Rebuilding all Docker containers ==="
SERVER_IP="$SERVER_IP" docker compose build --no-cache

echo "=== Running database migrations ==="
docker compose up -d postgres keycloak discovery-server
sleep 10

echo "=== Starting all services ==="
SERVER_IP="$SERVER_IP" docker compose up -d --force-recreate

echo "=== Waiting for services to start ==="
sleep 30

echo "=== Checking service health ==="
docker compose ps

echo "=== Deployment completed ==="
echo "Frontend: http://$SERVER_IP:5174"
echo "API Gateway: http://$SERVER_IP:8085"
echo "Keycloak: http://$SERVER_IP:8080"
echo "Admin panel: http://$SERVER_IP:5174/admin"
echo "Admin dashboard: http://$SERVER_IP:5174/admin-dashboard"
