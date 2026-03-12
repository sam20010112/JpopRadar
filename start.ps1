$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$JavaHome = "C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot"
$PidsFile = Join-Path $RepoRoot ".pids"

# --- Backend ---
Set-Location "$RepoRoot\backend"
New-Item -ItemType Directory -Force -Path logs | Out-Null

Write-Host "Compiling backend..."
$env:JAVA_HOME = $JavaHome
& mvn compile -q
if ($LASTEXITCODE -ne 0) { Write-Error "Compilation failed"; exit 1 }

Write-Host "Resolving classpath..."
& mvn dependency:build-classpath "-Dmdep.outputFile=target\classpath.txt" -q
if ($LASTEXITCODE -ne 0) { Write-Error "Classpath resolution failed"; exit 1 }

$cp = Get-Content "$RepoRoot\backend\target\classpath.txt"

Write-Host "Starting backend..."
$backendProc = Start-Process -PassThru -NoNewWindow `
    -FilePath "$JavaHome\bin\java.exe" `
    -ArgumentList @("-cp", "target\classes;$cp", "com.jpopradar.JpopRadarApplication") `
    -RedirectStandardOutput "$RepoRoot\backend\logs\backend.log" `
    -RedirectStandardError  "$RepoRoot\backend\logs\backend-err.log" `
    -WorkingDirectory "$RepoRoot\backend"
$backendPid = $backendProc.Id

# --- Frontend ---
Set-Location "$RepoRoot\frontend"
New-Item -ItemType Directory -Force -Path logs | Out-Null

if (-not (Test-Path "$RepoRoot\frontend\node_modules")) {
    Write-Host "Installing frontend dependencies..."
    & npm install --prefix "$RepoRoot\frontend"
    if ($LASTEXITCODE -ne 0) { Write-Error "npm install failed"; exit 1 }
}

Write-Host "Starting frontend..."
$frontendProc = Start-Process -PassThru -NoNewWindow `
    -FilePath "cmd.exe" `
    -ArgumentList "/c npm run dev" `
    -RedirectStandardOutput "$RepoRoot\frontend\logs\frontend.log" `
    -RedirectStandardError  "$RepoRoot\frontend\logs\frontend-err.log" `
    -WorkingDirectory "$RepoRoot\frontend"
$frontendPid = $frontendProc.Id

# Save PIDs
"$backendPid`n$frontendPid" | Set-Content $PidsFile

# Alive check after 3 s
Start-Sleep 3
foreach ($id in @($backendPid, $frontendPid)) {
    if (-not (Get-Process -Id $id -ErrorAction SilentlyContinue)) {
        Write-Warning "PID $id exited immediately — check logs"
    }
}

Write-Host "Backend :8080  (PID $backendPid)  | Frontend :5173 (PID $frontendPid)"
Write-Host "Logs: backend\logs\backend.log   frontend\logs\frontend.log"
