@echo off
setlocal
echo ===================================================
echo Starting EduSync Spring Boot Backend
echo ===================================================

powershell.exe -ExecutionPolicy Bypass -File "%~dp0run-backend.ps1"
