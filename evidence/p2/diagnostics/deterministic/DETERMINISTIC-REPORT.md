# Remaining deterministic stage investigation

Production acceptance is unchanged: 18 deterministic frames from seven specs
fail strict per-channel MAE8 <= 2 and p99 <= 8. All 28 hardware probe hashes
match the production frame manifest. None of these diagnostic frames replace
`evidence/p2/frames`, reference inputs or comparison policy.

The unchanged shaders produce 10 strict passes/18 failures on NVIDIA and
4 passes/24 failures on WARP across these 28 selected cases. The WARP flat
Diamond fields at 0.3/0.9/2.1 are exact, but its photo-bearing 1.5 s frame fails.
A backend switch therefore regresses this set.

The sampling probe reads the stored SWAY texture, uploads exactly those bytes
and reruns the same unmodified catalog shader. All **56 same-backend replays
are byte-exact**. It then crosses prepared inputs independently of shader
backend, with the same uniforms and mobile quad. This isolates the prepared
input stage from later shader execution/storage without guessing phone pixels.

| Spec | NVIDIA shader/input passes; max MAE8 | WARP shader/input passes; max MAE8 | NVIDIA shader/WARP input passes; max MAE8 | WARP shader/NVIDIA input passes; max MAE8 |
|---|---|---|---|---|
| fx-ov-tr-diamond | 1/4; 8.467969 | 3/4; 0.492036 | 0/4; 8.467969 | 4/4; 0.068728 |
| fx-cmyk-print | 0/4; 2.112782 | 0/4; 2.242470 | 0/4; 2.243490 | 0/4; 2.112739 |
| fx-mosaic-pulse | 3/4; 1.696224 | 1/4; 0.704948 | 0/4; 2.044553 | 3/4; 0.358247 |
| fx-pb-pixel-creation | 3/4; 1.547808 | 0/4; 0.833485 | 0/4; 1.857574 | 3/4; 0.552105 |
| fx-carousel | 0/4; 0.961523 | 0/4; 1.025065 | 0/4; 1.026736 | 0/4; 0.960851 |
| fx-ne-edges | 0/4; 0.431163 | 0/4; 1.692166 | 0/4; 1.695139 | 0/4; 0.431250 |
| fx-cc-dance-flash | 3/4; 0.353277 | 0/4; 1.483225 | 0/4; 1.480013 | 4/4; 0.354991 |

In aggregate, NVIDIA/NVIDIA passes 10/28, WARP/WARP 4/28,
NVIDIA/WARP 0/28 and WARP/NVIDIA 14/28. These are diagnostic strict results;
the cross-backend route is not adopted. It requires transfers and does not
establish the phone's complete sampling/precision behaviour.

With NVIDIA inputs, WARP's Neon Edges worst MAE drops from 1.692166 to
0.431250, and Dance Flash from 1.483225 to 0.354991. This supports a major
SWAY-input contribution to WARP's increased errors. Neon still fails; Dance
passes all four control times. CMYK and Carousel remain failing with crossed
inputs. Pixel Creation/Mosaic large grid errors shrink under WARP shader
execution but still fail, consistent with the existing floor-boundary lead.

The constant input [137,113,83,255] removes photo variation. Neon, Dance Flash,
Mosaic and Pixel Creation produce identical raw NVIDIA/WARP output. CMYK's
largest channel MAE is below 0.004 and Carousel below 0.020 in this control.
These are backend comparisons, not mobile golden claims.

Both Windows contexts report range 127 and 23 precision bits for high, medium
and low fragment floats. Phone queries remain pending. Translated fragment
HLSL is byte-identical for all seven specs. Vertex translations differ only
in the allocation of uFlipY to c1 versus c0; their operations are identical.
[Full translation hashes](translation-comparison.json) retain that difference;
no translated source is edited or compiled as a replacement.

CMYK's preserved source uses smoothstep with decreasing bounds (0.05, 0.0).
The [Khronos smoothstep reference](https://raw.githubusercontent.com/KhronosGroup/OpenGL-Refpages/main/es3.0/smoothstep.xml)
defines results as undefined for reversed bounds. This is a mobile portability
quirk to retain and review, not a proven explanation for the phone mismatch or
a reason to reclassify it as noise or loosen strict limits.

[All 112 per-time/per-channel stage measurements](stage-metrics.csv),
[seven mobile/desktop/control comparisons and heatmaps](remaining-seven.html),
[raw quantization signatures](deterministic-probe.json) and
[sampling/precision/constant controls](../sampling/sampling-probe.json).
The signature distinguishes errors of multiple RGB565 cells from one-cell
ties; raw value 127 alone does not prove a shader produced exact 0.5.

Owner phone precision, neutral SWAY and raw numeric inputs remain necessary
to validate a production repair. An initial LWJGL output-buffer guard failure
was corrected by allocating its required two slots; the successful full run
and the complete initial output are retained in final-sampling-and-runtime-checks.txt
and sampling-and-runtime-checks.txt under evidence/p2.
