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
    port('media/video/' + name + '.kt', lambda text: text.replace('private val legacyOverlays', 'val legacyOverlays'))

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
    start = text.index('data class FrameLayout')
    end = text.index('    /** A small soft copy')
    pure = text[start:end]
    pure = re.sub(r'    val rect: RectF.*\n', '', pure)
    return 'package com.pinwheel.core.media\n\nimport com.pinwheel.core.model.PhotoFrame\nimport kotlin.math.*\n\n' + pure + '}\n'
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
for folder in ('caption_fonts', 'caption_packages', 'audio_catalog', 'fx_samples', 'studio_scenes', 'template_gallery'):
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

# Pure mathematics required by the remaining JVM executable specifications.
# These are headless contracts, not a P6 photo editor or LibRaw decoder.
for source in sorted((SOURCE / 'media').glob('*.kt')):
    text = source.read_text(encoding='utf-8-sig')
    if not re.search(r'^import (android|androidx)\.', text, flags=re.M):
        port('media/' + source.name)
for relative in ('media/video/VideoCropGeometry.kt', 'media/video/VideoCanvasSizing.kt',
                 'media/audio/VoiceoverSession.kt', 'media/captions/CaptionWords.kt', 'ui/PhotoCropFrame.kt'):
    port(relative)

def caption_time(text):
    return 'package com.pinwheel.core.ui\nimport com.pinwheel.core.model.MAX_VIDEO_CAPTION_TIME_MS\n\n' + text[text.index('fun captionTime('):]
port('ui/VideoCaptionPanels.kt', caption_time, 'ui/CaptionTimeText.kt')

def cutout_math(text):
    text = re.sub(r'^import (android|androidx)\..*\n', '', text, flags=re.M)
    return text[:text.index('        fun fromBitmap(')] + '    }\n}\n'
port('media/PhotoCutout.kt', cutout_math, 'media/CutoutMask.kt')

def clone_math(text):
    return 'package com.pinwheel.core.media\nimport kotlin.math.roundToInt\n\nobject PhotoClone {\n' + text[text.index('    internal fun blend('):].replace('internal fun blend', 'fun blend')
port('media/PhotoClone.kt', clone_math, 'media/PhotoClone.kt')

def raw_linear(text):
    text = re.sub(r'^import (android|androidx)\..*\n', '', text, flags=re.M)
    begin = text.index('    fun toBitmap(')
    end = text.index('/** All development mathematics')
    return text[:begin] + '}\n\n' + text[end:]
port('media/RawLinearImage.kt', raw_linear)

def raw_tiff(text):
    imports = re.findall(r'^import (?!android)(.*)$', text, flags=re.M)
    eligibility = text[text.index('    fun eligibilityIssue'):text.index('    fun export(')]
    return 'package com.pinwheel.core.media\n' + ''.join('import ' + i + '\n' for i in imports) + '\nobject RawTiff {\n' + eligibility + '}\n\n' + text[text.index('object Tiff16Writer {'):]
port('media/RawTiff.kt', raw_tiff)

def crop_geometry(text):
    start = text.index('    fun aspect(')
    end = text.index('    fun geometry(')
    return 'package com.pinwheel.core.media\nimport com.pinwheel.core.model.Adjustments\nimport kotlin.math.*\n\ndata class CropRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {\n    fun width() = right - left\n    fun height() = bottom - top\n}\n\nobject PhotoEngine {\n' + text[start:end].replace('Rect', 'CropRect') + '}\n'
port('media/PhotoEngine.kt', crop_geometry, 'media/PhotoEngine.kt')

def envelope(text):
    text = re.sub(r'^import androidx\..*\n|^@UnstableApi\n', '', text, flags=re.M)
    return text.replace(' : GainProcessor.GainProvider', '').replace('override fun ', 'fun ')
port('media/video/VideoAudioEnvelope.kt', envelope)

