# P1 phase report

Status: implementation and local validation ready. P1 acceptance is pending real
mobile project round trips. No P2 playback/render parity claim is made.

## 1. What was built and where

| Module | P1 work |
|---|---|
| pinwheel-core | All 10 mobile model files; v10 JSON reading 1..10; codecs, atomic project/preset/version stores and library cleanup; project-package remapping; catalogs and shader strings; typed JSON commands, ProjectSession, persisted undo and one-commit gesture drafts; Free/Plus export policy; pure math for all mobile JVM tests |
| pinwheel-platform | AuthProvider fake, initially signed out; Windows storage paths retained |
| pinwheel-media | In-process persistent video/audio decoder interface for P3, D3D11VA/software selection, bounded queue/byte budgets and seek generation contract. Subprocess probe is excluded from playback |
| pinwheel-render | Read-only mobile package/golden input harness, exact per-channel metrics and diff heatmaps. No P2 effect runtime yet |
| pinwheel-app | Shared application auth/entitlement services; Plus toggle compiled only in debug; separate variant outputs and release-jar guard; copied notices remain visible |
| pinwheel-photo | P6 native/bitmap editor boundary retained. Tested pure photo/RAW math is in core |

Windows target is 10 22H2+ and 11, x64. OAuth client and shared-account backend
remain scheduled for P7. Account-linked Plus checks the signed-in Google subject.
No phone/emulator actions or mobile writes occurred.

Pinned inventory: 277 effects, 46 transitions, 80 active overlays plus 19 legacy
overlays. All 99 overlay IDs are preserved. This differs from the brief's 90.
Font inventory is 17 TTF fonts and 15 license files, rather than 32 fonts.
404 source/test/asset hashes and GLSL literals are checked by the provenance audit.
See `P1-PORT-NOTES.md` for adapters and retained mobile quirks.

## 2. Tests and results

Local Windows x64, JDK 23, bytecode targeting JDK 17:

```text
pinwheel-core: 205 tests, 0 failures, 0 skipped
pinwheel-platform: 2 tests, 0 failures, 0 skipped
pinwheel-media: 3 tests, 0 failures, 0 skipped
pinwheel-render: 9 tests, 0 failures, 0 skipped
TOTAL: 219 tests, 0 failures, 0 skipped
BUILD SUCCESSFUL in 1m 27s
33 actionable tasks: 33 executed
Caption-shortening regression follow-up: BUILD SUCCESSFUL in 30s
PASS missing-native smoke returns nonzero exit code with the expected library failure
PASS 404 source, test and asset hashes; all GLSL literals preserved
```

All 44 mobile JVM test classes are ported, including photo/RAW, DSP, scheduling,
voiceover, captions and UI helper mathematics. New checks cover autosave failures,
history reload/branching, Free bypasses, layer limits, package media/history,
unsafe zips, automatic caption trim/drop after clip shortening, account-linked export gates and golden metric thresholds.

Debug checks passed: the toggle drives shared core/export entitlement, fake
sign-out blocks export, and debug controls are included only in debug jars.
Release checks passed: developer classes and service metadata are absent.
The final runtime commit ca92d2b passed [Windows/JDK 17 CI](https://github.com/ShivankXD/Pinwheel-windows/actions/runs/36981850747), including clean build, source/asset hashes, debug controls, real-window smoke and expected startup failure. `evidence/p1/ci-result.json` records every successful step. The local total includes the caption-shortening regression follow-up.

Real-window smoke verifies ANGLE D3D11, demo decode and notice display. Current
hardware is RTX 4060 Laptop; later preview/seek performance budgets are untested.

`evidence/p1/clean-build-output.txt`, `debug-build-output.txt` and
`test-summary.txt` preserve passing output. Full failing outputs are attached as
`first-core-compile-failure.txt`, `platform-dependency-compile-failure.txt`,
`package-compile-failure.txt`, `session-test-failure.txt`,
`contracts-test-failure.txt`, `math-dependency-compile-failure.txt`,
`catalog-test-failure.txt` and `variant-script-compile-failure.txt`.
These were repaired and subsequent checks pass. Failures included Android imports,
missing adapter dependencies, incorrect synthetic fixture expectations and a
preview sample mapping mistake caught by the catalog asset test.

Strict reference checks deliberately fail while inputs are absent:

```text
Golden references: 0 passed, 0 failed, 1688 missing
Real mobile projects: 0 passed, 0 failed, MISSING
```

Full outputs: `expected-missing-mobile-projects.txt`,
`expected-missing-goldens.txt`. Inventory is in `reference-status.json`.
Missing input is not counted as passing parity. P1 real-project acceptance and
P2 golden pixel acceptance remain pending.

## 3. Screenshots and matching mobile screens

`evidence/p1/debug-controls.png` is the real debug window with the developer
Free/Plus control. `release-workspace.png` shows the default release shell.
Core tests have no new editor screen. Matching owner-supplied mobile screenshots
are unavailable, so no side-by-side visual parity claim is made. No mobile
reference was fabricated and no phone/emulator was accessed.

## 4. Parity checklist

`PARITY.md` now records proven P1 contracts separately from incomplete feature
parity. Catalogs/models/commands are implemented; GPU pixels, production playback,
editing screens, encoding, installers, real OAuth/billing and P8 sign-off remain.

## 5. Owner inputs still needed

Provide a couple of real mobile photo/video project JSONs or packages, dedicated
per-effect reference PNGs and screenshots at the read-only reference location
when ready. `REFERENCE-INPUTS.md` documents the layout and strict check commands.
The reference folder is currently absent and was not created by this work.
Google Desktop-app OAuth client ID remains due before P7. Shared-account Plus
backend direction is recorded; neither blocks current P1 implementation.

## 6. Mobile repository confirmation

```text
git -C "D:\Nativeoffice-photo&videoeditor" status --short
?? output/
```

This matches the original baseline exactly. Checked with GIT_OPTIONAL_LOCKS=0.
Mobile HEAD remains ae5ed52cb676cdb0e401d53124b7d5c049382377.
Commits use ShivankXD's configured author/committer identity, without em dashes
or AI/co-author trailers. Every commit is pushed to origin/main.
