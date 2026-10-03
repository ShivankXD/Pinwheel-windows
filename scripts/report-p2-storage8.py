"""Report 8-bit attachment controls; never alters frames or comparison limits."""
from pathlib import Path
import csv
import json

root = Path(__file__).resolve().parents[1]
folder = root / 'evidence/p2/diagnostics/storage8'
reports = json.loads((folder / 'storage8-probe.json').read_text(encoding='utf-8'))
assert len(reports) == 8
assert {r['storage'] for r in reports} == {'rgba-unsized', 'rgba8-sized', 'bgra-unsized', 'bgra8-sized'}
rows = []
summary = []
for r in reports:
    assert r['status'] == 'DIAGNOSTIC_ONLY' and r['channelBitsRGBA'] == [8, 8, 8, 8]
    assert r['allocationError'] == 0 and r['framebufferStatus'] == 0x8CD5
    assert r['rawChangedFrames'] == r['changedFrames'] == 0 and len(r['cases']) == 28
    expected = [64, 127, 166, 235] if r['backend'] == 'hardware' else [64, 128, 166, 235]
    assert [c['rgba8Levels'] for c in r['controls']['constants']] == [[x] for x in expected]
    assert r['controls']['distinctUploadRGBA'] == [17, 83, 149, 211]
    assert r['controls']['distinctUploadVerifiedPixels'] == 192 * 240
    for control in r['controls']['halfValueStorage']:
        assert sum(control[k] for k in ['storedBelowHalfPixels', 'storedAboveHalfPixels', 'storedExactlyHalfPixels']) == 192 * 240
        level = 127 if control['origin'] == 'upload-127' else 128 if control['origin'] == 'upload-128' else expected[1]
        assert control['rgba8Levels'] == [level] and control['storedExactlyHalfPixels'] == 0
        assert control['storedBelowHalfPixels'] == (192 * 240 if level == 127 else 0)
        assert control['storedAboveHalfPixels'] == (192 * 240 if level == 128 else 0)
    for c in r['cases']:
        assert c['rawMatchesDefault'] and c['matchesDefault']
        row = {'backend': r['backend'], 'storage': r['storage'], 'id': c['id'], 'time': c['time'],
               'strictPassed': c['strictPassed'], 'rawRgbaSha256': c['rawRgbaSha256'], 'rgbaSha256': c['rgbaSha256']}
        for channel, metrics in zip('RGBA', c['channelsRGBA']):
            row[channel + '_mae8'] = metrics['mae8']
            row[channel + '_p99_8'] = metrics['p99_8']
        rows.append(row)
    baseline = next(x for x in reports if x['backend'] == r['backend'] and x['storage'] == 'rgba-unsized')
    assert r['cases'] == baseline['cases']
    summary.append(f"| {r['backend']} | {r['storage']} | {r['strictPassed']} / {r['strictFailed']} | 0 / 0 | {expected[1]} |")
assert len(rows) == 224
with (folder / 'storage8-metrics.csv').open('w', encoding='utf-8', newline='') as stream:
    writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
    writer.writeheader()
    writer.writerows(rows)
(folder / 'STORAGE8-REPORT.md').write_text('''# Alternative 8-bit attachment controls

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
''' + '\n'.join(summary) + '''

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
''', encoding='utf-8', newline='\n')
print('PASS 224 raw/RGB565 attachment comparisons and channel metric rows identical; all 8-bit precision/channel/storage controls pass')
