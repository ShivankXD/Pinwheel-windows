# P1 closeout and P2 runtime report

P1 is closed with four passing real mobile inputs. P2 runtime implementation is
available, but P2 visual acceptance remains open. The RGB565 readback fix reduces the strict audit from 277 to 252 failures.
Under the owner noise amendment, 46 deterministic frames and 84 noise structural
frames still fail; 688 noise structure passes are provisional. P3 has not started. References are from the owner's mobile
`WindowsReferenceExport`, commit `2a417fd`, as described in the read-only README.

## 1. What was built

| Module | Implementation and location |
|---|---|
| pinwheel-core | P1 models, v10 codecs, catalogs, commands, persisted undo and core/export Free/Plus enforcement remain intact. Four owner JSON/package inputs now validate real mobile round trips. |
| pinwheel-render | `AngleDevice`, `FxProgram`, `FxChain`, `EffectPlan`, `EffectRuntime`, `LegacyEffects`, `LayerFx` and `LayerCompositor`: persistent thread-owned ANGLE/D3D11 context; ordered active effects; ping-pong targets; previous/trail history and seek resets; cut transitions; opacity-scaled library looks; all six legacy kinds; colour/coverage layer effects. |
| pinwheel-render | `PreviewTileRenderer`: mobile sample crop/resample, SWAY uniforms, history sample offsets, 2.4-second/16-slot loop, 40-spec cache, orientation and normalized RGB565 readback matching mobile PNG channel levels. All 422 unchanged catalog shaders compile. |
| pinwheel-render/qa | `P2Evidence` writes 1688 frames to `evidence/p2/frames/<id>/<t>.png`. Strict comparison measures every RGBA channel without resizing, alignment or filtering. Independent project/golden modes prevent one check from reporting the other's failures. |
| pinwheel-app | Debug-only `EffectLab`: searchable catalog, fixed times/loop, parameter controls, mobile/desktop/heatmap panes and per-channel MAE/p99. Release jar guards exclude the Lab and local Plus toggle. |
| pinwheel-media / platform / photo | Existing P1 boundaries remain. Production decoding, audio, frame evaluation, UI painters, encoding and photo pipeline belong to later phases. The runtime accepts decoded main frames and already-painted layer frames; it does not use subprocess playback. |
| scripts / CI | Provenance audits, reproducible metrics/heatmap gallery, P2 clean-build/runtime checks, all shader/frame evidence, debug guard and real-window smoke on Windows/JDK 17. CI has no owner reference folder and does not claim mobile golden acceptance. |

Partial GPU construction now cleans up acquired resources. Context disposal
preserves other live renderers. Legacy effect limits count entries in the
original mobile effect list, including catalog positions.

## 2. Tests and results

```text
Desktop unit tests: 234 passed, 0 failures, 0 errors, 0 skipped
  core 205; platform 2; media 3; render 24
Catalog shaders: 422 passed, 0 failed
P2 frames: 1688 rendered, 0 failures
Real mobile projects: 4 passed, 0 failed, provided
Golden references: 1436 passed, 252 failed, 0 missing
Whole specs: 336 passed, 86 failed (all four times must pass)
PASS 404 source, test and asset hashes; all GLSL literals preserved
PASS 8 unchanged P2 legacy/glow/layer shader literals; 7 pinned mobile runtime source hashes
```

The runtime tests cover orientation, inclusive/exclusive timing, effect order,
history and the exact 250 ms seek boundary, cut windows, overlay intensity,
orphan target routing, layer coverage/compositing, legacy effects, context
lifetime/thread ownership, constructor failure recovery, animation and cache
behaviour. See `evidence/p2/test-summary.json`, `final-clean-build.txt`,
`final-runtime-checks.txt`, `final-structural-tests.txt` and `shader-compile.json`.
The clean build covered 230 tests; the later three structural tests bring the
aggregate to 233. The exhaustive RGB565 format regression adds one more, bringing the current total to 234; see `rgb565-runtime-checks.txt`, `rgb565-regression-before.txt` and `rgb565-regression-after.txt`. A debug compile failure caused by a missing direct JSON dependency
was fixed and the real Lab smoke/project checks then passed; full initial output
is in `noise-lab-and-project-checks.txt`, final output in
`final-noise-lab-and-project-checks.txt` and `final-noise-lab-smoke.txt`.

