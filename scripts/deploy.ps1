[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$ErrorActionPreference = 'Stop'

function Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Cyan
}

function Warn {
    param([string]$Message)
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function ErrorMsg {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Start-DockerDesktop {
    try {
        $dockerInfo = & docker info *>&1
        if ($LASTEXITCODE -eq 0) {
            Info "Docker works: $((docker version --format '{{.Server.Version}}') -replace '\s','')"
            return
        }
    } catch {}

    Warn "Docker Desktop is not running. Attempting to start..."
    $dockerDesktop = Get-ItemProperty "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*" -ErrorAction SilentlyContinue |
        Where-Object { $_.DisplayName -like "*Docker Desktop*" } |
        Select-Object -First 1

    if ($dockerDesktop) {
        $installLocation = $dockerDesktop.InstallLocation
        $exePath = Join-Path $installLocation "Docker Desktop.exe"
        if (Test-Path $exePath) {
            Start-Process $exePath -ErrorAction SilentlyContinue | Out-Null
        } else {
            Start-Process "docker" -ErrorAction SilentlyContinue | Out-Null
        }
    } else {
        Start-Process "docker" -ErrorAction SilentlyContinue | Out-Null
    }

    Info "Waiting for Docker Desktop to start (up to 120 seconds)..."
    for ($i = 1; $i -le 60; $i++) {
        Start-Sleep -Seconds 2
        try {
            $dockerInfo = & docker info *>&1
            if ($LASTEXITCODE -eq 0) {
                Info "Docker Desktop started."
                return
            }
        } catch {}
    }
    ErrorMsg "Docker Desktop did not start in time. Please start it manually."
    exit 1
}

function Test-Docker {
    try {
        $null = & docker info *>&1
        if ($LASTEXITCODE -ne 0) {
            ErrorMsg "Docker is not running or not available."
            exit 1
        }
    } catch {
        ErrorMsg "Docker is not running or not available."
        exit 1
    }
}

function Test-Compose {
    $composeVersion = & docker compose version --short *>&1
    if ($LASTEXITCODE -eq 0) {
        Info "Docker Compose: $composeVersion"
        return
    }
    ErrorMsg "Docker Compose not found. Install Docker Desktop with Compose support."
    exit 1
}

function Stop-Existing {
    Info "Stopping existing containers..."
    $null = & docker compose -f $composeFile down --remove-orphans *>&1
}

function Build-Images {
    Info "Building Docker images..."
    & docker compose -f $composeFile build --no-cache
}

function Start-Services {
    Info "Starting services..."
    & docker compose -f $composeFile up -d --wait
}

function Show-Status {
    Info "Container status:"
    & docker compose -f $composeFile ps
}

function Show-Urls {
    Write-Host ""
    Info "Available services:"
    Write-Host "  Frontend:        http://localhost:5174"
    Write-Host "  API Gateway:     http://localhost:8085"
    Write-Host "  Discovery:       http://localhost:8761"
    Write-Host "  Keycloak:        http://localhost:8080  (admin / admin)"
    Write-Host "  Order Service:   http://localhost:8081"
    Write-Host "  Client Service:  http://localhost:8082"
    Write-Host "  File Service:    http://localhost:8087"
    Write-Host "  Comment Service: http://localhost:8088"
    Write-Host "  Generate Data:   http://localhost:8090"
    Write-Host "  PostgreSQL:      localhost:5433"
}

function Show-LogsHint {
    Write-Host ""
    Info "View logs: docker compose -f `"$composeFile`" logs -f service-name"
    Info "Stop:      docker compose -f `"$composeFile`" down"
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$composeFile = Join-Path $projectRoot "docker-compose.yml"

Info "Deploying project 'sv' in Docker (Windows 11)"
Write-Host ""

Start-DockerDesktop
Test-Docker
Test-Compose

Set-Location $projectRoot

Stop-Existing
Build-Images
Start-Services
Show-Status
Show-Urls
Show-LogsHint

Write-Host ""
Info "Deployment completed."
