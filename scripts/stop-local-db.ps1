$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$pidFile = Join-Path $projectRoot '.local/mysql.pid'
if(-not (Test-Path -LiteralPath $pidFile)){Write-Host 'No project database PID file.'; return}
$databaseProcessId = [int](Get-Content -LiteralPath $pidFile)
$process = Get-CimInstance Win32_Process -Filter "ProcessId = $databaseProcessId"
$expectedData = Join-Path $projectRoot '.local\mysql-data'
if(-not $process -or $process.Name -ne 'mysqld.exe' -or -not $process.CommandLine.Contains($expectedData)){throw 'PID is not the database instance belonging to this project. Nothing stopped.'}
$mysqlAdmin = Join-Path (Split-Path -Parent $process.ExecutablePath) 'mysqladmin.exe'
& $mysqlAdmin --no-defaults --protocol=tcp '--host=127.0.0.1' '--port=3307' '--user=root' '--password=LocalRoot2026!' shutdown
if($LASTEXITCODE -ne 0){throw 'Graceful shutdown failed; no process was forcibly terminated.'}
Write-Host 'Project database shut down. Data retained in .local/mysql-data. MySQL80 on port 3306 was not modified.'
