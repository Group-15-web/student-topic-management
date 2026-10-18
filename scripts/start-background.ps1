$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$jar = Join-Path $projectRoot 'target/de-tai-hub-1.0.0.jar'
if(Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue){throw 'Port 8080 is already in use. No process was changed.'}
& "$PSScriptRoot/start-local-db.ps1"
if(-not (Test-Path -LiteralPath $jar)){
    & ./mvnw.cmd package
    if($LASTEXITCODE -ne 0){throw 'Build failed.'}
}
$java = (Get-Command java -ErrorAction Stop).Source
$runtime = Join-Path $projectRoot '.local'
$appProcess = Start-Process -FilePath $java -ArgumentList '-jar',('"'+$jar+'"') -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $runtime 'app.log') -RedirectStandardError (Join-Path $runtime 'app-error.log') -PassThru
$appProcess.Id | Set-Content -LiteralPath (Join-Path $runtime 'web.pid')
Write-Host "Web starting in background (PID $($appProcess.Id)). Open http://localhost:8080 after startup. Logs: .local/app.log"
