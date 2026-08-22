@echo off
chcp 65001 >nul
echo Stopping all Podman services...

powershell -ExecutionPolicy Bypass -Command "podman compose -f '%~dp0..\podman-compose.yml' -p sv down --remove-orphans"

echo.
echo All services stopped.
pause
