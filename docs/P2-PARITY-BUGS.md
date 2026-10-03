# P2 failing golden investigation

Status: **open**. After the sample matrix correction, the strict pixel audit has 215 failing frames from 68 specs. The preceding RGB565 baseline had 252 failures from 86 specs.
The owner then authorized structural review for noise-driven specs, preserving
strict MAE8 <= 2 and p99 <= 8 for deterministic specs. With provisional noise
metric limits of 2/255, 18 deterministic frames and 84 noise frames still fail.
Catalog GLSL, defaults, reference PNGs and the rendering backend are unchanged.

## Confirmed observations

All 422 catalog shaders compile on pinned ANGLE D3D11. All 1688 hardware
frames exist. 354 specs pass at every time. Source and shader-literal audits
pass. The preview recipe uses the same sample index, default params, SWAY,
history offsets and four timestamps; orientation has asymmetric-pixel tests.

Scene Cut at 0.9 s remains the largest MAE failure: 96.116276 on G, p99 157 after the sample fix (originally 96.39996 / 158).
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
the format expansion component is now proven below. Remaining sampling causes are **not yet proven**. The other 175 initially exceeded MAE. Current strict counts are 170 above MAE and 45 failing only p99, of which 19 have maximum p99 exactly 9. Hash/branch sensitivity
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

898 of 916 deterministic frames pass strict MAE/p99; 18 fail. No deterministic
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

## Repaired sample matrix preparation

`PreviewSamples` now follows Android's crop-local scale and mapped-bound
translation before rounding bitmap allocation. The previous destination
rectangle forced exactly 384 by 480; several mobile matrices map to
384.0000305 by 480.0000305. The [AOSP Bitmap.createBitmap source](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-14.0.0_r1/graphics/java/android/graphics/Bitmap.java)
establishes this order. The exact phone Android/Skia version remains unknown.

All 1688 production frames were rerendered with shared GLSL, defaults, NVIDIA
backend, RGB565 expansion and thresholds unchanged. 592 frame RGBA hashes
changed; 37 strict failures became passes and zero strict passes became failures.
Deterministic failures fell from 46 to 18, covering seven specs rather than 22.
Noise structural failures remain 84. Two regressions test mapped float bounds
and actual asymmetric crop/flip colours. All 236 unit tests pass.

The [matrix report](../evidence/p2/diagnostics/sample-matrix/SAMPLE-MATRIX-REPORT.md)
and full comparison JSON retain all 21 sample geometries and before/after
channel measurements against published baseline ddad406. Neutral SWAY tiles,
worst-20 galleries and actual-window Effect Lab capture were refreshed. Phone
neutral/raw probes are still needed to isolate remaining decode/interpolation
precision. The float framebuffer experiments below predate this source fix and
remain diagnostic; production stays RGBA8.

## Repaired crop filtering constraint

Android Canvas's [Skia bitmap path](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-14.0.0_r1/libs/hwui/SkiaCanvas.cpp)
uses the fast source-rectangle constraint. The desktop had used the strict
constraint, excluding neighbouring source pixels at upscaled crop edges.
`PreviewSamples` now retains those taps. An exact scale-2 fixture checks both
horizontal and flipped vertical edges: outside/inside channels 32/96 instead
of the former 0/128. The negative baseline fails; all 237 repaired tests pass.

The current 21 sample scales are at most 1, so all 1688 frame hashes and
per-time channel metric rows remain identical to published baseline 1b7c8a3.
This correction does not resolve any of the seven remaining strict specs.
The [filter report](../evidence/p2/diagnostics/sample-filter/SAMPLE-FILTER-REPORT.md)
includes the full failed baseline and explains the unsuitable initial
fractional-colour oracle that was replaced with aligned quarter-pixel centres.
No golden threshold changed. Full current golden failures are retained in
`evidence/p2/sample-filter-reference-checks.txt`.

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

### Dithering and storage isolation follow-up

Both Windows contexts start with GL_DITHER enabled. Toggling it leaves all four
constant results unchanged: NVIDIA gives 64/127/166/235; WARP gives
64/128/166/235 for 0.25/0.5/0.65/0.92. The diagnostic restores the original flag
before running Diamond and grid probes. Disabling dithering is not a repair.

A second shader samples the stored texture and emits only 0 or 1 to classify
each texel as below, above or exactly 0.5. Endpoint readback avoids another
half-value tie. Every one of the 46,080 texels gives the following result:

| Origin | NVIDIA stored value | WARP stored value |
|---|---|---|
| Upload byte 127 | below 0.5; readback 127 | below 0.5; readback 127 |
| Upload byte 128 | above 0.5; readback 128 | above 0.5; readback 128 |
| Clear to 0.5 | below 0.5; readback 127 | above 0.5; readback 128 |
| Draw uniform 0.5 | below 0.5; readback 127 | above 0.5; readback 128 |

The discrepancy therefore exists in GPU texture storage before CPU readback.
It cannot be repaired by changing row handling or the normalized RGB565
expansion. The constant and endpoint shaders isolate this observation from
catalog math and photo preparation. Full results are in `numeric-probe.json`
and `numeric-storage-probe-output.txt`.

