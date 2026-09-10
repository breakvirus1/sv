param(
    [string]$Service = "all"
)

$projectRoot = Resolve-Path "$PSScriptRoot\.."
Set-Location $projectRoot

function Test-DockerRunning {
    $output = docker info 2>&1
    return $LASTEXITCODE -eq 0
}

function Start-DockerDesktop {
    $dockerDesktop = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    if (-not (Test-Path $dockerDesktop)) {
        Write-Host "Docker Desktop not found at $dockerDesktop"
        exit 1
    }

    Write-Host "Starting Docker Desktop..."
    Start-Process -FilePath $dockerDesktop

    Write-Host "Waiting for Docker daemon to be ready..."
    $maxWait = 120
    $waited = 0
    while (-not (Test-DockerRunning) -and $waited -lt $maxWait) {
        Start-Sleep -Seconds 5
        $waited += 5
        Write-Host "  Waiting... ($waited seconds)"
    }

    if (-not (Test-DockerRunning)) {
        Write-Host "Docker Desktop did not start within $maxWait seconds. Please start it manually."
        exit 1
    }

    Write-Host "Docker is ready."
}

if (-not (Test-DockerRunning)) {
    Start-DockerDesktop
}

if ($Service -eq "all") {
    Write-Host "=== Building all services with Maven ==="
    mvn --% clean install -Dmaven.test.skip=true

    Write-Host "=== Rebuilding all Docker containers (no cache) ==="
    docker compose build --no-cache

    Write-Host "=== Recreating all services ==="
    docker compose up -d --force-recreate

    Write-Host "=== Done. Checking status ==="
    docker compose ps
}
else {
    Write-Host "=== Building $Service with Maven ==="
    mvn --% clean install -pl "back/$Service" -am -Dmaven.test.skip=true

    Write-Host "=== Rebuilding $Service Docker container (no cache) ==="
    docker compose build --no-cache $Service

    Write-Host "=== Restarting $Service ==="
    docker compose up -d --force-recreate --no-deps $Service

    Write-Host "=== Done. Checking status ==="
    docker compose ps $Service
}
