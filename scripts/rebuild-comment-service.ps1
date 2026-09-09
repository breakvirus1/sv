$projectRoot = Resolve-Path "$PSScriptRoot\.."
Set-Location $projectRoot

try {
    docker info | Out-Null
}
catch {
    Write-Host "Docker daemon is not running. Please start Docker Desktop."
    exit 1
}

Write-Host "=== Building comment-service with Maven ==="
mvn clean package -pl back/comment-service -am -DskipTests

Write-Host "=== Rebuilding comment-service Docker container (no cache) ==="
docker compose build --no-cache comment-service

Write-Host "=== Restarting comment-service ==="
docker compose up -d comment-service

Write-Host "=== Done. Checking status ==="
docker compose ps
