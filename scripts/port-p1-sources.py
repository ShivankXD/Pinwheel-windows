"""Copy selected P1 contracts. Mobile inputs are only opened for reading.

Re-running overwrites the explicitly listed port files, not handwritten commands.
Run only when intentionally refreshing the pinned mobile-source snapshot.
"""
from pathlib import Path
import hashlib
import json
import re
import shutil

ROOT = Path(__file__).resolve().parents[1]
MOBILE = Path(r'D:\Nativeoffice-photo&videoeditor')
SOURCE = MOBILE / 'app/src/main/java/com/nativeoffice/studio'
TARGET = ROOT / 'pinwheel-core/src/main/kotlin/com/pinwheel/core'
records = []

def transform(text):
    text = text.replace('\r\n', '\n')
    text = text.replace('com.nativeoffice.studio', 'com.pinwheel.core')
    return re.sub(r'^internal (object|data class|enum class|fun|class)', r'\1', text, flags=re.M)

def port(relative, adapt=lambda text: text, output=None):
    source = SOURCE / relative
    original = source.read_bytes()
    text = adapt(transform(original.decode('utf-8-sig')))
    target = TARGET / (output or relative)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text, encoding='utf-8', newline='\n')
    records.append({'source': str(source.relative_to(MOBILE)).replace('\\', '/'),
                    'sourceSha256': hashlib.sha256(original).hexdigest(),
                    'target': str(target.relative_to(ROOT)).replace('\\', '/'),
                    'targetSha256': hashlib.sha256(target.read_bytes()).hexdigest()})

for source in sorted((SOURCE / 'model').glob('*.kt')):
    port('model/' + source.name)
for name in ('AdjustmentCodec', 'CaptionFileText', 'PhotoHistoryCodec', 'SrtCodec', 'VideoEditCodec', 'VideoHistoryCodec'):
    port('data/' + name + '.kt')
for name in ('PhotoCurve', 'PhotoLookLibrary', 'PinwheelTemplates'):
    port('media/' + name + '.kt')
for source in sorted((SOURCE / 'media/video').glob('VideoFxShaders*.kt')):
    def shader_data(text):
        if source.name != 'VideoFxShaders.kt':
            return text
        helpers = text[text.index('        /** Full fragment source'):text.index('\n    }\n}')]
        constants = text[text.index('private const val FX_VERTEX'):]
        return 'package com.pinwheel.core.media.video\n\n/** Shader data only. GLES execution belongs to P2. */\nobject FxShaderSources {\n' + helpers + '\n}\n\n' + constants.replace('private const val FX_VERTEX', 'const val FX_VERTEX')
    port('media/video/' + source.name, shader_data)
for name in ('VideoFxCatalog', 'CaptionMotionTemplates', 'ImportedCaptionMotion'):
    port('media/video/' + name + '.kt')

def registry(text):
    text = re.sub(r'^import android\..*\n', '', text, flags=re.M)
    return text[:text.index('    private val paint =')] + '}\n'
for name in ('VideoAnimatedStickers', 'VideoStickerCards'):
    port('media/video/' + name + '.kt', registry)

def geometric(text):
    text = re.sub(r'^import android\..*\n', '', text, flags=re.M)
    return text[:text.index('    fun render(')] + '}\n'
port('media/video/VideoStickerCatalog.kt', geometric)

def filters(text):
    start = text.index('data class FilterRecipe(')
    end = text.index('    /** CPU twin')
    return 'package com.pinwheel.core.media.video\n\n' + text[start:end] + '}\n'
port('media/video/VideoGrade.kt', filters, 'media/video/VideoFilterCatalog.kt')

def font_registry(text):
    text = re.sub(r'^import android\..*\n', '', text, flags=re.M)
    text = text[:text.index('    @Volatile private var assets:')]
    return text.replace('private val files', 'val files').replace('private val fallbacks', 'val fallbacks') + '    val keys: Set<String> get() = files.keys\n}\n'
port('media/video/CaptionFonts.kt', font_registry)

def imported_catalog(text):
    text = text.replace('import android.content.Context', 'import java.nio.file.Path\nimport java.nio.file.Files')
    text = text.replace('fun load(context: Context)', 'fun load(assets: Path)')
    text = text.replace('context.assets.open("caption_packages/catalog.json")', 'Files.newInputStream(assets.resolve("caption_packages/catalog.json"))')
    return text
port('media/video/ImportedCaptionCatalog.kt', imported_catalog)

def audio_registry(text):
    text = re.sub(r'^import android\..*\n', '', text, flags=re.M)
    return text[:text.index('    /** A stable local file')] + '}\n'
port('media/audio/AudioAssetCatalog.kt', audio_registry)

def frame_registry(text):
    start = text.index('    val ratios =')
    end = text.index('    fun ratio(')
    return 'package com.pinwheel.core.media\n\n/** P1 registry only; rendering is P6. */\nobject PhotoFrameRenderer {\n' + text[start:end] + '}\n'
