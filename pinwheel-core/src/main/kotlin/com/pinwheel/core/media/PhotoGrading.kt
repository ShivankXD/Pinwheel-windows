package com.pinwheel.core.media

import com.pinwheel.core.model.Adjustments
import kotlin.math.*

/** Three smoothly blended tonal ranges. Tint vectors preserve luminance before clipping. */
class PhotoGrading(a: Adjustments) {
    val active = a.grading.any { it.saturation != 0f || it.luminance != 0f }
    val red = FloatArray(256)
    val green = FloatArray(256)
    val blue = FloatArray(256)

    init {
        if (active) {
            val vectors = a.grading.take(3).map { tone ->
                val hue = ((tone.hue % 360) + 360) % 360 / 60f
                val second = 1f - abs(hue % 2f - 1f)
                val rgb = when (hue.toInt()) {
                    0 -> floatArrayOf(1f, second, 0f)
                    1 -> floatArrayOf(second, 1f, 0f)
                    2 -> floatArrayOf(0f, 1f, second)
                    3 -> floatArrayOf(0f, second, 1f)
                    4 -> floatArrayOf(second, 0f, 1f)
                    else -> floatArrayOf(1f, 0f, second)
                }
                val luma = .2126f * rgb[0] + .7152f * rgb[1] + .0722f * rgb[2]
                FloatArray(3) { (rgb[it] - luma) * tone.saturation * .4f }
            }
            val power = 1f + (1f - a.gradingBlending.coerceIn(0f, 1f)) * 3f
            for (i in 0..255) {
                val originalLuma = i / 255f
                val t = (originalLuma + a.gradingBalance * .25f).coerceIn(0f, 1f)
                val weights = floatArrayOf((1f-t).pow(power), (4f*t*(1f-t)).pow(power), t.pow(power))
                val total = weights.sum().coerceAtLeast(.001f)
                val headroom = 4f * originalLuma * (1f-originalLuma)
                for (band in vectors.indices) {
                    val weight = weights[band] / total * headroom
                    val lift = a.grading[band].luminance * .2f
                    red[i] += (vectors[band][0] + lift) * weight
                    green[i] += (vectors[band][1] + lift) * weight
                    blue[i] += (vectors[band][2] + lift) * weight
                }
            }
        }
    }
}
