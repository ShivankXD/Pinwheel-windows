"""Report source-classified owner noise review, without changing frames or limits."""
from pathlib import Path
from collections import defaultdict
import csv
import html
import json
import shutil

root = Path(__file__).resolve().parents[1]
output = root / 'evidence/p2'
report = json.loads((output / 'structural-status.json').read_text(encoding='utf-8'))
policy = report['noisePolicy']
noise = [c for c in report['goldenCases'] if c['class'] == 'noise-driven']
deterministic = [c for c in report['goldenCases'] if c['class'] == 'deterministic']
groups = defaultdict(list)
for case in noise: groups[case['id']].append(case)

metrics = ('blurredMae8', 'histogramWasserstein8', 'meanError8')
flags = ('blurredMaePassed', 'histogramPassed', 'meanPassed')
fields = ['id', 'time', 'strictStatus', 'reviewStatus'] + [f'{c}_{m}' for c in 'RGBA' for m in metrics] + ['meanLuminanceError8']
with (output / 'noise-frame-metrics.csv').open('w', encoding='utf-8', newline='') as stream:
    writer = csv.DictWriter(stream, fields, lineterminator='\n'); writer.writeheader()
    for case in noise:
        row = {k: case[k] for k in ('id', 'time', 'strictStatus', 'reviewStatus')}
        for c, channel in zip('RGBA', case['structuralChannelsRGBA']):
            row.update({f'{c}_{m}': channel[m] for m in metrics})
        row['meanLuminanceError8'] = case['meanLuminanceError8']; writer.writerow(row)


def rank(case):
    return max([c[m] for c in case['structuralChannelsRGBA'] for m in metrics] + [case['meanLuminanceError8']])


specs = []
for id_, cases in groups.items():
    worst = max(cases, key=rank)
    row = {'id': id_, 'class': 'noise-driven', 'reviewStatus': 'FAIL' if any(c['reviewStatus'] == 'FAIL' for c in cases) else 'PROVISIONAL_PASS',
           'strictPassedFrames': sum(c['strictStatus'] == 'PASS' for c in cases),
           'structuralPassedFrames': sum(c['structuralCandidatePassed'] for c in cases), 'worstTime': worst['time']}
    for index, channel in enumerate('RGBA'):
        row.update({f'{channel}_max_{m}': max(c['structuralChannelsRGBA'][index][m] for c in cases) for m in metrics})
    for flag in flags: row[flag] = all(m[flag] for c in cases for m in c['structuralChannelsRGBA'])
    row['meanLuminanceError8Max'] = max(c['meanLuminanceError8'] for c in cases)
    row['luminancePassed'] = all(c['luminancePassed'] for c in cases)
    specs.append((row, worst))

with (output / 'noise-spec-metrics.csv').open('w', encoding='utf-8', newline='') as stream:
    writer = csv.DictWriter(stream, list(specs[0][0]), lineterminator='\n'); writer.writeheader(); writer.writerows(row for row, _ in specs)

ranked = sorted(specs, key=lambda item: rank(item[1]), reverse=True)[:20]
folder = output / 'noise-worst-20'; folder.mkdir(exist_ok=True)
sections = []
for index, (row, case) in enumerate(ranked, 1):
    tag = f'{index:02d}-{row["id"]}-{case["time"]}'
    paths = {}
    for kind, source in [('mobile', case['reference']), ('desktop', case['actual']), ('heatmap', case['structuralHeatmap'])]:
        target = folder / f'{tag}-{kind}.png'; shutil.copyfile(source, target); paths[kind] = target
    sections.append(f'<section><h2>{index}. {html.escape(row["id"])} at {case["time"]} s</h2><p>Largest structural error: {rank(case):.4f} /255; {row["reviewStatus"]}</p><div class="images">' +
                    ''.join(f'<figure><img loading="lazy" src="noise-worst-20/{path.name}"><figcaption>{kind}</figcaption></figure>' for kind, path in paths.items()) + '</div></section>')

rows = []
for row, worst in specs:
    labels = [f'{max(row[f"{c}_max_{metric}"] for c in "RGBA"):.4f} {"PASS" if row[flag] else "FAIL"}' for metric, flag in zip(metrics, flags)]
    rows.append(f'| {row["id"]} | {row["reviewStatus"]} | ' + ' | '.join(labels) + f' | {row["meanLuminanceError8Max"]:.4f} {"PASS" if row["luminancePassed"] else "FAIL"} | [heatmap]({worst["structuralHeatmap"].replace(chr(92), "/")}) |')

