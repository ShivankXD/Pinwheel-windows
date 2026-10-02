# P2 failing golden investigation

Status: **open**. The strict pixel audit has 277 failing frames from 101 specs.
The owner then authorized structural review for noise-driven specs, preserving
strict MAE8 <= 2 and p99 <= 8 for deterministic specs. With provisional noise
metric limits of 2/255, 69 deterministic frames and 86 noise frames still fail.
Catalog GLSL, defaults, reference PNGs and the rendering backend are unchanged.

## Confirmed observations

All 422 catalog shaders compile on pinned ANGLE D3D11. All 1688 hardware
frames exist. 321 specs pass at every time. Source and shader-literal audits
pass. The preview recipe uses the same sample index, default params, SWAY,
history offsets and four timestamps; orientation has asymmetric-pixel tests.

Scene Cut at 0.9 s is the largest MAE failure: 96.39996 on G, p99 158.
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

102 failures pass MAE but fail p99. Of these, 76 have maximum p99 exactly 9.
Quantization or sample/raster interpolation differences are plausible causes;
they are **not yet proven**. The other 175 exceed MAE. Hash/branch sensitivity
is a concrete lead for major time/noise mismatches, not a blanket explanation
for all 277 failures.

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

686 of 772 noise frames pass the proposed structural metrics; 86 fail. Film Grain
and Grain overlay pass all four times with blur/histogram/mean errors below 1/255.
Scene Cut still fails every time, with a histogram/mean mismatch around 96/255 at
0.9 s. Its whole-image branch mismatch is not treated as acceptable noise.

847 of 916 deterministic frames pass strict MAE/p99; 69 fail. No deterministic
case uses blur or a relaxed p99. See STRUCTURAL-REPORT.md and per-spec metrics for
all classes. Proposed structural limits remain subject to owner review.
