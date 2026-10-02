"""Report strict comparator output. Never changes references, metrics or acceptance thresholds."""
from pathlib import Path
from collections import defaultdict
import csv
import hashlib
import html
import json
import shutil
import sys

root = Path(__file__).resolve().parents[1]
source = Path(sys.argv[1]) if len(sys.argv) > 1 else root / 'evidence/p1/runtime/reference-status.json'
report = json.loads(source.read_text(encoding='utf-8-sig'))
output = root / 'evidence/p2'
output.mkdir(parents=True, exist_ok=True)
(output / 'golden-status.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8', newline='\n')
fields = ['id', 'time', 'status'] + [f'{c}_{m}' for c in 'RGBA' for m in ('MAE8', 'p99_8')]
groups = defaultdict(list)
with (output / 'golden-metrics.csv').open('w', encoding='utf-8', newline='') as stream:
    writer = csv.DictWriter(stream, fields); writer.writeheader()
    for case in report['goldenCases']:
        row = {key: case[key] for key in ('id', 'time', 'status')}
        for c, metric in zip('RGBA', case.get('channelsRGBA', [])):
            row[f'{c}_MAE8'] = metric['mae8']; row[f'{c}_p99_8'] = metric['p99_8']
        writer.writerow(row); groups[case['id']].append(case)

def key(case):
    metrics = case.get('channelsRGBA', [])
    return max((c['mae8'] for c in metrics), default=-1), max((c['p99_8'] for c in metrics), default=-1)

specs = []
for id_, cases in groups.items():
    worst = max(cases, key=key)
    row = {'id': id_, 'status': 'PASS' if all(c['status'] == 'PASS' for c in cases) else 'FAIL',
           'passedFrames': sum(c['status'] == 'PASS' for c in cases), 'worstTime': worst['time']}
    for c, index in zip('RGBA', range(4)):
        row[f'{c}_maxMAE8'] = max((case['channelsRGBA'][index]['mae8'] for case in cases if 'channelsRGBA' in case), default='')
        row[f'{c}_maxP99_8'] = max((case['channelsRGBA'][index]['p99_8'] for case in cases if 'channelsRGBA' in case), default='')
    specs.append((row, worst))
with (output / 'per-spec-metrics.csv').open('w', encoding='utf-8', newline='') as stream:
    writer = csv.DictWriter(stream, list(specs[0][0])); writer.writeheader(); writer.writerows(row for row, _ in specs)

ranked = sorted(specs, key=lambda item: key(item[1]), reverse=True)[:20]
folder = output / 'worst-20'; folder.mkdir(exist_ok=True)
rows = []; sections = []; inputs = []
for rank, (row, case) in enumerate(ranked, 1):
    id_ = row['id']; t = case['time']; tag = f'{rank:02d}-{id_}-{t}'
    paths = {}
    for kind, original in [('mobile', Path(case['reference'])), ('desktop', Path(case['actual'])), ('heatmap', Path(case['diff']))]:
        target = folder / f'{tag}-{kind}.png'; shutil.copyfile(original, target)
        paths[kind] = target
        if kind == 'mobile': inputs.append({'reference': str(original), 'copiedTo': str(target.relative_to(root)), 'sha256': hashlib.sha256(original.read_bytes()).hexdigest()})
    mae, p99 = key(case)
    metrics = ' / '.join(f"{c} {m['mae8']:.4f}/{m['p99_8']}" for c, m in zip('RGBA', case['channelsRGBA']))
    rows.append(f'| {rank} | {id_} | {t} | {mae:.4f} | {p99} | [heatmap]({paths["heatmap"].as_posix()}) |')
    sections.append(f'<section><h2>{rank}. {html.escape(id_)} at {t} s</h2><p>RGBA MAE8/p99: {metrics}</p><div class="images">' +
        ''.join(f'<figure><img src="worst-20/{p.name}" width="192" height="240"><figcaption>{kind}</figcaption></figure>' for kind, p in paths.items()) + '</div></section>')

text = f'''# P2 strict golden report

{report['goldenPassed']} passing frames, {report['goldenFailed']} failing frames, {report['goldenMissing']} missing frames.
{sum(row['status'] == 'PASS' for row, _ in specs)} passing specs out of {len(specs)} (all four times must pass).

MAE8 and p99 are in 8-bit channel units; divide by 255 for normalized error.
Thresholds remain MAE8 <= 2 and p99 <= 8 for every RGBA channel. No resizing,
alignment, flipping, filtering or tolerance changes are used in comparison.

[All {len(specs)} per-spec metrics]({(output / 'per-spec-metrics.csv').as_posix()})
and [all 1688 per-time metrics]({(output / 'golden-metrics.csv').as_posix()}).

Worst 20 distinct specs ranked by largest per-channel MAE over four times;
p99 breaks ties. Each row selects that spec's worst time. Alpha is included.
The CSV also records each channel's largest MAE and largest p99 independently,
which can occur at different times. Review the [image gallery]({(output / 'worst-20.html').as_posix()}).

| Rank | Spec | Time (s) | Max channel MAE8 | Max channel p99 | Difference x16 |
|---|---|---:|---:|---:|---|
''' + '\n'.join(rows) + '\n'
(output / 'GOLDEN-REPORT.md').write_text(text, encoding='utf-8', newline='\n')
(output / 'worst-20.html').write_text('''<!doctype html><html lang="en"><meta charset="utf-8"><title>P2 worst 20 goldens</title>
<style>body{background:#07080b;color:#eee;font:16px system-ui;max-width:960px;margin:32px auto}section{padding:20px;margin:20px 0;background:#1c1f23}.images{display:flex;gap:24px}figure{margin:0}img{image-rendering:auto}figcaption{padding:8px 0;color:#81cad4}</style>
<h1>P2 worst 20 specs</h1><p>Mobile / desktop / difference x16. Fixed per-channel limits: MAE8 &le; 2, p99 &le; 8. Ranked by largest channel MAE at each spec's worst time. Failures remain bugs.</p>''' + ''.join(sections) + '</html>', encoding='utf-8', newline='\n')
(output / 'worst-20-reference-hashes.json').write_text(json.dumps(inputs, indent=2) + '\n', encoding='utf-8', newline='\n')
# A compact contact sheet makes the twenty heatmaps reviewable without opening sixty files.
from PIL import Image, ImageDraw
sheet = Image.new('RGB', (660, 20 * 290), '#07080b'); draw = ImageDraw.Draw(sheet)
for rank, (row, case) in enumerate(ranked, 1):
    top = (rank - 1) * 290; tag = f'{rank:02d}-{row["id"]}-{case["time"]}'
    draw.text((12, top + 5), f'{rank}. {row["id"]} at {case["time"]} s', fill='white')
    draw.text((12, top + 24), f'Max channel MAE8={key(case)[0]:.4f}, p99={key(case)[1]}', fill='#81cad4')
    for index, kind in enumerate(('mobile', 'desktop', 'heatmap')):
        with Image.open(folder / f'{tag}-{kind}.png') as image: sheet.paste(image.convert('RGB'), (12 + index * 216, top + 45))
sheet.save(output / 'worst-20-contact-sheet.png')
print(f"P2 golden report: {report['goldenPassed']} passed, {report['goldenFailed']} failed, {report['goldenMissing']} missing; {len(specs)} specs; worst 20 with heatmaps")
