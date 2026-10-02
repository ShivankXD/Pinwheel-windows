"""Classify active catalog shader branches from source, independently of golden results."""
from pathlib import Path
import ast
import hashlib
import json
import re
import sys

root = Path(__file__).resolve().parents[1]
inventory = json.loads((root / 'evidence/p2/runtime/noise-sources.json').read_text(encoding='utf-8'))


def condition(expression, defines):
    expression = re.sub(r'\b[A-Za-z_]\w*\b', lambda m: str(defines.get(m[0], 0)), expression)
    expression = expression.replace('&&', ' and ').replace('||', ' or ')
    tree = ast.parse(expression, mode='eval')
    allowed = (ast.Expression, ast.Constant, ast.BoolOp, ast.And, ast.Or, ast.Compare,
               ast.Eq, ast.NotEq, ast.Lt, ast.LtE, ast.Gt, ast.GtE, ast.UnaryOp, ast.Not, ast.USub)
    assert all(isinstance(node, allowed) for node in ast.walk(tree)), expression
    return bool(eval(compile(tree, '<catalog condition>', 'eval'), {'__builtins__': {}}, {}))


def preprocess(source):
    source = re.sub(r'/\*[\s\S]*?\*/|//[^\n]*', '', source)
    defines = {'GL_FRAGMENT_PRECISION_HIGH': 1}
    stack = []; active = True; result = []
    for line in source.splitlines():
        directive = re.match(r'\s*#(\w+)\s*(.*)', line)
        if not directive:
            if active: result.append(line)
            continue
        command, value = directive.groups()
        if command == 'define' and active:
            parts = value.split()
            if len(parts) == 2 and re.fullmatch(r'-?\d+', parts[1]): defines[parts[0]] = int(parts[1])
        elif command in ('if', 'ifdef', 'ifndef'):
            selected = (value in defines) if command == 'ifdef' else (value not in defines) if command == 'ifndef' else condition(value, defines)
            stack.append([active, selected]); active = active and selected
        elif command == 'elif':
            parent, chosen = stack[-1]; selected = not chosen and condition(value, defines)
            stack[-1][1] |= selected; active = parent and selected
        elif command == 'else':
            parent, chosen = stack[-1]; active = parent and not chosen; stack[-1][1] = True
        elif command == 'endif':
            active = stack.pop()[0]
        elif command not in ('define', 'if', 'ifdef', 'ifndef', 'elif', 'else', 'endif'):
            raise ValueError(f'Unsupported shader directive: {command}')
    assert not stack
    return '\n'.join(result)


def functions(source):
    result = {}
    for match in re.finditer(r'\b(?:void|float|vec[234]|mat[234]|int|bool)\s+(\w+)\s*\([^;{}]*\)\s*\{', source):
        at = match.end(); depth = 1
        while depth:
            if source[at] == '{': depth += 1
            elif source[at] == '}': depth -= 1
            at += 1
        name = match[1]; assert name not in result, f'Overloaded function requires review: {name}'
        result[name] = source[match.end():at - 1]
    return result


def remove_unused_pure_locals(body):
    # Catalog helpers have no writable out/inout arguments. An unused local's
    # pure expression cannot drive output; shared preludes contain such noise.
    while True:
        previous = body
        for match in list(re.finditer(r'\b(?:float|vec[234]|mat[234]|int|bool)\s+(\w+)\s*=[^;{}]+;', body))[::-1]:
            if len(re.findall(r'\b' + re.escape(match[1]) + r'\b', body)) == 1:
                body = body[:match.start()] + body[match.end():]
        if body == previous: return body


records = []
for spec in inventory:
    original_bodies = functions(preprocess(spec['source'])); assert 'fx' in original_bodies
    bodies = {name: remove_unused_pure_locals(body) for name, body in original_bodies.items()}
    paths = {}; queue = [('fx', ['fx'])]
    while queue:
        name, path = queue.pop(0)
        if name in paths: continue
        paths[name] = path
        calls = sorted(set(re.findall(r'\b(\w+)\s*\(', bodies[name])) & bodies.keys())
        queue.extend((call, path + [call]) for call in calls)
    primitives = [name for name in paths if re.search(r'hash|noise|rand', name, re.I)
                  or re.search(r'fract\s*\(\s*sin\s*\(', bodies[name])]
    evidence = [{'function': name, 'path': paths[name], 'body': bodies[name].strip()} for name in primitives]
    records.append({'id': spec['id'], 'name': spec['name'], 'category': spec['category'],
                    'class': 'noise-driven' if primitives else 'deterministic',
                    'sourceSha256': hashlib.sha256(spec['source'].encode()).hexdigest(),
                    'defines': spec['defines'], 'noiseEvidence': evidence,
                    'activeFxBody': original_bodies['fx'].strip(), 'analyzedFxBody': bodies['fx'].strip()})

assert len(records) == 422 and len({r['id'] for r in records}) == 422
classes = {r['id']: r['class'] for r in records}
assert classes['fx-ct-scene-cut'] == classes['fx-film-grain'] == 'noise-driven'
assert all(classes[id_] == 'deterministic' for id_ in ['fx-blur', 'fx-fade-in', 'fx-cmyk-print', 'fx-comic-print', 'fx-faded-print', 'fx-sepia-flicker'])
generated = json.dumps(records, indent=2) + '\n'
if '--check' in sys.argv:
    assert (root / 'docs/p2-noise-classification.json').read_text(encoding='utf-8') == generated, 'Noise classification drift'
else:
    (root / 'docs/p2-noise-classification.json').write_text(generated, encoding='utf-8', newline='\n')
noise = [r for r in records if r['class'] == 'noise-driven']
rows = ['| Spec | Noise helper call paths |', '|---|---|']
for record in noise:
    rows.append('| ' + record['id'] + ' | ' + '; '.join(' -> '.join(e['path']) for e in record['noiseEvidence']) + ' |')
markdown = f'''# Source-based noise classification

Owner amendment: noise-driven specs use structural comparisons for owner review;
deterministic specs retain strict per-pixel MAE/p99. Shared shaders stay unchanged.
{len(noise)} noise-driven specs; {len(records) - len(noise)} deterministic specs.

The classifier selects each spec's active MODE preprocessor branch and traverses
calls from fx(). A reachable hash/noise/rand helper or inline fract(sin(...))
flags the spec. Ordinary sin/cos motion without hash noise stays deterministic.
Unused pure local assignments are removed from the analysis so noise in an
unused shared prelude cannot reclassify a deterministic output.
Classification is independent of golden results: passing noise specs are included.
The JSON records active bodies, helper paths and complete-source hashes for review.
Runtime parameter branches are conservatively included; this is a source-based
class of the spec, not a claim that noise contributes at every timestamp.

Structural comparisons use a separable Gaussian with sigma 8 px, radius 24,
edge clamping, unrounded float sRGB8 channels, channel histogram Wasserstein-1
distance and absolute channel mean error. Luminance uses Rec.709 RGB weights.
Metric limits and owner-review results are in the current structural report.
Strict results remain available separately with their original heatmaps.

''' + '\n'.join(rows) + '\n'
if '--check' not in sys.argv:
    (root / 'docs/P2-NOISE-CLASS.md').write_text(markdown, encoding='utf-8', newline='\n')
print(f'Noise classification: {len(noise)} noise-driven, {len(records) - len(noise)} deterministic; all 422 source hashes recorded')
