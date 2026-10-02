# P2 strict pixel audit

This preserves the original per-pixel results for every spec. The owner's later
noise-class amendment uses structural review for noise-driven specs; see
STRUCTURAL-REPORT.md. Deterministic specs retain the original strict limits.

1436 passing frames, 252 failing frames, 0 missing frames.
336 passing specs out of 422 (all four times must pass).

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
| 1 | fx-ct-scene-cut | 0.9 | 96.1236 | 157 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/01-fx-ct-scene-cut-0.9-heatmap.png) |
| 2 | fx-ct-negative-panels | 2.1 | 64.8907 | 247 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/02-fx-ct-negative-panels-2.1-heatmap.png) |
| 3 | fx-cr-signal-lost | 2.1 | 45.3923 | 148 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/03-fx-cr-signal-lost-2.1-heatmap.png) |
| 4 | fx-ct-chaotic-heat | 1.5 | 35.3152 | 154 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/04-fx-ct-chaotic-heat-1.5-heatmap.png) |
| 5 | fx-cc-wiggle-flicker | 0.3 | 33.5164 | 189 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/05-fx-cc-wiggle-flicker-0.3-heatmap.png) |
| 6 | fx-mt-photo-snap | 1.5 | 23.0799 | 166 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/06-fx-mt-photo-snap-1.5-heatmap.png) |
| 7 | fx-ct-error-quake | 0.3 | 21.9421 | 140 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/07-fx-ct-error-quake-0.3-heatmap.png) |
| 8 | fx-ov-tr-scribble | 0.9 | 21.1222 | 222 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/08-fx-ov-tr-scribble-0.9-heatmap.png) |
| 9 | fx-ov-grain | 0.3 | 19.1778 | 58 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/09-fx-ov-grain-0.3-heatmap.png) |
| 10 | fx-cc-shiny-stack | 0.9 | 18.3921 | 156 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/10-fx-cc-shiny-stack-0.9-heatmap.png) |
| 11 | fx-cc-feverish | 0.9 | 18.3447 | 29 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/11-fx-cc-feverish-0.9-heatmap.png) |
| 12 | fx-mt-zoom-shake | 1.5 | 15.2225 | 74 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/12-fx-mt-zoom-shake-1.5-heatmap.png) |
| 13 | fx-signal-loss | 0.9 | 14.6357 | 150 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/13-fx-signal-loss-0.9-heatmap.png) |
| 14 | fx-ct-lightning-crack | 0.3 | 13.1195 | 156 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/14-fx-ct-lightning-crack-0.3-heatmap.png) |
| 15 | fx-ov-bg-3d-shapes | 0.3 | 13.1139 | 126 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/15-fx-ov-bg-3d-shapes-0.3-heatmap.png) |
| 16 | fx-cc-vintage-film | 1.5 | 11.7742 | 42 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/16-fx-cc-vintage-film-1.5-heatmap.png) |
| 17 | fx-cc-retro-flicker | 2.1 | 11.4962 | 41 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/17-fx-cc-retro-flicker-2.1-heatmap.png) |
| 18 | fx-film-8mm | 0.9 | 11.3778 | 33 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/18-fx-film-8mm-0.9-heatmap.png) |
| 19 | fx-cr-tri-split | 1.5 | 10.7110 | 74 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/19-fx-cr-tri-split-1.5-heatmap.png) |
| 20 | fx-film-grain | 1.5 | 9.8423 | 33 | [heatmap](D:/Pinwheel-Windows/evidence/p2/worst-20/20-fx-film-grain-1.5-heatmap.png) |
