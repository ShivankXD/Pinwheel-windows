"""Report backend and prepared-input controls without changing acceptance or production frames."""
from pathlib import Path
import csv
import hashlib
import html
import json
import re
import shutil

root = Path(__file__).resolve().parents[1]
output = root / 'evidence/p2/diagnostics/deterministic'
sampling = root / 'evidence/p2/diagnostics/sampling'
probe = json.loads((output / 'deterministic-probe.json').read_text(encoding='utf-8'))
stages = json.loads((sampling / 'sampling-probe.json').read_text(encoding='utf-8'))
reference_root = Path(json.loads((root / 'evidence/p2/golden-status.json').read_text(encoding='utf-8'))['referenceRoot'])
ids = list(dict.fromkeys(case['id'] for case in probe[0]['cases']))
assert len(ids) == 7 and len(probe) == 2 and len(stages['cases']) == 4
assert all(c['productionHashVerified'] for c in probe[0]['cases'])
assert all(c['preparedRoundTripExact'] for b in stages['cases'][:2] for c in b['cases'])

fields = ['shaderBackend', 'inputBackend', 'id', 'time', 'strictPassed'] + [f'{c}_{m}' for c in 'RGBA' for m in ('MAE8', 'p99_8')]
with (output / 'stage-metrics.csv').open('w', encoding='utf-8', newline='') as stream:
    writer = csv.DictWriter(stream, fields, lineterminator='\n'); writer.writeheader()
    for mode in stages['cases']:
        for case in mode['cases']:
            row = {k: mode[k] for k in ('shaderBackend', 'inputBackend')}
            row.update({k: case[k] for k in ('id', 'time', 'strictPassed')})
            for c, metric in zip('RGBA', case['channelsRGBA']):
                row[f'{c}_MAE8'] = metric['mae8']; row[f'{c}_p99_8'] = metric['p99_8']
            writer.writerow(row)

translations = []
for hardware in sorted((sampling / 'translated/hardware').glob('*.hlsl')):
    warp = sampling / 'translated/warp' / hardware.name
    a, b = hardware.read_bytes(), warp.read_bytes()
    # This normalization is an analysis of uniform allocation only; text is never compiled.
    normalized_a = re.sub(rb'(_uFlipY : register\(c)\d+(\))', rb'\g<1>0\2', a)
    normalized_b = re.sub(rb'(_uFlipY : register\(c)\d+(\))', rb'\g<1>0\2', b)
    translations.append({'shader': hardware.name, 'hardwareSha256': hashlib.sha256(a).hexdigest(),
                         'warpSha256': hashlib.sha256(b).hexdigest(), 'byteIdentical': a == b,
                         'identicalAfterFlipUniformRegisterNormalization': normalized_a == normalized_b})
(output / 'translation-comparison.json').write_text(json.dumps(translations, indent=2) + '\n', encoding='utf-8', newline='\n')
assert len(translations) == 14 and all(t['identicalAfterFlipUniformRegisterNormalization'] for t in translations)

def maximum(case):
    return max(m['mae8'] for m in case['channelsRGBA']), max(m['p99_8'] for m in case['channelsRGBA'])

def case_for(mode, id_, time):
    return next(c for c in mode['cases'] if c['id'] == id_ and c['time'] == time)

def label(case):
    mae, p99 = maximum(case)
    return f"{'PASS' if case['strictPassed'] else 'FAIL'}; max channel MAE8 {mae:.6f}, p99 {p99}"

table = []; sections = []; reference_hashes = []
for id_ in ids:
    cases = [c for c in probe[0]['cases'] if c['id'] == id_]
    worst = max((c for c in cases if not c['strictPassed']), key=maximum)
    time = worst['time']; tag = f'{id_}-{time}'
    reference = reference_root / 'golden' / id_ / f'{time}.png'
    # The existing probe has already checked these references; this script only copies licensed sample tiles.
    target = output / 'mobile' / f'{tag}.png'; target.parent.mkdir(exist_ok=True); shutil.copyfile(reference, target)
    reference_hashes.append({'source': str(reference), 'copiedTo': str(target.relative_to(root)),
                             'sha256': hashlib.sha256(reference.read_bytes()).hexdigest()})
    images = [('Phone golden', f'mobile/{tag}.png', None),
              ('NVIDIA shader + NVIDIA SWAY', f'hardware/{id_}/{time}.png', case_for(stages['cases'][0], id_, time)),
              ('NVIDIA strict heatmap x16', f'hardware/{id_}/{time}-diff.png', None),
              ('WARP shader + WARP SWAY', f'warp/{id_}/{time}.png', case_for(stages['cases'][1], id_, time)),
              ('WARP shader + NVIDIA SWAY: control', f'../sampling/warp-shader-hardware-input/{id_}/{time}.png', case_for(stages['cases'][3], id_, time)),
              ('NVIDIA shader + WARP SWAY: control', f'../sampling/hardware-shader-warp-input/{id_}/{time}.png', case_for(stages['cases'][2], id_, time))]
    figures = ''.join(f'<figure><img src="{html.escape(path)}"><figcaption>{html.escape(title)}' +
                      (f'<br>{html.escape(label(case))}' if case else '') + '</figcaption></figure>' for title, path, case in images)
    sections.append(f'<section><h2>{html.escape(id_)} at {time} s</h2><div class="images">{figures}</div></section>')
    row = [id_]
    for mode in stages['cases']:
        group = [c for c in mode['cases'] if c['id'] == id_]
        row.append(f"{sum(c['strictPassed'] for c in group)}/4; {max(maximum(c)[0] for c in group):.6f}")
    table.append('| ' + ' | '.join(row) + ' |')

(output / 'reference-hashes.json').write_text(json.dumps(reference_hashes, indent=2) + '\n', encoding='utf-8', newline='\n')
(output / 'remaining-seven.html').write_text('''<!doctype html><html lang="en"><meta charset="utf-8"><title>P2 remaining deterministic specs</title>
<style>body{background:#07080b;color:#eee;font:16px system-ui;max-width:1100px;margin:32px auto}section{background:#1c1f23;padding:20px;margin:24px 0}.images{display:grid;grid-template-columns:repeat(3,1fr);gap:24px}figure{margin:0}img{width:192px;height:240px}figcaption{font-size:14px;padding:8px 0}</style>
<h1>Seven remaining deterministic specs</h1><p>Each section selects that spec's worst failing NVIDIA time. All comparisons keep per-channel MAE8 &le; 2 and p99 &le; 8. Crossed inputs and WARP frames are diagnostic controls, never production replacements or owner acceptance. The full CSV contains all four times and every channel.</p>''' + ''.join(sections) + '</html>', encoding='utf-8', newline='\n')

report = '''# Remaining deterministic stage investigation

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
''' + '\n'.join(table) + '''

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
'''
(output / 'DETERMINISTIC-REPORT.md').write_text(report, encoding='utf-8', newline='\n')
print('Remaining deterministic report: 7 specs, 112 per-frame RGBA measurements, 56 exact input replays; production acceptance unchanged')
