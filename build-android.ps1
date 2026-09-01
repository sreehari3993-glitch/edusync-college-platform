# EduSync Capacitor Android Helper
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  EduSync Android Mobile App Generator (Capacitor)" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    Write-Host "Node.js (npm) is not installed or not in PATH." -ForegroundColor Red
    Write-Host "Please install Node.js from: https://nodejs.org/" -ForegroundColor Yellow
    Exit 1
}

Write-Host "1. Installing Capacitor dependencies..." -ForegroundColor Yellow
npm install

Write-Host "2. Initializing Android native project..." -ForegroundColor Yellow
if (-not (Test-Path "$PSScriptRoot\android")) {
    npx cap add android
}

Write-Host "3. Syncing web assets into Android project..." -ForegroundColor Yellow
npx cap sync android

Write-Host "4. Opening Android Studio..." -ForegroundColor Green
npx cap open android
