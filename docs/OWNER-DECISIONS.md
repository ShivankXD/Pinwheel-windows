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
