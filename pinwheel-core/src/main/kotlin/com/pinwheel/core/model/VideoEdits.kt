package com.pinwheel.core.model

import java.util.UUID
import kotlin.math.roundToLong

/** Non-destructive clip corrections. Neutral controls are zero; volume and look strength are unit scales. */
data class VideoClipEdits(
    val rotation: Int = 0,
    val mirrored: Boolean = false,
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val lookId: String = "original",
    val lookIntensity: Float = 1f,
    val volume: Float = 1f,
    val speed: Float = 1f,
    val transition: String = "Cut",
    val transitionDurationMs: Long = 600,
    /** 0 gentle, 1 snappy: how sharply the transition accelerates into and out of the cut. */
    val transitionSpeed: Float = .5f,
    val motion: String = "None",
    val motionAmount: Float = .35f,
    /** Crop zoom and anchor within the source frame. The crop keeps the source aspect ratio. */
    val cropZoom: Float = 1f,
    val cropX: Float = .5f,
    val cropY: Float = .5f,
    /** CapCut-style Adjust controls, all neutral at zero. */
    val brightness: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val sharpen: Float = 0f,
    val fade: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
    val tint: Float = 0f,
    val vibrance: Float = 0f,
) {
    fun sanitized() = copy(
        brightness = brightness.safe(-1f, 1f, 0f), highlights = highlights.safe(-1f, 1f, 0f), shadows = shadows.safe(-1f, 1f, 0f),
        sharpen = sharpen.safe(0f, 1f, 0f), fade = fade.safe(0f, 1f, 0f), vignette = vignette.safe(-1f, 1f, 0f),
        grain = grain.safe(0f, 1f, 0f), tint = tint.safe(-1f, 1f, 0f), vibrance = vibrance.safe(-1f, 1f, 0f),
        rotation = (((rotation % 360) + 360) % 360 / 90) * 90,
        exposure = exposure.safe(-1f, 1f, 0f),
        contrast = contrast.safe(-1f, 1f, 0f),
        saturation = saturation.safe(-1f, 1f, 0f),
        warmth = warmth.safe(-1f, 1f, 0f),
        lookId = lookId.take(64).ifBlank { "original" },
        lookIntensity = lookIntensity.safe(0f, 1f, 1f),
        volume = volume.safe(0f, 2f, 1f),
        speed = speed.safe(.25f, 4f, 1f),
        transition = transition.takeIf { it in VIDEO_TRANSITIONS || it.matches(Regex("fx-tr-[a-z0-9-]{1,30}")) } ?: "Cut",
        transitionDurationMs = transitionDurationMs.coerceIn(100, 3000), transitionSpeed = transitionSpeed.safe(0f, 1f, .5f),
        motion = motion.takeIf { it in VIDEO_CLIP_MOTIONS } ?: "None",
        motionAmount = motionAmount.safe(0f, 1f, .35f),
        cropZoom = cropZoom.safe(1f, 4f, 1f),
        cropX = cropX.safe(0f, 1f, .5f),
        cropY = cropY.safe(0f, 1f, .5f),
    )
}

/** x/y locate the overlay center from the canvas top-left; size is a fraction of its shorter edge. */
data class VideoTextOverlay(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "Your title",
    val startMs: Long = 0,
    val endMs: Long = 3000,
    val x: Float = .5f,
    val y: Float = .5f,
    val size: Float = .07f,
    val color: Long = 0xffffffff,
    val background: Long = 0x00000000,
    val bold: Boolean = true,
    val style: String = "Clean",
    val animation: String = "None",
    val animationDurationMs: Long = 300,
    val fontFamily: String = "sans-serif",
    val italic: Boolean = false,
    val alignment: String = "Center",
    val widthScale: Float = 1f,
    /** Timeline row inside the text group; rows are display-only and never change rendering. */
    val lane: Int = 0,
) {
    fun sanitized(): VideoTextOverlay {
        val start = startMs.coerceIn(0, MAX_VIDEO_TIME_MS - 1)
        return copy(
            id = id.safeId(), text = text.take(MAX_VIDEO_TEXT_LENGTH),
            startMs = start, endMs = endMs.coerceIn(start + 1, MAX_VIDEO_TIME_MS),
            x = x.safe(0f, 1f, .5f), y = y.safe(0f, 1f, .5f), size = size.safe(.02f, .3f, .07f),
            color = color and 0xffffffffL, background = background and 0xffffffffL,
            style = style.take(64).ifBlank { "Clean" },
            animation = animation.takeIf { it in VIDEO_TEXT_ANIMATIONS } ?: "None",
            animationDurationMs = animationDurationMs.coerceIn(100, 1500),
            fontFamily = fontFamily.takeIf { it in VIDEO_TITLE_FONTS } ?: "sans-serif",
            alignment = alignment.takeIf { it in VIDEO_CAPTION_ALIGNMENTS } ?: "Center",
            widthScale = widthScale.safe(.7f, 1.4f, 1f), lane = lane.coerceIn(0, MAX_TIMELINE_LANE),
        )
    }
}

