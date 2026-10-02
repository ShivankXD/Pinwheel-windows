# P2 failing golden investigation

Status: **open**. After the RGB565 expansion fix, the strict pixel audit has 252 failing frames from 86 specs, down from 277 frames and 101 specs.
The owner then authorized structural review for noise-driven specs, preserving
strict MAE8 <= 2 and p99 <= 8 for deterministic specs. With provisional noise
metric limits of 2/255, 46 deterministic frames and 84 noise frames still fail.
Catalog GLSL, defaults, reference PNGs and the rendering backend are unchanged.

## Confirmed observations

All 422 catalog shaders compile on pinned ANGLE D3D11. All 1688 hardware
frames exist. 336 specs pass at every time. Source and shader-literal audits
pass. The preview recipe uses the same sample index, default params, SWAY,
history offsets and four timestamps; orientation has asymmetric-pixel tests.

Scene Cut at 0.9 s remains the largest MAE failure: 96.123589 on G, p99 157 after the readback fix (originally 96.39996 / 158).
Its time-dependent branch uses `fract(sin(n * 12.9898) * 43758.5453)`.
The exact same reduced GLSL, inputs and 1x1 RGBA8 target produce:

| Time (s) | NVIDIA D3D11 hash x255 | Microsoft WARP D3D11 hash x255 |
|---:|---:|---:|
| 0.3 | 251 | 18 |
| 0.9 | 150 | 170 |
| 1.5 | 168 | 160 |
| 2.1 | 199 | 139 |

This confirms backend-dependent evaluation of that hash. In a full Scene Cut
render, WARP substantially reduces the first two time comparisons but they
still fail p99; 1.5 and 2.1 pass. Film Grain still fails all four times on
WARP. These diagnostics are in `evidence/p2/diagnostics/math-probe.json` and
the separate hardware/warp frame folders. WARP is not a parity fix and does
not replace hardware actuals. The reduced probe's compiler context differs
from the full catalog shader; its quantized values are not a complete causal
proof for every full-shader branch or every failing spec.

In the initial run, 102 failures passed MAE but failed p99. Of these, 76 have maximum p99 exactly 9.
Quantization or sample/raster interpolation differences are plausible causes;
the format expansion component is now proven below. Remaining sampling causes are **not yet proven**. The other 175 initially exceeded MAE. Current strict counts are 170 above MAE and 82 failing only p99, of which 56 have maximum p99 exactly 9. Hash/branch sensitivity
is a concrete lead for major time/noise mismatches, not a blanket explanation
for all failures.

## Next diagnostic and repair constraints

`evidence/p2/diagnostics/neutral` contains 84 hardware tiles, covering all
21 samples at four times through a neutral `return src(uv)` effect and the
same SWAY preparation. Matching phone copies would isolate sample decoding,
crop/resample, GL sampling and RGB565 readback from effect math.

`tools/mobile/P2ParityProbe.kt` proposes those neutral outputs plus phone GL
metadata and the reduced hash. It exists only in the Windows repo and is
unverified on Android. The owner must choose any phone-side execution; no
mobile files were changed and no app was installed or instrumented here.

A repair must preserve the source-backed effect behaviour, make failing
provided comparisons pass and be rechecked on the complete 1688-frame set.
The only comparison change is the owner-authorized Gaussian/histogram/mean
review for source-classified noise specs. No shader approximation, reference-derived
output, missing-case suppression or backend substitution has been introduced. P2 remains below its acceptance gate and P3 has not begun.

## Owner noise amendment results

The final class list has 193 noise-driven and 229 deterministic specs. Removing
unused shared noise locals prevents four deterministic outputs from being wrongly
flagged. Classification is independent of golden success. Every source hash,
active branch and helper call path is available for review.

688 of 772 noise frames pass the proposed structural metrics; 84 fail. Film Grain
and Grain overlay pass all four times with blur/histogram/mean errors below 1/255.
Scene Cut still fails every time, with a histogram/mean mismatch around 96/255 at
0.9 s. Its whole-image branch mismatch is not treated as acceptable noise.

870 of 916 deterministic frames pass strict MAE/p99; 46 fail. No deterministic
case uses blur or a relaxed p99. See STRUCTURAL-REPORT.md and per-spec metrics for
all classes. Proposed structural limits remain subject to owner review.

## Repaired RGB565 expansion

Desktop Skia's low-precision readPixels expansion repeats the 5/6-bit channel
bits. The owner PNGs use rounded normalized channels. For example, a packed
5-bit value 3 expands to 24 by bit repetition and to 25 by normalized rounding;
green value 11 expands to 44 versus 45. All 1688 supplied PNGs use exactly the
32 red/blue and 64 green normalized levels, with alpha 255. The input analysis
is in `evidence/p2/diagnostics/rgb565-input-analysis.json`.

