$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$pidFile = Join-Path $projectRoot '.local/web.pid'
if(-not (Test-Path -LiteralPath $pidFile)){Write-Host 'No background web PID file. For foreground runs, press Ctrl+C in their terminal.'; return}
$webProcessId = [int](Get-Content -LiteralPath $pidFile)
$process = Get-CimInstance Win32_Process -Filter "ProcessId = $webProcessId"
if(-not $process){Write-Host 'Web process is already stopped.'; return}
$expectedJar = Join-Path $projectRoot 'target/de-tai-hub-1.0.0.jar'
if($process.Name -ne 'java.exe' -or -not $process.CommandLine.Contains($expectedJar)){throw 'PID is not the web process belonging to this project. Nothing stopped.'}
Stop-Process -Id $webProcessId
Write-Host 'Project web stopped. Database and uploaded files retained.'
