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
lists each ID and expected sample. Legacy references can be supplied alongside
active ones. No fabricated reference is accepted as mobile evidence.

P2 desktop renders go to `evidence/p2/frames/<id>/<time>.png`. Comparisons use
sRGB8 per-channel MAE <= 2 and p99 <= 8 (equivalent to 2/255 and 8/255).
RGBA metrics and heatmap paths are recorded without resizing or alignment.

```powershell
./scripts/build.ps1 -Tasks ':pinwheel-render:p1References'
./scripts/build.ps1 -Tasks ':pinwheel-render:mobilePackagesCheck'
./scripts/build.ps1 -Tasks ':pinwheel-render:goldenCheck'
```

Inventory reports MISSING without treating it as a passing parity check.
Provided failing references always fail the task. Strict checks also fail if
inputs are absent. `-Ppinwheel.refs=<path>` supports another read-only input
location. Reports/diffs are under `evidence/p1/runtime`; package test imports
are under `pinwheel-core/build/reference-work`. P1 approval remains pending real
mobile round trips; P2 pixel parity additionally needs the render runtime.
