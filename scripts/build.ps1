param([string[]]$Tasks = @('clean', 'build', 'p0Check'), [switch]$Smoke)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskJava = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { (Get-Command java).Source }
$taskArgs = @($Tasks)
if ($Smoke) { $taskArgs += ':pinwheel-app:p0Smoke' }
Push-Location -LiteralPath $taskRoot
try {
    & $taskJava '-Xmx64m' '-classpath' (Join-Path $taskRoot 'gradle\wrapper\gradle-wrapper.jar') 'org.gradle.wrapper.GradleWrapperMain' @taskArgs '--console' 'plain'
    if ($LASTEXITCODE -ne 0) { throw "Gradle exited with code $LASTEXITCODE" }
} finally { Pop-Location }
