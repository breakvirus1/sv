#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

require_docker() {
    if ! docker info >/dev/null 2>&1; then
        echo "Error: Docker is not running or not accessible."
        exit 1
    fi
}

kill_java() {
    echo "=== Stopping running project Java processes to release file locks ==="
    pkill -f 'exec\.jar' || true
    sleep 3
}

build_frontend() {
    echo "=== Building frontend (npm run build) ==="
    (cd "$PROJECT_ROOT/front" && npm install && npm run build)
}
