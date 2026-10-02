# P2 owner structural review

Policy: **PROVISIONAL_OWNER_REVIEW**. Shared GLSL and deterministic strict limits
are unchanged. Classification comes from active shader branches and output helper
calls, independently of pass/fail images. Unused pure noise locals are excluded.
See [source classification](D:/Pinwheel-Windows/docs/P2-NOISE-CLASS.md).

| Class | Specs | Frames passing | Frames failing | Specs passing all times |
|---|---:|---:|---:|---:|
| Deterministic, strict MAE/p99 | 229 | 870 | 46 | 207 |
| Noise-driven, provisional structure | 193 | 688 | 84 | 160 |

Noise review uses separable Gaussian **sigma 8 px**, radius 24 (3 sigma), edge
clamping, float sRGB8 without rounding or rescaling. Provisional limits are
channel blurred MAE <= 2, histogram
Wasserstein-1 distance <= 2 and channel
mean error <= 2; Rec.709 mean luminance error
<= 2. These are proposed review limits, not owner
sign-off. Every channel and all four timestamps must pass. Histogram distance
is the sum of absolute cumulative histogram differences divided by pixel count,
in 8-bit channel units. Values divided by 255 are normalized errors.

Strict per-pixel diagnostics remain for all specs in [GOLDEN-REPORT.md](GOLDEN-REPORT.md).
Noise p99 failures do not fail the structural gate; global branch/colour errors
still do. Film Grain passes the provisional metrics at all four times. Scene Cut
still fails all four times, including mean/histogram errors around 96/255 at 0.9 s.

[Per-channel noise metrics for every spec](noise-spec-metrics.csv),
[all noise frames](noise-frame-metrics.csv), [worst 20 structural heatmaps](noise-worst-20.html).
The following lists every noise-driven spec and the metrics it passes. Each
metric shows its maximum over RGBA and all times; full channel values are in CSV.
Heatmaps use the selected worst structural time and visualize Gaussian difference x16.

