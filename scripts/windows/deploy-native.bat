@echo off
:: Deploy Print SV application on Windows natively (without Docker/Podman)
:: Run as Administrator

echo Checking for Administrator privileges...
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo Please run this script as Administrator!
    pause
    exit /b 1
)

echo.
echo ===========================================
echo   Print SV Native Windows Deployment
echo ===========================================
echo.

where powershell >nul 2>&1
if %errorLevel% neq 0 (
    echo PowerShell is not available. Please install PowerShell 5.1 or later.
    pause
    exit /b 1
)

powershell -ExecutionPolicy Bypass -File "%~dp0deploy-native.ps1" %*

pause
