"""Read-only audit of P2 mobile sources and the literal legacy/glow/layer shaders."""
from pathlib import Path
import hashlib
import json
import re
import sys

root = Path(__file__).resolve().parents[1]
mobile = Path(r'D:\Nativeoffice-photo&videoeditor')
desktop_only = '--desktop-only' in sys.argv


def literals(path):
    text = path.read_text(encoding='utf-8-sig')
    result = {}
    pattern = r'(?:const )?val\s+(\w+)\s*=\s*(?:(\w+)\s*\+\s*)?"""([\s\S]*?)"""(?:\s*\+\s*"""([\s\S]*?)""")?'
    for name, prefix, first, second in re.findall(pattern, text):
        result[name] = result.get(prefix, '') + first + second
    return result


sources = json.loads((root / 'docs/p2-mobile-source-manifest.json').read_text(encoding='utf-8'))
if not desktop_only:
    for item in sources:
        path = mobile / item['source']
        assert hashlib.sha256(path.read_bytes()).hexdigest() == item['sourceSha256'], f'Changed mobile runtime source: {path}'

shaders = json.loads((root / 'docs/p2-shader-manifest.json').read_text(encoding='utf-8'))
for item in shaders:
    desktop = literals(root / item['target'])[item['targetLiteral']]
    assert hashlib.sha256(desktop.encode()).hexdigest() == item['shaderSha256'], f'Changed desktop shader: {item["targetLiteral"]}'
    if not desktop_only:
        original = literals(mobile / item['source'])[item['sourceLiteral']]
        assert desktop == original, f'Shader differs from mobile: {item["targetLiteral"]}'
print(f'PASS {len(shaders)} unchanged P2 legacy/glow/layer shader literals' + ('' if desktop_only else f'; {len(sources)} pinned mobile runtime source hashes'))
