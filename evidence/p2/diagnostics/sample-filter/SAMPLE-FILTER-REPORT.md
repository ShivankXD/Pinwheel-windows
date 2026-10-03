# Sample crop filtering correction

`PreviewSamples` now uses Skia's fast source-rectangle constraint, matching
the Android Canvas bitmap path in [AOSP](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-14.0.0_r1/libs/hwui/SkiaCanvas.cpp).
The previous strict constraint excluded outside crop neighbours during
upscaling. Android 11 and 15 sources also use the fast constraint.

A synthetic two-colour boundary at scale 2 gives exact quarter-pixel weights.
The regression checks both horizontal edges, both flipped vertical edges and
unchanged interior colour. Source channels of 128 yield outside contribution
32 and inside contribution 96. The published implementation fails with
[0,0,128,255]; the repair passes all six colour assertions.

The initial small fixture used fractional weights whose ideal result differed
from Skia's raster output (125/130 versus 127/127). That unsuitable oracle was
replaced by aligned quarter-pixel centres. Its complete failed logs and XML
are retained as `sample-filter-initial-baseline` and `sample-filter-fractional-oracle`
under evidence/p2. The final baseline failure is in `sample-filter-before.txt`
and `.xml`; the passing full run is in `sample-filter-runtime-and-frames.txt`.
No golden threshold was altered.

All 237 unit tests pass. All 422 shaders compile and all 1688 frames render.
Every saved PNG's decoded RGBA hash matches the published 1b7c8a3 baseline;
all 1688 per-time/per-channel metric rows and acceptance counters are unchanged.
The current 21 samples all downscale or use identity sizing, so this edge
correction does not repair any of the seven remaining deterministic specs.

Current failures remain 18 deterministic and 84 provisional noise structural
frames. Shared GLSL, GPU storage and the backend are unchanged. P2 acceptance
remains open, with phone precision/raw probes pending.

[Verification data](sample-filter-comparison.json),
[all per-time MAE/p99 metrics](../../golden-metrics.csv),
[full failing reference output](../../sample-filter-reference-checks.txt).
