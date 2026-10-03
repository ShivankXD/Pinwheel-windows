"""Current-baseline conversion diagnostics. Does not alter shaders, frames or limits."""
from pathlib import Path
import csv, hashlib, html, json, math, shutil
import numpy as np
from PIL import Image

root = Path(__file__).resolve().parents[1]
output = root / 'evidence/p2/diagnostics/framebuffer-review'
output.mkdir(parents=True, exist_ok=True)
def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()
files = ['evidence/p2/golden-metrics.csv', 'evidence/p2/frame-status.json',
         'evidence/p2/structural-status.json', 'docs/p2-noise-policy.json']
baseline = list(csv.DictReader((root/files[0]).read_text(encoding='utf-8-sig').splitlines()))
by_key = {f"{c['id']}/{c['time']}": c for c in baseline}
assert len(by_key) == 1688
reference_index = {c['path']: c['sha256'] for c in json.loads((root/'evidence/p2/reference-input-hashes.json').read_text())['files']}
routes = []
for folder in ['framebuffer-current', 'framebuffer-rounding', 'framebuffer-staged-rounding']:
    for report in json.loads((output.parent/folder/'framebuffer-probe.json').read_text()):
        if report['status'] == 'UNSUPPORTED': continue
        report['folder'] = folder
        for field, name in zip(['baselineMetricsSha256','baselineFrameStatusSha256','baselineStructuralSha256','noisePolicySha256'], files):
            assert report[field] == digest(root/name), (report['storage'], name)
        assert len(report['cases']) == 1688
        assert all(c['baselineStrictPassed'] == (by_key[f"{c['id']}/{c['time']}"]['status'] == 'PASS') for c in report['cases'])
        assert sum(c['diagnosticStrictPassed'] for c in report['cases']) == report['strictPassed']
        assert sum(c['noiseCandidatePassed'] for c in report['cases'] if not c['deterministic']) == report['noiseCandidatePassed']
        routes.append(report)
assert len(routes) == 4

def write_csv(name, rows):
    with (output/name).open('w', encoding='utf-8', newline='') as stream:
        writer=csv.DictWriter(stream, fieldnames=list(rows[0])); writer.writeheader(); writer.writerows(rows)

strict_rows=[]; noise_rows=[]; raw_rows=[]; changes=[]; spec_rows=[]
def reference(case):
    relative=f"golden/{case['id']}/{case['time']}.png"
    source=Path(r'D:\Pinwheel-Windows-refs')/relative
    assert digest(source)==reference_index[relative]
    dest=output/'references'/case['id']/f"{case['time']}.png"
    dest.parent.mkdir(parents=True,exist_ok=True); shutil.copyfile(source,dest)
    assert digest(dest)==reference_index[relative]
    return dest.relative_to(output).as_posix()
def picture(case, report, suffix=''):
    path=output.parent/report['folder']/report['storage']/case['id']/f"{case['time']}{suffix}.png"
    assert path.is_file(), path
    if not suffix:
        with Image.open(path) as frame:
            assert frame.size==(192,240)
            assert hashlib.sha256(frame.convert('RGBA').tobytes()).hexdigest()==case['rgbaSha256']
    return '../'+path.relative_to(output.parent).as_posix()
def gallery(name, cases, report, structural=False):
    parts=['<!doctype html><meta charset="utf-8"><title>P2 conversion diagnostic</title>',
      '<style>body{background:#10141d;color:#ddd;font:14px system-ui;margin:24px}section{margin:24px 0}img{width:192px;height:240px;margin-right:12px;image-rendering:pixelated}p{max-width:920px}a{color:#67d7eb}</style>',
      f"<h1>{html.escape(report['storage'])}: {html.escape(name)}</h1><p>Diagnostic only. Shared shaders, owner limits and production frames remain unchanged. Images: mobile / candidate / heatmap.</p>"]
    for c in cases:
        metrics=[[x['blurredMae8'],x['histogramWasserstein8'],x['meanError8']] for x in c['structuralChannelsRGBA']] if structural else [[x['mae8'],x['p99_8']] for x in c['channelsRGBA']]
        text=f"{c['id']} at {c['time']} s; "+('blur/histogram/mean ' if structural else 'RGBA MAE8/p99 ')+str(metrics)
        parts.append('<section><p>'+html.escape(text)+'</p>'+''.join(f'<img alt="{label}" src="{src}">' for label,src in
          [('mobile',reference(c)),('candidate',picture(c,report)),('heatmap',picture(c,report,'-structural-diff' if structural else '-diff'))])+'</section>')
    (output/name).write_text('\n'.join(parts),encoding='utf-8',newline='\n')

