package com.pinwheel.core.media

import com.pinwheel.core.model.Adjustments
import com.pinwheel.core.model.ColorBand
import com.pinwheel.core.model.ToneGrade
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class ColorTarget(val bandIndex: Int, val hue: Float, val argb: Int) {
    val name: String get() = PhotoColorTarget.names[bandIndex]
}

/** Band centres mirror the colour mixer's piecewise hue weights, including the red wraparound. */
object PhotoColorTarget {
    val names = listOf("Red", "Orange", "Yellow", "Green", "Aqua", "Blue", "Purple", "Magenta")
    val hues = listOf(0f, 30f, 60f, 120f, 180f, 240f, 280f, 320f)

    /** Retain the tone, white balance, channel curves and global saturation that feed HSL mixing. */
    fun samplingRecipe(a: Adjustments): Adjustments = a.copy(
        mix = List(8) { ColorBand() }, grading = List(3) { ToneGrade() },
        fade = 0f, vignette = 0f, grain = 0f, masks = emptyList(),
    )

    fun nearestBand(hue: Float): Int {
        require(hue.isFinite()) { "Hue must be finite" }
        val normalized = ((hue % 360f) + 360f) % 360f
        return hues.indices.minBy { index ->
            val distance = abs(normalized - hues[index])
            min(distance, 360f - distance)
        }
    }

    /** A neutral or nearly transparent patch has no reliable hue to target. */
    fun fromArgb(argb: Int): ColorTarget? {
        if ((argb ushr 24) < 240) return null
        val r = ((argb ushr 16) and 255) / 255f
        val g = ((argb ushr 8) and 255) / 255f
        val b = (argb and 255) / 255f
        val high = max(r, max(g, b)); val low = min(r, min(g, b))
        val chroma = high - low
        val light = (high + low) / 2f
        if (chroma < .025f || light < .025f || light > .975f) return null
        val saturation = chroma / (1f - abs(2f * light - 1f)).coerceAtLeast(.0001f)
        if (saturation < .08f) return null
        val hue = when (high) {
            r -> 60f * ((g - b) / chroma % 6f)
            g -> 60f * ((b - r) / chroma + 2f)
            else -> 60f * ((r - g) / chroma + 4f)
        }.let { (it + 360f) % 360f }
        return ColorTarget(nearestBand(hue), hue, argb)
    }
}
