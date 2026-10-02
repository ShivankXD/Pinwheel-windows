$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$manifest = Get-Content -LiteralPath (Join-Path $taskRoot 'native\dependencies.json') -Raw | ConvertFrom-Json
$cache = Join-Path $taskRoot '.downloads'
$native = Join-Path $taskRoot 'native\windows-x64'
New-Item -ItemType Directory -Path $cache,$native -Force | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
foreach ($name in @('angle','ffmpeg')) {
    $spec = $manifest.$name
    $archivePath = Join-Path $cache $spec.archive
    if (!(Test-Path -LiteralPath $archivePath)) {
        & curl.exe -fL --retry 3 --silent --show-error $spec.url -o $archivePath
        if ($LASTEXITCODE -ne 0) { throw "$name download failed" }
    }
    $hash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($hash -ne $spec.sha256) { throw "$name archive hash mismatch. Expected $($spec.sha256), got $hash. No files extracted." }
    $archive = [System.IO.Compression.ZipFile]::OpenRead($archivePath)
    try {
        foreach ($entry in $archive.Entries) {
            $leaf = [System.IO.Path]::GetFileName($entry.FullName)
            $wanted = if ($name -eq 'angle') { $leaf -in @('libEGL.dll','libGLESv2.dll') }
                else { $entry.FullName -match '/bin/[^/]+\.dll$' -or $leaf -in @('ffmpeg.exe','ffprobe.exe') }
            if ($wanted) { [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, (Join-Path $native $leaf), $true) }
            if ($name -eq 'ffmpeg' -and $leaf -eq 'LICENSE.txt') {
                [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, (Join-Path $taskRoot 'licenses\FFMPEG-LICENSE.txt'), $true)
            }
        }
    } finally { $archive.Dispose() }
    Write-Output "PASS $name archive SHA-256 $hash"
}
$version = & (Join-Path $native 'ffmpeg.exe') -version 2>&1 | Out-String
if ($LASTEXITCODE -ne 0 -or $version -match '--enable-(gpl|nonfree)|--enable-libx26[45]' -or $version -notmatch '--enable-shared') {
    throw "FFmpeg must be shared and have no GPL, nonfree, x264 or x265 flags. Got:`n$version"
}
$license = & (Join-Path $native 'ffmpeg.exe') -L 2>&1 | Out-String
if ($license -notmatch 'Lesser General Public License') { throw "Unexpected FFmpeg license:`n$license" }
Write-Output 'PASS FFmpeg shared LGPL configuration verified'
