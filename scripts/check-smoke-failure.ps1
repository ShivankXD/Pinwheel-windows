$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot 'bootstrap-gradle.ps1')
$taskEvidence = Join-Path $taskRoot 'evidence\p0\runtime'
New-Item -ItemType Directory -Path $taskEvidence -Force | Out-Null
$taskInit = Join-Path $taskRoot '.downloads\p0-missing-native.init.gradle'
@'
allprojects {
    tasks.withType(JavaExec).configureEach {
        if (name == 'p0Smoke') {
            systemProperty 'pinwheel.home', rootDir.toPath().resolve('evidence/p0/runtime/missing-native-home').toString()
        }
    }
}
'@ | Set-Content -LiteralPath $taskInit -Encoding utf8
$taskLog = Join-Path $taskEvidence 'expected-native-failure.txt'
Push-Location -LiteralPath $taskRoot
try {
    & (Join-Path $taskRoot '.downloads\gradle-8.14.5\bin\gradle.bat') ':pinwheel-app:p0Smoke' '-I' $taskInit '--console' 'plain' *> $taskLog
    $taskExit = $LASTEXITCODE
    $taskOutput = Get-Content -LiteralPath $taskLog -Raw
    if ($taskExit -eq 0 -or $taskOutput -notmatch 'UnsatisfiedLinkError' -or $taskOutput -notmatch 'non-zero exit value 1') {
        throw "Missing-native smoke did not fail for the expected reason. See $taskLog"
    }
    Write-Output 'PASS missing-native smoke returns nonzero exit code with the expected library failure'
} finally { Pop-Location }
# The deliberately failing child must not become the CI step's final exit code.
exit 0