The latest strict check deliberately exits nonzero because provided images
fail. **Current failing output:** `evidence/p2/rgb565-owner-policy-check.txt`. The original strict output remains in `final-reference-checks.txt`.
Earlier failures and their repairs remain in `first-gpu-tests.txt`,
`runtime-and-lab-checks.txt` and `first-mobile-checks.txt`. An initial order-test
fixture used an inactive burst time; its corrected active-time assertion passes.
An initial project-mode harness erroneously included golden failures; modes
now compare independently. Neither repair changed mobile shaders or limits.

For deterministic specs, MAE8 must be <= 2 and p99 <= 8 in **each RGBA channel**, equivalent to
2/255 and 8/255 in normalized sRGB error. 170 failing frames exceed MAE;
another 82 pass MAE but fail p99, including 56 whose maximum p99 is 9.
This is the preserved strict audit. The owner later authorized structural review for noise-driven specs, as detailed below. The worst frame is Scene Cut at 0.9 s: R 94.760/156,
G 96.124/157, B 95.707/157, A 0/0 (MAE8/p99).

- [All 422 per-spec metrics](../evidence/p2/per-spec-metrics.csv): each channel's maximum MAE and p99 over four times, plus spec status.
- [All 1688 per-time metrics](../evidence/p2/golden-metrics.csv): exact channel metrics at every required time.
- [Worst 20 report](../evidence/p2/GOLDEN-REPORT.md), [mobile/desktop/heatmap gallery](../evidence/p2/worst-20.html) and [contact sheet](../evidence/p2/worst-20-contact-sheet.png).
- [Parity bug investigation](P2-PARITY-BUGS.md): measured diagnostics, all 22 remaining deterministic specs and unresolved causes. [Deterministic per-channel failures](../evidence/p2/deterministic-failures.csv) retain every channel maximum.

