@echo off
setlocal
cd /d "%~dp0"
echo ===================================================
echo   EduSync Android Mobile App Generator (Capacitor)
echo ===================================================

powershell.exe -ExecutionPolicy Bypass -File "%~dp0build-android.ps1"
pause