for r in routes:
    cases={f"{c['id']}/{c['time']}":c for c in r['cases']}
    for c in r['cases']:
        prefix={'storage':r['storage'],'id':c['id'],'time':c['time']}
        row={**prefix,'baselinePassed':c['baselineStrictPassed'],'candidatePassed':c['diagnosticStrictPassed']}
        for channel,x in zip('RGBA',c['channelsRGBA']): row[channel+'_mae8']=x['mae8']; row[channel+'_p99_8']=x['p99_8']
        strict_rows.append(row)
        if not c['deterministic']:
            row={**prefix,'baselineProvisionalPass':c['baselineNoiseCandidatePassed'],'candidateProvisionalPass':c['noiseCandidatePassed'],'luminanceError8':c['meanLuminanceError8']}
            for channel,x in zip('RGBA',c['structuralChannelsRGBA']):
                for metric,value in x.items(): row[channel+'_'+metric]=value
            noise_rows.append(row)
        strict_change=c['baselineStrictPassed']!=c['diagnosticStrictPassed']
        noise_change=not c['deterministic'] and c['baselineNoiseCandidatePassed']!=c['noiseCandidatePassed']
        if strict_change or noise_change:
            changes.append({**prefix,'strictChanged':strict_change,'strictAfter':c['diagnosticStrictPassed'],'noiseChanged':noise_change,'noiseAfter':c.get('noiseCandidatePassed','')})
        folder=output.parent/r['folder']/r['storage']/c['id']; path=folder/f"{c['time']}-stored-floats.f32le"
        if path.is_file():
            values=np.fromfile(path,dtype='<f4').reshape(240,192,4)
            assert np.isfinite(values).all()
            default=np.asarray(Image.open(folder/f"{c['time']}-default-raw.png").convert('RGBA'))
            converted=np.asarray(Image.open(folder/f"{c['time']}-converted-raw.png").convert('RGBA'))
            scaled=np.clip(values,0,1)*np.float32(255)
            fraction=scaled-np.floor(scaled)
            for channel in range(4):
                delta=converted[:,:,channel].astype(int)-default[:,:,channel].astype(int)
                changed=delta!=0; source=values[:,:,channel]
                levels,counts=np.unique(source[changed],return_counts=True); order=np.argsort(counts)[::-1][:10]
                raw_rows.append({**prefix,'channel':'RGBA'[channel],'changedRawPixels':int(changed.sum()),'maxRawDelta':int(np.abs(delta).max()),
                    'storedExactlyHalfAtChanges':int((changed&(source==.5)).sum()),
                    'scaledF32HalfAtChanges':int((changed&(fraction[:,:,channel]==.5)).sum()),
                    'frequentStoredFloats':json.dumps([[float(levels[i]),int(counts[i])] for i in order])})
    for id in sorted({c['id'] for c in r['cases']}):
        group=[c for c in r['cases'] if c['id']==id]
        row={'storage':r['storage'],'id':id,'failedTimes':','.join(str(c['time']) for c in group if not c['diagnosticStrictPassed'])}
        for channel in range(4):
            row['RGBA'[channel]+'_maxMAE8']=max(c['channelsRGBA'][channel]['mae8'] for c in group)
            row['RGBA'[channel]+'_maxP99_8']=max(c['channelsRGBA'][channel]['p99_8'] for c in group)
        spec_rows.append(row)
    for kind in ['Strict','Noise']:
        selected=[cases[k] for k in r['worst20'+kind]]; assert len(selected)==20
        gallery(r['storage']+'-worst20-'+kind.lower()+'.html',selected,r,kind=='Noise')
    changed=[c for c in r['cases'] if c['baselineStrictPassed']!=c['diagnosticStrictPassed'] or
             (not c['deterministic'] and c['baselineNoiseCandidatePassed']!=c['noiseCandidatePassed'])]
    gallery(r['storage']+'-changes.html',changed,r)

