package com.pinwheel.core.media

import com.pinwheel.core.model.Adjustments
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Magic Enhance: reads the photo (tonal spread, colour cast, skin, sky and foliage, haze, low light)
 * and builds a tuned edit from the regular sliders, so everything it does stays visible and editable.
 * Works on packed ARGB pixels so it runs the same on the phone and in unit tests.
 */
object PhotoMagicEnhance {
    enum class Scene(val label: String) { PORTRAIT("Portrait"), LANDSCAPE("Landscape"), NIGHT("Low light"), EVERYDAY("Everyday") }
    data class Analysis(val scene: Scene, val median: Float, val low: Float, val high: Float, val clipped: Float,
        val castWarm: Float, val castGreen: Float, val saturation: Float, val haze: Float, val skin: Float, val nature: Float)
    data class Result(val adjustments: Adjustments, val analysis: Analysis)

    private fun luma(r: Float, g: Float, b: Float) = .2126f * r + .7152f * g + .0722f * b

    fun analyse(pixels: IntArray, width: Int, height: Int): Analysis {
        val hist = IntArray(256)
        var n = 0; var skin = 0; var nature = 0; var satSum = 0.0; var clipped = 0
        var nr = 0.0; var ng = 0.0; var nb = 0.0; var neutral = 0
        var darkSum = 0.0
        val step = max(1, (width * height / 60_000.0).pow(.5).toInt())
        for (y in 0 until height step step) for (x in 0 until width step step) {
            val p = pixels[y * width + x]
            val r = ((p shr 16) and 255) / 255f; val g = ((p shr 8) and 255) / 255f; val b = (p and 255) / 255f
            val l = luma(r, g, b); hist[(l * 255).toInt().coerceIn(0, 255)]++; n++
            val mx = max(r, max(g, b)); val mn = min(r, min(g, b)); val s = if (mx > 0f) (mx - mn) / mx else 0f
            satSum += s
            if (mx > .985f) clipped++
            darkSum += mn
            // Skin: the classic YCbCr skin cluster, narrowed by hue, saturation and brightness.
            val cr = 128f + 255f * (.5f * r - .4187f * g - .0813f * b); val cb = 128f + 255f * (-.1687f * r - .3313f * g + .5f * b)
            if (cr in 140f..170f && cb in 85f..122f && mx > .3f && s in .18f..0.55f && r > g && g >= b) skin++
            // Foliage or open sky.
            if ((g > r * 1.02f && g >= b * .9f && s > .12f) || (b > r * 1.08f && y < height / 2 && l > .3f)) nature++
            // Grey-world on low-saturation midtones only, so coloured scenes are not neutralised.
            if (s < .18f && l in .2f..0.85f) { nr += r; ng += g; nb += b; neutral++ }
        }
        fun pct(f: Float): Float { var sum = 0; for (i in 0..255) { sum += hist[i]; if (sum >= n * f) return i / 255f }; return 1f }
        val median = pct(.5f); val low = pct(.02f); val high = pct(.98f)
        val castWarm = if (neutral > n / 50) ((nr - nb) / neutral).toFloat() else 0f
        val castGreen = if (neutral > n / 50) ((ng - (nr + nb) / 2) / neutral).toFloat() else 0f
        val saturation = (satSum / n).toFloat()
        val haze = ((darkSum / n).toFloat() - .08f).coerceAtLeast(0f) * (1f - (high - low)).coerceAtLeast(0f) * 4f
        val skinF = skin.toFloat() / n; val natureF = nature.toFloat() / n
        val scene = when {
            median < .2f && high < .8f -> Scene.NIGHT
            // A face is a part of the frame; a frame that is mostly "skin" is warm stone, wood or food.
            skinF in .05f..0.28f && natureF < .25f -> Scene.PORTRAIT
            natureF > .15f -> Scene.LANDSCAPE
            else -> Scene.EVERYDAY
        }
        return Analysis(scene, median, low, high, clipped.toFloat() / n, castWarm, castGreen, saturation, haze.coerceIn(0f, 1f), skinF, natureF)
    }

    /** Builds the enhanced recipe on top of [base]; geometry, masks and frames are untouched. */
    fun enhance(pixels: IntArray, width: Int, height: Int, base: Adjustments): Result {
        val a = analyse(pixels, width, height)
        val portrait = a.scene == Scene.PORTRAIT
        // Exposure: move the midtones towards a pleasing level, in linear light.
        val target = when (a.scene) { Scene.NIGHT -> .34f; Scene.PORTRAIT -> .5f; else -> .46f }
        val ev = (ln((target.toDouble().pow(2.2)) / (a.median.coerceAtLeast(.02f).toDouble().pow(2.2))) / ln(2.0) * .6).toFloat().coerceIn(-.9f, if (a.scene == Scene.NIGHT) .7f else 1f)
        val spread = a.high - a.low
        val whites = if (a.high < .9f) ((.94f - a.high) * 1.6f).coerceAtMost(.45f) else if (a.clipped > .03f) -.12f else 0f
        val blacks = if (a.low > .05f) (-(a.low - .02f) * 2.2f).coerceAtLeast(-.4f) else 0f
        val highlights = when { a.clipped > .02f -> -.4f; a.high > .9f -> -.22f; else -> -.08f }
        val shadows = when { a.low < .06f && a.median < .45f -> .32f; a.median < .3f -> .25f; else -> .1f }
        val contrast = if (spread < .55f) 1.14f else if (spread < .75f) 1.07f else 1.02f
        val warmth = (-a.castWarm * 1.6f + if (portrait) .06f else 0f).coerceIn(-.25f, .25f)
        val tint = (a.castGreen * 2f).coerceIn(-.15f, .15f)
        val vibrance = ((.34f - a.saturation * .5f) * if (portrait) .55f else 1f).coerceIn(.04f, .32f)
        val clarity = when (a.scene) { Scene.LANDSCAPE -> .22f; Scene.PORTRAIT -> .04f; Scene.NIGHT -> .06f; else -> .14f }
        val texture = when (a.scene) { Scene.PORTRAIT -> -.06f; Scene.LANDSCAPE -> .16f; else -> .1f }
        val dehaze = if (a.haze > .15f || a.scene == Scene.LANDSCAPE) (a.haze * .5f + .06f).coerceAtMost(.3f) else 0f
        val sharp = if (portrait) .12f else .26f
        val noise = if (a.scene == Scene.NIGHT) .32f else if (a.median < .3f) .15f else 0f
        val out = base.copy(exposure = base.exposure + ev, contrast = contrast, highlights = highlights, shadows = shadows, whites = whites,
            blacks = blacks, warmth = base.warmth + warmth, tint = base.tint + tint, vibrance = vibrance, clarity = clarity, texture = texture,
            dehaze = dehaze, sharpness = sharp, noiseReduction = noise, colorNoiseReduction = if (noise > 0f) .3f else base.colorNoiseReduction)
        return Result(out, a)
    }
}
