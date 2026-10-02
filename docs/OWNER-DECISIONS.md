# Owner decisions after P0 approval

These decisions amend the original brief and take precedence where they differ.

1. P0 is approved. Proceed with P1 core models, codecs, catalogs, typed commands,
   persisted undo/redo and relevant ported JVM tests.
2. Target Windows 10 22H2+ and Windows 11, x64. Windows 10 21H2 is dropped.
3. Define `AuthProvider` and a fake implementation now. The Google Desktop-app
   OAuth client ID will arrive before P7 and does not block P1.
4. Define `Entitlement`. The intended Plus entitlement is shared by phone and PC,
   linked to the Google account and verified by a later backend. A local Plus
   toggle exists only in debug builds. Enforce Free/Plus rules in core/export
   policy now; backend verification and billing are P7 work.
5. FFmpeg subprocess decoding is allowed only for P0/P1 tests and probes.
   P1 defines an in-process playback decoder contract for P3: libav/JavaCPP/JNI,
   D3D11VA, persistent contexts, bounded queues and audio. The playback path
   cannot accept the subprocess probe adapter.
6. `writeContactSheets` uses 0.2, 0.7, 1.2 and 1.8 seconds. Golden references
   will be dedicated mobile `VideoFxPreviewRenderer` PNGs at 0.3, 0.9, 1.5 and
   2.1 seconds, with the same sample photos and SWAY motion. Mobile screenshots
   and project packages will also arrive later. Build the harness against
   `D:/Pinwheel-Windows-refs`, which is read-only input. Do not create or edit
   files in that folder. Missing references must be reported, never fabricated.
7. All other brief rules remain. Mobile remains strictly read-only.
8. P1 implementation reviewed. P1 acceptance follows real mobile input checks.
   P2 is authorized while those inputs are being generated. Golden coverage is
   277 effects, 46 transitions, 80 active overlays and 19 legacy overlays, all
   at 0.3/0.9/1.5/2.1 s from VideoFxPreviewRenderer. Inputs also include raw
   projects, .pinwheel packages, device-library JSONs and mobile screenshots.
   Failures must be explained as bugs; thresholds must never be relaxed.
9. Real references arrived from WindowsReferenceExport at mobile commit
   2a417fd: 1688 golden PNGs, four JSON/package inputs and 21 mobile screenshots.
   Close P1 using those inputs, continue P2 and report per-spec MAE/p99 plus the
   worst 20 with heatmaps. Read the reference README first. Export settings
   use VideoExportScreens.kt because the supplied export screen is the sign-in
   gate. Media-picker gallery thumbnails are layout references only.
10. Noise-class amendment: preserve shared GLSL; flag specs driven by hash/sin
    noise and review Gaussian-blurred MAE plus channel histogram/mean-luminance
    metrics instead of strict per-pixel p99. Keep the strict pixel thresholds for
    deterministic specs. List every noise spec, metric result and heatmap for
    owner review. Exact phone GL_RENDERER/GL_VERSION will be added to the
    reference manifest later. Structural limits are currently provisional.
