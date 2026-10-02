# P1 closeout and current P2 report

P1 is closed with four passing real mobile inputs. P2 runtime is implemented,
but visual acceptance remains open. The sample matrix correction reduced
strict failures from 252 to 215, with 37 new passes and zero pass-to-fail
regressions. Under the owner's noise policy, 18 deterministic frames and 84
noise structural frames still fail. Noise limits remain provisional. P3 has
not started.

## 1. What was built

| Module | Implementation and location |
|---|---|
| pinwheel-core | P1 models, ProjectStore v10 codecs, catalogs, typed commands, persisted undo and Free/Plus core/export limits. All four real JSON/package inputs still round-trip. |
| pinwheel-render | Existing FxProgram, ordered chain, previous/trail history, transitions, overlay looks, legacy effects, layer effects and compositor remain intact. All 422 preserved catalog shaders compile. |
| pinwheel-render | New `PreviewSamples` reproduces Android's crop-local float matrix and mapped-bound translation before rounding bitmap allocation. `PreviewTileRenderer` uploads those pixels and retains SWAY, default params, mobile quad, RGBA8 targets, RGB565 expansion, history and loop/cache behaviour. |
| pinwheel-render/qa | All 1688 actual frames refreshed in `evidence/p2/frames/<id>/<t>.png`. Full strict and structural comparisons, every channel metric, worst-20 galleries and 84 neutral sample tiles refreshed. |
| pinwheel-app | Debug-only Effect Lab actual-window capture refreshed. Search, fixed times/loop, params and mobile/desktop/heatmap panes remain available; release excludes the Lab and local Plus toggle. |
| pinwheel-render/qa | New deterministic and sampling probes isolate all seven remaining specs with raw RGBA8, quantization signatures, stored-input replays, crossed backends, precision queries and translated HLSL evidence. They never replace production frames. |
| scripts | `report-p2-sample-matrix.py` compares all frames against published baseline ddad406, records 21 sample geometries and every channel's before/after metrics. Structural reporting now regenerates `deterministic-failures.csv` to prevent stale manual reports. |
| media / platform / photo | Existing P1 boundaries remain. Playback/audio/evaluation are P3, editor UI P4, export P5 and photo work P6. No subprocess decoding was added to playback. |

The sample fix follows [AOSP Bitmap.createBitmap](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-14.0.0_r1/graphics/java/android/graphics/Bitmap.java).
Several samples map to 384.0000305 by 480.0000305 before allocating 384 by 480.
The former fixed destination rectangle discarded those float bounds. This is
a source-backed preparation repair; exact phone decode/raster bytes still
require the pending phone version and neutral probes. Shared GLSL, defaults,
thresholds, noise classes and NVIDIA/ANGLE backend were unchanged.

## 2. Tests and results

```text
Desktop unit tests: 236 passed, 0 failures, 0 errors, 0 skipped
  core 205; platform 2; media 3; render 26
Catalog shaders: 422 passed, 0 failed
P2 frames: 1688 rendered, 0 failures
Real mobile projects: 4 passed, 0 failed
Golden references: 1473 passed, 215 failed, 0 missing
Whole strict specs: 354 passed, 68 failed
Deterministic strict frames: 898 passed, 18 failed
Deterministic specs: 222 passed, 7 failed
Noise structural frames: 688 provisional passes, 84 failures
Noise specs: 160 provisional passes, 33 failures
Sample matrix: 592 changed frame hashes, 37 new strict passes, 0 strict regressions
PASS 404 source, test and asset hashes; all GLSL literals preserved
PASS 8 unchanged P2 shader literals; 7 pinned mobile runtime source hashes
PASS 193 noise and 229 deterministic classes; all 422 shader source hashes
PASS debug includes and release excludes debug entitlement controls
```

Two new regression tests cover float mapped bounds and actual asymmetric
crop/flip colours. Existing tests cover channel format, orientation, timing,
ordered effects, history resets, legacy effects, layers, resource ownership,
shader compilation, animation and bounded cache. Local aggregate runtime
checks pass. See `test-summary.json`, `sample-matrix-runtime-checks.txt`,
`sample-matrix-all-runtime-checks.txt`, `sample-matrix-lab-and-neutral-checks.txt`
and the successful follow-up `final-sampling-and-runtime-checks.txt`
under `evidence/p2`.

`goldenCheck` intentionally exits nonzero. **Full current failing output:**
[sampling-reference-checks.txt](../evidence/p2/sampling-reference-checks.txt).
The preceding matrix failure log remains retained.
Deterministic limits remain MAE8 <= 2 and p99 <= 8 in each RGBA channel, with
no reference alignment, resizing or filtering. The original strict audit
retains all specs: 170 frames exceed MAE; 45 fail only p99, including 19 with
maximum p99 exactly 9. Worst Scene Cut at 0.9 s is R 94.757856/156,
G 96.116276/157, B 95.700412/157, A 0/0 (MAE8/p99).