port('media/PhotoFrame.kt', frame_registry, 'media/PhotoFrameRenderer.kt')

def photo_looks(text):
    text = text.replace('import android.content.Context\n', '').replace('import android.util.AtomicFile', 'import com.pinwheel.core.data.AtomicFile')
    text = text.replace('context:Context', 'filesDir:File').replace('context.filesDir', 'filesDir').replace('read(context)', 'read(filesDir)')
    text = text.replace('else context.getSharedPreferences("photo-looks",Context.MODE_PRIVATE).getString("looks","[]")?:"[]"', 'else "[]"')
    return text
port('media/PhotoLooks.kt', photo_looks)

def storage(text):
    text = text.replace('import android.content.Context\n', '').replace('import android.util.AtomicFile\n', '')
    text = text.replace('context: Context', 'filesDir: File').replace('context.filesDir', 'filesDir')
    return text
port('data/PhotoVersionStore.kt', storage)

def library(text):
    text = re.sub(r'^import android\..*\n', '', text, flags=re.M)
    text = text.replace('import androidx.core.net.toUri', 'import java.net.URI').replace('import android.net.Uri\n', '')
    text += '\nprivate data class SourceUri(val scheme: String?, val path: String?)\n' \
        + 'private fun String.toUri(): SourceUri {\n' \
        + '    val uri = URI(replace(" ", "%20"))\n' \
        + '    val path = uri.path?.let { if (it.matches(Regex("/[A-Za-z]:/.*"))) it.drop(1) else it }\n' \
        + '    return SourceUri(uri.scheme, path)\n}\n'
    return text
port('data/LibraryStorage.kt', library)

project = storage(transform((SOURCE / 'data/ProjectStore.kt').read_text(encoding='utf-8-sig')))
split = project.index('    private fun encode(p: StudioProject)')
codec = project[split:project.rfind('}')].replace('private fun encode', 'fun encode').replace('private fun decode', 'fun decode')
store = project[:split] + '}\n'
store = store.replace('decode(AtomicFile', 'ProjectCodec.decode(AtomicFile').replace('stream.write(encode(project)', 'stream.write(ProjectCodec.encode(project)')
port('data/ProjectStore.kt', lambda text: store)
port('data/ProjectStore.kt', lambda text: 'package com.pinwheel.core.data\n\nimport com.pinwheel.core.model.*\nimport org.json.JSONArray\nimport org.json.JSONObject\n\n/** Mobile JSON contract, separated from file access. */\nobject ProjectCodec {\n' + codec + '}\n', 'data/ProjectCodec.kt')

test_source = MOBILE / 'app/src/test/java/com/nativeoffice/studio'
for folder in ('model', 'data'):
    for source in sorted((test_source / folder).glob('*.kt')):
        original = source.read_bytes()
        target = ROOT / 'pinwheel-core/src/test/kotlin/com/pinwheel/core' / folder / source.name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(transform(original.decode('utf-8-sig')), encoding='utf-8', newline='\n')
        records.append({'source': str(source.relative_to(MOBILE)).replace('\\', '/'),
                        'sourceSha256': hashlib.sha256(original).hexdigest(),
                        'target': str(target.relative_to(ROOT)).replace('\\', '/'),
                        'targetSha256': hashlib.sha256(target.read_bytes()).hexdigest()})

assets = MOBILE / 'app/src/main/assets'
for folder in ('caption_fonts', 'caption_packages', 'audio_catalog', 'fx_samples', 'studio_scenes'):
    source = assets / folder
    target = ROOT / 'assets' / folder
    shutil.copytree(source, target, dirs_exist_ok=True)
    for asset in sorted(source.rglob('*')):
        if asset.is_file():
            dest = target / asset.relative_to(source)
            records.append({'source': str(asset.relative_to(MOBILE)).replace('\\', '/'),
                            'sourceSha256': hashlib.sha256(asset.read_bytes()).hexdigest(),
                            'target': str(dest.relative_to(ROOT)).replace('\\', '/'),
                            'targetSha256': hashlib.sha256(dest.read_bytes()).hexdigest()})
    if folder == 'caption_fonts':
        notices = ROOT / 'licenses/mobile/caption_fonts'
        notices.mkdir(parents=True, exist_ok=True)
        for notice in source.glob('*.txt'):
            shutil.copy2(notice, notices / notice.name)
    if folder == 'audio_catalog':
        shutil.copy2(source / 'LICENSES.md', ROOT / 'licenses/mobile/AUDIO-CATALOG-LICENSES.md')

manifest = ROOT / 'docs/p1-source-manifest.json'
manifest.write_text(json.dumps(records, indent=2) + '\n', encoding='utf-8')
print(f'Copied {len(records)} source/test contracts with provenance; all mobile inputs read-only.')
