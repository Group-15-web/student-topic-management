param([switch]$SkipDatabase)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
if(-not $SkipDatabase -and -not $env:DB_URL){ & "$PSScriptRoot/start-local-db.ps1" }
if(Get-Command mvn -ErrorAction SilentlyContinue){mvn spring-boot:run}
else { & ./mvnw.cmd spring-boot:run }