write_csv('channel-metrics.csv',strict_rows); write_csv('per-spec-metrics.csv',spec_rows)
write_csv('noise-metrics.csv',noise_rows); write_csv('raw-storage-signatures.csv',raw_rows); write_csv('changed-cases.csv',changes)
single=next(r for r in routes if r['storage']=='float32-round-even')
staged=next(r for r in routes if r['storage']=='float32-round-even-staged')
assert len(single['roundingControls'])==len(staged['roundingControls'])==511
assert sum(not c['passed'] for c in single['roundingControls'])==single['roundingControlFailures']==126
assert staged['roundingControlFailures']==0 and all(c['passed'] for c in staged['roundingControls'])
summary=['| Production RGBA8 | 1473 / 215 | 18 | 84 | baseline | baseline |']
for r in routes:
    summary.append(f"| {r['storage']} | {r['strictPassed']} / {r['strictFailed']} | {r['deterministicFailed']} | {r['noiseCandidateFailed']} | {r['newStrictPasses']} / {r['strictRegressions']} | {r['newNoiseCandidatePasses']} / {r['noiseCandidateRegressions']} |")
links='\n'.join(f"- {r['storage']}: [strict worst 20]({r['storage']}-worst20-strict.html), [structural worst 20]({r['storage']}-worst20-noise.html), [changed cases]({r['storage']}-changes.html)." for r in routes)
(output/'FRAMEBUFFER-REVIEW.md').write_text('''# Current-baseline framebuffer and rounding investigation

All experiments use the current sample matrix/filter preparation, unchanged
shared GLSL/defaults/quad, NVIDIA D3D11, and the original strict and provisional
noise limits. Four supported routes cover 6752 effect frames and 3088 noise
structural comparisons. Every route pins the production frame/metric/policy
hashes. The older pre-matrix experiment remains under ../framebuffer.

| Route | Strict pass / fail | Deterministic failures | Noise failures | New strict passes / regressions | New noise passes / regressions |
|---|---:|---:|---:|---:|---:|
'''+'\n'.join(summary)+'''

The explicit half-up route fixes Diamond at three times and Dance Flash at
0.3 s, but regresses Color Pixel twice, Mirror Beat and Blink. Raw float32
captures show many changed channels around scaled byte-half boundaries;
Diamond's flat-field changes all occur at stored 0.5. Float16 has no strict
or noise pass-to-fail regressions, but changes output precision and remains
an unadopted control with phone medium/low precision unknown.

Single-pass ties-to-even initially fails 126 of 511 synthetic controls.
All 256 normalized-byte round trips pass; failures are in the 255 boundary
inputs. The CPU reference rounds the float32 scaling result before selecting
the even integer; the single shader does not enforce that intermediate
storage boundary. Optimizer/evaluation fusion is a lead, not a proven driver
conformance failure. The full failed task output is retained.

The repaired staged control stores the scaled value in a separate RGBA32F
attachment before rounding. All 256 byte round trips and 255 boundary tests
pass. It still introduces strict and noise regressions, so it is not a
production repair. No epsilon or threshold change is used. Raw comparisons
are against desktop default storage, not invented phone raw values.

Current production remains RGBA8 with 18 deterministic and 84 provisional
noise failures. None of these routes is adopted. Real phone raw/neutral and
precision probes remain required to distinguish source/input and conversion
differences. Shared shaders and authoritative frames are unchanged.

The [float-buffer extension](https://registry.khronos.org/OpenGL/extensions/EXT/EXT_color_buffer_float.txt)
and [half-float extension](https://registry.khronos.org/OpenGL/extensions/EXT/EXT_color_buffer_half_float.txt)
describe these attachment/readback capabilities; the probe checks allocation
and completeness instead of assuming support. Unsized RGBA/FLOAT remains
unsupported in this context. Staging enforces an experimental float32 scale
boundary; it does not establish the phone's evaluation or rounding rule.

[6752 per-time RGBA MAE/p99 rows](channel-metrics.csv),
[1688 per-spec route rows](per-spec-metrics.csv),
[3088 structural channel rows](noise-metrics.csv),
[raw float/storage signatures](raw-storage-signatures.csv),
[all changed cases](changed-cases.csv).

'''+links+'''

Full logs: [current baseline](../../framebuffer-current-structural-output.txt),
[single-pass failure](../../framebuffer-rounding-output.txt),
[staged repair](../../framebuffer-staged-rounding-output.txt).
''',encoding='utf-8',newline='\n')
print(f'PASS {len(strict_rows)} strict rows, {len(noise_rows)} structural rows, 8 paired worst-20 galleries; staged 511 controls pass, initial 126 failures retained')
