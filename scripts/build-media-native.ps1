$ErrorActionPreference = 'Stop'
if (!$env:JAVA_HOME -or !(Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'include/jni.h'))) { throw 'Set JAVA_HOME to a JDK with JNI headers before native bootstrap' }
$pinwheelRoot = Split-Path -Parent $PSScriptRoot
$pinwheelManifest = Get-Content -LiteralPath (Join-Path $pinwheelRoot 'native/dependencies.json') -Raw | ConvertFrom-Json
$pinwheelArchive = Join-Path $pinwheelRoot ('.downloads/' + $pinwheelManifest.ffmpeg.archive)
if ((Get-FileHash -LiteralPath $pinwheelArchive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $pinwheelManifest.ffmpeg.sha256) { throw 'FFmpeg SDK archive hash mismatch' }
$pinwheelSdk = Join-Path $pinwheelRoot 'native/sdk/ffmpeg'
New-Item -ItemType Directory -Path $pinwheelSdk -Force | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$pinwheelZip = [IO.Compression.ZipFile]::OpenRead($pinwheelArchive)
try {
    foreach ($pinwheelEntry in $pinwheelZip.Entries) {
        if ($pinwheelEntry.FullName -match '^[^/]+/((include|lib)/.+[^/])$') {
            $pinwheelTarget = Join-Path $pinwheelSdk $Matches[1]
            [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($pinwheelTarget)) | Out-Null
            [IO.Compression.ZipFileExtensions]::ExtractToFile($pinwheelEntry,$pinwheelTarget,$true)
        }
    }
} finally { $pinwheelZip.Dispose() }
$pinwheelVsWhere = Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio/Installer/vswhere.exe'
$pinwheelVs = & $pinwheelVsWhere -version '[17.0,18.0)' -products '*' -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
if (!$pinwheelVs) { throw 'Visual Studio 2022 C++ Build Tools required for the JNI bridge' }
$pinwheelCmake = Join-Path $pinwheelVs 'Common7/IDE/CommonExtensions/Microsoft/CMake/CMake/bin/cmake.exe'
$pinwheelBuild = Join-Path $pinwheelRoot 'native/media/build'
& $pinwheelCmake -S (Join-Path $pinwheelRoot 'native/media') -B $pinwheelBuild -G 'Visual Studio 17 2022' -A x64 "-DFFMPEG_SDK=$pinwheelSdk" "-DNATIVE_OUTPUT=$(Join-Path $pinwheelRoot 'native/windows-x64')" "-DCMAKE_GENERATOR_INSTANCE=$pinwheelVs"
if ($LASTEXITCODE -ne 0) { throw 'JNI configure failed' }
& $pinwheelCmake --build $pinwheelBuild --config Release --parallel 2
if ($LASTEXITCODE -ne 0) { throw 'JNI build failed' }
Write-Output 'PASS persistent libav JNI bridge built against pinned shared LGPL SDK'
