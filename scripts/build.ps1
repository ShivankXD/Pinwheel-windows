param([string[]]$Tasks = @('clean', 'build', 'p3RuntimeCheck'), [switch]$Smoke)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot 'bootstrap-gradle.ps1')
$taskArgs = @($Tasks)
if ($Smoke) { $taskArgs += ':pinwheel-app:p0Smoke' }
Push-Location -LiteralPath $taskRoot
try {
    & (Join-Path $taskRoot '.downloads\gradle-8.14.5\bin\gradle.bat') @taskArgs '--console' 'plain'
    if ($LASTEXITCODE -ne 0) { throw "Gradle exited with code $LASTEXITCODE" }
} finally { Pop-Location }
if ($Smoke) { & (Join-Path $PSScriptRoot 'check-smoke-failure.ps1') }
