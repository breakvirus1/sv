$SERVICE = "calculator-service"

$projectRoot = Resolve-Path "$PSScriptRoot\.."
Set-Location $projectRoot

try {
    docker info | Out-Null
}
catch {
    Write-Host "Docker daemon is not running. Please start Docker Desktop."
    exit 1
}

Write-Host "=== Building $SERVICE with Maven ==="
mvn clean install -pl "back/$SERVICE" -am -DskipTests

Write-Host "=== Rebuilding $SERVICE Docker container (no cache) ==="
docker compose build --no-cache $SERVICE

Write-Host "=== Restarting $SERVICE ==="
docker compose up -d $SERVICE

Write-Host "=== Done. Checking status ==="
docker compose ps $SERVICE