- [All 422 per-spec channel metrics](../evidence/p2/per-spec-metrics.csv) and [all 1688 per-time metrics](../evidence/p2/golden-metrics.csv).
- [Worst 20 strict report](../evidence/p2/GOLDEN-REPORT.md), [mobile/desktop/heatmap gallery](../evidence/p2/worst-20.html) and [contact sheet](../evidence/p2/worst-20-contact-sheet.png).
- [Every noise spec and structural metric](../evidence/p2/STRUCTURAL-REPORT.md), [full channel CSV](../evidence/p2/noise-spec-metrics.csv) and [worst 20 structural heatmaps](../evidence/p2/noise-worst-20.html).
- [Source-backed matrix comparison](../evidence/p2/diagnostics/sample-matrix/SAMPLE-MATRIX-REPORT.md) and [full before/after JSON](../evidence/p2/diagnostics/sample-matrix/sample-matrix-comparison.json).
- [Seven remaining deterministic specs](../evidence/p2/deterministic-failures.csv) and [parity investigation](P2-PARITY-BUGS.md).

Noise review uses Gaussian sigma 8 px, radius 24, edge clamping and float
sRGB8; channel histogram Wasserstein-1 and mean errors; Rec.709 mean luminance
error. Proposed limits are 2/255 for each metric, pending owner review. Scene
Cut's global branch mismatch still fails; blur does not excuse it. No failing
deterministic spec was reclassified.

Remaining strict bugs include Diamond's proven GPU storage half-value tie,
Pixel Creation/Mosaic grid-floor sensitivity, CMYK fine-dot sampling, Carousel,
Neon Edges and Dance Flash. Raw constants, endpoint classifications and grid
heatmaps are under `diagnostics/numeric`. Earlier RGBA32F/RGBA16F conversion
experiments are retained under `diagnostics/framebuffer`; they used the
pre-matrix baseline. Neither format is adopted. Production stays RGBA8.

The follow-up stage controls confirm 56 byte-exact stored-input replays and
28 unchanged NVIDIA production hashes. On the seven remaining specs,
NVIDIA/NVIDIA passes 10/28, WARP/WARP 4/28, NVIDIA/WARP 0/28 and WARP/NVIDIA
14/28. Crossed routes remain diagnostics. Input-stage bias accounts for much
of WARP's increased Neon/Dance error; constant photo-free controls and
identical fragment HLSL further narrow the investigation. Both desktop
contexts report 23-bit precision for high, medium and low floats. Phone
precision and raw inputs are still required before adopting a production repair.

[Stage report](../evidence/p2/diagnostics/deterministic/DETERMINISTIC-REPORT.md),
[all stage channel metrics](../evidence/p2/diagnostics/deterministic/stage-metrics.csv)
and [remaining-seven gallery](../evidence/p2/diagnostics/deterministic/remaining-seven.html)
provide the measurements and heatmaps. An initial LWJGL buffer guard failure
in the precision query was corrected; full initial output is
`sampling-and-runtime-checks.txt`, successful output is
`final-sampling-and-runtime-checks.txt`. All 236 unit tests still pass.
Production acceptance counts remain unchanged.

Sample matrix runtime 614e447 passed hosted Windows/JDK 17 CI [run 37011571663](https://github.com/ShivankXD/Pinwheel-windows/actions/runs/37011571663).
All 15 CI steps succeeded, including the two new sample regressions. CI checks
runtime, shader/frame generation, numeric probes, source/class
verification, debug guard and actual-window smoke. Owner goldens are local
inputs; hosted CI does not certify pixel acceptance. `ci-result.json` names
the exact tested runtime commit. Historical failures and repaired diagnostics
remain in earlier logs and Git history.

## 3. Screenshots

[Effect Lab and matching mobile effects page](../evidence/p2/effect-lab-and-mobile.html)
pairs the refreshed actual Compose capture with the owner's effects screenshot.
The Lab shows the Scene Cut mismatch and its current failing channel metrics.
The mobile editor page is a P4 layout reference; this does not claim editor UI
parity. Both worst-20 galleries pair mobile/desktop/heatmap images. The new
remaining-seven gallery also shows both input-stage controls and their metrics.

No personal gallery thumbnail from `06-media-picker.png` is published. Export
settings will use mobile `VideoExportScreens.kt`; supplied screen 15 is the
sign-in gate.

## 4. PARITY.md

[PARITY.md](../PARITY.md) records the matrix fix, current gate counts and
source/reference provenance. Catalog groups remain partial until required
comparisons pass. Unit tests do not establish editing/UI or playback/export
parity. P2 acceptance remains open.

## 5. Owner inputs and remaining work

Exact phone GL_RENDERER/GL_VERSION, Android/Skia version, medium/low precision,
and matching raw numeric/neutral probes remain pending. The proposed
`tools/mobile/P2ParityProbe.kt` is stored only in this Windows repo and has
not been compiled, copied or executed on Android. Existing goldens remain
authoritative. Structural metric limits still need owner review.

Device-library JSONs under `projects/device-library` were not supplied.
Google Desktop OAuth client ID and the verified shared Plus backend remain
P7 inputs and do not block this P2 work.

## 6. Read-only confirmation

Mobile status remains exactly `?? output/`; HEAD remains
`2a417fd29ef43a8792eb4d58cbe4d102327b11c9`. All 1715 reference hashes remain
unchanged, with zero additions, missing or modified files. See
`evidence/p2/mobile-status.txt` and `reference-read-only-check.txt`.
No mobile write, build, Git mutation or phone action occurred. Generated
frames, imported test data, logs and reports stay in the Windows repo.
