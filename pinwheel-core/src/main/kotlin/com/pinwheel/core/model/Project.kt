package com.pinwheel.core.model

import java.util.UUID
import kotlin.math.roundToLong

enum class ProjectKind { PHOTO, VIDEO }
data class Adjustments(
    val exposure: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val warmth: Float = 0f,
    val rotation: Int = 0,
    val crop: String = "Original",
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val whites: Float = 0f,
    val blacks: Float = 0f,
    val tint: Float = 0f,
    val vibrance: Float = 0f,
    val fade: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    val sharpness: Float = 0f,
    val straighten: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    val curve: List<Float> = listOf(0f, .25f, .5f, .75f, 1f),
    val redCurve: List<Float> = listOf(0f, .25f, .5f, .75f, 1f),
    val greenCurve: List<Float> = listOf(0f, .25f, .5f, .75f, 1f),
    val blueCurve: List<Float> = listOf(0f, .25f, .5f, .75f, 1f),
    val curveX: List<Float> = listOf(0f, .25f, .5f, .75f, 1f),
    val redCurveX: List<Float> = listOf(0f, .25f, .5f, .75f, 1f),
    val greenCurveX: List<Float> = listOf(0f, .25f, .5f, .75f, 1f),
    val blueCurveX: List<Float> = listOf(0f, .25f, .5f, .75f, 1f),
    val perspectiveHorizontal: Float = 0f,
    val perspectiveVertical: Float = 0f,
    val lensDistortion: Float = 0f,
    val lensProfileId: String? = null,
    val lensProfileEnabled: Boolean = false,
    val lensProfileCrop: Float = 1f,
    val mix: List<ColorBand> = List(8) { ColorBand() },
    val grading: List<ToneGrade> = List(3) { ToneGrade() },
    val gradingBalance: Float = 0f,
    val gradingBlending: Float = .5f,
    val texture: Float = 0f,
    val clarity: Float = 0f,
    val dehaze: Float = 0f,
    val noiseReduction: Float = 0f,
    val colorNoiseReduction: Float = 0f,
    val masks: List<PhotoMask> = emptyList(),
    val cloneSpots: List<CloneSpot> = emptyList(),
    val raw: RawDevelopment = RawDevelopment(),
    val lensTcaEnabled: Boolean = false,
    val frame: PhotoFrame = PhotoFrame(),
    val cutout: PhotoCutout = PhotoCutout(),
)

/**
 * One background-removal step, applied in order. A brush stroke (points, radius as a fraction of the
 * source short edge) or a magic select (tap at points[0], grows over similar colour). smart strokes only
 * affect colours close to where the stroke began, so they stop at edges. Coordinates are normalized in
 * the EXIF-oriented original, like masks, so crops and rotations keep the cutout in place.
 */
data class CutoutOp(val points: List<BrushPoint>, val radius: Float = .05f, val restore: Boolean = false,
    val smart: Boolean = true, val fill: Boolean = false, val tolerance: Float = .16f)

/** Background removal and replacement. background: Transparent, Color, Blur or Image. */
data class PhotoCutout(val ops: List<CutoutOp> = emptyList(), val background: String = "Transparent", val color: Long = 0xFFFFFFFF,
    val imageUri: String? = null, val feather: Float = .5f) {
    val active: Boolean get() = ops.isNotEmpty()
}

/**
 * Social-ready canvas around the finished photo. ratio: None, Even (same border on every side), 1:1,
 * 4:5, 9:16, 3:4, 16:9 or 3:2. border is the margin as a fraction of the canvas short edge.
 */
data class PhotoFrame(val ratio: String = "None", val style: String = "White", val border: Float = .05f) {
    val active: Boolean get() = ratio != "None"
}

/** Linear working-RGB development before display quantization. Temperature is relative, not kelvin. */
data class RawDevelopment(val exposure: Float = 0f, val temperature: Float = 0f,
    val tint: Float = 0f, val highlights: Float = 0f)

/** Source and destination in the EXIF-oriented original; radius is a fraction of its short edge. */
data class CloneSpot(val id: String = UUID.randomUUID().toString(), val x: Float = .5f, val y: Float = .5f,
    val sourceX: Float = .3f, val sourceY: Float = .5f, val radius: Float = .06f, val feather: Float = .65f,
    val mode: RetouchMode = RetouchMode.CLONE, val opacity: Float = 1f)

enum class RetouchMode { CLONE, HEAL }

