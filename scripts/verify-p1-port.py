"""Read-only provenance audit. Never runs or modifies the mobile build."""
from pathlib import Path
import hashlib
import json
import re

root = Path(__file__).resolve().parents[1]
mobile = Path(r'D:\Nativeoffice-photo&videoeditor')
records = json.loads((root / 'docs/p1-source-manifest.json').read_text(encoding='utf-8'))
for record in records:
    for base, path_key, hash_key in ((mobile, 'source', 'sourceSha256'), (root, 'target', 'targetSha256')):
        path = base / record[path_key]
        assert hashlib.sha256(path.read_bytes()).hexdigest() == record[hash_key], f'Changed contract: {path}'
# Kotlin compilers normalize source newlines; compare every shader literal's actual text.
for source in (mobile / 'app/src/main/java/com/nativeoffice/studio/media/video').glob('VideoFxShaders*.kt'):
    target = root / 'pinwheel-core/src/main/kotlin/com/pinwheel/core/media/video' / source.name
    original = source.read_text(encoding='utf-8-sig')
    port = target.read_text(encoding='utf-8')
    literals = re.findall(r'(?:const )?val (FX_[A-Z0-9_]+)\s*=\s*"""([\s\S]*?)"""', original)
    for name, shader in literals:
        match = re.search(r'(?:const )?val ' + name + r'\s*=\s*"""([\s\S]*?)"""', port)
        assert match and shader == match.group(1), f'Shader changed: {name}'
print(f'PASS {len(records)} source, test and asset hashes; all GLSL literals preserved')
