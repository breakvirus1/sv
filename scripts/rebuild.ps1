Set-ExecutionPolicy Bypass -Scope Process -Force
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

Write-Host "=== Stopping all containers and removing volumes ==="
docker compose down

Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 3

Write-Host "=== Building microservices with Maven ==="
mvn --% clean install -Dmaven.test.skip=true

Write-Host "=== Rebuilding Docker containers (no cache) ==="
docker compose build --no-cache

Write-Host "=== Starting all services ==="
docker compose up -d

Write-Host "=== Done. Checking status ==="
docker compose ps
