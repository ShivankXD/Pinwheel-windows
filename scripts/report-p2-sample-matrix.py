"""Compare the source-backed sample matrix correction with its published baseline."""
from pathlib import Path
import csv
import hashlib
import json
import re
import struct
import subprocess
import sys
from PIL import Image

root = Path(__file__).resolve().parents[1]
baseline = sys.argv[1] if len(sys.argv) > 1 else 'ddad406'
output = root / 'evidence/p2/diagnostics/sample-matrix'
output.mkdir(parents=True, exist_ok=True)

def published(name):
    return subprocess.check_output(['git', 'show', f'{baseline}:{name}'], cwd=root).decode('utf-8-sig')

def read(name):
    return json.loads((root / name).read_text(encoding='utf-8-sig'))

def index(rows):
    return {(row['id'], float(row['time'])): row for row in rows}

before = index(csv.DictReader(published('evidence/p2/golden-metrics.csv').splitlines()))
after = index(csv.DictReader((root / 'evidence/p2/golden-metrics.csv').read_text(encoding='utf-8-sig').splitlines()))
assert before.keys() == after.keys() and len(after) == 1688
before_frames = index(json.loads(published('evidence/p2/frame-status.json'))['cases'])
after_frames = index(read('evidence/p2/frame-status.json')['cases'])
classes = {spec['id']: spec['class'] for spec in read('docs/p2-noise-classification.json')}
old_structure = json.loads(published('evidence/p2/structural-status.json'))
new_structure = read('evidence/p2/structural-status.json')
old_noise = index(c for c in old_structure['goldenCases'] if c['class'] == 'noise-driven')
new_noise = index(c for c in new_structure['goldenCases'] if c['class'] == 'noise-driven')
assert old_noise.keys() == new_noise.keys() and len(new_noise) == 772
noise_changes = [{'id': key[0], 'time': key[1],
                  'before': old_noise[key]['structuralCandidatePassed'],
                  'after': case['structuralCandidatePassed']}
                 for key, case in new_noise.items()
                 if old_noise[key]['structuralCandidatePassed'] != case['structuralCandidatePassed']]
changes = []
for key, row in after.items():
    old = before[key]
    changes.append({'id': key[0], 'time': key[1], 'class': classes[key[0]],
                    'before': old['status'], 'after': row['status'],
                    'pixelsChanged': before_frames[key]['rgbaSha256'] != after_frames[key]['rgbaSha256'],
                    'beforeChannelsRGBA': [{'mae8': float(old[f'{c}_MAE8']), 'p99_8': int(old[f'{c}_p99_8'])} for c in 'RGBA'],
                    'afterChannelsRGBA': [{'mae8': float(row[f'{c}_MAE8']), 'p99_8': int(row[f'{c}_p99_8'])} for c in 'RGBA']})

def f32(value):
    return struct.unpack('f', struct.pack('f', value))[0]

