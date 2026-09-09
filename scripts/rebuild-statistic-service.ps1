param(
    [string]$Service = "statistic-service"
)

$projectRoot = Resolve-Path "$PSScriptRoot\.."
Set-Location $projectRoot

try {
    docker info | Out-Null
}
catch {
    Write-Host "Docker daemon is not running. Please start Docker Desktop."
    exit 1
}

if ($Service -eq "all") {
    Write-Host "=== Building all services with Maven ==="
    mvn clean install -DskipTests

    Write-Host "=== Rebuilding all Docker containers (no cache) ==="
    docker compose build --no-cache

    Write-Host "=== Restarting all services ==="
    docker compose up -d --force-recreate

    Write-Host "=== Done. Checking status ==="
    docker compose ps
}
else {
    Write-Host "=== Building $Service with Maven ==="
    mvn clean install -pl "back/$Service" -am -DskipTests

    Write-Host "=== Rebuilding $Service Docker container (no cache) ==="
    docker compose build --no-cache $Service

    Write-Host "=== Restarting $Service ==="
    docker compose up -d --force-recreate --no-deps $Service

    Write-Host "=== Done. Checking status ==="
    docker compose ps $Service
}
