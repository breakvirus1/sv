@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0rebuild-menu.ps1" %*
