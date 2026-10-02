# P2 owner structural review

Policy: **PROVISIONAL_OWNER_REVIEW**. Shared GLSL and deterministic strict limits
are unchanged. Classification comes from active shader branches and output helper
calls, independently of pass/fail images. Unused pure noise locals are excluded.
See [source classification](D:/Pinwheel-Windows/docs/P2-NOISE-CLASS.md).

| Class | Specs | Frames passing | Frames failing | Specs passing all times |
|---|---:|---:|---:|---:|
| Deterministic, strict MAE/p99 | 229 | 847 | 69 | 194 |
| Noise-driven, provisional structure | 193 | 686 | 86 | 159 |

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
| fx-shake | FAIL | 2.7636 FAIL | 0.4082 PASS | 0.1214 PASS | 0.0854 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-shake/2.1.png) |
| fx-cc-slash-reveal | PROVISIONAL_PASS | 1.6877 PASS | 0.2767 PASS | 0.1828 PASS | 0.1391 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-slash-reveal/2.1.png) |
| fx-cc-diamond-zoom | PROVISIONAL_PASS | 0.2277 PASS | 0.2640 PASS | 0.1143 PASS | 0.1003 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-diamond-zoom/2.1.png) |
| fx-ct-error-quake | FAIL | 6.4844 FAIL | 4.6451 FAIL | 4.4933 FAIL | 3.3978 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-error-quake/0.3.png) |
| fx-ct-explosion | PROVISIONAL_PASS | 0.1914 PASS | 0.2257 PASS | 0.1506 PASS | 0.1279 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-explosion/0.9.png) |
| fx-ct-scene-cut | FAIL | 95.5385 FAIL | 95.8671 FAIL | 95.8671 FAIL | 95.4906 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-scene-cut/0.9.png) |
| fx-cc-shiny-stack | FAIL | 11.6399 FAIL | 2.4078 FAIL | 0.7487 PASS | 0.7035 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-shiny-stack/0.3.png) |
| fx-ct-glass-breaking | FAIL | 4.3795 FAIL | 1.4593 PASS | 1.1475 PASS | 1.0993 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-glass-breaking/1.5.png) |
| fx-ct-negative-panels | FAIL | 58.9922 FAIL | 49.3269 FAIL | 49.3269 FAIL | 31.7604 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-negative-panels/2.1.png) |
| fx-ct-fireplace | PROVISIONAL_PASS | 0.5887 PASS | 0.3950 PASS | 0.2740 PASS | 0.2100 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-fireplace/0.9.png) |
| fx-ct-vignette-noir | PROVISIONAL_PASS | 0.2597 PASS | 0.2784 PASS | 0.1626 PASS | 0.1446 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-vignette-noir/2.1.png) |
| fx-ct-lightning-crack | FAIL | 13.0726 FAIL | 4.7549 FAIL | 4.0717 FAIL | 3.7938 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-lightning-crack/0.3.png) |
| fx-vignette | PROVISIONAL_PASS | 0.4875 PASS | 0.4255 PASS | 0.3106 PASS | 0.3067 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-vignette/0.3.png) |
| fx-ct-offset-slice | FAIL | 2.6146 FAIL | 1.2367 PASS | 1.0705 PASS | 0.7040 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-offset-slice/0.9.png) |
| fx-cc-feverish | FAIL | 18.3643 FAIL | 18.3781 FAIL | 18.3781 FAIL | 15.5904 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-feverish/0.9.png) |
| fx-ct-tension-zoom | PROVISIONAL_PASS | 0.2042 PASS | 0.2566 PASS | 0.1535 PASS | 0.1065 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-tension-zoom/1.5.png) |
| fx-ct-cut-shift | FAIL | 5.5044 FAIL | 1.9068 PASS | 1.8951 PASS | 1.1541 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-cut-shift/0.3.png) |
| fx-cc-move-cloud | PROVISIONAL_PASS | 0.4958 PASS | 0.5128 PASS | 0.4887 PASS | 0.3415 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-move-cloud/0.9.png) |
| fx-ct-chaotic-heat | FAIL | 21.5513 FAIL | 18.9042 FAIL | 14.2337 FAIL | 8.3397 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-chaotic-heat/0.3.png) |
| fx-ct-magical-tome | PROVISIONAL_PASS | 0.2641 PASS | 0.2906 PASS | 0.2198 PASS | 0.1648 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-magical-tome/2.1.png) |
| fx-ct-grim-reaper | PROVISIONAL_PASS | 0.1898 PASS | 0.2323 PASS | 0.1664 PASS | 0.1290 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-grim-reaper/0.9.png) |
| fx-ct-phone-zoom | PROVISIONAL_PASS | 0.2103 PASS | 0.2424 PASS | 0.2010 PASS | 0.1683 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ct-phone-zoom/0.3.png) |
| fx-cc-retro-flicker | FAIL | 5.0915 FAIL | 5.0592 FAIL | 5.0592 FAIL | 4.3440 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-retro-flicker/2.1.png) |
| fx-cc-handheld | PROVISIONAL_PASS | 0.1535 PASS | 0.1781 PASS | 0.1178 PASS | 0.0968 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-handheld/1.5.png) |
| fx-cc-wiggle-flicker | FAIL | 24.0248 FAIL | 23.2497 FAIL | 23.2132 FAIL | 22.7429 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-wiggle-flicker/0.3.png) |
| fx-cc-hex-split | PROVISIONAL_PASS | 0.8779 PASS | 0.5049 PASS | 0.3765 PASS | 0.3711 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-hex-split/1.5.png) |
| fx-cc-heart-ascent | PROVISIONAL_PASS | 1.3931 PASS | 0.6130 PASS | 0.5508 PASS | 0.2723 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-heart-ascent/2.1.png) |
| fx-cc-butterfly | PROVISIONAL_PASS | 0.2017 PASS | 0.2652 PASS | 0.1153 PASS | 0.0409 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-butterfly/2.1.png) |
| fx-cc-butterfly-dream | PROVISIONAL_PASS | 0.1954 PASS | 0.2112 PASS | 0.1186 PASS | 0.0913 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-butterfly-dream/0.3.png) |
| fx-cc-leak-warm | PROVISIONAL_PASS | 0.1822 PASS | 0.1976 PASS | 0.1359 PASS | 0.1129 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-leak-warm/2.1.png) |
| fx-cc-leak-neon | PROVISIONAL_PASS | 0.1946 PASS | 0.2257 PASS | 0.1278 PASS | 0.0865 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-leak-neon/0.3.png) |
| fx-cc-falling-petals | PROVISIONAL_PASS | 0.1935 PASS | 0.2485 PASS | 0.1364 PASS | 0.0434 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-falling-petals/1.5.png) |
| fx-cc-smoky-focus | PROVISIONAL_PASS | 0.1952 PASS | 0.2135 PASS | 0.1470 PASS | 0.0877 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-smoky-focus/2.1.png) |
| fx-cc-vintage-film | FAIL | 6.9529 FAIL | 6.9639 FAIL | 6.9628 FAIL | 6.1491 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-vintage-film/0.9.png) |
| fx-cc-stellar | PROVISIONAL_PASS | 0.2098 PASS | 0.2789 PASS | 0.1996 PASS | 0.1686 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-stellar/1.5.png) |
| fx-cc-ink-spill | PROVISIONAL_PASS | 0.2109 PASS | 0.2391 PASS | 0.1787 PASS | 0.1611 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-ink-spill/0.3.png) |
| fx-cc-exploding-love | FAIL | 6.3659 FAIL | 1.5099 PASS | 1.2467 PASS | 0.4496 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-exploding-love/0.3.png) |
| fx-cc-damaged-vignette | PROVISIONAL_PASS | 0.1872 PASS | 0.2065 PASS | 0.1851 PASS | 0.1443 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-damaged-vignette/0.3.png) |
| fx-cc-firefly | FAIL | 5.2227 FAIL | 0.8824 PASS | 0.8029 PASS | 0.7466 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-firefly/0.9.png) |
| fx-cc-misty-tint | PROVISIONAL_PASS | 0.0965 PASS | 0.1359 PASS | 0.0463 PASS | 0.0216 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-misty-tint/1.5.png) |
| fx-cc-sepia-cool | PROVISIONAL_PASS | 0.2751 PASS | 0.2975 PASS | 0.1206 PASS | 0.1035 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-sepia-cool/0.3.png) |
| fx-cc-snow-night | PROVISIONAL_PASS | 0.1650 PASS | 0.2389 PASS | 0.1473 PASS | 0.1300 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-snow-night/0.9.png) |
| fx-cc-rain | PROVISIONAL_PASS | 0.1707 PASS | 0.2373 PASS | 0.0984 PASS | 0.0383 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-rain/0.3.png) |
| fx-cc-rose-bloom | FAIL | 2.2097 FAIL | 0.7209 PASS | 0.4792 PASS | 0.3161 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-rose-bloom/2.1.png) |
| fx-cc-sparkle-shine | PROVISIONAL_PASS | 0.1817 PASS | 0.2477 PASS | 0.1337 PASS | 0.0347 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-sparkle-shine/1.5.png) |
| fx-cc-pixel-blocks | PROVISIONAL_PASS | 0.2090 PASS | 0.2368 PASS | 0.1015 PASS | 0.0741 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-pixel-blocks/2.1.png) |
| fx-cc-pixel-mutant | FAIL | 2.4791 FAIL | 0.6482 PASS | 0.1660 PASS | 0.1095 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-pixel-mutant/0.9.png) |
| fx-cc-pixel-universe | PROVISIONAL_PASS | 0.2243 PASS | 0.2452 PASS | 0.2149 PASS | 0.1829 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cc-pixel-universe/0.9.png) |
| fx-pb-pixel-scan | PROVISIONAL_PASS | 0.1736 PASS | 0.2164 PASS | 0.1227 PASS | 0.0850 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pb-pixel-scan/0.3.png) |
| fx-pb-flip-phone | PROVISIONAL_PASS | 0.2225 PASS | 0.2372 PASS | 0.1942 PASS | 0.1435 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pb-flip-phone/2.1.png) |
| fx-pb-pixel-breakdown | PROVISIONAL_PASS | 0.3746 PASS | 0.2698 PASS | 0.2653 PASS | 0.2220 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pb-pixel-breakdown/0.9.png) |
| fx-pb-sweet-party | FAIL | 4.0660 FAIL | 1.2697 PASS | 0.7256 PASS | 0.4884 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pb-sweet-party/1.5.png) |
| fx-pb-misty-tint | PROVISIONAL_PASS | 0.2072 PASS | 0.2329 PASS | 0.1010 PASS | 0.0382 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pb-misty-tint/2.1.png) |
| fx-pb-bead-art | PROVISIONAL_PASS | 0.1521 PASS | 0.2622 PASS | 0.0997 PASS | 0.0921 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pb-bead-art/0.9.png) |
| fx-pb-pixel-rain | PROVISIONAL_PASS | 0.1781 PASS | 0.2263 PASS | 0.1626 PASS | 0.1478 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pb-pixel-rain/2.1.png) |
| fx-st-polaroid | PROVISIONAL_PASS | 0.2874 PASS | 0.3437 PASS | 0.1732 PASS | 0.1497 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-st-polaroid/0.3.png) |
| fx-st-retro-tv | PROVISIONAL_PASS | 0.5096 PASS | 0.6386 PASS | 0.3439 PASS | 0.3055 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-st-retro-tv/1.5.png) |
| fx-st-billboard | PROVISIONAL_PASS | 0.2394 PASS | 0.2564 PASS | 0.2358 PASS | 0.2162 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-st-billboard/2.1.png) |
| fx-st-film-strip | PROVISIONAL_PASS | 1.4216 PASS | 1.4076 PASS | 1.0450 PASS | 0.9027 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-st-film-strip/1.5.png) |
| fx-ne-frame | FAIL | 1.8092 PASS | 2.0167 FAIL | 1.8401 PASS | 1.7892 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ne-frame/0.9.png) |
| fx-ne-ring | FAIL | 2.1569 FAIL | 2.3231 FAIL | 2.0971 FAIL | 0.8785 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ne-ring/0.9.png) |
| fx-ne-heart | PROVISIONAL_PASS | 0.1610 PASS | 0.1883 PASS | 0.1380 PASS | 0.0855 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ne-heart/0.3.png) |
| fx-ne-lightning | PROVISIONAL_PASS | 0.1579 PASS | 0.1954 PASS | 0.1317 PASS | 0.0504 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ne-lightning/0.9.png) |
| fx-ne-stars | FAIL | 6.3112 FAIL | 1.7597 PASS | 1.5360 PASS | 1.3591 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ne-stars/2.1.png) |
| fx-cr-sparkle-burst | FAIL | 2.6240 FAIL | 0.7213 PASS | 0.3817 PASS | 0.3518 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-sparkle-burst/2.1.png) |
| fx-cr-glitter-rain | PROVISIONAL_PASS | 0.1848 PASS | 0.2508 PASS | 0.1371 PASS | 0.0328 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-glitter-rain/1.5.png) |
| fx-cr-matrix | PROVISIONAL_PASS | 0.2483 PASS | 0.2760 PASS | 0.2308 PASS | 0.2199 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-matrix/2.1.png) |
| fx-cr-signal-lost | FAIL | 37.5597 FAIL | 21.3115 FAIL | 21.1155 FAIL | 13.9524 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-signal-lost/2.1.png) |
| fx-cr-slice-drift | PROVISIONAL_PASS | 0.2718 PASS | 0.3144 PASS | 0.1803 PASS | 0.1215 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-slice-drift/0.9.png) |
| fx-cr-projector | FAIL | 3.1418 FAIL | 3.1656 FAIL | 3.1408 FAIL | 2.6217 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-projector/0.3.png) |
| fx-cr-flash-cut | PROVISIONAL_PASS | 0.1914 PASS | 0.2698 PASS | 0.1027 PASS | 0.0987 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-flash-cut/0.9.png) |
| fx-cr-aurora | PROVISIONAL_PASS | 0.1609 PASS | 0.2052 PASS | 0.1324 PASS | 0.1170 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-aurora/2.1.png) |
| fx-cr-twinkle-sky | PROVISIONAL_PASS | 0.1599 PASS | 0.2227 PASS | 0.1533 PASS | 0.1384 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-twinkle-sky/0.9.png) |
| fx-cr-tri-split | FAIL | 2.8340 FAIL | 1.0672 PASS | 0.9655 PASS | 0.7276 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-cr-tri-split/0.3.png) |
| fx-mt-photo-snap | FAIL | 13.5956 FAIL | 0.8925 PASS | 0.3480 PASS | 0.2499 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-mt-photo-snap/1.5.png) |
| fx-mt-zoom-shake | PROVISIONAL_PASS | 1.5046 PASS | 0.7284 PASS | 0.5580 PASS | 0.5371 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-mt-zoom-shake/1.5.png) |
| fx-globe | PROVISIONAL_PASS | 0.2175 PASS | 0.2276 PASS | 0.1502 PASS | 0.1089 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-globe/0.3.png) |
| fx-hyperspace | FAIL | 2.8178 FAIL | 2.5220 FAIL | 2.1734 FAIL | 1.6353 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-hyperspace/2.1.png) |
| fx-star-rush | FAIL | 2.9412 FAIL | 0.6862 PASS | 0.3543 PASS | 0.2780 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-star-rush/0.9.png) |
| fx-camera-shake | PROVISIONAL_PASS | 0.1381 PASS | 0.1902 PASS | 0.1108 PASS | 0.0888 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-camera-shake/1.5.png) |
| fx-film-grain | PROVISIONAL_PASS | 0.6912 PASS | 0.4648 PASS | 0.2838 PASS | 0.2111 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-film-grain/2.1.png) |
| fx-glitch-in | PROVISIONAL_PASS | 0.3826 PASS | 0.4265 PASS | 0.3322 PASS | 0.0747 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-glitch-in/0.9.png) |
| fx-film-burn | PROVISIONAL_PASS | 0.2606 PASS | 0.3092 PASS | 0.1479 PASS | 0.1086 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-film-burn/0.9.png) |
| fx-torn-reveal | PROVISIONAL_PASS | 0.1375 PASS | 0.1977 PASS | 0.1244 PASS | 0.1078 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-torn-reveal/1.5.png) |
| fx-mirror-ball | PROVISIONAL_PASS | 0.1991 PASS | 0.2409 PASS | 0.1527 PASS | 0.1077 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-mirror-ball/0.9.png) |
| fx-confetti | PROVISIONAL_PASS | 0.1726 PASS | 0.2328 PASS | 0.0929 PASS | 0.0910 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-confetti/0.3.png) |
| fx-balloons | PROVISIONAL_PASS | 0.1682 PASS | 0.2116 PASS | 0.1051 PASS | 0.0756 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-balloons/0.3.png) |
| fx-earthquake | PROVISIONAL_PASS | 0.1437 PASS | 0.1836 PASS | 0.1201 PASS | 0.0944 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-earthquake/0.3.png) |
| fx-light-leak | PROVISIONAL_PASS | 0.1720 PASS | 0.2128 PASS | 0.1164 PASS | 0.0848 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-light-leak/0.3.png) |
| fx-bokeh | PROVISIONAL_PASS | 0.1355 PASS | 0.1868 PASS | 0.1269 PASS | 0.0921 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-bokeh/1.5.png) |
| fx-grid-flash | PROVISIONAL_PASS | 0.5154 PASS | 0.5831 PASS | 0.1879 PASS | 0.1614 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-grid-flash/0.3.png) |
| fx-pencil-sketch | PROVISIONAL_PASS | 0.2027 PASS | 0.3530 PASS | 0.1592 PASS | 0.0527 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pencil-sketch/2.1.png) |
| fx-marker-lines | PROVISIONAL_PASS | 0.1583 PASS | 0.1918 PASS | 0.0708 PASS | 0.0355 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-marker-lines/2.1.png) |
| fx-spray-neon | PROVISIONAL_PASS | 0.1563 PASS | 0.1896 PASS | 0.1527 PASS | 0.1287 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-spray-neon/1.5.png) |
| fx-chalkboard | PROVISIONAL_PASS | 0.2545 PASS | 0.3532 PASS | 0.1384 PASS | 0.1166 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-chalkboard/0.3.png) |
| fx-scribble-frame | PROVISIONAL_PASS | 0.1642 PASS | 0.2146 PASS | 0.1084 PASS | 0.0934 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-scribble-frame/1.5.png) |
| fx-confetti-burst | PROVISIONAL_PASS | 0.5976 PASS | 0.3471 PASS | 0.1874 PASS | 0.1788 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-confetti-burst/0.3.png) |
| fx-gold-glitter | PROVISIONAL_PASS | 0.1812 PASS | 0.2330 PASS | 0.1028 PASS | 0.0978 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-gold-glitter/0.3.png) |
| fx-heart-rain | PROVISIONAL_PASS | 0.1659 PASS | 0.2243 PASS | 0.1002 PASS | 0.0982 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-heart-rain/2.1.png) |
| fx-bubbles | PROVISIONAL_PASS | 0.1700 PASS | 0.2362 PASS | 0.1054 PASS | 0.0248 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-bubbles/2.1.png) |
| fx-star-pop | PROVISIONAL_PASS | 0.1840 PASS | 0.2328 PASS | 0.1156 PASS | 0.0807 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-star-pop/2.1.png) |
| fx-petals | PROVISIONAL_PASS | 0.1548 PASS | 0.2270 PASS | 0.1017 PASS | 0.0753 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-petals/0.3.png) |
| fx-snowfall | PROVISIONAL_PASS | 0.5117 PASS | 0.3173 PASS | 0.1038 PASS | 0.0419 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-snowfall/0.9.png) |
| fx-eight-bit | FAIL | 4.3206 FAIL | 4.5191 FAIL | 4.3321 FAIL | 4.2377 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-eight-bit/0.9.png) |
| fx-pixel-breakdown | PROVISIONAL_PASS | 0.2384 PASS | 0.2849 PASS | 0.1257 PASS | 0.0950 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pixel-breakdown/0.9.png) |
| fx-pixel-glitch | PROVISIONAL_PASS | 0.1832 PASS | 0.2347 PASS | 0.0973 PASS | 0.0929 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-pixel-glitch/1.5.png) |
| fx-digital-blocks | PROVISIONAL_PASS | 0.3916 PASS | 0.4197 PASS | 0.3912 PASS | 0.3100 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-digital-blocks/0.3.png) |
| fx-vhs | PROVISIONAL_PASS | 0.2997 PASS | 0.2864 PASS | 0.1684 PASS | 0.1460 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-vhs/0.3.png) |
| fx-scanline-jitter | PROVISIONAL_PASS | 0.3198 PASS | 0.3641 PASS | 0.2037 PASS | 0.1733 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-scanline-jitter/0.3.png) |
| fx-datamosh | PROVISIONAL_PASS | 0.3449 PASS | 0.3951 PASS | 0.2660 PASS | 0.2110 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-datamosh/0.3.png) |
| fx-signal-loss | PROVISIONAL_PASS | 0.7293 PASS | 0.4575 PASS | 0.3349 PASS | 0.3242 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-signal-loss/0.9.png) |
| fx-bad-tv | PROVISIONAL_PASS | 0.4231 PASS | 0.3605 PASS | 0.1657 PASS | 0.1436 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-bad-tv/0.3.png) |
| fx-glitch-flash | PROVISIONAL_PASS | 0.2610 PASS | 0.3087 PASS | 0.1658 PASS | 0.1466 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-glitch-flash/0.3.png) |
| fx-vhs-rewind | PROVISIONAL_PASS | 0.5106 PASS | 0.3576 PASS | 0.2047 PASS | 0.1864 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-vhs-rewind/0.9.png) |
| fx-film-8mm | PROVISIONAL_PASS | 0.9176 PASS | 0.5504 PASS | 0.2965 PASS | 0.2560 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-film-8mm/0.9.png) |
| fx-old-tv | PROVISIONAL_PASS | 0.4994 PASS | 0.4307 PASS | 0.2624 PASS | 0.2317 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-old-tv/1.5.png) |
| fx-film-burn-leak | PROVISIONAL_PASS | 0.1672 PASS | 0.2120 PASS | 0.1067 PASS | 0.0802 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-film-burn-leak/0.3.png) |
| fx-seventies | PROVISIONAL_PASS | 0.4526 PASS | 0.3897 PASS | 0.1940 PASS | 0.1351 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-seventies/0.9.png) |
| fx-dust-scratches | PROVISIONAL_PASS | 0.1997 PASS | 0.2315 PASS | 0.1971 PASS | 0.1815 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-dust-scratches/1.5.png) |
| fx-tr-slam-merge | PROVISIONAL_PASS | 0.3418 PASS | 0.4127 PASS | 0.3394 PASS | 0.1956 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-tr-slam-merge/2.1.png) |
| fx-tr-heat-flicks | PROVISIONAL_PASS | 0.4025 PASS | 0.4335 PASS | 0.4004 PASS | 0.3164 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-tr-heat-flicks/1.5.png) |
| fx-tr-shake | PROVISIONAL_PASS | 0.5560 PASS | 0.5769 PASS | 0.5507 PASS | 0.1451 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-tr-shake/1.5.png) |
| fx-tr-glitch | PROVISIONAL_PASS | 0.4104 PASS | 0.4733 PASS | 0.2263 PASS | 0.1708 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-tr-glitch/1.5.png) |
| fx-tr-flare | PROVISIONAL_PASS | 0.5064 PASS | 0.5127 PASS | 0.5067 PASS | 0.4175 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-tr-flare/1.5.png) |
| fx-tr-film-burn | PROVISIONAL_PASS | 0.4886 PASS | 0.5122 PASS | 0.4841 PASS | 0.1205 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-tr-film-burn/0.3.png) |
| fx-ov-glitter-rain | PROVISIONAL_PASS | 0.1286 PASS | 0.1611 PASS | 0.1088 PASS | 0.0679 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-glitter-rain/0.3.png) |
| fx-ov-gold-dust | PROVISIONAL_PASS | 0.1967 PASS | 0.2037 PASS | 0.1965 PASS | 0.1491 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-gold-dust/2.1.png) |
| fx-ov-bokeh-gold | PROVISIONAL_PASS | 0.3952 PASS | 0.4280 PASS | 0.3914 PASS | 0.3188 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bokeh-gold/0.9.png) |
| fx-ov-bokeh-pink | PROVISIONAL_PASS | 0.1499 PASS | 0.1613 PASS | 0.1446 PASS | 0.1052 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bokeh-pink/1.5.png) |
| fx-ov-sparks | PROVISIONAL_PASS | 0.5826 PASS | 0.6204 PASS | 0.5799 PASS | 0.4838 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sparks/2.1.png) |
| fx-ov-embers | PROVISIONAL_PASS | 0.2200 PASS | 0.2491 PASS | 0.2089 PASS | 0.0366 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-embers/0.9.png) |
| fx-ov-smoke | PROVISIONAL_PASS | 0.4134 PASS | 0.4371 PASS | 0.3985 PASS | 0.3380 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-smoke/2.1.png) |
| fx-ov-fog-white | PROVISIONAL_PASS | 0.2934 PASS | 0.3403 PASS | 0.1701 PASS | 0.1236 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-fog-white/0.3.png) |
| fx-ov-galaxy | PROVISIONAL_PASS | 0.2166 PASS | 0.2284 PASS | 0.2177 PASS | 0.1978 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-galaxy/0.9.png) |
| fx-ov-fireflies | PROVISIONAL_PASS | 0.1722 PASS | 0.2018 PASS | 0.1575 PASS | 0.0762 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-fireflies/1.5.png) |
| fx-ov-magic-dust | PROVISIONAL_PASS | 0.1749 PASS | 0.1840 PASS | 0.1632 PASS | 0.1281 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-magic-dust/2.1.png) |
| fx-ov-sparkle-white | PROVISIONAL_PASS | 0.1909 PASS | 0.2101 PASS | 0.1907 PASS | 0.1326 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sparkle-white/2.1.png) |
| fx-ov-glitter | PROVISIONAL_PASS | 0.2827 PASS | 0.3050 PASS | 0.2583 PASS | 0.2034 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-glitter/0.9.png) |
| fx-ov-hearts-pink | PROVISIONAL_PASS | 1.1789 PASS | 0.6913 PASS | 0.6167 PASS | 0.3084 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-hearts-pink/0.9.png) |
| fx-ov-stars | PROVISIONAL_PASS | 0.4363 PASS | 0.4616 PASS | 0.4360 PASS | 0.3515 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-stars/0.3.png) |
| fx-ov-confetti | PROVISIONAL_PASS | 0.2031 PASS | 0.2297 PASS | 0.1944 PASS | 0.0057 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-confetti/0.9.png) |
| fx-ov-snow | PROVISIONAL_PASS | 0.3713 PASS | 0.3959 PASS | 0.3062 PASS | 0.0646 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-snow/0.3.png) |
| fx-ov-rain | PROVISIONAL_PASS | 0.2075 PASS | 0.2952 PASS | 0.1867 PASS | 0.1632 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-rain/0.3.png) |
| fx-ov-bubbles | PROVISIONAL_PASS | 0.1726 PASS | 0.1876 PASS | 0.1689 PASS | 0.1205 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bubbles/1.5.png) |
| fx-ov-leak-warm | PROVISIONAL_PASS | 0.1945 PASS | 0.2056 PASS | 0.1798 PASS | 0.0747 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-leak-warm/0.3.png) |
| fx-ov-leak-pink | PROVISIONAL_PASS | 0.1958 PASS | 0.2337 PASS | 0.1789 PASS | 0.1697 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-leak-pink/2.1.png) |
| fx-ov-leak-blue | PROVISIONAL_PASS | 0.2554 PASS | 0.2897 PASS | 0.1595 PASS | 0.1433 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-leak-blue/0.3.png) |
| fx-ov-light-streaks | FAIL | 6.1877 FAIL | 4.2348 FAIL | 3.9222 FAIL | 3.0348 FAIL | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-light-streaks/2.1.png) |
| fx-ov-stage-lights | PROVISIONAL_PASS | 0.2389 PASS | 0.2448 PASS | 0.2389 PASS | 0.2025 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-stage-lights/0.9.png) |
| fx-ov-bg-purple-smoke | PROVISIONAL_PASS | 0.1192 PASS | 0.1175 PASS | 0.1164 PASS | 0.0632 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-purple-smoke/0.3.png) |
| fx-ov-bg-pastel-ink | PROVISIONAL_PASS | 1.0184 PASS | 1.0183 PASS | 1.0183 PASS | 0.7438 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-pastel-ink/2.1.png) |
| fx-ov-bg-pink-gradient | PROVISIONAL_PASS | 0.3841 PASS | 0.3848 PASS | 0.3840 PASS | 0.0497 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-pink-gradient/0.3.png) |
| fx-ov-bg-blue-hearts | PROVISIONAL_PASS | 0.3429 PASS | 0.3482 PASS | 0.3426 PASS | 0.1892 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-blue-hearts/2.1.png) |
| fx-ov-bg-pink-bokeh | PROVISIONAL_PASS | 0.0432 PASS | 0.0444 PASS | 0.0410 PASS | 0.0226 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-pink-bokeh/2.1.png) |
| fx-ov-bg-golden-band | PROVISIONAL_PASS | 1.1016 PASS | 1.1031 PASS | 1.1015 PASS | 0.8113 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-golden-band/0.3.png) |
| fx-ov-bg-blue-smoke | PROVISIONAL_PASS | 0.1099 PASS | 0.1210 PASS | 0.1090 PASS | 0.0461 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-blue-smoke/1.5.png) |
| fx-ov-bg-light-wall | PROVISIONAL_PASS | 0.0598 PASS | 0.0666 PASS | 0.0414 PASS | 0.0357 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-light-wall/1.5.png) |
| fx-ov-bg-3d-shapes | FAIL | 10.4997 FAIL | 1.8558 PASS | 0.8963 PASS | 0.8067 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-3d-shapes/0.3.png) |
| fx-ov-bg-red-hearts | PROVISIONAL_PASS | 0.1554 PASS | 0.1574 PASS | 0.1553 PASS | 0.0035 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bg-red-hearts/0.3.png) |
| fx-ov-sc-aurora | PROVISIONAL_PASS | 0.1425 PASS | 0.1405 PASS | 0.1397 PASS | 0.0827 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sc-aurora/0.9.png) |
| fx-ov-sc-milky-way | PROVISIONAL_PASS | 0.7126 PASS | 0.7146 PASS | 0.7114 PASS | 0.5673 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sc-milky-way/0.3.png) |
| fx-ov-sc-sunset-sea | PROVISIONAL_PASS | 0.1778 PASS | 0.1836 PASS | 0.1313 PASS | 0.0823 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sc-sunset-sea/1.5.png) |
| fx-ov-sc-moon-dunes | PROVISIONAL_PASS | 0.2002 PASS | 0.2064 PASS | 0.2005 PASS | 0.1141 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sc-moon-dunes/2.1.png) |
| fx-ov-sc-clouds | PROVISIONAL_PASS | 0.3720 PASS | 0.3727 PASS | 0.3713 PASS | 0.0801 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sc-clouds/2.1.png) |
| fx-ov-sc-snowy-night | PROVISIONAL_PASS | 0.1952 PASS | 0.1990 PASS | 0.1150 PASS | 0.0303 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sc-snowy-night/2.1.png) |
| fx-ov-sc-ocean | PROVISIONAL_PASS | 0.1762 PASS | 0.1782 PASS | 0.1761 PASS | 0.1203 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sc-ocean/2.1.png) |
| fx-ov-sc-shooting-stars | PROVISIONAL_PASS | 0.6814 PASS | 0.5392 PASS | 0.4645 PASS | 0.2406 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sc-shooting-stars/2.1.png) |
| fx-ov-tr-shape-pop | PROVISIONAL_PASS | 0.5327 PASS | 0.5563 PASS | 0.5247 PASS | 0.2908 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-tr-shape-pop/0.9.png) |
| fx-ov-tr-glow-blob | PROVISIONAL_PASS | 1.0000 PASS | 1.0000 PASS | 1.0000 PASS | 0.2126 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-tr-glow-blob/0.9.png) |
| fx-ov-tr-fire-wipe | PROVISIONAL_PASS | 0.5182 PASS | 0.5340 PASS | 0.5232 PASS | 0.4191 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-tr-fire-wipe/2.1.png) |
| fx-ov-tr-ink | PROVISIONAL_PASS | 0.0406 PASS | 0.0429 PASS | 0.0405 PASS | 0.0182 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-tr-ink/2.1.png) |
| fx-ov-tr-light-burst | FAIL | 6.0563 FAIL | 1.6519 PASS | 1.3804 PASS | 1.2590 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-tr-light-burst/2.1.png) |
| fx-ov-tr-scribble | FAIL | 8.6137 FAIL | 1.2770 PASS | 1.0867 PASS | 0.3097 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-tr-scribble/0.9.png) |
| fx-ov-el-speed-lines | PROVISIONAL_PASS | 0.2621 PASS | 0.3105 PASS | 0.1654 PASS | 0.1056 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-el-speed-lines/0.9.png) |
| fx-ov-el-lightning | PROVISIONAL_PASS | 1.6467 PASS | 0.5443 PASS | 0.5071 PASS | 0.4142 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-el-lightning/0.9.png) |
| fx-ov-el-moon | PROVISIONAL_PASS | 0.4091 PASS | 0.4359 PASS | 0.3788 PASS | 0.3208 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-el-moon/0.3.png) |
| fx-ov-el-smoke-puff | PROVISIONAL_PASS | 0.2067 PASS | 0.2329 PASS | 0.1887 PASS | 0.0511 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-el-smoke-puff/2.1.png) |
| fx-ov-film-dust | PROVISIONAL_PASS | 0.4173 PASS | 0.4010 PASS | 0.2880 PASS | 0.2588 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-film-dust/0.9.png) |
| fx-ov-grain | PROVISIONAL_PASS | 0.7318 PASS | 0.4974 PASS | 0.2097 PASS | 0.1870 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-grain/0.9.png) |
| fx-ov-vhs | PROVISIONAL_PASS | 0.3649 PASS | 0.3352 PASS | 0.2354 PASS | 0.2190 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-vhs/0.9.png) |
| fx-ov-paper | PROVISIONAL_PASS | 0.2903 PASS | 0.3139 PASS | 0.2655 PASS | 0.2050 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-paper/0.9.png) |
| fx-ov-burnt-edges | PROVISIONAL_PASS | 0.2109 PASS | 0.2289 PASS | 0.1962 PASS | 0.0214 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-burnt-edges/2.1.png) |
| fx-ov-leak-green | PROVISIONAL_PASS | 0.2687 PASS | 0.2913 PASS | 0.2095 PASS | 0.1885 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-leak-green/0.3.png) |
| fx-ov-leak-purple | PROVISIONAL_PASS | 0.3835 PASS | 0.4043 PASS | 0.3695 PASS | 0.2932 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-leak-purple/0.3.png) |
| fx-ov-leak-red | PROVISIONAL_PASS | 0.3835 PASS | 0.4043 PASS | 0.3695 PASS | 0.2996 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-leak-red/0.3.png) |
| fx-ov-lightning | PROVISIONAL_PASS | 0.1906 PASS | 0.2081 PASS | 0.1908 PASS | 0.1332 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-lightning/1.5.png) |
| fx-ov-bokeh-blue | PROVISIONAL_PASS | 0.1436 PASS | 0.1507 PASS | 0.1310 PASS | 0.0966 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bokeh-blue/1.5.png) |
| fx-ov-bokeh-rainbow | PROVISIONAL_PASS | 0.1492 PASS | 0.1664 PASS | 0.1121 PASS | 0.0842 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-bokeh-rainbow/1.5.png) |
| fx-ov-sparkle-pink | PROVISIONAL_PASS | 0.4478 PASS | 0.4747 PASS | 0.4475 PASS | 0.3619 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sparkle-pink/0.3.png) |
| fx-ov-sparkle-blue | PROVISIONAL_PASS | 0.4462 PASS | 0.4737 PASS | 0.4459 PASS | 0.3615 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-sparkle-blue/0.3.png) |
| fx-ov-hearts-red | PROVISIONAL_PASS | 1.1789 PASS | 0.6913 PASS | 0.6167 PASS | 0.3027 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-hearts-red/0.9.png) |
| fx-ov-fog-pink | PROVISIONAL_PASS | 0.1847 PASS | 0.2461 PASS | 0.1649 PASS | 0.1472 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-fog-pink/2.1.png) |
| fx-ov-fog-blue | PROVISIONAL_PASS | 0.1694 PASS | 0.2318 PASS | 0.1510 PASS | 0.1419 PASS | [heatmap](D:/Pinwheel-Windows/evidence/p2/structural-heatmaps/fx-ov-fog-blue/0.3.png) |
