@echo off
chcp 65001 >nul
echo Checking for Administrator privileges...

net session >nul 2>&1
if %errorLevel% == 0 (
    echo Administrator privileges confirmed.
) else (
    echo Please run this script as Administrator!
    pause
    exit /b 1
)

echo.
echo ==========================================
echo   Print SV Full Environment Setup
echo   Target: 192.168.88.121
echo ==========================================
echo.

powershell -ExecutionPolicy Bypass -File "%~dp0install-all.ps1"
