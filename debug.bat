@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0debug.ps1" %*
if errorlevel 1 (
    echo.
    echo [x] Debug exited with code %errorlevel%.
    if "%~1"=="" pause
    exit /b %errorlevel%
)
if "%~1"=="" pause
