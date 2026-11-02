Set-ExecutionPolicy Bypass -Scope Process -Force
$projectRoot = Resolve-Path "$PSScriptRoot\.."
Set-Location $projectRoot

if (-not (Test-Path "$projectRoot\.env")) {
    Write-Host "Error: .env not found in project root. Copy .env.example to .env and set POSTGRES_PASSWORD, KC_BOOTSTRAP_ADMIN_PASSWORD, KEYCLOAK_ADMIN_CLIENT_SECRET."
    exit 1
}

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
mvn clean install -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: Maven build failed"
    exit 1
}

Write-Host "=== Building frontend (npm run build) ==="
Push-Location "$projectRoot\front"
npm ci --no-audit --no-fund
if ($LASTEXITCODE -ne 0) { Pop-Location; Write-Host "Error: npm ci failed (run 'npm ci' in front/ manually)"; exit 1 }
npm run build
if ($LASTEXITCODE -ne 0) { Pop-Location; Write-Host "Error: frontend build failed"; exit 1 }
Pop-Location

Write-Host "=== Rebuilding Docker containers (no cache) ==="
docker compose build --no-cache
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: Docker image build failed"
    exit 1
}

Write-Host "=== Starting all services ==="
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: docker compose up failed"
    exit 1
}

Write-Host "=== Done. Checking status ==="
docker compose ps
