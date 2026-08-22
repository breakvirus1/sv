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

function Test-Command {
    param([string]$Cmd)
    try {
        $null = Get-Command $Cmd -ErrorAction Stop
        return $true
    } catch {
        return $false
    }
}

function Test-Podman {
    try {
        $null = & podman info *>&1
        return $LASTEXITCODE -eq 0
    } catch {
        return $false
    }
}

function Test-PodmanCompose {
    try {
        $null = & podman compose version *>&1
        return $LASTEXITCODE -eq 0
    } catch {
        return $false
    }
}

function Start-PodmanService {
    if (Test-Podman) {
        Info "Podman is running."
        return
    }

    Warn "Podman is not running. Attempting to start..."

    $podmanDesktop = Get-ItemProperty "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*" -ErrorAction SilentlyContinue |
        Where-Object { $_.DisplayName -like "*Podman*" -or $_.DisplayName -like "*Red Hat Podman*" } |
        Select-Object -First 1

    if ($podmanDesktop) {
        $installLocation = $podmanDesktop.InstallLocation
        $exePath = Join-Path $installLocation "podman.exe"
        if (Test-Path $exePath) {
            Start-Process $exePath -ArgumentList "system service --time=0" -WindowStyle Hidden -ErrorAction SilentlyContinue | Out-Null
        } else {
            Start-Process "podman" -ArgumentList "system service --time=0" -WindowStyle Hidden -ErrorAction SilentlyContinue | Out-Null
        }
    } else {
        Start-Process "podman" -ArgumentList "system service --time=0" -WindowStyle Hidden -ErrorAction SilentlyContinue | Out-Null
    }

    Info "Waiting for Podman to start (up to 120 seconds)..."
    for ($i = 1; $i -le 60; $i++) {
        Start-Sleep -Seconds 2
        if (Test-Podman) {
            Info "Podman started."
            return
        }
    }
    ErrorMsg "Podman did not start in time. Please start it manually and rerun the script."
    exit 1
}

function Stop-ExistingContainers {
    param([string]$ComposeFile, [string]$ProjectName)
    Info "Stopping existing containers..."
    if (Test-Path $ComposeFile) {
        & podman compose -f $ComposeFile -p $ProjectName down --remove-orphans 2>$null
    }
}

function Build-Backend {
    Info "Building microservices with Maven..."
    Set-Location $projectRoot
    & mvn clean install -DskipTests
    if ($LASTEXITCODE -ne 0) {
        ErrorMsg "Maven build failed."
        exit 1
    }
    Info "Maven build completed."
}

function Build-PodmanImages {
    param([string]$ComposeFile, [string]$ProjectName)
    Info "Building Podman images..."
    & podman compose -f $ComposeFile -p $ProjectName build --no-cache
    if ($LASTEXITCODE -ne 0) {
        ErrorMsg "Podman image build failed."
        exit 1
    }
    Info "Podman images built."
}

function Start-PodmanServices {
    param([string]$ComposeFile, [string]$ProjectName)
    Info "Starting services with Podman..."
    & podman compose -f $ComposeFile -p $ProjectName up -d
    if ($LASTEXITCODE -ne 0) {
        ErrorMsg "Failed to start Podman services."
        exit 1
    }
    Info "Services started."
}

function Show-Status {
    param([string]$ComposeFile, [string]$ProjectName)
    Info "Container status:"
    & podman compose -f $ComposeFile -p $ProjectName ps
}

function Show-Urls {
    $hostIP = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike "127.*" -and $_.InterfaceAlias -notlike "vEthernet (WSL*)" } | Select-Object -First 1).IPAddress
    if (-not $hostIP) {
        $hostIP = "localhost"
    }

    Write-Host ""
    Info "Available services:"
    Write-Host "  Frontend:        http://${hostIP}:5174"
    Write-Host "  API Gateway:     http://${hostIP}:8085"
    Write-Host "  Discovery:       http://${hostIP}:8761"
    Write-Host "  Keycloak:        http://${hostIP}:8080  (admin / admin)"
    Write-Host "  Order Service:   http://${hostIP}:8081"
    Write-Host "  Client Service:  http://${hostIP}:8082"
    Write-Host "  File Service:    http://${hostIP}:8087"
    Write-Host "  Comment Service: http://${hostIP}:8088"
    Write-Host "  Generate Data:   http://${hostIP}:8090"
    Write-Host "  PostgreSQL:      ${hostIP}:5433"
}

function Show-LogsHint {
    param([string]$ComposeFile, [string]$ProjectName)
    Write-Host ""
    Info "View logs: podman compose -f `"$composeFile`" -p `"$projectName`" logs -f service-name"
    Info "Stop:      podman compose -f `"$composeFile`" -p `"$projectName`" down"
}

function Ensure-Directories {
    param([string]$ProjectRoot)
    $dirs = @("uploads", "images", "keycloak")
    foreach ($dir in $dirs) {
        $path = Join-Path $ProjectRoot $dir
        if (-not (Test-Path $path)) {
            New-Item -ItemType Directory -Path $path -Force | Out-Null
            Info "Created directory: $dir"
        }
    }
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$composeFile = Join-Path $projectRoot "podman-compose.yml"
$projectName = "sv"

Info "Deploying project 'sv' with Podman (Windows 10/11)"
Write-Host ""

if (-not (Test-Command "podman")) {
    ErrorMsg "Podman not found. Install Podman Desktop from https://podman.io/ and add it to PATH."
    exit 1
}
Info "Podman found."

if (-not (Test-Podman)) {
    Start-PodmanService
} else {
    Info "Podman is running."
}

if (-not (Test-PodmanCompose)) {
    ErrorMsg "Podman Compose plugin not found. Install Podman Desktop with Compose support."
    exit 1
}
Info "Podman Compose available."

if (-not (Test-Command "mvn")) {
    ErrorMsg "Maven not found. Install Maven and add it to PATH."
    exit 1
}

Ensure-Directories -ProjectRoot $projectRoot

Set-Location $projectRoot

Stop-ExistingContainers -ComposeFile $composeFile -ProjectName $projectName
Build-Backend
Build-PodmanImages -ComposeFile $composeFile -ProjectName $projectName
Start-PodmanServices -ComposeFile $composeFile -ProjectName $projectName
Show-Status -ComposeFile $composeFile -ProjectName $projectName
Show-Urls
Show-LogsHint -ComposeFile $composeFile -ProjectName $projectName

Write-Host ""
Info "Deployment completed."