| Spec | Review | Blurred MAE8 | Histogram W1 | Channel mean error | Mean luminance error | Difference |
|---|---|---:|---:|---:|---:|---|
| fx-shake | FAIL | 2.7221 FAIL | 0.2039 PASS | 0.1518 PASS | 0.1066 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-shake/2.1.png) |
| fx-cc-slash-reveal | PROVISIONAL_PASS | 1.6510 PASS | 0.1438 PASS | 0.0647 PASS | 0.0531 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-slash-reveal/2.1.png) |
| fx-cc-diamond-zoom | PROVISIONAL_PASS | 0.0599 PASS | 0.0635 PASS | 0.0477 PASS | 0.0446 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-diamond-zoom/0.3.png) |
| fx-ct-error-quake | FAIL | 6.5116 FAIL | 4.6205 FAIL | 4.5701 FAIL | 3.5974 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-error-quake/0.3.png) |
| fx-ct-explosion | PROVISIONAL_PASS | 0.0474 PASS | 0.0586 PASS | 0.0280 PASS | 0.0250 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-explosion/1.5.png) |
| fx-ct-scene-cut | FAIL | 95.2579 FAIL | 95.5868 FAIL | 95.5868 FAIL | 95.2500 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-scene-cut/0.9.png) |
| fx-cc-shiny-stack | FAIL | 11.6347 FAIL | 2.3510 FAIL | 0.7852 PASS | 0.7750 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-shiny-stack/0.3.png) |
| fx-ct-glass-breaking | FAIL | 4.3900 FAIL | 1.3628 PASS | 0.9864 PASS | 0.8759 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-glass-breaking/1.5.png) |
| fx-ct-negative-panels | FAIL | 58.9031 FAIL | 49.2483 FAIL | 49.2483 FAIL | 31.6097 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-negative-panels/2.1.png) |
| fx-ct-fireplace | PROVISIONAL_PASS | 0.5088 PASS | 0.2276 PASS | 0.1844 PASS | 0.1318 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-fireplace/0.9.png) |
| fx-ct-vignette-noir | PROVISIONAL_PASS | 0.2005 PASS | 0.1200 PASS | 0.0406 PASS | 0.0314 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-vignette-noir/0.3.png) |
| fx-ct-lightning-crack | FAIL | 13.0580 FAIL | 4.6415 FAIL | 4.0875 FAIL | 3.8704 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-lightning-crack/0.3.png) |
| fx-vignette | PROVISIONAL_PASS | 0.4328 PASS | 0.2351 PASS | 0.1607 PASS | 0.1511 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-vignette/0.9.png) |
| fx-ct-offset-slice | FAIL | 2.5499 FAIL | 1.0036 PASS | 0.9484 PASS | 0.6641 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-offset-slice/0.9.png) |
| fx-cc-feverish | FAIL | 18.3290 FAIL | 18.3447 FAIL | 18.3447 FAIL | 15.5600 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-feverish/0.9.png) |
| fx-ct-tension-zoom | PROVISIONAL_PASS | 0.0419 PASS | 0.0474 PASS | 0.0221 PASS | 0.0196 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-tension-zoom/0.3.png) |
| fx-ct-cut-shift | FAIL | 5.4751 FAIL | 1.8679 PASS | 1.8623 PASS | 1.0875 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-cut-shift/0.3.png) |
| fx-cc-move-cloud | PROVISIONAL_PASS | 0.0526 PASS | 0.0561 PASS | 0.0441 PASS | 0.0354 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-move-cloud/0.9.png) |
| fx-ct-chaotic-heat | FAIL | 21.5532 FAIL | 18.8008 FAIL | 14.1304 FAIL | 8.4214 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-chaotic-heat/0.3.png) |
| fx-ct-magical-tome | PROVISIONAL_PASS | 0.0395 PASS | 0.0344 PASS | 0.0153 PASS | 0.0093 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-magical-tome/2.1.png) |
| fx-ct-grim-reaper | PROVISIONAL_PASS | 0.0281 PASS | 0.0340 PASS | 0.0102 PASS | 0.0077 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-grim-reaper/0.3.png) |
| fx-ct-phone-zoom | PROVISIONAL_PASS | 0.0434 PASS | 0.0450 PASS | 0.0257 PASS | 0.0230 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-phone-zoom/1.5.png) |
| fx-cc-retro-flicker | FAIL | 5.0029 FAIL | 4.9623 FAIL | 4.9623 FAIL | 4.2563 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-retro-flicker/2.1.png) |
| fx-cc-handheld | PROVISIONAL_PASS | 0.0368 PASS | 0.0396 PASS | 0.0230 PASS | 0.0215 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-handheld/1.5.png) |
| fx-cc-wiggle-flicker | FAIL | 24.0836 FAIL | 23.2901 FAIL | 23.2901 FAIL | 22.8381 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-wiggle-flicker/0.3.png) |
| fx-cc-hex-split | PROVISIONAL_PASS | 0.8606 PASS | 0.4003 PASS | 0.3944 PASS | 0.3623 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-hex-split/1.5.png) |
| fx-cc-heart-ascent | PROVISIONAL_PASS | 1.3407 PASS | 0.5130 PASS | 0.5130 PASS | 0.1988 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-heart-ascent/2.1.png) |
| fx-cc-butterfly | PROVISIONAL_PASS | 0.0462 PASS | 0.0497 PASS | 0.0149 PASS | 0.0050 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-butterfly/1.5.png) |
| fx-cc-butterfly-dream | PROVISIONAL_PASS | 0.0487 PASS | 0.0469 PASS | 0.0156 PASS | 0.0070 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-butterfly-dream/0.3.png) |
| fx-cc-leak-warm | PROVISIONAL_PASS | 0.0342 PASS | 0.0389 PASS | 0.0231 PASS | 0.0211 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-leak-warm/0.3.png) |
| fx-cc-leak-neon | PROVISIONAL_PASS | 0.0386 PASS | 0.0469 PASS | 0.0174 PASS | 0.0111 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-leak-neon/0.3.png) |
| fx-cc-falling-petals | PROVISIONAL_PASS | 0.0426 PASS | 0.0606 PASS | 0.0163 PASS | 0.0148 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-falling-petals/2.1.png) |
| fx-cc-smoky-focus | PROVISIONAL_PASS | 0.0423 PASS | 0.0464 PASS | 0.0134 PASS | 0.0088 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-smoky-focus/0.9.png) |
| fx-cc-vintage-film | FAIL | 6.9153 FAIL | 6.9263 FAIL | 6.9252 FAIL | 6.0374 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-vintage-film/0.9.png) |
| fx-cc-stellar | PROVISIONAL_PASS | 0.0556 PASS | 0.0976 PASS | 0.0428 PASS | 0.0277 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-stellar/1.5.png) |
| fx-cc-ink-spill | PROVISIONAL_PASS | 0.0316 PASS | 0.0486 PASS | 0.0131 PASS | 0.0080 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-ink-spill/0.3.png) |
| fx-cc-exploding-love | FAIL | 6.2980 FAIL | 1.4146 PASS | 1.2540 PASS | 0.3605 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-exploding-love/0.3.png) |
| fx-cc-damaged-vignette | PROVISIONAL_PASS | 0.0552 PASS | 0.0728 PASS | 0.0482 PASS | 0.0146 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-damaged-vignette/2.1.png) |
| fx-cc-firefly | FAIL | 5.2096 FAIL | 0.7958 PASS | 0.7340 PASS | 0.6260 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-firefly/0.9.png) |
| fx-cc-misty-tint | PROVISIONAL_PASS | 0.0341 PASS | 0.0445 PASS | 0.0233 PASS | 0.0200 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-misty-tint/1.5.png) |
| fx-cc-sepia-cool | PROVISIONAL_PASS | 0.1806 PASS | 0.1020 PASS | 0.0242 PASS | 0.0167 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-sepia-cool/0.3.png) |
| fx-cc-snow-night | PROVISIONAL_PASS | 0.0420 PASS | 0.0764 PASS | 0.0212 PASS | 0.0076 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-snow-night/2.1.png) |
| fx-cc-rain | PROVISIONAL_PASS | 0.0380 PASS | 0.0480 PASS | 0.0289 PASS | 0.0175 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-rain/2.1.png) |
| fx-cc-rose-bloom | FAIL | 2.1497 FAIL | 0.5459 PASS | 0.5366 PASS | 0.3887 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-rose-bloom/2.1.png) |
| fx-cc-sparkle-shine | PROVISIONAL_PASS | 0.0299 PASS | 0.0572 PASS | 0.0118 PASS | 0.0054 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-sparkle-shine/2.1.png) |
| fx-cc-pixel-blocks | PROVISIONAL_PASS | 0.0817 PASS | 0.0636 PASS | 0.0271 PASS | 0.0194 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-pixel-blocks/1.5.png) |
| fx-cc-pixel-mutant | FAIL | 2.4481 FAIL | 0.5417 PASS | 0.1042 PASS | 0.0355 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-pixel-mutant/0.9.png) |
| fx-cc-pixel-universe | PROVISIONAL_PASS | 0.0440 PASS | 0.0470 PASS | 0.0179 PASS | 0.0081 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-pixel-universe/2.1.png) |
| fx-pb-pixel-scan | PROVISIONAL_PASS | 0.0414 PASS | 0.0437 PASS | 0.0145 PASS | 0.0085 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pb-pixel-scan/0.3.png) |
| fx-pb-flip-phone | PROVISIONAL_PASS | 0.0322 PASS | 0.0308 PASS | 0.0204 PASS | 0.0095 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pb-flip-phone/2.1.png) |
| fx-pb-pixel-breakdown | PROVISIONAL_PASS | 0.2250 PASS | 0.0771 PASS | 0.0288 PASS | 0.0146 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pb-pixel-breakdown/0.9.png) |
| fx-pb-sweet-party | FAIL | 3.9860 FAIL | 1.0928 PASS | 0.6846 PASS | 0.4236 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pb-sweet-party/1.5.png) |
| fx-pb-misty-tint | PROVISIONAL_PASS | 0.0490 PASS | 0.0482 PASS | 0.0148 PASS | 0.0142 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pb-misty-tint/0.9.png) |
| fx-pb-bead-art | PROVISIONAL_PASS | 0.0736 PASS | 0.0622 PASS | 0.0359 PASS | 0.0302 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pb-bead-art/0.9.png) |
| fx-pb-pixel-rain | PROVISIONAL_PASS | 0.0559 PASS | 0.0496 PASS | 0.0196 PASS | 0.0136 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pb-pixel-rain/2.1.png) |
| fx-st-polaroid | PROVISIONAL_PASS | 0.0416 PASS | 0.0464 PASS | 0.0324 PASS | 0.0204 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-st-polaroid/2.1.png) |
| fx-st-retro-tv | PROVISIONAL_PASS | 0.3757 PASS | 0.3689 PASS | 0.3682 PASS | 0.3360 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-st-retro-tv/1.5.png) |
| fx-st-billboard | PROVISIONAL_PASS | 0.0211 PASS | 0.0222 PASS | 0.0105 PASS | 0.0088 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-st-billboard/1.5.png) |
| fx-st-film-strip | PROVISIONAL_PASS | 1.3261 PASS | 1.2203 PASS | 1.2165 PASS | 1.0721 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-st-film-strip/1.5.png) |
| fx-ne-frame | PROVISIONAL_PASS | 1.7824 PASS | 1.8574 PASS | 1.8574 PASS | 1.8215 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ne-frame/0.9.png) |
| fx-ne-ring | FAIL | 2.2001 FAIL | 2.2012 FAIL | 2.2012 FAIL | 0.9625 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ne-ring/0.9.png) |
| fx-ne-heart | PROVISIONAL_PASS | 0.0461 PASS | 0.0491 PASS | 0.0196 PASS | 0.0035 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ne-heart/2.1.png) |
| fx-ne-lightning | PROVISIONAL_PASS | 0.0362 PASS | 0.0514 PASS | 0.0186 PASS | 0.0041 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ne-lightning/1.5.png) |
| fx-ne-stars | FAIL | 6.2452 FAIL | 1.6152 PASS | 1.4188 PASS | 1.2619 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ne-stars/2.1.png) |
| fx-cr-sparkle-burst | FAIL | 2.5530 FAIL | 0.5564 PASS | 0.2852 PASS | 0.2776 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-sparkle-burst/2.1.png) |
| fx-cr-glitter-rain | PROVISIONAL_PASS | 0.0331 PASS | 0.0567 PASS | 0.0142 PASS | 0.0059 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-glitter-rain/2.1.png) |
| fx-cr-matrix | PROVISIONAL_PASS | 0.0819 PASS | 0.0737 PASS | 0.0353 PASS | 0.0244 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-matrix/2.1.png) |
| fx-cr-signal-lost | FAIL | 37.5093 FAIL | 21.3741 FAIL | 21.1771 FAIL | 13.9916 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-signal-lost/2.1.png) |
| fx-cr-slice-drift | PROVISIONAL_PASS | 0.0456 PASS | 0.0505 PASS | 0.0351 PASS | 0.0319 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-slice-drift/0.3.png) |
| fx-cr-projector | FAIL | 3.0499 FAIL | 3.0494 FAIL | 3.0494 FAIL | 2.5427 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-projector/0.3.png) |
| fx-cr-flash-cut | PROVISIONAL_PASS | 0.0834 PASS | 0.0718 PASS | 0.0298 PASS | 0.0155 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-flash-cut/0.9.png) |
| fx-cr-aurora | PROVISIONAL_PASS | 0.0332 PASS | 0.0386 PASS | 0.0185 PASS | 0.0136 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-aurora/0.3.png) |
| fx-cr-twinkle-sky | PROVISIONAL_PASS | 0.0325 PASS | 0.0549 PASS | 0.0137 PASS | 0.0020 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-twinkle-sky/0.9.png) |
| fx-cr-tri-split | FAIL | 2.7920 FAIL | 0.9925 PASS | 0.9019 PASS | 0.6927 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-tri-split/0.3.png) |
| fx-mt-photo-snap | FAIL | 13.4940 FAIL | 0.5039 PASS | 0.3597 PASS | 0.3466 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-mt-photo-snap/1.5.png) |
| fx-mt-zoom-shake | PROVISIONAL_PASS | 1.4879 PASS | 0.5960 PASS | 0.4391 PASS | 0.4333 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-mt-zoom-shake/1.5.png) |
| fx-globe | PROVISIONAL_PASS | 0.0395 PASS | 0.0434 PASS | 0.0296 PASS | 0.0210 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-globe/0.3.png) |
| fx-hyperspace | FAIL | 2.8071 FAIL | 2.4937 FAIL | 2.1307 FAIL | 1.6004 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-hyperspace/2.1.png) |
| fx-star-rush | FAIL | 2.9287 FAIL | 0.5230 PASS | 0.2646 PASS | 0.2587 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-star-rush/0.9.png) |
| fx-camera-shake | PROVISIONAL_PASS | 0.0469 PASS | 0.0484 PASS | 0.0297 PASS | 0.0211 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-camera-shake/1.5.png) |
| fx-film-grain | PROVISIONAL_PASS | 0.6841 PASS | 0.3131 PASS | 0.2805 PASS | 0.2577 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-film-grain/0.3.png) |
| fx-glitch-in | PROVISIONAL_PASS | 0.0653 PASS | 0.0660 PASS | 0.0535 PASS | 0.0400 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-glitch-in/0.9.png) |
| fx-film-burn | PROVISIONAL_PASS | 0.0281 PASS | 0.0449 PASS | 0.0183 PASS | 0.0082 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-film-burn/0.3.png) |
| fx-torn-reveal | PROVISIONAL_PASS | 0.0278 PASS | 0.0481 PASS | 0.0147 PASS | 0.0066 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-torn-reveal/2.1.png) |
| fx-mirror-ball | PROVISIONAL_PASS | 0.0945 PASS | 0.0795 PASS | 0.0248 PASS | 0.0240 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-mirror-ball/2.1.png) |
| fx-confetti | PROVISIONAL_PASS | 0.0289 PASS | 0.0411 PASS | 0.0152 PASS | 0.0079 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-confetti/1.5.png) |
| fx-balloons | PROVISIONAL_PASS | 0.0453 PASS | 0.0443 PASS | 0.0120 PASS | 0.0021 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-balloons/0.9.png) |
| fx-earthquake | PROVISIONAL_PASS | 0.0477 PASS | 0.0490 PASS | 0.0364 PASS | 0.0208 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-earthquake/2.1.png) |
| fx-light-leak | PROVISIONAL_PASS | 0.0375 PASS | 0.0442 PASS | 0.0072 PASS | 0.0062 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-light-leak/0.3.png) |
| fx-bokeh | PROVISIONAL_PASS | 0.0314 PASS | 0.0458 PASS | 0.0172 PASS | 0.0055 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-bokeh/2.1.png) |
| fx-grid-flash | PROVISIONAL_PASS | 0.0937 PASS | 0.1146 PASS | 0.0990 PASS | 0.0523 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-grid-flash/0.9.png) |
| fx-pencil-sketch | PROVISIONAL_PASS | 0.0631 PASS | 0.0641 PASS | 0.0164 PASS | 0.0103 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pencil-sketch/0.9.png) |
| fx-marker-lines | PROVISIONAL_PASS | 0.0732 PASS | 0.0588 PASS | 0.0381 PASS | 0.0256 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-marker-lines/2.1.png) |
| fx-spray-neon | PROVISIONAL_PASS | 0.0614 PASS | 0.0580 PASS | 0.0223 PASS | 0.0161 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-spray-neon/0.3.png) |
| fx-chalkboard | PROVISIONAL_PASS | 0.0620 PASS | 0.0549 PASS | 0.0352 PASS | 0.0302 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-chalkboard/0.9.png) |
| fx-scribble-frame | PROVISIONAL_PASS | 0.0273 PASS | 0.0412 PASS | 0.0140 PASS | 0.0061 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-scribble-frame/2.1.png) |
| fx-confetti-burst | PROVISIONAL_PASS | 0.5122 PASS | 0.1953 PASS | 0.1660 PASS | 0.1151 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-confetti-burst/0.3.png) |
| fx-gold-glitter | PROVISIONAL_PASS | 0.0337 PASS | 0.0380 PASS | 0.0163 PASS | 0.0059 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-gold-glitter/1.5.png) |
| fx-heart-rain | PROVISIONAL_PASS | 0.0371 PASS | 0.0452 PASS | 0.0120 PASS | 0.0057 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-heart-rain/2.1.png) |
| fx-bubbles | PROVISIONAL_PASS | 0.0289 PASS | 0.0478 PASS | 0.0122 PASS | 0.0037 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-bubbles/2.1.png) |
| fx-star-pop | PROVISIONAL_PASS | 0.0320 PASS | 0.0393 PASS | 0.0127 PASS | 0.0071 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-star-pop/0.3.png) |
| fx-petals | PROVISIONAL_PASS | 0.0368 PASS | 0.0420 PASS | 0.0097 PASS | 0.0052 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-petals/0.3.png) |
| fx-snowfall | PROVISIONAL_PASS | 0.4775 PASS | 0.1489 PASS | 0.0674 PASS | 0.0634 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-snowfall/0.9.png) |
| fx-eight-bit | FAIL | 4.3675 FAIL | 4.3986 FAIL | 4.3982 FAIL | 4.3003 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-eight-bit/0.9.png) |
| fx-pixel-breakdown | PROVISIONAL_PASS | 0.1317 PASS | 0.1050 PASS | 0.0419 PASS | 0.0189 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pixel-breakdown/0.9.png) |
| fx-pixel-glitch | PROVISIONAL_PASS | 0.0414 PASS | 0.0452 PASS | 0.0141 PASS | 0.0109 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pixel-glitch/0.3.png) |
| fx-digital-blocks | PROVISIONAL_PASS | 0.0444 PASS | 0.0468 PASS | 0.0141 PASS | 0.0059 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-digital-blocks/2.1.png) |
| fx-vhs | PROVISIONAL_PASS | 0.2372 PASS | 0.1486 PASS | 0.0400 PASS | 0.0340 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-vhs/0.3.png) |
| fx-scanline-jitter | PROVISIONAL_PASS | 0.0735 PASS | 0.0711 PASS | 0.0646 PASS | 0.0532 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-scanline-jitter/0.3.png) |
| fx-datamosh | PROVISIONAL_PASS | 0.0806 PASS | 0.0595 PASS | 0.0174 PASS | 0.0167 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-datamosh/0.9.png) |
| fx-signal-loss | PROVISIONAL_PASS | 0.6587 PASS | 0.3040 PASS | 0.2432 PASS | 0.2426 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-signal-loss/0.9.png) |
| fx-bad-tv | PROVISIONAL_PASS | 0.3804 PASS | 0.1831 PASS | 0.0613 PASS | 0.0544 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-bad-tv/0.3.png) |
| fx-glitch-flash | PROVISIONAL_PASS | 0.0556 PASS | 0.0559 PASS | 0.0444 PASS | 0.0402 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-glitch-flash/0.9.png) |
| fx-vhs-rewind | PROVISIONAL_PASS | 0.5094 PASS | 0.2508 PASS | 0.2257 PASS | 0.2058 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-vhs-rewind/0.3.png) |
| fx-film-8mm | PROVISIONAL_PASS | 0.8832 PASS | 0.3939 PASS | 0.3546 PASS | 0.3447 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-film-8mm/0.9.png) |
| fx-old-tv | PROVISIONAL_PASS | 0.4864 PASS | 0.2985 PASS | 0.2477 PASS | 0.2450 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-old-tv/0.3.png) |
| fx-film-burn-leak | PROVISIONAL_PASS | 0.0370 PASS | 0.0428 PASS | 0.0056 PASS | 0.0053 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-film-burn-leak/0.3.png) |
| fx-seventies | PROVISIONAL_PASS | 0.4168 PASS | 0.2676 PASS | 0.1662 PASS | 0.1634 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-seventies/0.9.png) |
| fx-dust-scratches | PROVISIONAL_PASS | 0.0954 PASS | 0.0872 PASS | 0.0872 PASS | 0.0802 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-dust-scratches/1.5.png) |
| fx-tr-slam-merge | PROVISIONAL_PASS | 0.1090 PASS | 0.1120 PASS | 0.0999 PASS | 0.0684 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-tr-slam-merge/0.3.png) |
| fx-tr-heat-flicks | PROVISIONAL_PASS | 0.0495 PASS | 0.0490 PASS | 0.0388 PASS | 0.0374 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-tr-heat-flicks/1.5.png) |
| fx-tr-shake | PROVISIONAL_PASS | 0.1037 PASS | 0.1107 PASS | 0.0975 PASS | 0.0572 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-tr-shake/1.5.png) |
| fx-tr-glitch | PROVISIONAL_PASS | 0.0505 PASS | 0.0495 PASS | 0.0399 PASS | 0.0307 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-tr-glitch/1.5.png) |
| fx-tr-flare | PROVISIONAL_PASS | 0.0928 PASS | 0.1060 PASS | 0.0857 PASS | 0.0616 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-tr-flare/1.5.png) |
| fx-tr-film-burn | PROVISIONAL_PASS | 0.0436 PASS | 0.0490 PASS | 0.0342 PASS | 0.0250 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-tr-film-burn/0.3.png) |
| fx-ov-glitter-rain | PROVISIONAL_PASS | 0.0505 PASS | 0.0574 PASS | 0.0327 PASS | 0.0233 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-glitter-rain/0.3.png) |
| fx-ov-gold-dust | PROVISIONAL_PASS | 0.0268 PASS | 0.0337 PASS | 0.0186 PASS | 0.0095 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-gold-dust/0.9.png) |
| fx-ov-bokeh-gold | PROVISIONAL_PASS | 0.0308 PASS | 0.0288 PASS | 0.0190 PASS | 0.0107 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bokeh-gold/0.9.png) |
| fx-ov-bokeh-pink | PROVISIONAL_PASS | 0.0313 PASS | 0.0356 PASS | 0.0247 PASS | 0.0095 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bokeh-pink/0.9.png) |
| fx-ov-sparks | PROVISIONAL_PASS | 0.0306 PASS | 0.0378 PASS | 0.0247 PASS | 0.0192 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sparks/2.1.png) |
| fx-ov-embers | PROVISIONAL_PASS | 0.0500 PASS | 0.0607 PASS | 0.0395 PASS | 0.0233 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-embers/0.9.png) |
| fx-ov-smoke | PROVISIONAL_PASS | 0.0295 PASS | 0.0316 PASS | 0.0143 PASS | 0.0094 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-smoke/2.1.png) |
| fx-ov-fog-white | PROVISIONAL_PASS | 0.0422 PASS | 0.0475 PASS | 0.0207 PASS | 0.0193 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-fog-white/2.1.png) |
| fx-ov-galaxy | PROVISIONAL_PASS | 0.0341 PASS | 0.0348 PASS | 0.0142 PASS | 0.0134 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-galaxy/2.1.png) |
| fx-ov-fireflies | PROVISIONAL_PASS | 0.1714 PASS | 0.1844 PASS | 0.1727 PASS | 0.0986 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-fireflies/1.5.png) |
| fx-ov-magic-dust | PROVISIONAL_PASS | 0.0718 PASS | 0.0836 PASS | 0.0650 PASS | 0.0162 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-magic-dust/0.9.png) |
| fx-ov-sparkle-white | PROVISIONAL_PASS | 0.0276 PASS | 0.0680 PASS | 0.0199 PASS | 0.0138 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sparkle-white/2.1.png) |
| fx-ov-glitter | PROVISIONAL_PASS | 0.0278 PASS | 0.0523 PASS | 0.0201 PASS | 0.0131 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-glitter/0.3.png) |
| fx-ov-hearts-pink | PROVISIONAL_PASS | 1.1192 PASS | 0.5616 PASS | 0.5557 PASS | 0.1897 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-hearts-pink/0.9.png) |
| fx-ov-stars | PROVISIONAL_PASS | 0.0350 PASS | 0.0297 PASS | 0.0184 PASS | 0.0126 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-stars/2.1.png) |
| fx-ov-confetti | PROVISIONAL_PASS | 0.0293 PASS | 0.0660 PASS | 0.0157 PASS | 0.0032 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-confetti/0.9.png) |
| fx-ov-snow | PROVISIONAL_PASS | 0.0471 PASS | 0.0507 PASS | 0.0326 PASS | 0.0321 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-snow/0.9.png) |
| fx-ov-rain | PROVISIONAL_PASS | 0.0315 PASS | 0.0821 PASS | 0.0197 PASS | 0.0154 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-rain/0.9.png) |
| fx-ov-bubbles | PROVISIONAL_PASS | 0.0229 PASS | 0.0325 PASS | 0.0186 PASS | 0.0079 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bubbles/0.9.png) |
| fx-ov-leak-warm | PROVISIONAL_PASS | 0.0423 PASS | 0.0444 PASS | 0.0311 PASS | 0.0102 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-leak-warm/0.3.png) |
| fx-ov-leak-pink | PROVISIONAL_PASS | 0.0436 PASS | 0.0603 PASS | 0.0337 PASS | 0.0282 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-leak-pink/0.3.png) |
| fx-ov-leak-blue | PROVISIONAL_PASS | 0.0462 PASS | 0.0503 PASS | 0.0336 PASS | 0.0309 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-leak-blue/0.3.png) |
| fx-ov-light-streaks | FAIL | 6.0954 FAIL | 4.1102 FAIL | 4.1102 FAIL | 3.1597 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-light-streaks/2.1.png) |
| fx-ov-stage-lights | PROVISIONAL_PASS | 0.0224 PASS | 0.0274 PASS | 0.0154 PASS | 0.0136 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-stage-lights/2.1.png) |
| fx-ov-bg-purple-smoke | PROVISIONAL_PASS | 0.0327 PASS | 0.0405 PASS | 0.0207 PASS | 0.0163 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-purple-smoke/2.1.png) |
| fx-ov-bg-pastel-ink | PROVISIONAL_PASS | 0.0446 PASS | 0.0442 PASS | 0.0442 PASS | 0.0333 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-pastel-ink/2.1.png) |
| fx-ov-bg-pink-gradient | PROVISIONAL_PASS | 0.0343 PASS | 0.0350 PASS | 0.0340 PASS | 0.0026 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-pink-gradient/0.3.png) |
| fx-ov-bg-blue-hearts | PROVISIONAL_PASS | 0.0372 PASS | 0.0405 PASS | 0.0336 PASS | 0.0052 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-blue-hearts/2.1.png) |
| fx-ov-bg-pink-bokeh | PROVISIONAL_PASS | 0.0401 PASS | 0.0393 PASS | 0.0368 PASS | 0.0242 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-pink-bokeh/0.3.png) |
| fx-ov-bg-golden-band | PROVISIONAL_PASS | 0.9698 PASS | 0.9708 PASS | 0.9697 PASS | 0.6983 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-golden-band/0.3.png) |
| fx-ov-bg-blue-smoke | PROVISIONAL_PASS | 0.0162 PASS | 0.0170 PASS | 0.0145 PASS | 0.0100 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-blue-smoke/0.3.png) |
| fx-ov-bg-light-wall | PROVISIONAL_PASS | 0.0247 PASS | 0.0302 PASS | 0.0079 PASS | 0.0075 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-light-wall/1.5.png) |
| fx-ov-bg-3d-shapes | FAIL | 10.3053 FAIL | 1.5475 PASS | 0.9118 PASS | 0.9054 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-3d-shapes/0.3.png) |
| fx-ov-bg-red-hearts | PROVISIONAL_PASS | 0.0462 PASS | 0.0467 PASS | 0.0464 PASS | 0.0325 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-red-hearts/0.3.png) |
| fx-ov-sc-aurora | PROVISIONAL_PASS | 0.0122 PASS | 0.0118 PASS | 0.0104 PASS | 0.0027 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sc-aurora/0.9.png) |
| fx-ov-sc-milky-way | PROVISIONAL_PASS | 0.6415 PASS | 0.6426 PASS | 0.6404 PASS | 0.4624 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sc-milky-way/2.1.png) |
| fx-ov-sc-sunset-sea | PROVISIONAL_PASS | 0.0123 PASS | 0.0149 PASS | 0.0086 PASS | 0.0062 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sc-sunset-sea/0.3.png) |
| fx-ov-sc-moon-dunes | PROVISIONAL_PASS | 0.0224 PASS | 0.0220 PASS | 0.0220 PASS | 0.0174 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sc-moon-dunes/2.1.png) |
| fx-ov-sc-clouds | PROVISIONAL_PASS | 0.0771 PASS | 0.0765 PASS | 0.0754 PASS | 0.0054 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sc-clouds/0.3.png) |
| fx-ov-sc-snowy-night | PROVISIONAL_PASS | 0.0143 PASS | 0.0154 PASS | 0.0143 PASS | 0.0052 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sc-snowy-night/2.1.png) |
| fx-ov-sc-ocean | PROVISIONAL_PASS | 0.0345 PASS | 0.0363 PASS | 0.0344 PASS | 0.0249 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sc-ocean/2.1.png) |
| fx-ov-sc-shooting-stars | PROVISIONAL_PASS | 0.4306 PASS | 0.2910 PASS | 0.2910 PASS | 0.2633 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sc-shooting-stars/2.1.png) |
| fx-ov-tr-shape-pop | PROVISIONAL_PASS | 0.0257 PASS | 0.0507 PASS | 0.0172 PASS | 0.0119 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-tr-shape-pop/0.3.png) |
| fx-ov-tr-glow-blob | PROVISIONAL_PASS | 0.0056 PASS | 0.0065 PASS | 0.0042 PASS | 0.0029 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-tr-glow-blob/2.1.png) |
| fx-ov-tr-fire-wipe | PROVISIONAL_PASS | 0.0274 PASS | 0.0326 PASS | 0.0206 PASS | 0.0200 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-tr-fire-wipe/2.1.png) |
| fx-ov-tr-ink | PROVISIONAL_PASS | 0.0100 PASS | 0.0132 PASS | 0.0033 PASS | 0.0029 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-tr-ink/2.1.png) |
| fx-ov-tr-light-burst | FAIL | 6.0542 FAIL | 1.5386 PASS | 1.2773 PASS | 1.1740 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-tr-light-burst/0.3.png) |
| fx-ov-tr-scribble | FAIL | 8.5961 FAIL | 1.1778 PASS | 1.0723 PASS | 0.4411 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-tr-scribble/0.9.png) |
| fx-ov-el-speed-lines | PROVISIONAL_PASS | 0.0387 PASS | 0.0428 PASS | 0.0133 PASS | 0.0110 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-el-speed-lines/1.5.png) |
| fx-ov-el-lightning | PROVISIONAL_PASS | 1.6386 PASS | 0.1375 PASS | 0.0571 PASS | 0.0178 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-el-lightning/0.9.png) |
| fx-ov-el-moon | PROVISIONAL_PASS | 0.0321 PASS | 0.0394 PASS | 0.0291 PASS | 0.0153 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-el-moon/2.1.png) |
| fx-ov-el-smoke-puff | PROVISIONAL_PASS | 0.0469 PASS | 0.0784 PASS | 0.0381 PASS | 0.0196 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-el-smoke-puff/2.1.png) |
| fx-ov-film-dust | PROVISIONAL_PASS | 0.4258 PASS | 0.2562 PASS | 0.2035 PASS | 0.1320 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-film-dust/0.9.png) |
| fx-ov-grain | PROVISIONAL_PASS | 0.7241 PASS | 0.3183 PASS | 0.2200 PASS | 0.2098 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-grain/0.3.png) |
| fx-ov-vhs | PROVISIONAL_PASS | 0.3283 PASS | 0.2086 PASS | 0.1244 PASS | 0.1212 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-vhs/0.3.png) |
| fx-ov-paper | PROVISIONAL_PASS | 0.0650 PASS | 0.0720 PASS | 0.0594 PASS | 0.0108 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-paper/0.9.png) |
| fx-ov-burnt-edges | PROVISIONAL_PASS | 0.0431 PASS | 0.0886 PASS | 0.0210 PASS | 0.0090 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-burnt-edges/0.3.png) |
| fx-ov-leak-green | PROVISIONAL_PASS | 0.0331 PASS | 0.0349 PASS | 0.0194 PASS | 0.0041 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-leak-green/2.1.png) |
| fx-ov-leak-purple | PROVISIONAL_PASS | 0.0339 PASS | 0.0348 PASS | 0.0120 PASS | 0.0090 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-leak-purple/2.1.png) |
| fx-ov-leak-red | PROVISIONAL_PASS | 0.0356 PASS | 0.0355 PASS | 0.0170 PASS | 0.0089 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-leak-red/1.5.png) |
| fx-ov-lightning | PROVISIONAL_PASS | 0.0380 PASS | 0.0694 PASS | 0.0280 PASS | 0.0135 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-lightning/2.1.png) |
| fx-ov-bokeh-blue | PROVISIONAL_PASS | 0.0322 PASS | 0.0353 PASS | 0.0243 PASS | 0.0078 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bokeh-blue/2.1.png) |
| fx-ov-bokeh-rainbow | PROVISIONAL_PASS | 0.0285 PASS | 0.0353 PASS | 0.0243 PASS | 0.0074 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bokeh-rainbow/2.1.png) |
| fx-ov-sparkle-pink | PROVISIONAL_PASS | 0.0206 PASS | 0.0300 PASS | 0.0194 PASS | 0.0118 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sparkle-pink/2.1.png) |
| fx-ov-sparkle-blue | PROVISIONAL_PASS | 0.0220 PASS | 0.0285 PASS | 0.0187 PASS | 0.0115 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sparkle-blue/2.1.png) |
| fx-ov-hearts-red | PROVISIONAL_PASS | 1.1192 PASS | 0.5616 PASS | 0.5557 PASS | 0.1841 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-hearts-red/0.9.png) |
| fx-ov-fog-pink | PROVISIONAL_PASS | 0.0425 PASS | 0.0705 PASS | 0.0282 PASS | 0.0231 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-fog-pink/1.5.png) |
| fx-ov-fog-blue | PROVISIONAL_PASS | 0.0391 PASS | 0.0717 PASS | 0.0256 PASS | 0.0200 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-fog-blue/0.9.png) |
