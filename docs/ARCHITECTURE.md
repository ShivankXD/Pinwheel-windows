# Desktop architecture through P2

P0 implements the brief's six module boundaries. The core has no Compose, Android,
AWT or native dependency. Native adapters return `RgbaFrame`: straight-alpha,
sRGB 8-bit RGBA, top row first. ANGLE readback reverses GL rows exactly once.
The asymmetric triangle test verifies this convention.

`pinwheel-app` depends on the core, platform and rendering/media/photo adapters.
The adapters depend on the core and never on the UI. The photo module is a P6
boundary; the pure math required by mobile JVM tests is already in core.
P1 adds version-10 JSON, codecs, file stores, catalogs, typed serializable commands,
ProjectSession, persisted undo, gesture drafts and durable autosave. Core editing
and export policy consumes AuthProvider/Entitlement rather than UI state.
There is no editor UI, production playback/export or MCP implementation yet.

## Implemented contracts and later phases

- Copy mobile models and codecs with their current names, defaults, sanitization,
  stable IDs and project version 10 (read versions 1 through 10). Retired effect IDs
  are dropped on load. Add project-package import with relative media remapping.
- `ProjectSession.apply(EditCommand)` validates an action, records one undo entry,
  and autosaves. Slider and drag drafts produce one command when released.
  Command coverage follows every mobile action. No editing logic belongs in a UI callback.
- Keep main clips, effects, overlays, captions and audio as ordered timed layers.
  One time-based frame evaluator serves preview, still capture and export. It has
  no wall-clock inputs. Future keyframes must keep constant values compatible
  with today's JSON keys; no keyframe fields are implemented in P0.
- P2 owns shader programs, ping-pong buffers, history/trails and catalog goldens.
  P3 owns persistent decode, bounded queues and the audio master clock. P5 owns
  Media Foundation encoding and mixdown. P1 already enforces export limits.
- Future MCP clients can call the same session and evaluator after P8 owner
  approval. No MCP module or tool is added before that approval.

The P0 triangle uses the same context wrapper as P2 and releases its diagnostic
context afterward. P2 keeps a context alive for each renderer, owns it on one
thread and reference-counts shared EGL displays. It proves binding/lifetime and
orientation, not playback speed.
The P0 FFmpeg subprocess accurately decodes a requested demo frame but provides
no audio playback, hardware-decode verification, frame cache or low-latency seek claim.
It cannot implement MediaDecoder: playback factories are explicitly in-process.
The P3 interface carries D3D11VA/software selection, bounded frame/audio queues,
byte budgets, timestamps and seek generations, persistent lifetime and audio EOS.

P1 golden image I/O, metrics, reports and heatmaps live in pinwheel-render/qa.
Core remains free of AWT, Compose, Android and native bindings. See port notes for
the preserved shader data and preview input recipe.

P2 owns GLES textures/FBOs, programs, ordered ping-pong chains, previous/trail
buffers and seek resets. EffectRuntime executes legacy effects, main catalog
effects, Soft Glow, cut transitions, library looks and targeted layer effects
in mobile order. Caller-supplied layer frames are already painted at the layer
effect size, converted to premultiplied input and processed with colour/coverage
passes before compositing. Geometry, stickers, PIP and titles/caption painters
remain later work. Main and layer CPU boundaries are top-down straight RGBA8.

Preview tiles have their own source-backed crop/resample, SWAY, history offsets,
orientation and RGB565 conversion. Packed channel values come from Skia; normalized 5/6-bit expansion matches mobile PNG encoding rather than desktop bit repetition. Effect Lab creates a renderer on a dedicated
worker, not the Compose UI thread. Repeated timestamps reuse deterministic
16-slot loop entries within a 40-spec LRU cache. GPU constructors unwind partial
allocations; closing one context leaves other live renderers usable.

Catalog GLSL stays unchanged. Golden failures are recorded in P2-PARITY-BUGS.md;
diagnostic WARP/neutral frames never replace hardware golden outputs. P2 is not
accepted while its 252 strict-audit frame comparisons fail. No P3 evaluator/player
or P5 encoder has been added.

Storage resolves to `%APPDATA%/Pinwheel/{projects,originals}` and
`%LOCALAPPDATA%/Pinwheel/{cache,crash-reports}`. P0 only defines paths;
settings persistence and crash-report writing are later-phase work. P1 file
stores and autosave operate on an injected Windows files directory.

Owner amendment: noise classification selects the active shader MODE, removes
unused pure locals and traverses helpers from fx(). The manifest has source hashes
and complete evidence for all 422 specs. 193 noise specs use structural review;
229 deterministic specs retain strict pixel limits. Gaussian convolution and
histogram transport live in render QA, never in production frame evaluation.
The original pixel audit remains available. All structural limits are provisional
for owner review, and the current owner-policy gate still fails on 130 frames.
