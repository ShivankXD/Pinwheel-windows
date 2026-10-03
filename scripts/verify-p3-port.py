"""Read-only checks of pinned P3 shader, recipe, animation, fixture and Sonic inputs."""
from pathlib import Path
import hashlib
import json
import re
import sys

root = Path(__file__).resolve().parents[1]
mobile = Path(r'D:\Nativeoffice-photo&videoeditor')
desktop_only = '--desktop-only' in sys.argv
manifest = json.loads((root / 'docs/p3-port-manifest.json').read_text(encoding='utf-8'))


def sha(data):
    return hashlib.sha256(data).hexdigest()


def selected(path, item):
    if item['kind'] == 'asset':
        return path.read_bytes()
    text = path.read_text(encoding='utf-8-sig')
    if item['kind'] == 'literal':
        literals = {}
        for name, prefix, first in re.findall(r'(?:const )?val\s+(\w+)\s*=\s*(?:(\w+)\s*\+\s*)?"""([\s\S]*?)"""', text):
            literals[name] = literals.get(prefix, '') + first
        return literals[item['literal']].encode()
    match = re.search(item['pattern'], text)
    assert match, f'Missing port section: {path}'
    return match.group(0).rstrip().encode()


for item in manifest['ports']:
    target = selected(root / item['target'], item)
    assert sha(target) == item['sha256'], f'Changed P3 port: {item["target"]} {item.get("literal", "")}'
    if not desktop_only:
        assert target == selected(mobile / item['source'], item), f'P3 port differs from mobile: {item["target"]}'
if not desktop_only:
    for item in manifest['sources']:
        assert sha((mobile / item['source']).read_bytes()) == item['sha256'], f'Changed mobile P3 source: {item["source"]}'

sonic = manifest['sonic']
original = root / sonic['original']
ported = root / sonic['target']
assert sha(original.read_bytes()) == sonic['sourceSha256'], 'Changed pinned Media3 Sonic source'
assert sha(ported.read_bytes()) == sonic['targetSha256'], 'Changed ported Sonic implementation'
adapted = original.read_text(encoding='utf-8').replace('package androidx.media3.common.audio;', 'package com.pinwheel.media;')
adapted = adapted.replace('import static com.google.common.base.Preconditions.checkState;\n', '')
adapted = adapted.replace('import org.checkerframework.checker.nullness.qual.NonNull;\n', '').replace('@NonNull ', '')
adapted = adapted.replace('/* package */ final class Sonic {', '/* package */ final class Sonic {\n  private static void checkState(boolean condition) { if (!condition) throw new IllegalStateException(); }')
assert adapted == ported.read_text(encoding='utf-8'), 'Sonic differs beyond package, annotation and checkState adaptation'

playback_files = ['LibavDecoder.kt', 'VideoPlayer.kt', 'VideoFrameSource.kt', 'TimelineAudioMixer.kt', 'PcmOutput.kt']
for name in playback_files:
    content = (root / 'pinwheel-media/src/main/kotlin/com/pinwheel/media' / name).read_text(encoding='utf-8')
    assert 'ProcessBuilder' not in content and 'Runtime.getRuntime().exec' not in content, f'Subprocess in playback: {name}'
print(f'PASS {len(manifest["ports"])} unchanged P3 port sections/assets; unchanged Media3 1.11.1 Sonic DSP; in-process playback files' + ('' if desktop_only else f'; {len(manifest["sources"])} pinned read-only mobile sources'))
