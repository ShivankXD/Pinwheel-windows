# P2 failing golden investigation

Status: **open**. 277 frames from 101 specs fail. Required limits stay at
per-channel MAE8 <= 2 and p99 <= 8. Catalog GLSL, defaults, reference PNGs and
comparison policy are unchanged. No new backend or tolerance is accepted here.

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
No shader approximation, reference-derived output, post-comparison filtering,
missing-case suppression, backend substitution or threshold relaxation has
been introduced. P2 remains below its acceptance gate and P3 has not begun.