### Alternative 8-bit formats, no repair

The next control keeps eight bits per channel and compares unsized RGBA,
sized RGBA8, unsized BGRA and sized BGRA8 on NVIDIA and WARP. All allocations
are complete, every channel reports eight bits and a distinct-channel upload
checks all texels. Uniform half-value storage remains below 0.5 on NVIDIA
and above 0.5 on WARP in every format, observed by endpoint shaders before
readback. All 224 selected renders and every strict MAE/p99 metric match the
default within their backend. These format choices do not repair the seven
remaining specs on either measured implementation. No format was adopted;
all production PNGs and metrics remain unchanged.

[Full 8-bit control report](../evidence/p2/diagnostics/storage8/STORAGE8-REPORT.md),
[all selected per-time metrics](../evidence/p2/diagnostics/storage8/storage8-metrics.csv)
and `evidence/p2/storage8-probe-output.txt` retain the successful controls.
This selected diagnostic does not establish the phone's storage precision.

### Explicit conversion experiment, not adopted

`p2FramebufferProbe` renders the unchanged effect into a floating-point final
attachment, then uses a separate GPU pass for `floor(clamp(x,0,1)*255+0.5)/255`
before the existing RGB565 packing/expansion. Sample decoding, all RGBA8 SWAY
inputs, shader source, uniforms, geometry and comparisons remain the same.
This task writes only diagnostics, never `evidence/p2/frames` or references.

| Final attachment | Strict passes / failures | New passes / regressions | Deterministic failures |
|---|---:|---:|---:|
| Existing production RGBA8 | 1436 / 252 | baseline | 46 |
| Sized RGBA32F with explicit conversion | 1438 / 250 | 9 / 7 | 47 |
| RGBA16F with explicit conversion | 1447 / 241 | 11 / 0 | 39 |

Both floating routes reproduce Diamond Burst exactly at 0.3, 0.9 and 2.1 s,
with every RGBA MAE and p99 equal to zero. This confirms that final attachment
conversion can explain its field mismatch without editing the effect shader.
RGBA32F introduces six deterministic regressions: Background Fit at 0.9 s,
Color Pixel at 0.3/0.9 s, Mirror Beat at 0.9/2.1 s and Blink at 0.9 s; Shake
transition at 1.5 s is the seventh strict regression, in the noise class.

