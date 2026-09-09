$projectRoot = Resolve-Path "$PSScriptRoot\.."
Set-Location $projectRoot

try {
    docker info | Out-Null
}
catch {
    Write-Host "Docker daemon is not running. Please start Docker Desktop."
    exit 1
}

Write-Host "=== Stopping all containers and removing volumes ==="
docker compose down

Write-Host "=== Building microservices with Maven ==="
mvn clean install

Write-Host "=== Rebuilding Docker containers (no cache) ==="
docker compose build --no-cache

Write-Host "=== Starting all services ==="
docker compose up -d

Write-Host "=== Done. Checking status ==="
docker compose ps
