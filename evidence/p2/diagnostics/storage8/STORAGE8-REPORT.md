# Alternative 8-bit attachment controls

All four linear formats report eight bits per RGBA channel, allocate without
GL errors and have complete framebuffers on NVIDIA D3D11 and Microsoft WARP.
The [Khronos BGRA extension](https://registry.khronos.org/OpenGL/extensions/EXT/EXT_texture_format_BGRA8888.txt)
defines the BGRA unsigned-byte formats and their colour-renderable storage.
The extension is advertised in both measured contexts. No floating format,
sRGB conversion, shared shader edit or explicit rounding pass is used.

A distinct upload [17,83,149,211] verifies all 46,080 texels in each format,
independently checking RGBA channel order and exact byte storage including
alpha. Upload bytes 127 and 128 remain exact. Endpoint observation shaders
confirm every clear/draw-half texel is below 0.5 on NVIDIA and above 0.5 on
WARP, before CPU readback. Uniform constants 0.25/0.5/0.65/0.92 give
64/127/166/235 on NVIDIA and 64/128/166/235 on WARP in every format.

All 224 effect cases use the seven remaining deterministic specs at the four
required times with unchanged sample/SWAY preparation, parameters, quad,
shader and RGB565 conversion. The default hardware route matches all 28
production hashes; both default routes match 56 earlier raw control hashes.
Each alternative route reproduces its backend's raw and RGB565 pixels and
every strict channel metric exactly. This rules out the tested 8-bit format
choices as a repair for these seven specs on these two implementations.

| Backend | Storage | Strict passes / failures | Raw / RGB565 changed frames | Draw 0.5 byte |
|---|---|---:|---:|---:|
| hardware | rgba-unsized | 10 / 18 | 0 / 0 | 127 |
| hardware | rgba8-sized | 10 / 18 | 0 / 0 | 127 |
| hardware | bgra-unsized | 10 / 18 | 0 / 0 | 127 |
| hardware | bgra8-sized | 10 / 18 | 0 / 0 | 127 |
| warp | rgba-unsized | 4 / 24 | 0 / 0 | 128 |
| warp | rgba8-sized | 4 / 24 | 0 / 0 | 128 |
| warp | bgra-unsized | 4 / 24 | 0 / 0 | 128 |
| warp | bgra8-sized | 4 / 24 | 0 / 0 | 128 |

No diagnostic image differs from the existing remaining-seven gallery, so
no duplicate PNGs are saved. Production remains the original unsized RGBA
unsigned-byte path. Full acceptance is unchanged: 18 deterministic failures
and 84 provisional noise structural failures. This selected diagnostic does
not claim that every catalog spec was tested in an alternative format or
that the phone's format/precision is known. Phone raw/neutral probes remain
pending. All 1688 production PNGs and metrics were independently rechecked
by `report-p2-sample-filter.py` after the diagnostic.

[All 224 per-time channel MAE/p99 rows](storage8-metrics.csv),
[raw controls and frame hashes](storage8-probe.json),
[full successful task output](../../storage8-probe-output.txt),
[existing mobile/desktop heatmaps](../deterministic/remaining-seven.html),
[current complete failing golden output](../../sample-filter-reference-checks.txt).
