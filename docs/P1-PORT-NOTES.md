# P1 source contract

Pinned mobile commit: ae5ed52cb676cdb0e401d53124b7d5c049382377.
All mobile inputs are read-only. `p1-source-manifest.json` records original and
ported file hashes. `scripts/port-p1-sources.py` reproduces the listed ports.

Models, codecs, history and catalog data retain field names, defaults, clamps,
IDs, ordering and shader strings. Namespace and top-level visibility change so
the Windows modules can consume the contracts. No project version bump.

Windows adapters replace Context with File/Path, Android AtomicFile with an
fsynced staging file and atomic rename, and Android URI parsing with java.net.URI.
The project JSON codec is separated from its store for headless import/testing.
PhotoLooks has no Android SharedPreferences migration source on Windows.
Android painters and GLES execution are excluded from catalog registries. The
shader source builder and SWAY strings are preserved for P2.

The caption_fonts folder has 32 files: 17 TTF fonts and 15 license files. The
brief's reference to 32 fonts does not match the pinned mobile inventory.

The first compile failed because the source extraction included Android GLES
execution and a stray Android import. Full output is preserved in
`evidence/p1/first-core-compile-failure.txt`; extraction was corrected.

Passing synthetic/ported tests do not establish real mobile package or visual
parity. Owner packages, screenshots and dedicated per-effect PNGs are pending
at the read-only D:/Pinwheel-Windows-refs input location.
