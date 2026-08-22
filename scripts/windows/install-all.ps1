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

function Get-CommandPath {
    param([string]$Cmd)
    try {
        $cmdInfo = Get-Command $Cmd -ErrorAction Stop
        return $cmdInfo.Source
    } catch {
        return $null
    }
}

function Refresh-EnvPath {
    $env:PATH = [System.Environment]::GetEnvironmentVariable("PATH","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("PATH","User")
}

function Download-File {
    param(
        [string]$Url,
        [string]$OutputPath,
        [string]$Description = ""
    )
    if ($Description) {
        Info "Downloading $Description..."
    }
    $maxRetries = 3
    $retry = 0
    while ($retry -lt $maxRetries) {
        try {
            curl.exe -fL -o $OutputPath $Url --max-time 300
            if ($LASTEXITCODE -eq 0 -and (Test-Path $OutputPath)) {
                $fileSize = (Get-Item $OutputPath).Length
                if ($fileSize -gt 1MB) {
                    Info "Downloaded $Description ($([math]::Round($fileSize / 1MB, 1)) MB)"
                    return $true
                } else {
                    Remove-Item $OutputPath -ErrorAction SilentlyContinue
                    Warn "File too small, retrying..."
                }
            }
        } catch {
            Warn "Download attempt $($retry+1) failed: $_"
        }
        $retry++
        Start-Sleep -Seconds 3
    }
    ErrorMsg "Failed to download $Description from $Url"
    return $false
}

function Install-WithWinget {
    param([string]$Name, [string]$Id, [string]$Args = "")
    Info "Installing $Name via winget..."
    try {
        $null = & winget install --id $Id --accept-source-agreements --accept-package-agreements $Args --silent 2>&1
        if ($LASTEXITCODE -eq 0) {
            Info "$Name installed via winget."
            return $true
        }
    } catch {}
    Warn "Failed to install $Name via winget."
    return $false
}

function Install-WithChoco {
    param([string]$Name, [string]$Id)
    if (-not (Test-Command "choco")) {
        return $false
    }
    Info "Installing $Name via Chocolatey..."
    try {
        $null = & choco install $Id -y --no-progress 2>&1
        if ($LASTEXITCODE -eq 0) {
            Info "$Name installed via Chocolatey."
            return $true
        }
    } catch {}
    Warn "Failed to install $Name via Chocolatey."
    return $false
}

function Install-Git-Direct {
    Info "Installing Git via direct download..."
    $gitUrl = "https://github.com/git-for-windows/git/releases/download/v2.45.2.windows.1/Git-2.45.2-64-bit.exe"
    $installer = Join-Path $env:TEMP "git-installer.exe"
    if (-not (Download-File -Url $gitUrl -OutputPath $installer -Description "Git for Windows")) {
        return $false
    }
    Info "Installing Git silently..."
    Start-Process -FilePath $installer -ArgumentList "/VERYSILENT","/NORESTART","/NOCANCEL","/SP-","/CLOSEAPPLICATIONS","/RESTARTAPPLICATIONS","/COMPONENTS=icons,ext\reg\shellhere,assoc,assoc_sh" -Wait -NoNewWindow
    Remove-Item $installer -ErrorAction SilentlyContinue
    Refresh-EnvPath
    if (Test-Command "git") {
        Info "Git installed."
        return $true
    }
    return $false
}

function Install-Git {
    if (Test-Command "git") {
        $gitVersion = git --version
        Info "Git already installed: $gitVersion"
        return $true
    }
    $installed = $false
    if (Test-Command "winget") {
        $installed = Install-WithWinget "Git" "Git.Git"
    }
    if (-not $installed -and (Test-Command "choco")) {
        $installed = Install-WithChoco "Git" "git"
    }
    if (-not $installed) {
        $installed = Install-Git-Direct
    }
    if (-not $installed) {
        ErrorMsg "Please install Git manually from: https://git-scm.com/download/win"
        return $false
    }
    Info "Waiting for Git installation to complete..."
    Start-Sleep -Seconds 5
    Refresh-EnvPath
    $gitPath = Get-CommandPath "git"
    if ($gitPath) {
        $gitVersion = git --version
        Info "Git: $gitVersion"
        return $true
    }
    ErrorMsg "Git installation failed or not found in PATH"
    return $false
}

function Install-Java21-Direct {
    Info "Installing Java 21 via direct download..."
    $javaUrl = "https://github.com/adoptium/temurin21-binaries/releases/latest/download/OpenJDK21U-jdk_x64_windows_hotspot_21.0.3_9.msi"
    $installer = Join-Path $env:TEMP "jdk21-installer.msi"
    if (-not (Download-File -Url $javaUrl -OutputPath $installer -Description "Java 21 JDK (Adoptium)")) {
        return $false
    }
    Info "Installing Java 21 JDK silently..."
    Start-Process msiexec.exe -ArgumentList "/i",$installer,"ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureOracleJavaSoft","/qn","/norestart","INSTALLDIR=C:\Program Files\Java\jdk-21" -Wait -NoNewWindow
    Remove-Item $installer -ErrorAction SilentlyContinue
    Refresh-EnvPath
    $javaPath = Get-CommandPath "java"
    if ($javaPath) {
        $javaVersion = java -version 2>&1 | Select-Object -First 1
        Info "Java: $javaVersion"
        return $true
    }
    return $false
}

function Install-Java21 {
    if (Test-Command "java") {
        $javaVersion = java -version 2>&1 | Select-Object -First 1
        if ($javaVersion -match '21') {
            Info "Java 21 already installed: $javaVersion"
            return $true
        }
        Warn "Different Java version found: $javaVersion. Installing Java 21..."
    }
    $installed = $false
    if (Test-Command "winget") {
        $installed = Install-WithWinget "Java 21 JDK" "EclipseAdoptium.Temurin.21.JDK"
    }
    if (-not $installed -and (Test-Command "choco")) {
        $installed = Install-WithChoco "Java 21 JDK" "temurin21"
    }
    if (-not $installed) {
        $installed = Install-Java21-Direct
    }
    if (-not $installed) {
        ErrorMsg "Please install Java 21 JDK manually from: https://adoptium.net/temurin/releases/"
        return $false
    }
    Info "Waiting for Java installation to complete..."
    Start-Sleep -Seconds 5
    Refresh-EnvPath
    $javaPath = Get-CommandPath "java"
    if ($javaPath) {
        $javaVersion = java -version 2>&1 | Select-Object -First 1
        Info "Java: $javaVersion"
        return $true
    }
    ErrorMsg "Java installation failed or not found in PATH"
    return $false
}

function Install-Maven-Direct {
    Info "Installing Maven via direct download..."
    $mavenUrl = "https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip"
    $zipFile = Join-Path $env:TEMP "maven-3.9.9-bin.zip"
    if (-not (Download-File -Url $mavenUrl -OutputPath $zipFile -Description "Apache Maven 3.9.9")) {
        return $false
    }
    $mavenDir = "C:\Program Files\Apache\Maven"
    if (-not (Test-Path $mavenDir)) {
        New-Item -ItemType Directory -Path $mavenDir -Force | Out-Null
    }
    Info "Extracting Maven..."
    Expand-Archive -Path $zipFile -DestinationPath $mavenDir -Force
    Remove-Item $zipFile -ErrorAction SilentlyContinue
    $mvnCmd = Join-Path $mavenDir "apache-maven-3.9.9\bin\mvn.cmd"
    if (-not (Test-Path $mvnCmd)) {
        return $false
    }
    $currentPath = [System.Environment]::GetEnvironmentVariable("PATH", "User")
    $mavenBinPath = Join-Path $mavenDir "apache-maven-3.9.9\bin"
    if ($currentPath -notlike "*$mavenBinPath*") {
        [System.Environment]::SetEnvironmentVariable("PATH", "$currentPath;$mavenBinPath", "User")
    }
    Refresh-EnvPath
    Info "Maven installed to $mavenDir"
    return $true
}

function Install-Maven {
    if (Test-Command "mvn") {
        $mavenVersion = & mvn -version 2>&1 | Select-String "Apache Maven" | Select-Object -First 1
        Info "Maven already installed: $mavenVersion"
        return $true
    }
    $installed = $false
    if (Test-Command "winget") {
        $installed = Install-WithWinget "Maven" "Apache.Maven"
    }
    if (-not $installed -and (Test-Command "choco")) {
        $installed = Install-WithChoco "Maven" "maven"
    }
    if (-not $installed) {
        $installed = Install-Maven-Direct
    }
    if (-not $installed) {
        ErrorMsg "Please install Maven manually from: https://maven.apache.org/"
        return $false
    }
    Info "Waiting for Maven installation to complete..."
    Start-Sleep -Seconds 5
    Refresh-EnvPath
    $mvnPath = Get-CommandPath "mvn"
    if ($mvnPath) {
        $mavenVersion = & mvn -version 2>&1 | Select-String "Apache Maven" | Select-Object -First 1
        Info "Maven: $mavenVersion"
        return $true
    }
    ErrorMsg "Maven installation failed or not found in PATH"
    return $false
}

function Install-NodeJS-Direct {
    Info "Installing Node.js 20 LTS via direct download..."
    $nodeUrl = "https://nodejs.org/dist/v20.15.0/node-v20.15.0-win-x64.zip"
    $zipFile = Join-Path $env:TEMP "nodejs-20.15.0-win-x64.zip"
    if (-not (Download-File -Url $nodeUrl -OutputPath $zipFile -Description "Node.js 20 LTS")) {
        return $false
    }
    $nodeDir = "C:\Program Files\nodejs"
    if (-not (Test-Path $nodeDir)) {
        New-Item -ItemType Directory -Path $nodeDir -Force | Out-Null
    }
    Info "Extracting Node.js..."
    Expand-Archive -Path $zipFile -DestinationPath $nodeDir -Force
    Remove-Item $zipFile -ErrorAction SilentlyContinue
    $currentPath = [System.Environment]::GetEnvironmentVariable("PATH", "Machine")
    if ($currentPath -notlike "*$nodeDir*") {
        [System.Environment]::SetEnvironmentVariable("PATH", "$currentPath;$nodeDir", "Machine")
    }
    Refresh-EnvPath
    Info "Node.js installed to $nodeDir"
    return $true
}

function Install-NodeJS {
    if (Test-Command "node") {
        $nodeVersion = node --version
        Info "Node.js already installed: $nodeVersion"
    } else {
        $installed = $false
        if (Test-Command "winget") {
            $installed = Install-WithWinget "Node.js" "OpenJS.NodeJS.LTS"
        }
        if (-not $installed -and (Test-Command "choco")) {
            $installed = Install-WithChoco "Node.js" "nodejs-lts"
        }
        if (-not $installed) {
            $installed = Install-NodeJS-Direct
        }
        if (-not $installed) {
            ErrorMsg "Please install Node.js manually from: https://nodejs.org/"
            return $false
        }
        Info "Waiting for Node.js installation to complete..."
        Start-Sleep -Seconds 5
        Refresh-EnvPath
    }
    $nodePath = Get-CommandPath "node"
    if ($nodePath) {
        $nodeVersion = node --version
        Info "Node.js: $nodeVersion"
    } else {
        ErrorMsg "Node.js installation failed or not found in PATH"
        return $false
    }
    $npmPath = Get-CommandPath "npm"
    if ($npmPath) {
        $npmVersion = npm --version
        Info "npm: v$npmVersion"
    } else {
        ErrorMsg "npm installation failed or not found in PATH"
        return $false
    }
    return $true
}

function Install-Vite {
    if (-not (Test-Command "node") -or -not (Test-Command "npm")) {
        ErrorMsg "Node.js/npm required for Vite installation."
        return $false
    }
    Info "Installing Vite globally via npm..."
    try {
        $null = & npm install -g vite 2>&1
        if ($LASTEXITCODE -eq 0) {
            Info "Vite installed globally."
            return $true
        }
    } catch {
        Warn "Failed to install Vite globally: $_"
    }
    Warn "Vite global install failed. It will be installed per-project during build."
    return $true
}

function Install-DockerDesktop-Direct {
    Info "Installing Docker Desktop via direct download..."
    $dockerUrl = "https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe"
    $installer = Join-Path $env:TEMP "docker-desktop-installer.exe"
    if (-not (Download-File -Url $dockerUrl -OutputPath $installer -Description "Docker Desktop")) {
        return $false
    }
    Info "Installing Docker Desktop silently..."
    Start-Process -FilePath $installer -ArgumentList "install --quiet --accept-license" -Wait -NoNewWindow
    Remove-Item $installer -ErrorAction SilentlyContinue
    Info "Docker Desktop installation completed. A restart may be required."
    return $true
}

function Install-DockerDesktop {
    if (Test-Command "docker") {
        try {
            $dockerVersion = & docker version --format '{{.Server.Version}}' 2>&1
            if ($LASTEXITCODE -eq 0) {
                Info "Docker already installed: $dockerVersion"
                return $true
            }
        } catch {}
    }
    $installed = $false
    if (Test-Command "winget") {
        $installed = Install-WithWinget "Docker Desktop" "Docker.DockerDesktop"
    }
    if (-not $installed -and (Test-Command "choco")) {
        $installed = Install-WithChoco "Docker Desktop" "docker-desktop"
    }
    if (-not $installed) {
        $installed = Install-DockerDesktop-Direct
    }
    if (-not $installed) {
        ErrorMsg "Please install Docker Desktop manually from: https://www.docker.com/products/docker-desktop/"
        return $false
    }
    Info "Docker Desktop installed. Please start it manually and accept the license agreement."
    Info "After starting Docker Desktop, run this script again."
    return $true
}

function Test-Docker {
    try {
        $null = & docker info *>&1
        if ($LASTEXITCODE -eq 0) {
            $dockerVersion = & docker version --format '{{.Server.Version}}' 2>&1
            Info "Docker is running: $dockerVersion"
            return $true
        }
    } catch {}
    ErrorMsg "Docker is not running. Please start Docker Desktop and try again."
    return $false
}

function Test-DockerCompose {
    try {
        $composeVersion = & docker compose version --short 2>&1
        if ($LASTEXITCODE -eq 0) {
            Info "Docker Compose: $composeVersion"
            return $true
        }
    } catch {}
    ErrorMsg "Docker Compose not found. Please install Docker Desktop with Compose support."
    return $false
}

function Clone-Repository {
    param([string]$TargetDir)
    if (Test-Path $TargetDir) {
        Warn "Directory $TargetDir already exists."
        $response = Read-Host "Do you want to remove it and clone again? (y/N)"
        if ($response -eq "y" -or $response -eq "Y") {
            Remove-Item $TargetDir -Recurse -Force
        } else {
            Warn "Skipping clone. Using existing directory."
            return $true
        }
    }
    Info "Cloning repository to $TargetDir..."
    Set-Location (Split-Path -Parent $TargetDir)
    & git clone https://github.com/breakvirus1/sv.git $TargetDir
    if ($LASTEXITCODE -eq 0) {
        Info "Repository cloned successfully."
        return $true
    }
    ErrorMsg "Failed to clone repository."
    return $false
}

function Show-Urls {
    param([string]$HostIP)
    Write-Host ""
    Info "Application will be available at:"
    Write-Host "  Frontend:        http://${HostIP}:5174"
    Write-Host "  API Gateway:     http://${HostIP}:8085"
    Write-Host "  Discovery:       http://${HostIP}:8761"
    Write-Host "  Keycloak:        http://${HostIP}:8080  (admin / admin)"
    Write-Host "  PostgreSQL:      ${HostIP}:5433"
}

# Main
$targetDir = "C:\sv"
$hostIP = "192.168.88.121"

Info "=========================================="
Info "  Print SV Full Environment Setup"
Info "  Target machine: $hostIP"
Info "=========================================="
Write-Host ""

# Check admin privileges
$currentPrincipal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $currentPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    ErrorMsg "Please run this script as Administrator!"
    pause
    exit 1
}
Info "Administrator privileges confirmed."
Write-Host ""

