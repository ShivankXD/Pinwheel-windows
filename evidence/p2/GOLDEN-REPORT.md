# P2 strict pixel audit

This preserves the original per-pixel results for every spec. The owner's later
noise-class amendment uses structural review for noise-driven specs; see
STRUCTURAL-REPORT.md. Deterministic specs retain the original strict limits.

1411 passing frames, 277 failing frames, 0 missing frames.
321 passing specs out of 422 (all four times must pass).

MAE8 and p99 are in 8-bit channel units; divide by 255 for normalized error.
Thresholds remain MAE8 <= 2 and p99 <= 8 for every RGBA channel. No resizing,
alignment, flipping, filtering or tolerance changes are used in comparison.

[All 422 per-spec metrics](D:/Pinwheel-Windows/evidence/p2/per-spec-metrics.csv)
and [all 1688 per-time metrics](D:/Pinwheel-Windows/evidence/p2/golden-metrics.csv).

Worst 20 distinct specs ranked by largest per-channel MAE over four times;
p99 breaks ties. Each row selects that spec's worst time. Alpha is included.
The CSV also records each channel's largest MAE and largest p99 independently,
which can occur at different times. Review the [image gallery](D:/Pinwheel-Windows/evidence/p2/worst-20.html).

| Rank | Spec | Time (s) | Max channel MAE8 | Max channel p99 | Difference x16 |
|---|---|---:|---:|---:|---|
| 1 | fx-ct-scene-cut | 0.9 | 96.4000 | 158 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/01-fx-ct-scene-cut-0.9-heatmap.png) |
| 2 | fx-ct-negative-panels | 2.1 | 64.9876 | 247 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/02-fx-ct-negative-panels-2.1-heatmap.png) |
| 3 | fx-cr-signal-lost | 2.1 | 45.4844 | 148 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/03-fx-cr-signal-lost-2.1-heatmap.png) |
| 4 | fx-ct-chaotic-heat | 1.5 | 35.3948 | 154 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/04-fx-ct-chaotic-heat-1.5-heatmap.png) |
| 5 | fx-cc-wiggle-flicker | 0.3 | 33.5428 | 189 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/05-fx-cc-wiggle-flicker-0.3-heatmap.png) |
| 6 | fx-mt-photo-snap | 1.5 | 23.2000 | 167 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/06-fx-mt-photo-snap-1.5-heatmap.png) |
| 7 | fx-ct-error-quake | 0.3 | 21.9712 | 140 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/07-fx-ct-error-quake-0.3-heatmap.png) |
| 8 | fx-ov-tr-scribble | 0.9 | 21.1749 | 223 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/08-fx-ov-tr-scribble-0.9-heatmap.png) |
| 9 | fx-ov-grain | 0.3 | 19.1762 | 58 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/09-fx-ov-grain-0.3-heatmap.png) |
| 10 | fx-cc-shiny-stack | 0.9 | 18.4580 | 156 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/10-fx-cc-shiny-stack-0.9-heatmap.png) |
| 11 | fx-cc-feverish | 0.9 | 18.3781 | 29 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/11-fx-cc-feverish-0.9-heatmap.png) |
| 12 | fx-mt-zoom-shake | 1.5 | 15.2718 | 74 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/12-fx-mt-zoom-shake-1.5-heatmap.png) |
| 13 | fx-signal-loss | 0.9 | 14.7506 | 150 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/13-fx-signal-loss-0.9-heatmap.png) |
| 14 | fx-ov-bg-3d-shapes | 0.3 | 13.4304 | 126 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/14-fx-ov-bg-3d-shapes-0.3-heatmap.png) |
| 15 | fx-ct-lightning-crack | 0.3 | 13.2026 | 156 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/15-fx-ct-lightning-crack-0.3-heatmap.png) |
| 16 | fx-cc-vintage-film | 1.5 | 11.7968 | 42 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/16-fx-cc-vintage-film-1.5-heatmap.png) |
| 17 | fx-cc-retro-flicker | 2.1 | 11.5464 | 41 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/17-fx-cc-retro-flicker-2.1-heatmap.png) |
| 18 | fx-film-8mm | 0.9 | 11.4201 | 33 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/18-fx-film-8mm-0.9-heatmap.png) |
| 19 | fx-cr-tri-split | 1.5 | 10.7531 | 74 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/19-fx-cr-tri-split-1.5-heatmap.png) |
| 20 | fx-film-grain | 1.5 | 9.8983 | 33 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/20-fx-film-grain-1.5-heatmap.png) |
