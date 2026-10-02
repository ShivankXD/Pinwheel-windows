# Pinwheel for Windows

Windows parity port of Pinwheel: Edit Photos & Videos. P0 foundation is complete.
Editing features and mobile visual parity remain pending their planned phases.
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
and exits after successful rendering and decoding. It captures only the app's Skia
surface, including when another window covers it.
It also checks that a missing native runtime makes app startup return a failing
exit code; that deliberate failure is captured and verified separately.
See [PARITY.md](PARITY.md) and the [P0 report](docs/P0-REPORT.md).

The clean build and smoke run also passed on GitHub's Windows runner with JDK 17.
Local validation used JDK 23 and an NVIDIA RTX 4060 Laptop GPU. This does not
establish the later playback, seek or export performance budgets.

Native provenance and the P0 FFmpeg adapter choice are documented in
[NATIVE-DEPENDENCIES.md](docs/NATIVE-DEPENDENCIES.md).

Enable the owner's commit message guard with `git config core.hooksPath scripts`.
