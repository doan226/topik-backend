# Chay backend: tu dong tim port trong 8080-8082
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\..

$port = 8080
foreach ($candidate in 8080, 8081, 8082) {
    $inUse = Get-NetTCPConnection -LocalPort $candidate -State Listen -ErrorAction SilentlyContinue
    if (-not $inUse) { $port = $candidate; break }
    if ($candidate -eq 8082) {
        Write-Host "[topikai] Loi: 8080-8082 deu bi chiem. Dung run-backend.cmd hoac dat PORT=8083"
        exit 1
    }
}
$env:PORT = "$port"

$frontendEnv = "C:\topik-frontend\.env.development.local"
"VITE_API_PORT=$port" | Set-Content -Path $frontendEnv -Encoding utf8
Write-Host "[topikai] Backend: http://localhost:$port"
Write-Host "[topikai] Da ghi $frontendEnv -> VITE_API_PORT=$port"

& .\mvnw.cmd spring-boot:run
