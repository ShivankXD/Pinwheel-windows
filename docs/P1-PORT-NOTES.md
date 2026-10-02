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
The catalog has 277 effects, 46 transitions, 80 active overlay looks and 19
legacy overlay looks. The brief's 90 overlays does not match this source
snapshot. All 99 overlay IDs are preserved; legacy entries stay out of picker
categories, as on mobile.

All 44 JVM test classes are ported. Pure photo, RAW colour/TIFF, timeline,
caption, voiceover, scheduling and audio DSP math needed by those tests is in
core. Bitmap and native photo processing remain P6 work. Media3 DSP bindings
become PcmFormat/ByteBuffer methods; filter state and sample math are unchanged.
Pitch shifting via Sonic is P3 work and is not covered by these mobile JVM tests.
Frame/crop/clone/cutout and RAW linear/TIFF source extraction retains the tested
pure functions while excluding Android bitmap/Context entry points. Test
annotations, PCM bindings and ICC asset location change; assertions do not.

Mobile quirks retained: only the project's matching history is serialized;
the adjustments object and modified timestamp are required even in old
versions; inactive cutout background selections are not serialized; deleting
an image leaves its effects targeted at the now-absent layer, rather than
retargeting them to the main video. Mobile LibraryStorage does not count photo
cutout background references during original cleanup. Package import/export
does remap active cutout image references in current and historical recipes.

ProjectSession publishes state only after durable autosave succeeds; failures
retain the prior committed project and allow retry of a gesture draft. Core
Free checks catch canvas/draft effect injection. Undo/redo retains mobile
history behaviour after entitlement changes; export still checks the current
plan and rejects enabled effects on Free. A backend-linked Plus must match the
signed-in Google subject. No backend or real OAuth is implemented in P1.

Golden tooling lives in pinwheel-render, keeping image I/O and AWT out of core.
VideoPreviewRecipe retains mobile float arithmetic, sample crop, sample switch,
SWAY and default uniforms. P2 must preserve the 384x480 flipped sample upload,
the effect pass uFlipY=-1 and mobile RGB565 tile quantization. The golden harness
does no alignment, flipping or resampling to make comparisons pass.

The first compile failed because the source extraction included Android GLES
execution and a stray Android import. Full output is preserved in
`evidence/p1/first-core-compile-failure.txt`; extraction was corrected.

Passing synthetic/ported tests do not establish real mobile package or visual
parity. Owner packages, screenshots and dedicated per-effect PNGs are pending
at the read-only D:/Pinwheel-Windows-refs input location.
