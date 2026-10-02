# Desktop foundation

P0 implements the brief's six module boundaries. The core has no Compose, Android,
AWT or native dependency. Native adapters return `RgbaFrame`: straight-alpha,
sRGB 8-bit RGBA, top row first. ANGLE readback reverses GL rows exactly once.
The asymmetric triangle test verifies this convention.

`pinwheel-app` depends on the core, platform and rendering/media/photo adapters.
The adapters depend on the core and never on the UI. The photo module is an empty
P6 boundary. There is no editor, project codec, command bus or MCP implementation yet.

## Contracts planned for P1 and later

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
  Media Foundation encoding, mixdown and export limits.
- Future MCP clients can call the same session and evaluator after P8 owner
  approval. No MCP module or tool is added before that approval.

The P0 triangle allocates a new pbuffer/context for each diagnostic call and
releases it afterward. It proves the binding and orientation, not playback speed.
The P0 FFmpeg subprocess accurately decodes a requested demo frame but provides
no audio playback, hardware-decode verification, frame cache or low-latency seek claim.

Storage resolves to `%APPDATA%/Pinwheel/{projects,originals}` and
`%LOCALAPPDATA%/Pinwheel/{cache,crash-reports}`. P0 only defines paths;
settings persistence, autosave and crash-report writing are later-phase work.
