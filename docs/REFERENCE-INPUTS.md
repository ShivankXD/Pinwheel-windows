# Read-only mobile reference inputs

The owner provides inputs at `D:/Pinwheel-Windows-refs`. The application and
checks never create or modify that folder. Outputs remain in the Windows repo.

Expected layout:

```text
D:/Pinwheel-Windows-refs/
  projects/
    video-project.json
    photo-project.pinwheel
  golden/
    fx-shake/
      0.3.png
      0.9.png
      1.5.png
      2.1.png
  screenshots/
    ...owner supplied mobile screenshots...
```

Raw project JSON uses the mobile v1..10 schema. Packages use `project.json` plus
`media/...` relative paths in every URI, including persisted history and active
photo cutout background images. Virtual `sticker:`, `overlay:` and `asset:` URIs
stay unchanged. Import writes fresh originals into a Windows test library and
rejects conflicting project IDs, unsafe zip paths, missing/unreferenced files,
case-colliding entries and excessive sizes. Export refuses to overwrite a file.

Golden PNGs are dedicated VideoFxPreviewRenderer renders at 0.3, 0.9, 1.5 and
2.1 seconds, 192x240, default params, source sample photos and SWAY. Contact
sheets at 0.2/0.7/1.2/1.8 are not interchangeable. The pinned catalog has 277
effects, 46 transitions, 80 active overlays and 19 legacy overlays; the report
lists each ID and expected sample. Legacy references are supplied alongside
active ones. No fabricated reference is accepted as mobile evidence.

P2 desktop renders go to `evidence/p2/frames/<id>/<time>.png`. Comparisons use
sRGB8 per-channel MAE <= 2 and p99 <= 8 (equivalent to 2/255 and 8/255).
RGBA metrics and heatmap paths are recorded without resizing or alignment.

```powershell
./scripts/build.ps1 -Tasks ':pinwheel-render:p1References'
./scripts/build.ps1 -Tasks ':pinwheel-render:mobilePackagesCheck'
./scripts/build.ps1 -Tasks ':pinwheel-render:p2Frames'
./scripts/build.ps1 -Tasks ':pinwheel-render:goldenCheck'
python scripts/report-p2-goldens.py
```

Inventory reports MISSING without treating it as a passing parity check.
Provided failing references always fail the task. Strict checks also fail if
inputs are absent. `-Ppinwheel.refs=<path>` supports another read-only input
location. Reports/diffs are under `evidence/p1/runtime`; package test imports
are under `pinwheel-core/build/reference-work`. Project and golden modes execute
independently and save `projects-status.json` and `golden-status.json`. P1 is
closed with four passing real mobile JSON/package inputs. P2 has all 1688 actual
frames: 1411 pass, 277 fail, none are missing. Pixel acceptance is still open.

`evidence/p2/per-spec-metrics.csv` records every RGBA channel's maximum MAE and
p99 over the four times for all 422 specs. `golden-metrics.csv` records every
individual time/channel. `GOLDEN-REPORT.md` and `worst-20.html` show the 20 worst
distinct specs with copied mobile/desktop/heatmap triplets. No personal gallery
thumbnails are copied. The reference manifest records 1715 read-only input
hashes, verified unchanged at handoff. Device-library JSONs were not supplied.

Owner noise amendment: `goldenCheck` now uses strict MAE/p99 for 229 deterministic
specs and Gaussian/histogram/mean-luminance review for 193 noise-driven specs.
The proposed structural limits are marked PROVISIONAL_OWNER_REVIEW. The current
owner-policy gate fails: 69 deterministic and 86 structural frames fail.
`strictGoldenCheck` keeps the original all-spec pixel audit (277 failures).
Run `python scripts/report-p2-structure.py` for the full noise-spec list, metrics
and worst 20 structural heatmaps. `goldenAudit` writes strict measurements without
asserting the old all-spec gate; the owner-policy check applies the new class gate.
No frame, mobile reference or shader is edited by these comparisons.
