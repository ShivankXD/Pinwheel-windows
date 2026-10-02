package com.pinwheel.core.model

import java.util.UUID
import kotlin.math.sin

/** Non-destructive effect interval on the composition clock; its end is exclusive. */
data class VideoTimedEffect(
    val id: String = UUID.randomUUID().toString(),
    val kind: String = "Vignette",
    val startMs: Long = 0,
    val endMs: Long = 3000,
    val intensity: Float = .5f,
    val enabled: Boolean = true,
    /** Adjust values (0..1) for Pinwheel GL effects, keyed by the effect's own parameter names. */
    val params: Map<String, Float> = emptyMap(),
    val lane: Int = 0,
    /** Empty applies to the main video; otherwise the id of the overlay layer (image, sticker or video) it styles. */
    val target: String = "",
) {
    fun sanitized(): VideoTimedEffect {
        val start = startMs.coerceIn(0, MAX_VIDEO_EFFECT_TIME_MS - 1)
        return copy(id = id.take(100).ifBlank { UUID.randomUUID().toString() },
            kind = kind.takeIf { it in VIDEO_EFFECT_KINDS || it.matches(VIDEO_FX_ID) } ?: "Vignette",
            startMs = start, endMs = endMs.coerceIn(start + 1, MAX_VIDEO_EFFECT_TIME_MS),
            intensity = if (intensity.isFinite()) intensity.coerceIn(0f, 1f) else .5f,
            params = params.entries.take(12).filter { it.key.matches(Regex("[a-z]{1,16}")) && it.value.isFinite() }
                .associate { it.key to it.value.coerceIn(0f, 1f) }, lane = lane.coerceIn(0, MAX_TIMELINE_LANE),
            target = target.take(100))
    }
}

val VIDEO_EFFECT_KINDS = listOf("Vignette", "Grain", "RGB split", "Pixelate", "Soft Glow", "3D Tilt")
const val MAX_VIDEO_EFFECTS = 40
/** Ids of catalog effects (media/video/VideoFxCatalog); legacy kinds keep their display names. */
val VIDEO_FX_ID = Regex("fx-[a-z0-9-]{1,40}")
private const val MAX_VIDEO_EFFECT_TIME_MS = 24L * 60 * 60 * 1000

/** Same-kind overlaps add up to full intensity; list order does not affect the recipe. */
fun activeVideoEffectIntensities(effects: List<VideoTimedEffect>, timeUs: Long, result: FloatArray = FloatArray(4)): FloatArray {
    require(result.size >= 4)
    result.fill(0f)
    if (timeUs < 0) return result
    val timeMs = timeUs / 1000
    for (effectIndex in 0 until minOf(effects.size, MAX_VIDEO_EFFECTS)) {
        val effect = effects[effectIndex]
        if (!effect.enabled || timeMs < effect.startMs || timeMs >= effect.endMs) continue
        val index = VIDEO_EFFECT_KINDS.indexOf(effect.kind)
        if (index in 0..3 && effect.intensity.isFinite()) result[index] = (result[index] + effect.intensity.coerceIn(0f, 1f)).coerceAtMost(1f)
    }
    return result
}

/** A repeatable tilt driven only by composition time, including after seeks and in export. */
fun activeVideo3DTilt(effects: List<VideoTimedEffect>, timeUs: Long, result: FloatArray = FloatArray(2)): FloatArray {
    require(result.size >= 2)
    if (timeUs < 0) { result[0] = 0f; result[1] = 0f; return result }
    val timeMs = timeUs / 1000
    var horizontal = 0f
    var vertical = 0f
    for (effect in effects.take(MAX_VIDEO_EFFECTS)) {
        if (effect.kind != "3D Tilt" || !effect.enabled || !effect.intensity.isFinite() ||
            timeMs < effect.startMs || timeMs >= effect.endMs) continue
        val phase = (timeUs - effect.startMs * 1000).toDouble() * (2.0 * Math.PI / 1_800_000.0)
        val strength = effect.intensity.coerceIn(0f, 1f)
        horizontal += (sin(phase) * .78 * strength).toFloat()
        vertical += (sin(phase * .5 + Math.PI / 2) * .30 * strength).toFloat()
    }
    result[0] = horizontal.coerceIn(-.9f, .9f)
    result[1] = vertical.coerceIn(-.4f, .4f)
    return result
}

/** Combined strength for the source-backed LumiSoftGlow rendering component. */
fun activeVideoSoftGlow(effects: List<VideoTimedEffect>, timeUs: Long): Float {
    if (timeUs < 0) return 0f
    val timeMs = timeUs / 1000
    var amount = 0f
    for (effect in effects.take(MAX_VIDEO_EFFECTS)) {
        if (effect.kind == "Soft Glow" && effect.enabled && effect.intensity.isFinite() &&
            timeMs >= effect.startMs && timeMs < effect.endMs) {
            amount = (amount + effect.intensity.coerceIn(0f, 1f)).coerceAtMost(1f)
        }
    }
    return amount
}