# Install prerequisites
Info "Step 1: Installing prerequisites..."
Write-Host ""

$gitOk = Install-Git
if (-not $gitOk) {
    ErrorMsg "Git installation failed. Exiting."
    pause
    exit 1
}

$javaOk = Install-Java21
if (-not $javaOk) {
    ErrorMsg "Java 21 installation failed. Exiting."
    pause
    exit 1
}

$mavenOk = Install-Maven
if (-not $mavenOk) {
    ErrorMsg "Maven installation failed. Exiting."
    pause
    exit 1
}

$nodeOk = Install-NodeJS
if (-not $nodeOk) {
    ErrorMsg "Node.js installation failed. Exiting."
    pause
    exit 1
}

$viteOk = Install-Vite
if (-not $viteOk) {
    Warn "Vite installation skipped or failed. Will install per-project."
}

$dockerOk = Install-DockerDesktop
if (-not $dockerOk) {
    ErrorMsg "Docker Desktop installation failed. Exiting."
    pause
    exit 1
}

Write-Host ""
Info "Step 2: Verifying Docker..."
Write-Host ""

if (-not (Test-Docker)) {
    Warn "Docker is not running. Please start Docker Desktop manually and run this script again."
    pause
    exit 1
}

if (-not (Test-DockerCompose)) {
    Warn "Docker Compose not available. Please ensure Docker Desktop has Compose support enabled."
    pause
    exit 1
}

Write-Host ""
Info "Step 3: Cloning repository..."
Write-Host ""

$cloneOk = Clone-Repository -TargetDir $targetDir
if (-not $cloneOk) {
    ErrorMsg "Repository clone failed. Exiting."
    pause
    exit 1
}

Write-Host ""
Info "=========================================="
Info "  Setup completed successfully!"
Info "=========================================="
Write-Host ""
Show-Urls -HostIP $hostIP
Write-Host ""
Info "Next steps:"
Write-Host "  1. Open Docker Desktop and ensure it is running"
Write-Host "  2. cd C:\sv"
Write-Host "  3. Run: scripts\windows\deploy-podman.bat  (or deploy-native.bat)"
Write-Host ""
Info "Machine IP for local network access: $hostIP"
Write-Host ""
pause