def dsp(text):
    text = re.sub(r'^import androidx\..*\n|^@UnstableApi\n', '', text, flags=re.M)
    begin = text.index('fun voicePitchProcessor(')
    end = text.index('/**\n * Streaming PCM')
    text = text[:begin] + text[end:]
    text = text.replace(' : BaseAudioProcessor()', '')
    text = text.replace('override fun onConfigure', 'fun configure').replace('override fun onFlush', 'fun flush').replace('override fun onReset', 'fun reset').replace('override fun queueInput', 'fun queueInput')
    text = text.replace('AudioProcessor.AudioFormat', 'PcmFormat').replace('C.ENCODING_PCM_16BIT', 'PcmEncoding.PCM16').replace('C.ENCODING_PCM_FLOAT', 'PcmEncoding.FLOAT32')
    text = text.replace('throw AudioProcessor.UnhandledAudioFormatException("Voice effects require PCM16 or float PCM", inputAudioFormat)', 'error("Voice effects require PCM16 or float PCM")')
    text = text.replace('        return inputAudioFormat', '        require(inputAudioFormat.sampleRate > 0 && inputAudioFormat.channelCount in 1..8)\n        this.inputAudioFormat = inputAudioFormat\n        return inputAudioFormat')
    start = text.index('    private data class ChannelState')
    wrapper = '    private lateinit var inputAudioFormat: PcmFormat\n    private var outputBuffer = ByteBuffer.allocate(0)\n    private fun replaceOutputBuffer(size: Int): ByteBuffer = ByteBuffer.allocateDirect(size).also { outputBuffer = it }\n    fun getOutput(): ByteBuffer = outputBuffer.also { outputBuffer = ByteBuffer.allocate(0) }\n\n'
    text = text[:start] + wrapper + text[start:]
    return text + '\nenum class PcmEncoding { PCM16, FLOAT32 }\ndata class PcmFormat(val sampleRate: Int, val channelCount: Int, val encoding: PcmEncoding)\n'
port('media/video/VideoAudioDspProcessor.kt', dsp)

for source in sorted(test_source.rglob('*.kt')):
    if source.parent.name in ('model', 'data'):
        continue
    original = source.read_bytes()
    text = transform(original.decode('utf-8-sig'))
    # Only bindings/asset paths change; scenarios and assertions remain intact.
    text = re.sub(r'^import androidx\..*\n|^@androidx.annotation.OptIn.*\n', '', text, flags=re.M)
    text = text.replace('AudioProcessor.AudioFormat', 'PcmFormat').replace('C.ENCODING_PCM_16BIT', 'PcmEncoding.PCM16').replace('C.TIME_UNSET', '(Long.MIN_VALUE + 1)')
    if source.name == 'RawTiffTest.kt':
        text = text.replace('src/main/assets/third_party/icc/sRGB2014.icc', 'licenses/mobile/third_party/icc/sRGB2014.icc')
    target = ROOT / 'pinwheel-core/src/test/kotlin/com/pinwheel/core' / source.relative_to(test_source)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(text, encoding='utf-8', newline='\n')
    records.append({'source': str(source.relative_to(MOBILE)).replace('\\', '/'), 'sourceSha256': hashlib.sha256(original).hexdigest(),
                    'target': str(target.relative_to(ROOT)).replace('\\', '/'), 'targetSha256': hashlib.sha256(target.read_bytes()).hexdigest()})
shutil.copytree(MOBILE / 'app/src/test/resources', ROOT / 'pinwheel-core/src/test/resources', dirs_exist_ok=True)
for source in sorted((MOBILE / 'app/src/test/resources').rglob('*')):
    if source.is_file():
        target = ROOT / 'pinwheel-core/src/test/resources' / source.relative_to(MOBILE / 'app/src/test/resources')
        records.append({'source': str(source.relative_to(MOBILE)).replace('\\', '/'), 'sourceSha256': hashlib.sha256(source.read_bytes()).hexdigest(),
                        'target': str(target.relative_to(ROOT)).replace('\\', '/'), 'targetSha256': hashlib.sha256(target.read_bytes()).hexdigest()})
# A port can intentionally replace a registry with its pure-math superset above.
records = list({record['target']: record for record in records}.values())
manifest.write_text(json.dumps(records, indent=2) + '\n', encoding='utf-8')
print(f'Copied {len(records)} source/test contracts with provenance; all mobile inputs read-only.')
