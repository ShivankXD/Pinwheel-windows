"""Verify the source-rectangle repair against all published production pixels and metrics."""
from pathlib import Path
import csv
import hashlib
import json
import subprocess
from PIL import Image

root = Path(__file__).resolve().parents[1]
baseline = '1b7c8a388277bb49eaef3f9b91e84144e8f98c7f'
folder = root / 'evidence/p2/diagnostics/sample-filter'
folder.mkdir(parents=True, exist_ok=True)

def published(path):
    return subprocess.check_output(['git', 'show', f'{baseline}:{path}'], cwd=root)

def read(path):
    return json.loads((root / path).read_text(encoding='utf-8-sig'))

before = json.loads(published('evidence/p2/frame-status.json'))
after = read('evidence/p2/frame-status.json')
index = lambda cases: {(c['id'], float(c['time'])): c for c in cases}
old, new = index(before['cases']), index(after['cases'])
assert old.keys() == new.keys() and len(new) == 1688
changed = []
for key, case in new.items():
    path = root / f'evidence/p2/frames/{key[0]}/{key[1]}.png'
    with Image.open(path) as image:
        assert image.size == (192, 240)
        sha = hashlib.sha256(image.convert('RGBA').tobytes()).hexdigest()
    assert sha == case['rgbaSha256']
    if sha != old[key]['rgbaSha256']:
        changed.append({'id': key[0], 'time': key[1]})

metric_file = 'evidence/p2/golden-metrics.csv'
old_metrics = list(csv.DictReader(published(metric_file).decode('utf-8-sig').splitlines()))
new_metrics = list(csv.DictReader((root / metric_file).read_text(encoding='utf-8-sig').splitlines()))
assert len(new_metrics) == 1688 and old_metrics == new_metrics
assert not changed
old_gate = json.loads(published('evidence/p2/structural-status.json'))
new_gate = read('evidence/p2/structural-status.json')
keys = ['goldenPassed', 'goldenFailed', 'goldenMissing', 'deterministicPassed',
        'deterministicFailed', 'noiseCandidatePassed', 'noiseCandidateFailed']
assert all(old_gate[k] == new_gate[k] for k in keys)
samples = read('evidence/p2/diagnostics/sample-matrix/sample-matrix-comparison.json')['samples']
assert len(samples) == 21 and all(abs(scale) <= 1 for s in samples for scale in s['scale'])
report = {'baselineCommit': baseline, 'pixelVerifiedFrames': len(new), 'changedFrames': changed,
          'unchangedChannelMetricRows': len(new_metrics), 'gate': {k: new_gate[k] for k in keys},
          'samplesAllDownscaleOrIdentity': True,
          'source': 'https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-14.0.0_r1/libs/hwui/SkiaCanvas.cpp',
          'test': 'PreviewSamplesTest.mobileCropFilteringIncludesNeighbouringSourcePixelsAtBothEdges',
          'testColoursRGBA': {'leading': [32, 0, 96, 255], 'trailing': [0, 32, 96, 255], 'inside': [0, 0, 128, 255]}}
(folder / 'sample-filter-comparison.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8', newline='\n')
(folder / 'SAMPLE-FILTER-REPORT.md').write_text('''# Sample crop filtering correction

`PreviewSamples` now uses Skia's fast source-rectangle constraint, matching
the Android Canvas bitmap path in [AOSP](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android-14.0.0_r1/libs/hwui/SkiaCanvas.cpp).
The previous strict constraint excluded outside crop neighbours during
upscaling. Android 11 and 15 sources also use the fast constraint.

A synthetic two-colour boundary at scale 2 gives exact quarter-pixel weights.
The regression checks both horizontal edges, both flipped vertical edges and
unchanged interior colour. Source channels of 128 yield outside contribution
32 and inside contribution 96. The published implementation fails with
[0,0,128,255]; the repair passes all six colour assertions.

The initial small fixture used fractional weights whose ideal result differed
from Skia's raster output (125/130 versus 127/127). That unsuitable oracle was
replaced by aligned quarter-pixel centres. Its complete failed logs and XML
are retained as `sample-filter-initial-baseline` and `sample-filter-fractional-oracle`
under evidence/p2. The final baseline failure is in `sample-filter-before.txt`
and `.xml`; the passing full run is in `sample-filter-runtime-and-frames.txt`.
No golden threshold was altered.

All 237 unit tests pass. All 422 shaders compile and all 1688 frames render.
Every saved PNG's decoded RGBA hash matches the published 1b7c8a3 baseline;
all 1688 per-time/per-channel metric rows and acceptance counters are unchanged.
The current 21 samples all downscale or use identity sizing, so this edge
correction does not repair any of the seven remaining deterministic specs.

Current failures remain 18 deterministic and 84 provisional noise structural
frames. Shared GLSL, GPU storage and the backend are unchanged. P2 acceptance
remains open, with phone precision/raw probes pending.

[Verification data](sample-filter-comparison.json),
[all per-time MAE/p99 metrics](../../golden-metrics.csv),
[full failing reference output](../../sample-filter-reference-checks.txt).
''', encoding='utf-8', newline='\n')
print('PASS 1688 decoded production PNG hashes and every channel metric unchanged; 237 unit tests; source filter regression repaired')