The historical [Skia PNG encoder](https://skia.googlesource.com/skia/+/5637cd56be3214886e5a16767a6e480a6e36c946/src/images/SkImageEncoderFns.h)
passes RGB565 through skcms. Its [normalized channel load and 8-bit store](https://skia.googlesource.com/skcms/+/refs/heads/main/src/Transform_inl.h)
explain the observed levels. The phone's exact Skia version remains unknown;
this is a source-backed format match, not a claim about that version. Current
desktop Skia PNG encoding also takes a bit-repetition path and cannot serve
as the mobile format oracle. An attempted native float/PNG oracle was likewise
unsuitable, so the regression uses independently fixed normalized channel ramps.

The repair preserves Skia packing, then expands the packed channels with nearest
normalized rounding and opaque alpha. A format regression exercises all 65,536
packed colours. The old code fails at blue value 3; the repaired code passes.
No golden PNG is used to generate production pixels. All 1688 actual frames
were regenerated by the unchanged hardware shader pipeline. Strict passes rise
from 1411 to 1436; deterministic failures fall from 69 to 46; noise structural
failures fall from 86 to 84. Baseline and current counters are retained as
`diagnostics/rgb565-before.json` and `rgb565-after.json`.

## Remaining raw readback and grid differences

`:pinwheel-render:p2NumericProbe` runs the same shaders and uploaded values on
NVIDIA hardware and WARP. It does not replace golden actuals or change acceptance.

| Input | NVIDIA RGBA8 | WARP RGBA8 |
|---|---:|---:|
| Constant 0.25 | 64 | 64 |
| Constant 0.5 | 127 | 128 |
| Constant 0.65 | 166 | 166 |
| Constant 0.92 | 235 | 235 |

The full unchanged Diamond Burst shader, using a constant input texture,
produces its large pink field as [235,64,127] on NVIDIA and [235,64,128] on WARP.
At 0.3 s this field has the same 43,112 pixels. The mobile PNG field is
[239,65,132]; NVIDIA's normalized RGB565 result is [239,65,123]. This ties the
remaining 9-level blue error to raw RGBA8 readback, independent of input decoding,
SWAY or effect geometry. It still fails strict MAE/p99 at three times.

These are measured portability differences, not proof of a driver conformance
violation. [OpenGL ES 2.0 sections 2.1.2 and 4.1.7](https://registry.khronos.org/OpenGL/specs/es/2.0/es_full_spec_2.0.pdf)
describe fixed-point conversion and implementation-dependent dithering. The
Windows/mobile compatibility bug remains open under the owner's strict gate.

The unchanged mobile vertex shader and quad also show pixel-cell floor
sensitivity. For grid [51.2f,64f], NVIDIA disagrees with an exact rational
pixel-center calculation at 3,120 pixels in columns 7,22,37,...,187. WARP
matches the x calculation but differs on y at 1,344 pixels. The mobile Pixel
Creation and Mosaic Pulse large errors concentrate at those same x columns.
This supports the interpolation/floor lead; a matching phone cell map is still
needed to confirm the phone's calculation. CMYK Print also remains unresolved;
its rotated fine dots do not reduce to that simple grid probe.

An oversized full-screen triangle was tested with the unchanged vertex shader.
It worsens NVIDIA's grid disagreement to 5,984 pixels and has not been adopted.
Production keeps the mobile quad, backend, shaders and default values.

Raw cell maps, raw Diamond colours, per-pixel boundary comparisons and heatmaps
are in `evidence/p2/diagnostics/numeric`. The proposed owner-run probe now exports
matching raw RGBA8 constant and cell-map PNGs before RGB565 conversion. It remains
uncompiled and unexecuted on Android, stored only under `tools/mobile` here.
The 22 failing deterministic specs and every time/channel measurement remain
in the current CSV reports. No deterministic failure has been reclassified
as noise to remove it from the strict gate.

## Current deterministic failures

Every listed spec keeps strict per-channel MAE8 <= 2 and p99 <= 8. Maxima
are taken independently over RGBA and all four times. The [full channel CSV](../evidence/p2/deterministic-failures.csv) retains each channel.

| Spec | Failing times (s) | Max MAE8 | Max p99 |
|---|---|---:|---:|
| fx-ov-tr-diamond | 0.3,0.9,2.1 | 8.467969 | 9 |
| fx-cmyk-print | 0.3,0.9,1.5,2.1 | 2.112782 | 49 |
| fx-mosaic-pulse | 2.1 | 1.750543 | 49 |
| fx-pb-pixel-creation | 1.5 | 1.547808 | 49 |
| fx-carousel | 0.3,0.9,1.5,2.1 | 0.961523 | 12 |
| fx-cc-dance-flash | 0.3,0.9,1.5,2.1 | 0.591385 | 9 |
| fx-tr-black-fade | 0.3,2.1 | 0.536176 | 9 |
| fx-tr-ripple | 1.5,2.1 | 0.487695 | 9 |
| fx-split-3 | 0.3,0.9,1.5,2.1 | 0.471332 | 9 |
| fx-chroma-wave | 0.3,0.9,1.5,2.1 | 0.463108 | 9 |
| fx-tr-blink | 0.3 | 0.448937 | 9 |
| fx-tr-cutout-scan | 2.1 | 0.446636 | 9 |
| fx-tr-kaleido | 0.3,0.9 | 0.445399 | 9 |
| fx-tr-panel-clasp | 0.3 | 0.445052 | 9 |
| fx-tr-clock | 0.3 | 0.443533 | 9 |
| fx-ne-edges | 0.3,0.9,1.5,2.1 | 0.431163 | 9 |
| fx-tr-heart | 2.1 | 0.426866 | 9 |
| fx-tr-whip-left | 0.3,2.1 | 0.415755 | 9 |
| fx-tr-whip-down | 0.3 | 0.366884 | 9 |
| fx-cc-cut-twist | 0.9 | 0.314366 | 9 |
| fx-tr-slide-left | 2.1 | 0.291189 | 9 |
| fx-tr-white-flash | 0.3 | 0.267144 | 9 |
