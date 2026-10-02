package com.pinwheel.core.media.video

import kotlin.math.sin

/** VideoFxPreviewRenderer's deterministic inputs. P2 will execute these passes on ANGLE. */
object VideoPreviewRecipe {
    const val WIDTH = 192
    const val HEIGHT = 240
    const val FRAMES = 16
    const val LOOP_SECONDS = 2.4f
    val goldenTimes = listOf(.3, .9, 1.5, 2.1)
    val samples = listOf("fx_samples/portrait.webp", "fx_samples/action.webp", "fx_samples/ride.webp", "fx_samples/product.webp",
        "studio_scenes/lisbon-street.webp", "fx_samples/tr-1.webp", "fx_samples/tr-2.webp", "fx_samples/tr-3.webp", "fx_samples/tr-4.webp",
        "fx_samples/tr-5.webp", "fx_samples/tr-6.webp", "fx_samples/tr-7.webp", "fx_samples/tr-8.webp", "studio_scenes/alpine-lake.webp", "fx_samples/tr-10.webp",
        "fx_samples/ov-1.webp", "fx_samples/ov-2.webp", "fx_samples/ov-3.webp", "fx_samples/ov-4.webp", "fx_samples/ov-5.webp", "fx_samples/ov-6.webp")
    data class Sway(val time: Double, val x: Float, val y: Float, val zoom: Float, val body: Float)
    data class Inputs(val sample: String, val progress: Float, val current: Sway, val previous: Sway,
        val trail: Sway, val uniforms: List<Float>)
    fun inputs(spec: VideoFxSpec, seconds: Double): Inputs {
        val time = seconds.toFloat()
        val progress = if (spec.id.startsWith("fx-tr-")) ((time / LOOP_SECONDS - .1f) / .8f).coerceIn(0f, 1f)
            else (time / (LOOP_SECONDS * .8f)).coerceIn(0f, 1f)
        val sampleIndex = if (spec.id.startsWith("fx-tr-") && progress >= .5f) 5 + (spec.sample - 5 + 3) % 10 else spec.sample
        fun sway(t: Float): Sway {
            val drift = if (spec.history) .045f else .018f
            val loop = (2 * Math.PI / LOOP_SECONDS).toFloat()
            return Sway(t.toDouble(), sin(t * loop * if (spec.history) 2f else 1f) * drift,
                sin(t * loop + 1f) * drift * .4f,
                if (spec.history) .9f else .9f - .035f * (.5f + .5f * sin(t * loop)), if (spec.history) 1f else 0f)
        }
        return Inputs(samples[sampleIndex.coerceIn(0, samples.lastIndex)], progress,
            sway(time), sway(if (spec.history) time - .07f else time), sway(if (spec.history) time - .4f else time), spec.uniforms(spec.defaults()).toList())
    }
    data class Crop(val x: Int, val y: Int, val width: Int, val height: Int)
    fun crop(width: Int, height: Int): Crop {
        require(width > 0 && height > 0)
        val aspect = WIDTH.toFloat() / HEIGHT
        val w = minOf(width, (height * aspect).toInt())
        val h = minOf(height, (w / aspect).toInt())
        return Crop((width - w) / 2, (height - h) / 3, w, h)
    }
}
