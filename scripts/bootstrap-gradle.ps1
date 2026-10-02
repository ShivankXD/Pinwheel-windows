$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$cache = Join-Path $taskRoot '.downloads'
$archive = Join-Path $cache 'gradle-8.14.5-bin.zip'
$expected = '6f74b601422d6d6fc4e1f9a1ab6522f642c2fdcbc15ae33ebd30ba3d7198e854'
New-Item -ItemType Directory -Path $cache -Force | Out-Null
if (!(Test-Path -LiteralPath $archive)) {
    & curl.exe -fL --retry 3 --connect-timeout 30 --silent --show-error 'https://downloads.gradle.org/distributions/gradle-8.14.5-bin.zip' -o $archive
    if ($LASTEXITCODE -ne 0) { throw 'Gradle download failed' }
}
if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expected) { throw 'Gradle distribution hash mismatch' }
$launcher = Join-Path $cache 'gradle-8.14.5\bin\gradle.bat'
if (!(Test-Path -LiteralPath $launcher)) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory($archive, $cache, $true)
}
Write-Output 'PASS Gradle 8.14.5 official distribution SHA-256 verified'