/** Source timing for a moving overlay; display geometry stays shared with image layers. */
data class VideoOverlaySource(
    val durationMs: Long,
    val startMs: Long = 0,
    val width: Int = 1920,
    val height: Int = 1080,
) {
    fun sanitized(): VideoOverlaySource {
        val duration = durationMs.coerceIn(1, MAX_VIDEO_TIME_MS)
        return copy(durationMs = duration, startMs = startMs.coerceIn(0, duration - 1),
            width = width.coerceIn(1, 16384), height = height.coerceIn(1, 16384))
    }
}

// Historical name retained for project compatibility: this is the shared visual layer model.
data class VideoImageOverlay(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val name: String,
    val startMs: Long = 0,
    val endMs: Long = 3000,
    val x: Float = .5f,
    val y: Float = .5f,
    val width: Float = .35f,
    val opacity: Float = 1f,
    val rotation: Int = 0,
    val mirrored: Boolean = false,
    /** Shape the layer is cut to: None, Circle, Rounded, Heart, Star, Diamond. */
    val mask: String = "None",
    /** Entrance / exit motion: None, Fade, Zoom, Slide, Spin, Pop. */
    val animIn: String = "None",
    val animOut: String = "None",
    val shadow: Boolean = false,
    /** Outline colour; 0 means no border. */
    val border: Long = 0L,
    val video: VideoOverlaySource? = null,
    val lane: Int = 0,
) {
    fun sanitized(): VideoImageOverlay {
        val start = startMs.coerceIn(0, MAX_VIDEO_TIME_MS - 1)
        val source = video?.sanitized()
        val maxEnd = minOf(MAX_VIDEO_TIME_MS, start + (source?.let { it.durationMs - it.startMs } ?: MAX_VIDEO_TIME_MS))
        return copy(
            id = id.safeId(), uri = uri.take(8192), name = name.take(160), video = source,
            startMs = start, endMs = endMs.coerceIn(start + 1, maxEnd),
            x = x.safe(0f, 1f, .5f), y = y.safe(0f, 1f, .5f),
            width = width.safe(.05f, 1f, .35f), opacity = opacity.safe(0f, 1f, 1f),
            rotation = ((rotation % 360) + 360) % 360,
            mask = mask.takeIf { it in VIDEO_OVERLAY_MASKS } ?: "None",
            animIn = animIn.takeIf { it in VIDEO_OVERLAY_ANIMATIONS } ?: "None",
            animOut = animOut.takeIf { it in VIDEO_OVERLAY_ANIMATIONS } ?: "None",
            border = border and 0xffffffffL, lane = lane.coerceIn(0, MAX_TIMELINE_LANE),
        )
    }
}

val VIDEO_OVERLAY_MASKS = listOf("None", "Circle", "Rounded", "Heart", "Star", "Diamond")
val VIDEO_OVERLAY_ANIMATIONS = listOf("None", "Fade", "Zoom", "Slide", "Spin", "Pop")

data class VideoProjectEdits(
    val aspectRatio: String = "Original",
    val scaleMode: String = "Fit",
    val texts: List<VideoTextOverlay> = emptyList(),
    val images: List<VideoImageOverlay> = emptyList(),
    val audio: List<VideoAudioTrack> = emptyList(),
    val captions: List<VideoCaptionCue> = emptyList(),
    val captionStyle: VideoCaptionStyle = VideoCaptionStyle(),
    val effects: List<VideoTimedEffect> = emptyList(),
    val backgroundColor: Long = 0xff000000L,
    val backgroundMode: String = "Solid",
    val backgroundBlur: Float = .5f,
) {
    fun sanitized() = copy(
        aspectRatio = aspectRatio.takeIf { it in VIDEO_ASPECT_RATIOS } ?: "Original",
        scaleMode = scaleMode.takeIf { it == "Fit" || it == "Fill" } ?: "Fit",
        texts = texts.take(MAX_VIDEO_OVERLAYS).map { it.sanitized() }.distinctBy { it.id },
        images = images.take(MAX_VIDEO_OVERLAYS).map { it.sanitized() }.filter { it.uri.isNotBlank() }.distinctBy { it.id },
        audio = audio.take(MAX_VIDEO_AUDIO_TRACKS).map { it.sanitized() }.filter { it.uri.isNotBlank() && it.durationMs > 0 }.distinctBy { it.id },
        captions = captions.take(MAX_VIDEO_CAPTIONS).map { it.sanitized() }.distinctBy { it.id }.sortedBy { it.startMs },
        captionStyle = captionStyle.sanitized(),
        effects = effects.map { it.sanitized() }.filterNot { it.kind in com.pinwheel.core.media.video.VideoFxCatalog.retired }.distinctBy { it.id }.take(MAX_VIDEO_EFFECTS),
        backgroundColor = (backgroundColor and 0x00ffffffL) or 0xff000000L,
        backgroundMode = backgroundMode.takeIf { it == "Solid" || it == "Blur" } ?: "Solid",
        backgroundBlur = if (backgroundBlur.isFinite()) backgroundBlur.coerceIn(0f, 1f) else .5f,
    )
}

