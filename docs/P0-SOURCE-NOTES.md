# Read-only mobile reference notes

Reference repository: `D:/Nativeoffice-photo&videoeditor`.
Observed HEAD: `ae5ed52cb676cdb0e401d53124b7d5c049382377`.
Initial and final observed status: `?? output/`.

The supplied brief was read completely before setup. The section 1.2 documents
were read in order: README, the Native Office plan and checkpoint; video
blueprint, effects authoring, PIP, history and checkpoints 01 through 05; photo
reference and checkpoints, RAW, lens profiles and guided perspective; brand
and branding source; caption assessments/rechecks and voiceover reliability.

Source inspection followed models, data, catalog/shaders, render graph,
StudioViewModel, VideoEditor and PhotoEditor. Models, codecs, catalog contracts,
the render graph, intro/theme source, entry points and action inventories informed
the boundaries. Large editor files were inspected by their relevant sections
and action definitions; this is not a claim that every line of every editor
or every test has already been reviewed or ported.

Test inventories and named scenarios were inspected as specifications.
No mobile tests, Gradle builds, phone actions or emulator actions were run.
No mobile git checkout, stash, clean, commit or other state-changing command ran.

## Findings that constrain later phases

- Project JSON is version 10 and reads 1 through 10. Preserve defaults, clamps,
  stable catalog IDs, layer order and persisted undo rules.
- Keep limits: 40 effects, 32 overlays, 16 audio tracks, lane index 0 through 63.
- The current `HomeKit.kt` intro uses a 520 ms appearance, 2600 ms fan rotation,
  55 ms letter cadence and 650 ms finishing pause. The desktop copies that
  geometry and sequence. Current theme source supersedes older brand sketches.
  Desktop font rasterization and timing still require a current mobile capture.
- Free users are blocked from effects, with enforced 1080p/30 fps/8 Mbps export
  limits and a forced 2.5 s ending. Plus defaults to no ending and permits
  4K/60 fps/100 Mbps. Neither plan is implemented in P0.
- Preserve the fourteen regression invariants in section 5; they are explicitly
  tracked in PARITY.md. Do not silently alter a mobile quirk.
- `VideoFxCatalogTest.everyEffectDrawsAPictureAndMoves` checks the brief's
  animation times, but current `writeContactSheets` uses 0.2, 0.7, 1.2, 1.8 s.
  The required golden comparison times are 0.3, 0.9, 1.5, 2.1 s. Ask for renders
  at those exact times rather than treating the existing sheets as equivalent.
- Existing saved welcome screenshots use an earlier white screen and do not
  match the current dark startup intro. No matching current startup reference
  was found. Do not substitute a different screen or a third-party app capture.

P0 establishes the Windows runtime. All editing features and catalog parity
remain unimplemented until their scheduled phases and evidence are available.
