# Pinwheel for Windows

Windows parity port of Pinwheel: Edit Photos & Videos. P0 is approved. P1 is closed
with four passing owner-supplied mobile JSON/package inputs. P2 implements the
effect runtime and debug Effect Lab, with all 422 shaders compiling and 1688
frames rendered. Golden acceptance remains open. The strict audit has 215 failures; under the
owner noise-class amendment, 18 deterministic and 84 structural frames still
fail. The 688 noise structural passes use provisional limits for owner review.
P3 is authorized and now has an in-process libav/D3D11VA decoder, shared GPU frame
evaluator, PCM-clock player/mixer and debug Player Lab. P3 acceptance remains open
for the remaining painters, full mobile stress assertions and hardware checks.
The editing screens and encoder remain later phases.
The authoritative brief is [docs/Pinwheel-Windows-Build-Brief.md](docs/Pinwheel-Windows-Build-Brief.md).

Requirements: Windows 10 22H2+ or Windows 11 x64, JDK 17 or newer (locally validated with JDK 23), internet
for first dependency download. Open PowerShell in this directory:

```powershell
./scripts/bootstrap-native.ps1
./scripts/build.ps1 -Smoke
./scripts/build.ps1 -Tasks ':pinwheel-app:run'
```

Modules: `pinwheel-core` models, codecs, catalogs, commands, undo, storage and
export policy; `pinwheel-render` ANGLE/GLES and golden/reference tooling;
`pinwheel-media` P0 FFmpeg probe, P3 persistent JNI decoder, audio mixer and player;
`pinwheel-photo` P6 boundary; `pinwheel-platform` paths and fake AuthProvider;
`pinwheel-app` Compose shell and controls compiled only into debug builds.
No MCP, keyframes, or advanced desktop features have been implemented.

Native runtimes are downloaded with pinned hashes. They are not taken from PATH.
The smoke run launches a real Compose window, captures the intro and P0 workspace,
and exits after successful rendering and decoding. It captures only the app's Skia
surface, including when another window covers it.
It also checks that a missing native runtime makes app startup return a failing
exit code; that deliberate failure is captured and verified separately.
See [PARITY.md](PARITY.md), [P1 report](docs/P1-REPORT.md), [P2 report](docs/P2-REPORT.md), [P3 report](docs/P3-REPORT.md),
[strict metrics and worst 20 heatmaps](evidence/p2/GOLDEN-REPORT.md),
[noise structural review](evidence/p2/STRUCTURAL-REPORT.md),
[reference inputs](docs/REFERENCE-INPUTS.md) and [owner decisions](docs/OWNER-DECISIONS.md).

```powershell
./scripts/build.ps1 -Tasks ':pinwheel-app:run','-Ppinwheel.debug=true'
./scripts/build.ps1 -Tasks ':pinwheel-render:p1References'
./scripts/build.ps1 -Tasks ':pinwheel-render:p2Frames',':pinwheel-render:mobilePackagesCheck',':pinwheel-render:goldenCheck'
./scripts/build.ps1 -Tasks ':pinwheel-app:effectLab','-Ppinwheel.debug=true'
python scripts/verify-p1-port.py --desktop-only
python scripts/verify-p2-port.py --desktop-only
python scripts/verify-p3-port.py --desktop-only
./scripts/build.ps1 -Tasks 'p3RuntimeCheck'
./scripts/build.ps1 -Tasks 'p3RuntimeCheck','-Ppinwheel.p3.performance=true'
./scripts/build.ps1 -Tasks ':pinwheel-render:p3Frames'
./scripts/build.ps1 -Tasks ':pinwheel-app:playerLab','-Ppinwheel.debug=true'
./scripts/build.ps1 -Tasks ':pinwheel-app:p3LabSmoke','-Ppinwheel.debug=true'
```

The default build excludes the developer Plus toggle. Debug/release outputs use
separate directories, and a jar check prevents debug classes or service metadata
from leaking into release. Auth is a fake for now; OAuth and backend billing are
P7 work. Subprocess decoding is restricted to setup probes and tests.

The clean build and smoke run also passed on GitHub's Windows runner with JDK 17.
Local validation used JDK 23 and an NVIDIA RTX 4060 Laptop GPU. This does not
establish the Intel Iris Xe playback budget or export performance. The optional
`pinwheel.p3.performance` check keeps the mobile 24 fps engine stress gate on
local hardware; hosted CI checks functionality and the 2.5 s freeze limit using
an injected PCM device clock, without claiming a runner GPU meets that budget.

Native provenance and the P0 FFmpeg adapter choice are documented in
[NATIVE-DEPENDENCIES.md](docs/NATIVE-DEPENDENCIES.md).

Enable the owner's commit message guard with `git config core.hooksPath scripts`.