/** Source bounds are milliseconds in the audio asset; startMs places the trimmed sound on the composition. */
data class VideoAudioTrack(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val name: String,
    val sourceDurationMs: Long,
    val sourceStartMs: Long = 0,
    val sourceEndMs: Long = sourceDurationMs,
    val startMs: Long = 0,
    val volume: Float = .7f,
    val fadeInMs: Long = 0,
    val fadeOutMs: Long = 0,
    val speed: Float = 1f,
    val voiceEffect: String = "None",
    val reduceNoise: Boolean = false,
    /** Extracted source audio may deliberately extend past the last video frame. */
    val extendsVideo: Boolean = false,
    val lane: Int = 0,
) {
    val sourceSpanMs: Long get() = (sourceEndMs.coerceAtLeast(0) - sourceStartMs.coerceAtLeast(0)).coerceAtLeast(0)
    /** Placement and fade time after playback speed. Rounded to the nearest millisecond. */
    val durationMs: Long get() = if (sourceSpanMs == 0L) 0L else
        (sourceSpanMs / speed.safe(.1f, 10f, 1f).toDouble()).roundToLong().coerceAtLeast(1)
    fun sanitized(): VideoAudioTrack {
        val sourceDuration = sourceDurationMs.coerceIn(0, MAX_VIDEO_TIME_MS)
        val sourceStart = sourceStartMs.coerceIn(0, (sourceDuration - 1).coerceAtLeast(0))
        val sourceEnd = sourceEndMs.coerceIn((sourceStart + 1).coerceAtMost(sourceDuration), sourceDuration)
        val duration = if (sourceEnd == sourceStart) 0L else
            ((sourceEnd - sourceStart) / speed.safe(.1f, 10f, 1f).toDouble()).roundToLong().coerceAtLeast(1)
        return copy(
            id = id.safeId(), uri = uri.take(8192), name = name.take(160), sourceDurationMs = sourceDuration,
            sourceStartMs = sourceStart, sourceEndMs = sourceEnd, startMs = startMs.coerceIn(0, MAX_VIDEO_TIME_MS - 1),
            volume = volume.safe(0f, 8f, .7f), fadeInMs = fadeInMs.coerceIn(0, duration), fadeOutMs = fadeOutMs.coerceIn(0, duration),
            speed = speed.safe(.1f, 10f, 1f), voiceEffect = voiceEffect.takeIf { it in VIDEO_VOICE_EFFECTS } ?: "None",
            lane = lane.coerceIn(0, MAX_TIMELINE_LANE),
        )
    }
}

val VIDEO_VOICE_EFFECTS = listOf("None", "Deep", "Chipmunk", "Robot", "Radio", "Telephone",
    "Megaphone", "Alien", "Whisper", "Echo", "Underwater")

val VIDEO_ASPECT_RATIOS = listOf("Original", "9:16", "16:9", "1:1", "4:5", "4:3", "3:4", "2:3", "3:2", "2.35:1")
val VIDEO_TRANSITIONS = listOf("Cut", "Dip black", "Dip white", "Flash", "Zoom burst", "Glitch", "Whip left")
val VIDEO_TEXT_ANIMATIONS = listOf("None", "Fade", "Rise")
const val MAX_VIDEO_OVERLAYS = 32
const val MAX_TIMELINE_LANE = 63
const val MAX_VIDEO_AUDIO_TRACKS = 16
const val MAX_VIDEO_TEXT_LENGTH = 500
private const val MAX_VIDEO_TIME_MS = 24L * 60 * 60 * 1000
private fun Float.safe(min: Float, max: Float, fallback: Float) = if (isFinite()) coerceIn(min, max) else fallback
private fun String.safeId() = take(100).ifBlank { UUID.randomUUID().toString() }

val VIDEO_TITLE_FONTS = listOf("sans-serif", "serif", "monospace", "sans-serif-condensed", "cursive",
    "sans-serif-light", "sans-serif-medium", "sans-serif-black", "serif-monospace", "casual")

val VIDEO_CLIP_MOTIONS = listOf("None", "Zoom in", "Zoom out", "Pan left", "Pan right", "Pan up", "Pan down")
