@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1" %*
if errorlevel 1 (
    echo.
    echo [x] Build failed with exit code %errorlevel%.
    if "%~1"=="" pause
    exit /b %errorlevel%
)
if "%~1"=="" pause