RGBA16F has no strict pass-to-fail regressions in this set, but it rounds
intermediate shader outputs: for example, 0.65 becomes 0.64990234. Its better
golden count is not sufficient evidence to adopt a precision change. The
[GLSL ES 1.00 specification, section 7.2](https://registry.khronos.org/OpenGL/specs/es/2.0/GLSL_ES_Specification_1.00.pdf)
declares gl_FragColor as mediump; that does not prove this phone stores an
IEEE binary16 output or that explicit nearest conversion matches all of its
fixed-function behaviour. Phone medium/low-float precision and raw numeric
outputs are still needed. Neither experiment changes production storage.

The unsized RGBA/FLOAT attachment is incomplete in this ANGLE context. Sized
RGBA32F succeeds, as does RGBA/HALF_FLOAT_OES. The advertised context reports
OpenGL ES 3.0 even though EGL creation requested client version 2. The
[float-buffer extension](https://registry.khronos.org/OpenGL/extensions/EXT/EXT_color_buffer_float.txt)
describes the sized formats; the [half-float extension](https://registry.khronos.org/OpenGL/extensions/EXT/EXT_color_buffer_half_float.txt)
defines floating-point readback and half-float attachments. Completeness is
checked for each allocation instead of assuming support from a texture flag.

All 1688 comparisons, individual regressions, constants and selected tile/diff
PNGs are retained in `evidence/p2/diagnostics/framebuffer`. That experiment used the pre-matrix baseline of 46 deterministic and 84 noise
failures. Current acceptance reports 18 deterministic and 84 noise failures,
with the original limits.

Raw cell maps, raw Diamond colours, per-pixel boundary comparisons and heatmaps
are in `evidence/p2/diagnostics/numeric`. The proposed owner-run probe now exports
matching raw RGBA8 constant and cell-map PNGs before RGB565 conversion. It remains
uncompiled and unexecuted on Android, stored only under `tools/mobile` here.
The seven failing deterministic specs and every time/channel measurement remain
in the current CSV reports. No deterministic failure has been reclassified
as noise to remove it from the strict gate.

## Current-baseline conversion and staged rounding follow-up

The float experiments were rerun after the sample matrix/filter repairs,
preserving the older pre-matrix folder. All current source, metric, frame and
noise-policy hashes are pinned. Four supported routes cover 6752 strict frame
comparisons and 3088 noise structural comparisons, with identical owner limits.

| Route | Strict pass / fail | Deterministic fail | Noise fail | Strict regressions | Noise regressions |
|---|---:|---:|---:|---:|---:|
| Production RGBA8 | 1473 / 215 | 18 | 84 | baseline | baseline |
| Float32, half-up conversion | 1474 / 214 | 18 | 82 | 4 | 1 |
| Float16, half-up conversion | 1478 / 210 | 14 | 81 | 0 | 0 |
| Float32, single-pass even ties | 1477 / 211 | 15 | 82 | 1 | 1 |
| Float32, staged even ties | 1476 / 212 | 16 | 82 | 2 | 1 |

The single-pass ties-to-even shader failed 126/511 synthetic controls despite
passing all 256 normalized-byte round trips. It did not enforce the CPU
reference's float32 scaling boundary. Storing the scaled values in a separate
RGBA32F attachment resolves that diagnostic bug: all 256 byte and 255 boundary
tests pass. Optimizer/evaluation fusion remains a lead, not a proven driver
conformance failure. The original failed output is preserved.

The staged route still regresses Color Pixel at 0.3 s, Mirror Beat at 0.9 s
and the noise Tri Split comparison at 0.9 s. The latter's blue blurred MAE is
above 2/255; its threshold is unchanged. Float16 has zero strict/structural
pass-to-fail regressions but changes precision, with the phone's medium/low
behaviour still unknown. None of these routes is adopted. No per-effect
conversion choice, shader edit, epsilon or threshold relaxation is introduced.

Raw captures distinguish exact stored 0.5 fields from other scaled byte-half
boundaries and retain desktop default and converted RGBA8 PNGs. These are
desktop observations, not reconstructed phone raw values. The complete
[current conversion report](../evidence/p2/diagnostics/framebuffer-review/FRAMEBUFFER-REVIEW.md)
contains all metrics, changed cases and eight paired worst-20 heatmap galleries.
Full single-pass failure output is `evidence/p2/framebuffer-rounding-output.txt`;
the repaired staged run is `evidence/p2/framebuffer-staged-rounding-output.txt`.

## Deterministic backend and prepared-input controls

The new `p2DeterministicProbe` and `p2SamplingProbe` isolate all seven remaining
strict specs. All 28 NVIDIA output hashes match production. All 56 stored-input
read/upload/replay controls are byte-exact. The original strict limits and all
production frames remain unchanged.

Across the selected 28 cases, NVIDIA shader/NVIDIA input passes 10, WARP/WARP
passes 4, NVIDIA/WARP passes 0 and WARP/NVIDIA passes 14. The crossed route is
only a control. WARP repairs Diamond's three flat-field cases but regresses its
photo-bearing 1.5 s case; it cannot be substituted as the production backend.
Neon Edges worst MAE is 1.692166 on WARP/WARP and 0.431250 on WARP/NVIDIA;
Dance Flash is 1.483225 versus 0.354991. This supports a major SWAY-input
contribution to those WARP errors. Neon still fails; Dance passes its four
crossed control times. CMYK and Carousel remain failing. Pixel Creation/Mosaic
large grid errors shrink under WARP shader execution, while strict p99 still fails.

Both contexts report 23-bit high/medium/low fragment floats. All seven translated
fragment HLSL files are byte-identical. Vertex translations differ only in
uFlipY allocation to c1 versus c0, with the same operations. Translations are
read-only evidence and never replace shared GLSL. Exact phone precision and
neutral/raw inputs remain pending; backend metadata alone cannot establish a
repair for the phone.

CMYK retains a mobile portability quirk: `smoothstep(0.05, 0.0, ...)` uses
reversed bounds. The [Khronos function reference](https://raw.githubusercontent.com/KhronosGroup/OpenGL-Refpages/main/es3.0/smoothstep.xml)
defines reversed-bound results as undefined. This is not yet a proven cause
of the phone mismatch. It stays in the strict deterministic class, unchanged.

[Full stage report](../evidence/p2/diagnostics/deterministic/DETERMINISTIC-REPORT.md),
[all 112 per-frame RGBA measurements](../evidence/p2/diagnostics/deterministic/stage-metrics.csv)
and [seven mobile/desktop/control comparisons with heatmaps](../evidence/p2/diagnostics/deterministic/remaining-seven.html)
retain every time and channel. Raw signatures distinguish one-cell quantization
errors from larger sampling/grid errors; a raw value 127 alone is not proof
that a shader produced exact 0.5. New diagnostics do not constitute a parity fix.

## Current deterministic failures

Every listed spec keeps strict per-channel MAE8 <= 2 and p99 <= 8. Maxima
are taken independently over RGBA and all four times. The [full channel CSV](../evidence/p2/deterministic-failures.csv) retains each channel.

| Spec | Failing times (s) | Max MAE8 | Max p99 |
|---|---|---:|---:|
| fx-ov-tr-diamond | 0.3,0.9,2.1 | 8.467969 | 9 |
| fx-cmyk-print | 0.3,0.9,1.5,2.1 | 2.112782 | 49 |
| fx-mosaic-pulse | 2.1 | 1.696224 | 49 |
| fx-pb-pixel-creation | 1.5 | 1.547808 | 49 |
| fx-carousel | 0.3,0.9,1.5,2.1 | 0.961523 | 12 |
| fx-ne-edges | 0.3,0.9,1.5,2.1 | 0.431163 | 9 |
| fx-cc-dance-flash | 0.3 | 0.353277 | 9 |