det_groups = defaultdict(list)
for case in deterministic: det_groups[case['id']].append(case)
det_spec_pass = sum(all(c['strictStatus'] == 'PASS' for c in cases) for cases in det_groups.values())
noise_spec_pass = sum(row['reviewStatus'] != 'FAIL' for row, _ in specs)
text = f'''# P2 owner structural review

Policy: **{policy['limitsStatus']}**. Shared GLSL and deterministic strict limits
are unchanged. Classification comes from active shader branches and output helper
calls, independently of pass/fail images. Unused pure noise locals are excluded.
See [source classification]({(root / 'docs/P2-NOISE-CLASS.md').as_posix()}).

| Class | Specs | Frames passing | Frames failing | Specs passing all times |
|---|---:|---:|---:|---:|
| Deterministic, strict MAE/p99 | {len(det_groups)} | {report['deterministicPassed']} | {report['deterministicFailed']} | {det_spec_pass} |
| Noise-driven, provisional structure | {len(groups)} | {report['noiseCandidatePassed']} | {report['noiseCandidateFailed']} | {noise_spec_pass} |

Noise review uses separable Gaussian **sigma 8 px**, radius 24 (3 sigma), edge
clamping, float sRGB8 without rounding or rescaling. Provisional limits are
channel blurred MAE <= {policy['perChannelBlurredMae8Max']}, histogram
Wasserstein-1 distance <= {policy['perChannelHistogramWasserstein8Max']} and channel
mean error <= {policy['perChannelMeanError8Max']}; Rec.709 mean luminance error
<= {policy['meanLuminanceError8Max']}. These are proposed review limits, not owner
sign-off. Every channel and all four timestamps must pass. Histogram distance
is the sum of absolute cumulative histogram differences divided by pixel count,
in 8-bit channel units. Values divided by 255 are normalized errors.

Strict per-pixel diagnostics remain for all specs in [GOLDEN-REPORT.md](GOLDEN-REPORT.md).
Noise p99 failures do not fail the structural gate; global branch/colour errors
still do. Film Grain passes the provisional metrics at all four times. Scene Cut
still fails all four times, including mean/histogram errors around 96/255 at 0.9 s.

[Per-channel noise metrics for every spec](noise-spec-metrics.csv),
[all noise frames](noise-frame-metrics.csv), [worst 20 structural heatmaps](noise-worst-20.html).
The following lists every noise-driven spec and the metrics it passes. Each
metric shows its maximum over RGBA and all times; full channel values are in CSV.
Heatmaps use the selected worst structural time and visualize Gaussian difference x16.

| Spec | Review | Blurred MAE8 | Histogram W1 | Channel mean error | Mean luminance error | Difference |
|---|---|---:|---:|---:|---:|---|
''' + '\n'.join(rows) + '\n'
(output / 'STRUCTURAL-REPORT.md').write_text(text, encoding='utf-8', newline='\n')
(output / 'noise-worst-20.html').write_text('''<!doctype html><html lang="en"><meta charset="utf-8"><title>P2 noise structural worst 20</title><style>body{background:#07080b;color:#eee;font:16px system-ui;max-width:960px;margin:32px auto}section{padding:20px;margin:20px 0;background:#1c1f23}.images{display:flex;gap:24px}figure{margin:0}img{width:192px;height:240px}figcaption{padding:8px 0;color:#81cad4}</style><h1>Noise-driven specs: worst 20 structural comparisons</h1><p>Owner review with provisional limits. Mobile / hardware desktop / Gaussian difference x16. The original strict pixel audit remains separately available.</p>''' + ''.join(sections) + '</html>', encoding='utf-8', newline='\n')
summary = {'noiseSpecs': len(groups), 'deterministicSpecs': len(det_groups), 'noiseSpecCandidatePass': noise_spec_pass,
           'noiseSpecFail': len(groups) - noise_spec_pass, 'deterministicSpecPass': det_spec_pass,
           'deterministicSpecFail': len(det_groups) - det_spec_pass, 'noiseFrameCandidatePass': report['noiseCandidatePassed'],
           'noiseFrameFail': report['noiseCandidateFailed'], 'deterministicFramePass': report['deterministicPassed'],
           'deterministicFrameFail': report['deterministicFailed'], 'policy': policy}
(output / 'structural-summary.json').write_text(json.dumps(summary, indent=2) + '\n', encoding='utf-8', newline='\n')
print(json.dumps({k: v for k, v in summary.items() if k != 'policy'}))
