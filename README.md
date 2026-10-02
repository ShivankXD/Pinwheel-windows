# Pinwheel for Windows

Windows parity port of Pinwheel: Edit Photos & Videos. P0 foundation in progress.
The authoritative brief is [docs/Pinwheel-Windows-Build-Brief.md](docs/Pinwheel-Windows-Build-Brief.md).

Requirements: Windows x64, JDK 17 or newer (P0 validated with JDK 23), internet
for first dependency download. Open PowerShell in this directory:

```powershell
./scripts/bootstrap-native.ps1
./scripts/build.ps1 -Smoke
./scripts/build.ps1 -Tasks ':pinwheel-app:run'
```

Modules: `pinwheel-core` headless frame contract, `pinwheel-render` ANGLE/GLES,
`pinwheel-media` FFmpeg adapter, `pinwheel-photo` P6 boundary,
`pinwheel-platform` Windows paths, `pinwheel-app` Compose theme and launch.
No MCP, keyframes, or advanced desktop features have been implemented.

Native runtimes are downloaded with pinned hashes. They are not taken from PATH.
The smoke run launches a real Compose window, captures the intro and P0 workspace,
and exits after successful rendering and decoding. See PARITY.md and docs/P0-REPORT.md.

Enable the owner's commit message guard with `git config core.hooksPath scripts`.
