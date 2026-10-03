# Current-baseline framebuffer and rounding investigation

All experiments use the current sample matrix/filter preparation, unchanged
shared GLSL/defaults/quad, NVIDIA D3D11, and the original strict and provisional
noise limits. Four supported routes cover 6752 effect frames and 3088 noise
structural comparisons. Every route pins the production frame/metric/policy
hashes. The older pre-matrix experiment remains under ../framebuffer.

| Route | Strict pass / fail | Deterministic failures | Noise failures | New strict passes / regressions | New noise passes / regressions |
|---|---:|---:|---:|---:|---:|
| Production RGBA8 | 1473 / 215 | 18 | 84 | baseline | baseline |
| float32-sized | 1474 / 214 | 18 | 82 | 5 / 4 | 3 / 1 |
| float16 | 1478 / 210 | 14 | 81 | 5 / 0 | 3 / 0 |
| float32-round-even | 1477 / 211 | 15 | 82 | 5 / 1 | 3 / 1 |
| float32-round-even-staged | 1476 / 212 | 16 | 82 | 5 / 2 | 3 / 1 |

The explicit half-up route fixes Diamond at three times and Dance Flash at
0.3 s, but regresses Color Pixel twice, Mirror Beat and Blink. Raw float32
captures show many changed channels around scaled byte-half boundaries;
Diamond's flat-field changes all occur at stored 0.5. Float16 has no strict
or noise pass-to-fail regressions, but changes output precision and remains
an unadopted control with phone medium/low precision unknown.

Single-pass ties-to-even initially fails 126 of 511 synthetic controls.
All 256 normalized-byte round trips pass; failures are in the 255 boundary
inputs. The CPU reference rounds the float32 scaling result before selecting
the even integer; the single shader does not enforce that intermediate
storage boundary. Optimizer/evaluation fusion is a lead, not a proven driver
conformance failure. The full failed task output is retained.

The repaired staged control stores the scaled value in a separate RGBA32F
attachment before rounding. All 256 byte round trips and 255 boundary tests
pass. It still introduces strict and noise regressions, so it is not a
production repair. No epsilon or threshold change is used. Raw comparisons
are against desktop default storage, not invented phone raw values.

Current production remains RGBA8 with 18 deterministic and 84 provisional
noise failures. None of these routes is adopted. Real phone raw/neutral and
precision probes remain required to distinguish source/input and conversion
differences. Shared shaders and authoritative frames are unchanged.

The [float-buffer extension](https://registry.khronos.org/OpenGL/extensions/EXT/EXT_color_buffer_float.txt)
and [half-float extension](https://registry.khronos.org/OpenGL/extensions/EXT/EXT_color_buffer_half_float.txt)
describe these attachment/readback capabilities; the probe checks allocation
and completeness instead of assuming support. Unsized RGBA/FLOAT remains
unsupported in this context. Staging enforces an experimental float32 scale
boundary; it does not establish the phone's evaluation or rounding rule.

[6752 per-time RGBA MAE/p99 rows](channel-metrics.csv),
[1688 per-spec route rows](per-spec-metrics.csv),
[3088 structural channel rows](noise-metrics.csv),
[raw float/storage signatures](raw-storage-signatures.csv),
[all changed cases](changed-cases.csv).

- float32-sized: [strict worst 20](float32-sized-worst20-strict.html), [structural worst 20](float32-sized-worst20-noise.html), [changed cases](float32-sized-changes.html).
- float16: [strict worst 20](float16-worst20-strict.html), [structural worst 20](float16-worst20-noise.html), [changed cases](float16-changes.html).
- float32-round-even: [strict worst 20](float32-round-even-worst20-strict.html), [structural worst 20](float32-round-even-worst20-noise.html), [changed cases](float32-round-even-changes.html).
- float32-round-even-staged: [strict worst 20](float32-round-even-staged-worst20-strict.html), [structural worst 20](float32-round-even-staged-worst20-noise.html), [changed cases](float32-round-even-staged-changes.html).

Full logs: [current baseline](../../framebuffer-current-structural-output.txt),
[single-pass failure](../../framebuffer-rounding-output.txt),
[staged repair](../../framebuffer-staged-rounding-output.txt).
