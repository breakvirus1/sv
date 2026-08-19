# Print SV - Native Windows Deployment Script
# Requirements: Windows 10/11, Java 17, Maven 3.8+, Node.js 18+, PostgreSQL 15+, Keycloak 26+
# Run as Administrator in PowerShell

param(
    [switch]$SkipBuild = $false,
    [switch]$SkipFrontend = $false,
    [string]$HostIp = "192.168.88.118"
)

$ErrorActionPreference = "Stop"

function Write-Info($msg) {
    Write-Host "[INFO] $msg" -ForegroundColor Cyan
}
function Write-Ok($msg) {
    Write-Host "[OK] $msg" -ForegroundColor Green
}
function Write-Err($msg) {
    Write-Host "[ERROR] $msg" -ForegroundColor Red
}
function Write-Warn($msg) {
    Write-Host "[WARN] $msg" -ForegroundColor Yellow
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir

Write-Info "=========================================="
Write-Info "  Print SV Native Windows Deployment"
Write-Info "=========================================="
Write-Info "Project root: $projectRoot"
Write-Info "Host IP: $HostIp"
Write-Info ""

# 1. Check prerequisites
Write-Info "Step 1: Checking prerequisites..."

function Test-Command($cmd) {
    try {
        $null = Get-Command $cmd -ErrorAction Stop
        return $true
    } catch {
        return $false
    }
}

$javaOk = $false
$mavenOk = $false
$nodeOk = $false
$npmOk = $false

if (Test-Command "java") {
    $javaVersion = java -version 2>&1 | Select-String "version" | Select-Object -First 1
    Write-Info "Java: $javaVersion"
    $javaOk = $true
} else {
    Write-Err "Java not found. Install from: https://adoptium.net/"
}

if (Test-Command "mvn") {
    $mavenVersion = mvn -version 2>&1 | Select-String "Apache Maven" | Select-Object -First 1
    Write-Info "Maven: $mavenVersion"
    $mavenOk = $true
} else {
    Write-Err "Maven not found. Install from: https://maven.apache.org/"
}

if (Test-Command "node") {
    $nodeVersion = node --version
    Write-Info "Node.js: $nodeVersion"
    $nodeOk = $true
} else {
    Write-Err "Node.js not found. Install from: https://nodejs.org/"
}

if (Test-Command "npm") {
    $npmVersion = npm --version
    Write-Info "npm: v$npmVersion"
    $npmOk = $true
} else {
    Write-Err "npm not found. Install Node.js from: https://nodejs.org/"
}

if (-not ($javaOk -and $mavenOk -and $nodeOk -and $npmOk)) {
    Write-Err "Missing prerequisites. Please install them and run this script again."
    pause
    exit 1
}

Write-Ok "All prerequisites found"
Write-Info ""

# 2. Check PostgreSQL
Write-Info "Step 2: Checking PostgreSQL..."

$pgInstalled = $false
$pgService = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue
if ($pgService) {
    Write-Info "PostgreSQL service found: $($pgService.Status)"
    $pgInstalled = $true
} else {
    $pgPath = Get-Command "psql" -ErrorAction SilentlyContinue
    if ($pgPath) {
        Write-Info "PostgreSQL client found at: $($pgPath.Source)"
        $pgInstalled = $true
    }
}

if (-not $pgInstalled) {
    Write-Warn "PostgreSQL not found. Please install PostgreSQL 15+ first."
    Write-Info "  Option 1: choco install postgresql15"
    Write-Info "  Option 2: Download from https://www.postgresql.org/download/windows/"
    pause
    exit 1
}

Write-Ok "PostgreSQL is available"
Write-Info ""

# 3. Check Keycloak
Write-Info "Step 3: Checking Keycloak..."

$keycloakDir = "C:\keycloak"
$keycloakRunning = $false

if (Test-Path $keycloakDir) {
    Write-Info "Keycloak directory found: $keycloakDir"
} else {
    Write-Warn "Keycloak not found at $keycloakDir"
    Write-Info "Please install Keycloak 26+ from: https://www.keycloak.org/downloads"
    Write-Info "Extract to: $keycloakDir"
    pause
    exit 1
}

# Check if Keycloak is running
try {
    $kcResponse = Invoke-WebRequest -Uri "http://localhost:8080/realms/print-sv/.well-known/openid-configuration" -UseBasicParsing -TimeoutSec 5
    if ($kcResponse.StatusCode -eq 200) {
        Write-Ok "Keycloak is running on http://localhost:8080"
        $keycloakRunning = $true
    }
} catch {}

if (-not $keycloakRunning) {
    Write-Warn "Keycloak is not running. Starting Keycloak..."
    $kcBin = Join-Path $keycloakDir "bin\kc.bat"
    if (-not (Test-Path $kcBin)) {
        Write-Err "Keycloak startup script not found at: $kcBin"
        pause
        exit 1
    }
    
    Start-Process -FilePath $kcBin -ArgumentList "start-dev --import-realm" -WindowStyle Hidden
    Write-Info "Waiting for Keycloak to start..."
    $maxRetries = 30
    $retry = 0
    while ($retry -lt $maxRetries) {
        try {
            $kcResponse = Invoke-WebRequest -Uri "http://localhost:8080/realms/print-sv/.well-known/openid-configuration" -UseBasicParsing -TimeoutSec 5
            if ($kcResponse.StatusCode -eq 200) {
                Write-Ok "Keycloak started successfully"
                $keycloakRunning = $true
                break
            }
        } catch {}
        Start-Sleep -Seconds 2
        $retry++
    }
    if (-not $keycloakRunning) {
        Write-Err "Failed to start Keycloak"
        pause
        exit 1
    }
}

Write-Ok "Keycloak is ready"
Write-Info ""

# 4. Set up PostgreSQL database
Write-Info "Step 4: Setting up PostgreSQL database..."

$pgPassword = "12345"
$pgUser = "postgres"
$pgDb = "svdb"
$pgHost = "localhost"
$pgPort = "5432"

# Try to connect and create database if not exists
$env:PGPASSWORD = $pgPassword
try {
    $dbExists = psql -U $pgUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$pgDb';" 2>$null
    if (-not $dbExists) {
        Write-Info "Creating database: $pgDb"
        createdb -U $pgUser $pgDb 2>&1
        Write-Ok "Database created"
    } else {
        Write-Info "Database $pgDb already exists"
    }
} catch {
    Write-Warn "Could not connect to PostgreSQL. Make sure it's running and password is correct."
    Write-Info "Default credentials: user=postgres, password=12345"
}

Write-Ok "PostgreSQL is ready"
Write-Info ""

# 5. Import Keycloak realm
Write-Info "Step 5: Importing Keycloak realm..."

$realmFile = Join-Path $projectRoot "keycloak\realm-export.json"
if (Test-Path $realmFile) {
    Write-Info "Realm file found: $realmFile"
    Write-Info "Please import manually via Keycloak Admin Console:"
    Write-Info "  http://localhost:8080"
    Write-Info "  Username: admin"
    Write-Info "  Password: admin"
    Write-Info "  Realm Settings -> Import -> Select realm-export.json"
} else {
    Write-Warn "Realm file not found at: $realmFile"
    Write-Info "You can import it manually later from Keycloak Admin Console"
}

Write-Ok "Keycloak realm setup complete"
Write-Info ""

# 6. Build backend services
if (-not $SkipBuild) {
    Write-Info "Step 6: Building backend services..."
    
    $services = @(
        "back/discovery-server",
        "back/order-service",
        "back/client-service",
        "back/employee-service",
        "back/material-service",
        "back/calculator-service",
        "back/file-service",
        "back/comment-service",
        "back/generate-data-service",
        "back/api-gateway"
    )
    
    foreach ($service in $services) {
        $servicePath = Join-Path $projectRoot $service
        if (-not (Test-Path $servicePath)) {
            Write-Warn "Service not found: $servicePath"
            continue
        }
        
        Write-Info "Building $service ..."
        Push-Location $servicePath
        try {
            mvn clean package -DskipTests 2>&1 | Out-Null
            Write-Ok "$service built successfully"
        } catch {
            Write-Err "Failed to build $service"
            pause
            exit 1
        }
        Pop-Location
    }
    
    Write-Ok "All backend services built"
} else {
    Write-Info "Step 6: Skipping backend build"
}
Write-Info ""

# 7. Start backend services
Write-Info "Step 7: Starting backend services..."

$env:EUREKA_URL = "http://localhost:8761/eureka/"
$env:DB_HOST = "localhost"
$env:KEYCLOAK_ISSUER_URI = "http://localhost:8080/realms/print-sv"
$env:SPRING_PROFILES_ACTIVE = "local"

$serviceProcesses = @()

function Start-Service($name, $path, $port, $args = "") {
    Write-Info "Starting $name on port $port..."
    $jarPath = Get-ChildItem -Path $path -Filter "*-0.0.1-SNAPSHOT.jar" | Select-Object -First 1
    if (-not $jarPath) {
        Write-Warn "JAR not found for $name, trying to run via mvn spring-boot:run..."
        Push-Location $path
        $proc = Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run -Dspring-boot.run.profiles=local" -WindowStyle Hidden -PassThru
        Pop-Location
    } else {
        $proc = Start-Process -FilePath "java" -ArgumentList "-jar", $jarPath.FullName, "--server.port=$port", "--spring.profiles.active=local" -WindowStyle Hidden -PassThru
    }
    $serviceProcesses += @{ Name = $name; Process = $proc; Port = $port }
    Write-Ok "$name started (PID: $($proc.Id))"
}

Start-Service "discovery-server" (Join-Path $projectRoot "back/discovery-server") 8761
Start-Sleep -Seconds 5

Start-Service "order-service" (Join-Path $projectRoot "back/order-service") 8081
Start-Service "client-service" (Join-Path $projectRoot "back/client-service") 8082
Start-Service "employee-service" (Join-Path $projectRoot "back/employee-service") 8083
Start-Service "material-service" (Join-Path $projectRoot "back/material-service") 8084
Start-Service "calculator-service" (Join-Path $projectRoot "back/calculator-service") 8086
Start-Service "file-service" (Join-Path $projectRoot "back/file-service") 8087
Start-Service "comment-service" (Join-Path $projectRoot "back/comment-service") 8088
Start-Service "generate-data-service" (Join-Path $projectRoot "back/generate-data-service") 8090
Start-Service "api-gateway" (Join-Path $projectRoot "back/api-gateway") 8085

Write-Ok "All backend services started"
Write-Info ""

# 8. Build and start frontend
if (-not $SkipFrontend) {
    Write-Info "Step 8: Building and starting frontend..."
    
    $frontendPath = Join-Path $projectRoot "front"
    Push-Location $frontendPath
    
    Write-Info "Installing npm dependencies..."
    npm install 2>&1 | Out-Null
    Write-Ok "Dependencies installed"
    
    Write-Info "Starting frontend dev server on port 5174..."
    $frontendProc = Start-Process -FilePath "npm" -ArgumentList "run", "dev", "--", "--host" -WindowStyle Hidden -PassThru
    $serviceProcesses += @{ Name = "frontend"; Process = $frontendProc; Port = 5174 }
    Write-Ok "Frontend started (PID: $($frontendProc.Id))"
    
    Pop-Location
} else {
    Write-Info "Step 8: Skipping frontend"
}
Write-Info ""

# 9. Summary
Write-Info "=========================================="
Write-Info "  Deployment Complete!"
Write-Info "=========================================="
Write-Info ""
Write-Info "Services running:"
Write-Info "  Eureka:        http://localhost:8761"
Write-Info "  Keycloak:      http://localhost:8080"
Write-Info "  Order Service: http://localhost:8081"
Write-Info "  Client Service: http://localhost:8082"
Write-Info "  Employee Service: http://localhost:8083"
Write-Info "  Material Service: http://localhost:8084"
Write-Info "  Calculator:    http://localhost:8086"
Write-Info "  File Service:  http://localhost:8087"
Write-Info "  Comment Service: http://localhost:8088"
Write-Info "  Generate Data: http://localhost:8090"
Write-Info "  API Gateway:   http://localhost:8085"
if (-not $SkipFrontend) {
    Write-Info "  Frontend:      http://localhost:5174"
}
Write-Info ""
Write-Info "Access from other devices in network:"
Write-Info "  Frontend:      http://$HostIp`:5174"
Write-Info "  API Gateway:   http://$HostIp`:8085"
Write-Info "  Keycloak:      http://$HostIp`:8080"
Write-Info ""
Write-Info "To stop all services, run: scripts\windows\stop-all.bat"
Write-Info ""
Write-Info "Press any key to exit..."
pause
