# ═══════════════════════════════════════════════════════════════════════════
#  🎓 EduSync Campus AI — Interactive Terminal CLI
#  College: TKM Institute of Technology (TKMIT) | KTU Regulations Engine
# ═══════════════════════════════════════════════════════════════════════════

param(
    [string]$BackendUrl = "http://localhost:8081"
)

Clear-Host
Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "       🎓  EduSync Campus AI Copilot — Terminal Interface       " -ForegroundColor Yellow
Write-Host "       TKMIT College Platform | KTU RAG & Database Engine       " -ForegroundColor Cyan
Write-Host "════════════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

# 1. Test Backend Connection
Write-Host "⏳ Connecting to EduSync Backend ($BackendUrl)... " -NoNewline
try {
    $ping = Invoke-RestMethod -Uri "$BackendUrl/api/auth/ping" -Method Get -TimeoutSec 3 -ErrorAction Stop
    Write-Host "ONLINE! ✅" -ForegroundColor Green
} catch {
    Write-Host "OFFLINE ⚠️" -ForegroundColor Red
    Write-Host "Please ensure Spring Boot backend is running on port 8081." -ForegroundColor Yellow
    exit
}

# 2. Login Prompt or Guest Demo
Write-Host "`nDo you want to log in as a student to test personal live records?" -ForegroundColor Cyan
$loginChoice = Read-Host "Login? (y/n, default: y)"

$token = $null
$studentName = "Guest / Demo Student"

if ($loginChoice -ne "n" -and $loginChoice -ne "N") {
    $email = Read-Host "Student Email (default: anzal.cs@tkmit.ac.in)"
    if ([string]::IsNullOrWhiteSpace($email)) { $email = "anzal.cs@tkmit.ac.in" }
    
    $password = Read-Host "Password (default: password123)"
    if ([string]::IsNullOrWhiteSpace($password)) { $password = "password123" }

    try {
        $loginBody = @{ email = $email; password = $password } | ConvertTo-Json
        $loginRes = Invoke-RestMethod -Uri "$BackendUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
        $token = $loginRes.data.token
        $studentName = $loginRes.data.name
        Write-Host "`n✅ Logged in as: $studentName ($($loginRes.data.department) - Sem $($loginRes.data.semester))" -ForegroundColor Green
    } catch {
        Write-Host "`n⚠️ Login failed. Continuing as Demo Student." -ForegroundColor Yellow
    }
}

Write-Host "`n----------------------------------------------------------------" -ForegroundColor Gray
Write-Host "💡 Features Active in Terminal:" -ForegroundColor Yellow
Write-Host " - 📊 Live Database Records (Attendance %, CGPA, Semesters, Leaves)" -ForegroundColor White
Write-Host " - 📑 KTU RAG PDFs (Manuals, 2024 Regulations, CSE & VLSI Syllabi)" -ForegroundColor White
Write-Host " - 🧠 Llama 3 Reasoning on RTX 4060 GPU" -ForegroundColor White
Write-Host " - ☁️ Real-time Cloud Sync to Google Drive" -ForegroundColor White
Write-Host " - Type 'exit', 'quit', or 'q' to leave." -ForegroundColor Gray
Write-Host "----------------------------------------------------------------`n" -ForegroundColor Gray

while ($true) {
    Write-Host "$studentName >>> " -ForegroundColor Yellow -NoNewline
    $query = Read-Host

    if ([string]::IsNullOrWhiteSpace($query)) { continue }
    if ($query -in @("exit", "quit", "q", "/bye")) {
        Write-Host "`n👋 Goodbye! EduSync session ended." -ForegroundColor Cyan
        break
    }

    $headers = @{ "Content-Type" = "application/json" }
    if ($token) {
        $headers["Authorization"] = "Bearer $token"
    }

    $body = @{ message = $query } | ConvertTo-Json

    Write-Host "✨ Thinking..." -ForegroundColor DarkGray
    try {
        $res = Invoke-RestMethod -Uri "$BackendUrl/api/ai/chat" -Method Post -Body $body -Headers $headers -TimeoutSec 45
        Write-Host "`n🤖 EduSync AI:" -ForegroundColor Green
        Write-Host $res.data.reply -ForegroundColor White
        Write-Host "`n----------------------------------------------------------------`n" -ForegroundColor Gray
    } catch {
        Write-Host "`n⚠️ Error communicating with EduSync AI: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "`n----------------------------------------------------------------`n" -ForegroundColor Gray
    }
}