recipe = (root / 'pinwheel-core/src/main/kotlin/com/pinwheel/core/media/video/VideoPreviewRecipe.kt').read_text(encoding='utf-8')
names = re.findall(r'"([^"]+\.webp)"', recipe.split('val samples = listOf(', 1)[1].split('data class Sway', 1)[0])
assert len(names) == 21
samples = []
for name in names:
    asset = root / 'assets' / name
    with Image.open(asset) as image:
        w, h = image.size
    aspect = f32(192 / 240)
    crop_w = min(w, int(f32(h * aspect)))
    crop_h = min(h, int(f32(crop_w / aspect)))
    sx, sy = f32(384 / crop_w), f32(-480 / crop_h)
    samples.append({'asset': name, 'sha256': hashlib.sha256(asset.read_bytes()).hexdigest(),
                    'decodedSize': [w, h], 'crop': [(w-crop_w)//2, (h-crop_h)//3, crop_w, crop_h],
                    'scale': [sx, sy], 'mappedSize': [f32(crop_w*sx), f32(-crop_h*sy)],
                    'allocatedSize': [384, 480]})

new_passes = [c for c in changes if c['before'] == 'FAIL' and c['after'] == 'PASS']
regressions = [c for c in changes if c['before'] == 'PASS' and c['after'] != 'PASS']
counts = ['goldenPassed', 'goldenFailed', 'goldenMissing', 'deterministicPassed', 'deterministicFailed', 'noiseCandidatePassed', 'noiseCandidateFailed']
report = {'baselineCommit': subprocess.check_output(['git', 'rev-parse', baseline], cwd=root).decode().strip(),
          'source': 'https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-14.0.0_r1/graphics/java/android/graphics/Bitmap.java',
          'sourceLimit': 'Phone Android/Skia version and raw neutral probes remain pending; AOSP source establishes the matrix path, not exact phone raster bytes.',
          'before': {k: old_structure[k] for k in counts}, 'after': {k: new_structure[k] for k in counts},
          'changedFrames': sum(c['pixelsChanged'] for c in changes), 'newStrictPasses': len(new_passes),
          'strictRegressions': len(regressions),
          'newNoiseStructuralPasses': sum(c['after'] for c in noise_changes),
          'noiseStructuralRegressions': sum(c['before'] for c in noise_changes),
          'noiseStructuralStatusChanges': noise_changes, 'samples': samples, 'cases': changes}
(output / 'sample-matrix-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8', newline='\n')

def max_mae(case, field):
    return max(m['mae8'] for m in case[field])

rows = []
for case in new_passes + regressions:
    rows.append(f"| {case['id']} | {case['time']} | {case['class']} | {case['before']} -> {case['after']} | {max_mae(case, 'beforeChannelsRGBA'):.6f} | {max_mae(case, 'afterChannelsRGBA'):.6f} |")
text = f'''# Sample matrix correction

Baseline: `{report['baselineCommit']}`. All 1688 frames rerendered on the same
NVIDIA/ANGLE backend, with shared shaders, defaults, strict thresholds and
noise classification unchanged. {report['changedFrames']} rendered RGBA frame hashes changed.
{len(new_passes)} strict failures became passes; {len(regressions)} strict passes became failures.
Noise structure has {report['newNoiseStructuralPasses']} new provisional passes and
{report['noiseStructuralRegressions']} pass-to-fail regressions.

| Gate | Before failures | After failures |
|---|---:|---:|
| Original strict audit, every spec | {old_structure['goldenFailed']} | {new_structure['goldenFailed']} |
| Deterministic strict MAE/p99 | {old_structure['deterministicFailed']} | {new_structure['deterministicFailed']} |
| Noise provisional structure | {old_structure['noiseCandidateFailed']} | {new_structure['noiseCandidateFailed']} |

The port now applies the crop-local float scale and translates using its mapped
bounds, before rounding the bitmap allocation. AOSP's
[Bitmap.createBitmap implementation]({report['source']}) uses that order.
The previous fixed destination rectangle lost these float bounds. Portrait and
most transition samples map to 384.0000305 by 480.0000305 before allocating
384 by 480 pixels. Sample geometry and full before/after channel metrics are in
[sample-matrix-comparison.json](sample-matrix-comparison.json).

This does not establish exact phone decode/raster bytes: the phone Android/Skia
version and matching neutral sample probes are still pending. Existing float
framebuffer experiments predate this correction and remain historical diagnostics.
Production stays RGBA8. P2 acceptance remains open.

| Spec | Time | Class | Strict change | Before max MAE8 | After max MAE8 |
|---|---:|---|---|---:|---:|
''' + '\n'.join(rows) + '\n'
(output / 'SAMPLE-MATRIX-REPORT.md').write_text(text, encoding='utf-8', newline='\n')
print(json.dumps({k: report[k] for k in ('changedFrames', 'newStrictPasses', 'strictRegressions', 'newNoiseStructuralPasses', 'noiseStructuralRegressions', 'before', 'after')}))