Runtime commit `76337f3` passed hosted Windows CI:
[run 36989939920](https://github.com/ShivankXD/Pinwheel-windows/actions/runs/36989939920).
Follow-up lifetime/audit commit `f1e7425` also passed hosted CI [run 36992291621](https://github.com/ShivankXD/Pinwheel-windows/actions/runs/36992291621). Noise-review runtime `2e233ee` passed [run 36995074569](https://github.com/ShivankXD/Pinwheel-windows/actions/runs/36995074569), including clean checks, shader/frame generation, source-based class verification and actual-window smoke. Details are in `evidence/p2/ci-result.json`. Hosted CI does not have the owner reference folder; local golden acceptance still fails. RGB565 runtime commit `3654d82` passed [run 36999180247](https://github.com/ShivankXD/Pinwheel-windows/actions/runs/36999180247), including the new numeric probes and all runtime/source/variant/smoke checks. The current CI evidence names that exact runtime commit.

The current owner-policy goldenCheck uses 193 source-classified noise specs and
229 deterministic specs. Unused noise locals in shared preludes are excluded.
It reports 870 deterministic frame passes and 46 failures; 688 noise provisional
passes and 84 structural failures. 207 deterministic specs pass all times;
160 noise specs pass every proposed metric at all times. P2 is not accepted.

Noise metrics use Gaussian sigma 8 px, radius 24, edge clamping and float sRGB8;
channel histogram Wasserstein-1 distance and channel mean error; Rec.709 mean
luminance error. Proposed limits are 2/255 for each metric, pending owner review.
Shared shaders are unchanged. Fine noise can pass structural metrics while strict
pixels differ; global Scene Cut differences still fail. Three tests verify this
distinction, histogram units and rejection of dimension changes.

[Every noise spec and metric pass/fail](../evidence/p2/STRUCTURAL-REPORT.md),
[full channel CSV](../evidence/p2/noise-spec-metrics.csv),
[worst 20 structural heatmaps](../evidence/p2/noise-worst-20.html).
Current failing output is `evidence/p2/rgb565-owner-policy-check.txt`. The prior policy run is retained in `owner-policy-check-output.txt`. The earlier
strict failure output remains attached. `strictGoldenCheck` preserves the original
pixel gate; `goldenCheck` applies the owner class policy and still exits nonzero.

RGB565 packing still uses Skia's Bitmap.copy equivalent. Expansion now rounds
normalized 5/6-bit channels, matching all supplied mobile PNG channel levels
and the historical Skia/skcms PNG path. The old bit-repetition expansion fails
the new 65,536-colour format test. All actual frames were regenerated on hardware;
no reference PNG, shared shader, threshold or backend was changed.

The numeric probe reproduces NVIDIA/WARP disagreement at a constant 0.5
readback and exact pixel-grid boundaries. These remain strict failures. Raw
cell maps and heatmaps are in `evidence/p2/diagnostics/numeric`; evidence and
causal limits are explained in `P2-PARITY-BUGS.md`. The proposed owner probe
now includes matching raw RGBA8 diagnostics and remains unverified on Android.

The follow-up storage probe rules out GL_DITHER and CPU readback as repairs for
the half-value case. A second GPU shader observes NVIDIA's stored value below
0.5 and WARP's above 0.5; byte uploads remain intact. The explicit conversion
experiment makes Diamond Burst exact at its three failing times, but RGBA32F
causes seven other strict regressions. RGBA16F gives eleven new strict passes
and zero pass-to-fail regressions, while changing intermediate precision. Both
are retained as diagnostics; production stays RGBA8. Full constants, all 1688
metrics for each supported format, and selected tile/diff PNGs are under
`evidence/p2/diagnostics/framebuffer`. See `framebuffer-probe-output.txt` and
`numeric-storage-probe-output.txt`. The proposed phone probe now records
medium/low-float precision, dither state and the same endpoint observations.

`numeric-follow-up-reference-checks.txt` is the new full failing output: four
mobile inputs still pass, and goldenCheck still reports 46 deterministic plus
84 noise failures. P3 remains unstarted following the owner's phase clarification.
The follow-up runtime check passes; all 24 render tests reran, and the aggregate
test results remain 234 passed with no failures/errors/skips. All 422 shaders
compile and 1688 production frames regenerate with identical RGBA hashes to
the previous run. `numeric-follow-up-runtime-checks.txt` and
`production-frame-hash-check.txt` retain that validation. No diagnostic output
was substituted for a production frame.

## 3. Screenshots

`evidence/p2/effect-lab.png` is an actual Compose-window capture showing the
worst Scene Cut mismatch and its failing per-channel metrics after the readback fix; `rgb565-lab-and-math-checks.txt` confirms the actual-window capture and debug guard. `rgb565-release-check.txt` confirms the release guard.
`evidence/p2/effect-lab-and-mobile.html` presents that capture next to the
owner's `11-effects-page.png`. The Lab is a diagnostic screen; the mobile
editor effects page is P4 work and has no UI parity claim. The 21 supplied
screens remain layout references for later phases. No gallery thumbnail from
`06-media-picker.png` is published. Export settings will use mobile
`VideoExportScreens.kt`; `15-export-signin-gate.png` is the signed-out gate.

## 4. PARITY.md

`PARITY.md` now records P1 real-input acceptance, P2 runtime evidence and the
failing pixel gate. All catalog groups retain partial visual status until
their required frames pass. Unit-test success does not mark editing/UI or
playback/export parity complete.

## 5. Owner inputs and remaining work

The original goldens, projects and screenshots are present. Device-library
JSONs under `projects/device-library` were not supplied and are not included
in the four passing input checks. OAuth client ID and verified shared Plus
backend remain P7 inputs, as agreed.

The exact phone GL_RENDERER/GL_VERSION (owner pending) and a small mobile math/neutral-tile probe would help
resolve the remaining precision/sampling bugs. A proposed owner-run source
and its limitations are in `tools/mobile/P2ParityProbe.kt` and its README.
It has not been compiled for Android or copied/run on the phone. Existing
goldens remain authoritative. No approval is assumed for phone actions.

## 6. Read-only confirmation

The mobile handoff status is exactly `?? output/`, matching the original
approved baseline. Mobile HEAD is the owner's reference-export commit
`2a417fd29ef43a8792eb4d58cbe4d102327b11c9`. At P2 start, the owner's exporter
was untracked; the owner committed it while generating these inputs.
No Windows-agent write, mobile build or mobile Git mutation occurred.
See `evidence/p2/mobile-status.txt` and the source provenance audit.

All 1715 reference-input hashes remain unchanged, with zero additions or
missing files: `evidence/p2/reference-read-only-check.txt`. Every generated
frame, imported test package, log, report and diagnostic stays in the Windows repo.
