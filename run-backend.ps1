# EduSync Spring Boot Runner
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  EduSync - TKMIT College Digital Platform" -ForegroundColor Cyan
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

# Check if port 8081 is already occupied and free it
try {
    $existingConns = Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue
    if ($existingConns) {
        foreach ($conn in $existingConns) {
            $procId = $conn.OwningProcess
            if ($procId -gt 0 -and $procId -ne $PID) {
                Write-Host "Port 8081 is occupied by PID $procId. Stopping existing instance..." -ForegroundColor Yellow
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            }
        }
        Start-Sleep -Seconds 1
    }
} catch {
    # Fallback to netstat if Get-NetTCPConnection is unavailable
    $lines = netstat -ano | Select-String ":8081\s+.*LISTENING\s+(\d+)"
    foreach ($line in $lines) {
        if ($line.Matches[0].Groups[1].Value) {
            $p = [int]$line.Matches[0].Groups[1].Value
            if ($p -gt 0 -and $p -ne $PID) {
                Stop-Process -Id $p -Force -ErrorAction SilentlyContinue
            }
        }
    }
}

# If Android device is connected via ADB/Wireless Debugging, reverse port 8081
try {
    $adb = (Get-ChildItem -Path "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe", "C:\Users\sreeh\AppData\Local\Android\Sdk\platform-tools\adb.exe" -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
    if ($adb) {
        & $adb reverse tcp:8081 tcp:8081 *>$null
        Write-Host "ADB Port Forwarding: Phone port 8081 -> Laptop port 8081 enabled ✅" -ForegroundColor Cyan
    }
} catch {}

Write-Host "Starting backend server on http://localhost:8081 ..." -ForegroundColor Green
& $java -jar $jarPath
