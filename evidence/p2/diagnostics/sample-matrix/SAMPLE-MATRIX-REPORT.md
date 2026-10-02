# Sample matrix correction

Baseline: `ddad406f2d57fe38c6f1605b252943737896478a`. All 1688 frames rerendered on the same
NVIDIA/ANGLE backend, with shared shaders, defaults, strict thresholds and
noise classification unchanged. 592 rendered RGBA frame hashes changed.
37 strict failures became passes; 0 strict passes became failures.
Noise structure has 0 new provisional passes and
0 pass-to-fail regressions.

| Gate | Before failures | After failures |
|---|---:|---:|
| Original strict audit, every spec | 252 | 215 |
| Deterministic strict MAE/p99 | 46 | 18 |
| Noise provisional structure | 84 | 84 |

The port now applies the crop-local float scale and translates using its mapped
bounds, before rounding the bitmap allocation. AOSP's
[Bitmap.createBitmap implementation](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-14.0.0_r1/graphics/java/android/graphics/Bitmap.java) uses that order.
The previous fixed destination rectangle lost these float bounds. Portrait and
most transition samples map to 384.0000305 by 480.0000305 before allocating
384 by 480 pixels. Sample geometry and full before/after channel metrics are in
[sample-matrix-comparison.json](sample-matrix-comparison.json).

This does not establish exact phone decode/raster bytes: the phone Android/Skia
version and matching neutral sample probes are still pending. Existing float
framebuffer experiments predate this correction and remain historical diagnostics.
Production stays RGBA8. P2 acceptance remains open.

| Spec | Time | Class | Strict change | Before max MAE8 | After max MAE8 |
|---|---:|---|---|---:|---:|
| fx-cc-slash-reveal | 0.9 | noise-driven | FAIL -> PASS | 0.365929 | 0.142318 |
| fx-cc-slash-reveal | 1.5 | noise-driven | FAIL -> PASS | 0.463802 | 0.099783 |
| fx-cc-dance-flash | 0.9 | deterministic | FAIL -> PASS | 0.591385 | 0.353277 |
| fx-cc-dance-flash | 1.5 | deterministic | FAIL -> PASS | 0.564887 | 0.330230 |
| fx-cc-dance-flash | 2.1 | deterministic | FAIL -> PASS | 0.571289 | 0.339605 |
| fx-cc-cut-twist | 0.9 | deterministic | FAIL -> PASS | 0.310829 | 0.119488 |
| fx-mirror-ball | 0.3 | noise-driven | FAIL -> PASS | 0.372157 | 0.321875 |
| fx-mirror-ball | 0.9 | noise-driven | FAIL -> PASS | 0.372786 | 0.321766 |
| fx-split-3 | 0.3 | deterministic | FAIL -> PASS | 0.440929 | 0.082205 |
| fx-split-3 | 0.9 | deterministic | FAIL -> PASS | 0.446615 | 0.090690 |
| fx-split-3 | 1.5 | deterministic | FAIL -> PASS | 0.471332 | 0.087522 |
| fx-split-3 | 2.1 | deterministic | FAIL -> PASS | 0.465951 | 0.096810 |
| fx-marker-lines | 0.9 | noise-driven | FAIL -> PASS | 0.344792 | 0.212283 |
| fx-marker-lines | 1.5 | noise-driven | FAIL -> PASS | 0.331055 | 0.211111 |
| fx-marker-lines | 2.1 | noise-driven | FAIL -> PASS | 0.333615 | 0.219965 |
| fx-datamosh | 0.9 | noise-driven | FAIL -> PASS | 0.355056 | 0.072721 |
| fx-datamosh | 2.1 | noise-driven | FAIL -> PASS | 0.327344 | 0.087261 |
| fx-chroma-wave | 0.3 | deterministic | FAIL -> PASS | 0.444878 | 0.155664 |
| fx-chroma-wave | 0.9 | deterministic | FAIL -> PASS | 0.443837 | 0.148220 |
| fx-chroma-wave | 1.5 | deterministic | FAIL -> PASS | 0.463108 | 0.158746 |
| fx-chroma-wave | 2.1 | deterministic | FAIL -> PASS | 0.451649 | 0.161046 |
| fx-tr-cutout-scan | 2.1 | deterministic | FAIL -> PASS | 0.446636 | 0.102626 |
| fx-tr-panel-clasp | 0.3 | deterministic | FAIL -> PASS | 0.445052 | 0.099154 |
| fx-tr-black-fade | 0.3 | deterministic | FAIL -> PASS | 0.323893 | 0.130208 |
| fx-tr-black-fade | 2.1 | deterministic | FAIL -> PASS | 0.536176 | 0.199674 |
| fx-tr-white-flash | 0.3 | deterministic | FAIL -> PASS | 0.267144 | 0.162218 |
| fx-tr-blink | 0.3 | deterministic | FAIL -> PASS | 0.448937 | 0.102995 |
| fx-tr-whip-left | 0.3 | deterministic | FAIL -> PASS | 0.304188 | 0.127474 |
| fx-tr-whip-left | 2.1 | deterministic | FAIL -> PASS | 0.415755 | 0.150629 |
| fx-tr-whip-down | 0.3 | deterministic | FAIL -> PASS | 0.366884 | 0.143989 |
| fx-tr-slide-left | 2.1 | deterministic | FAIL -> PASS | 0.291189 | 0.189453 |
| fx-tr-heart | 2.1 | deterministic | FAIL -> PASS | 0.426866 | 0.094705 |
| fx-tr-clock | 0.3 | deterministic | FAIL -> PASS | 0.443533 | 0.098980 |
| fx-tr-ripple | 1.5 | deterministic | FAIL -> PASS | 0.407704 | 0.161762 |
| fx-tr-ripple | 2.1 | deterministic | FAIL -> PASS | 0.487695 | 0.144683 |
| fx-tr-kaleido | 0.3 | deterministic | FAIL -> PASS | 0.445399 | 0.099501 |
| fx-tr-kaleido | 0.9 | deterministic | FAIL -> PASS | 0.401780 | 0.169379 |
