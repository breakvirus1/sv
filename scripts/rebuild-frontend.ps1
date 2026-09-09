$projectRoot = Resolve-Path "$PSScriptRoot\.."
Set-Location $projectRoot

try {
    docker info | Out-Null
}
catch {
    Write-Host "Docker daemon is not running. Please start Docker Desktop."
    exit 1
}

Write-Host "=== Rebuilding frontend Docker container (no cache) ==="
docker compose build --no-cache frontend

Write-Host "=== Restarting frontend ==="
docker compose up -d frontend

Write-Host "=== Done. Checking status ==="
docker compose ps frontend
