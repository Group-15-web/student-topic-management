param([string]$MySqlHome = 'C:\Program Files\MySQL\MySQL Server 8.0')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$localDir = Join-Path $projectRoot '.local'
$dataDir = Join-Path $localDir 'mysql-data'
$mysqlExe = Join-Path $MySqlHome 'bin\mysql.exe'
$serverExe = Join-Path $MySqlHome 'bin\mysqld.exe'
if (-not (Test-Path -LiteralPath $serverExe)) { throw "MySQL Server not found at $MySqlHome. Use compose.yaml or supply -MySqlHome." }
if (Get-NetTCPConnection -State Listen -LocalPort 3307 -ErrorAction SilentlyContinue) {
    & $mysqlExe --no-defaults --protocol=tcp '--host=127.0.0.1' '--port=3307' '--user=detaihub' '--password=LocalStudy2026!' --database=detaihub -e 'SELECT 1;' 2>$null | Out-Null
    if($LASTEXITCODE -ne 0){throw 'Port 3307 is occupied but the application database is unavailable. No existing server was modified. Set DB_URL/DB_USER/DB_PASSWORD for your own database.'}
    Write-Host 'Application database is already available on port 3307.'; return
}
New-Item -ItemType Directory -Force -Path $localDir | Out-Null
$newDatabase = -not (Test-Path -LiteralPath (Join-Path $dataDir 'mysql'))
if ($newDatabase) {
    New-Item -ItemType Directory -Force -Path $dataDir | Out-Null
    & $serverExe --no-defaults --initialize-insecure "--basedir=$MySqlHome" "--datadir=$dataDir" --console
    if ($LASTEXITCODE -ne 0) { throw 'MySQL initialization failed.' }
}
$argsDb = @('--no-defaults', "--basedir=`"$MySqlHome`"", "--datadir=`"$dataDir`"", '--port=3307', '--bind-address=127.0.0.1', '--mysqlx=0', '--skip-log-bin', "--log-error=`"$(Join-Path $localDir 'mysql.log')`"", "--pid-file=`"$(Join-Path $localDir 'mysql.pid')`"")
Start-Process -FilePath $serverExe -ArgumentList $argsDb -WindowStyle Hidden
for ($i=0; $i -lt 30; $i++) { if(Get-NetTCPConnection -State Listen -LocalPort 3307 -ErrorAction SilentlyContinue){break}; Start-Sleep -Seconds 1 }
if(-not (Get-NetTCPConnection -State Listen -LocalPort 3307 -ErrorAction SilentlyContinue)){throw 'MySQL did not become ready. Inspect .local/mysql.log.'}
if($newDatabase){
    & $mysqlExe --no-defaults --protocol=tcp '--host=127.0.0.1' '--port=3307' '--user=root' -e "CREATE DATABASE detaihub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE USER 'detaihub'@'127.0.0.1' IDENTIFIED BY 'LocalStudy2026!'; GRANT ALL PRIVILEGES ON detaihub.* TO 'detaihub'@'127.0.0.1'; ALTER USER 'root'@'localhost' IDENTIFIED BY 'LocalRoot2026!';"
    if($LASTEXITCODE -ne 0){throw 'Database/user setup failed. Inspect .local/mysql.log.'}
}
Write-Host 'Project MySQL is available on 127.0.0.1:3307. Existing MySQL80 on port 3306 was not modified.'
