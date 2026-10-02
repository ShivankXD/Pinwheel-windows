# P2 scope and acceptance

The owner reviewed P1 implementation and authorized P2 while generating mobile
inputs. P1 acceptance remains pending real projects. P2 ports the GLES program,
ordered chains, previous/trail buffers, cut transitions, library overlays,
layer colour/coverage effects and sample-photo SWAY preview renderer to ANGLE.
Effect Lab is debug-only. No P3 playback or beyond-mobile work is included.

Generate all 422 catalog specs at 0.3, 0.9, 1.5 and 2.1 seconds into
`evidence/p2/frames/<id>/<t>.png`. The mobile reference folder is read-only.
Run mobilePackagesCheck and goldenCheck when inputs appear. Each provided
failure is a bug to investigate, with full output and fixed tolerances.
Raw projects, packages and device-library JSONs are owner-provided inputs.

Mobile HEAD at P2 start: ae5ed52cb676cdb0e401d53124b7d5c049382377.
Read-only status at P2 start:

```text
?? app/src/androidTest/java/com/nativeoffice/studio/WindowsReferenceExport.kt
?? output/
```

The new untracked reference-export test was present before P2 work. It belongs
to the owner's reference generation. No mobile file or Git state is changed here.
The read-only reference folder was absent at P2 start.

Update: the owner supplied the complete reference folder at mobile commit
2a417fd. Its README was read first. P1 is closed with four passing JSON/package
inputs. P2 rendered all 1688 frames; 1411 pass and 277 fail the fixed golden
limits. P2 visual acceptance remains open; see P2-REPORT.md.

Later owner amendment: noise-driven specs receive Gaussian/histogram/mean review
with shared shaders preserved. Deterministic specs retain strict pixel limits.
See P2-NOISE-CLASS.md, STRUCTURAL-REPORT.md and provisional p2-noise-policy.json.