data class ColorBand(val hue: Float = 0f, val saturation: Float = 0f, val luminance: Float = 0f)
data class ToneGrade(val hue: Float = 0f, val saturation: Float = 0f, val luminance: Float = 0f)
enum class MaskShape { RADIAL, LINEAR, BRUSH }
data class BrushPoint(val x: Float, val y: Float)
data class MaskStroke(val points: List<BrushPoint>, val radiusX: Float, val radiusY: Float, val erase: Boolean = false)
/** Mask coordinates are normalized in the EXIF-oriented original, before the user's crop. */
data class PhotoMask(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Radial mask",
    val shape: MaskShape = MaskShape.RADIAL,
    val centerX: Float = .5f,
    val centerY: Float = .5f,
    val radiusX: Float = .28f,
    val radiusY: Float = .28f,
    val angle: Float = 0f,
    val feather: Float = .7f,
    val inverted: Boolean = false,
    val enabled: Boolean = true,
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val warmth: Float = 0f,
    val saturation: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val tint: Float = 0f,
    val clarity: Float = 0f,
    val strokes: List<MaskStroke> = emptyList()
)
enum class PhotoExportFormat(val extension: String, val mimeType: String) { JPEG("jpg","image/jpeg"), PNG("png","image/png"), WEBP_LOSSLESS("webp","image/webp"), RAW_ULTRA_HDR("jpg","image/jpeg"), RAW_TIFF_16("tif","image/tiff") }
data class PhotoExportOptions(val quality: Int = 95, val maxEdge: Int = 0, val format: PhotoExportFormat = PhotoExportFormat.JPEG)
data class Clip(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val name: String,
    val sourceDurationMs: Long,
    val startMs: Long = 0,
    val endMs: Long = sourceDurationMs,
    val muted: Boolean = false,
    val video: VideoClipEdits = VideoClipEdits()
) {
    val sourceSpanMs: Long get() = (endMs.coerceAtLeast(0) - startMs.coerceAtLeast(0)).coerceAtLeast(0)
    val playbackSpeed: Float get() = if (video.speed.isFinite()) video.speed.coerceIn(.25f, 4f) else 1f
    val durationMs: Long get() = if (sourceSpanMs == 0L) 0 else (sourceSpanMs / playbackSpeed.toDouble()).roundToLong().coerceAtLeast(1)
    fun split(positionMs: Long): Pair<Clip, Clip>? {
        if (positionMs < 100 || durationMs - positionMs < 100) return null
        if (sourceSpanMs < 2) return null
        val sourceOffset = (positionMs * playbackSpeed.toDouble()).roundToLong().coerceIn(1, sourceSpanMs - 1)
        val boundary = startMs + sourceOffset
        return copy(endMs = boundary, video = video.copy(transition = "Cut")) to copy(id = UUID.randomUUID().toString(), startMs = boundary)
    }
    fun trim(start: Long, end: Long): Clip {
        val safeStart = start.coerceIn(0, (sourceDurationMs - 100).coerceAtLeast(0))
        val safeEnd = end.coerceIn((safeStart + 100).coerceAtMost(sourceDurationMs), sourceDurationMs)
        return copy(startMs = safeStart, endMs = safeEnd)
    }
}
data class StudioProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val kind: ProjectKind,
    val clips: List<Clip>,
    val adjustments: Adjustments = Adjustments(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val photoHistory: PhotoHistory = PhotoHistory(),
    val video: VideoProjectEdits = VideoProjectEdits(),
    val videoHistory: VideoHistory = VideoHistory()
) {
    val durationMs: Long get() {
        val picture = clips.fold(0L) { total, clip -> total + clip.durationMs.coerceAtMost(Long.MAX_VALUE - total) }
        val extracted = video.audio.filter { it.extendsVideo }.maxOfOrNull {
            it.startMs.coerceIn(0, MAX_VIDEO_CAPTION_TIME_MS) + it.durationMs.coerceIn(0, MAX_VIDEO_CAPTION_TIME_MS)
        } ?: 0L
        return maxOf(picture, extracted)
    }
}

class EditHistory(private val limit: Int = 40) {
    private val past = ArrayDeque<StudioProject>()
    private val future = ArrayDeque<StudioProject>()
    val canUndo get() = past.isNotEmpty()
    val canRedo get() = future.isNotEmpty()
    fun record(project: StudioProject) {
        past.addLast(project)
        while (past.size > limit) past.removeFirst()
        future.clear()
    }
    fun undo(current: StudioProject): StudioProject? {
        if (past.isEmpty()) return null
        future.addLast(current)
        return past.removeLast()
    }
    fun redo(current: StudioProject): StudioProject? {
        if (future.isEmpty()) return null
        past.addLast(current)
        return future.removeLast()
    }
    fun clear() { past.clear(); future.clear() }
}
