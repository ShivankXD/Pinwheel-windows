# P0 dependency provenance and choices

Exact download URLs, source revisions and archive SHA-256 values are in
[`native/dependencies.json`](../native/dependencies.json). The bootstrap verifies
each archive before extracting into the ignored `native/windows-x64` directory.
No installed FFmpeg on PATH is used.

## ANGLE and LWJGL

The binaries come from Google's official Chromium Windows x64 snapshot 1368503.
Chromium revision: `111b71b7ed5839024c2c2df2e2b055e53c6de550`.
Its DEPS file pins ANGLE revision `367e9e74a865aa65f6fc1c03bab63397a1c67baa`.
The P0 run reports ANGLE 2.1.24077 and explicitly requests the D3D11 backend.
LWJGL 3.3.6 binds EGL and GLES. Shaders use GLSL ES syntax unchanged.

Written reason for the older snapshot: the initially checked snapshot 1709568
contained small stub DLLs with no usable `eglGetDisplay` export. The selected
official snapshot has working exported EGL/GLES functions. The deprecated
Microsoft Windows Store ANGLE package was considered and abandoned. This
foundation needs later catalog compilation and golden comparison before parity
can be claimed; a successful triangle is insufficient.

LWJGL's `EXTPlatformBase` wrapper rejects a zero native display, while ANGLE's
default display is zero. One call uses LWJGL JNI with the identical ABI and the
resolved `eglGetPlatformDisplayEXT` pointer. Global LWJGL safety checks remain on.
Two real GPU tests verify pixels, orientation and context teardown/recreation.

ANGLE, Chromium and LWJGL notices are in `licenses/`. Chromium's licence text
should be checked against the final packaged runtime during P7.

## FFmpeg

The chosen BtbN release is `autobuild-2026-09-30-13-08`, FFmpeg
`n8.1.3-9-g29e619e767`, Windows x64 **lgpl-shared-8.1**. The month-end release
is selected because the [producer's retention policy](https://github.com/BtbN/FFmpeg-Builds#release-retention-policy)
keeps those releases for two years, while daily releases are kept for 14 builds.
It is pinned, not the moving `latest` URL. Long-term source/binary retention
and installer distribution are P7 work.

The bootstrap checks `--enable-shared`, rejects `--enable-gpl`, `--enable-nonfree`,
`--enable-libx264` and `--enable-libx265`, and checks the reported LGPL licence.
The producer's LGPLv3 notice and the supplemental GPLv3 text incorporated by
LGPLv3 are displayed in the app. Including that supplemental text does not mean
the selected build enables GPL-only codecs.

Written reason for a subprocess adapter in P0: it proves real demux/decode and
RGBA delivery without introducing JNI lifecycle code before the frame engine
exists. P3 still requires persistent decode and bounded queues, using the
recommended native bridge/presets and D3D11VA. Hardware decode, playback
performance and exports are unverified. The producer currently gives Windows
10 22H2 as its minimum supported version; the brief's Windows 10 21H2 target
is not validated and needs a compatible build or an explicit owner decision
before release.

## Gradle and Compose

Gradle 8.14.5 comes from the official distribution with its published SHA-256.
The wrapper is present. On this machine the Java wrapper download timed out;
`scripts/build.ps1` uses a curl-based, checksum-verified bootstrap to the same
distribution. Kotlin and its Compose compiler are both 2.2.20; Compose Desktop
is 1.10.3 and Material3 is explicitly pinned to 1.9.0-beta03. JVM bytecode
targets 17. The local validation runtime is JDK 23. CI uses JDK 17.

Dependencies are pinned for this foundation; they are not described as latest.
The Compose version is a [published release](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html).
The experimental screenshot call is confined to the P0 smoke harness. It reads
only this application's Skia surface and never captures another desktop window.

## Copied mobile assets

The demo video/photo and fan artwork are byte-identical copies of mobile assets.
Copied mobile notices remain under `licenses/mobile`, including the original
third-party notice file. P0 does not bundle caption fonts/packages, music,
LibRaw binaries or a lens engine. Their assets and notices must travel together
when those features are ported. The P0 licences dialog reads the checked-in
notices; complete distributable notices and source/relink materials remain P7.
