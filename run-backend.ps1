# EduSync Spring Boot Runner
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  EduSync — TKMIT College Digital Platform" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

$java = "java"
if (Test-Path "$env:JAVA_HOME\bin\java.exe") {
    $java = "$env:JAVA_HOME\bin\java.exe"
} elseif (Test-Path "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\java.exe") {
    $java = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin\java.exe"
} elseif (Get-Command java -ErrorAction SilentlyContinue) {
    $java = (Get-Command java).Source
}

Set-Location $PSScriptRoot

$jarPath = "$PSScriptRoot\target\edusync-backend-1.0.0.jar"

if (-not (Test-Path $jarPath)) {
    Write-Host "Building project artifact with Maven..." -ForegroundColor Yellow
    $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
    $env:Path = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin;C:\Users\sreeh\apache-maven-3.9.9\bin;" + $env:Path
    mvn clean package -DskipTests
}

Write-Host "Starting backend server on http://localhost:8081 ..." -ForegroundColor Green
& $java -jar $jarPath
