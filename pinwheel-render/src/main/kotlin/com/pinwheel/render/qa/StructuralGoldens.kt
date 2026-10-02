package com.pinwheel.render.qa

import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp

/** Owner-requested noise diagnostics. Strict pixel metrics remain a separate, unchanged audit. */
object StructuralGoldens {
    data class Limits(val blurredMae8: Double = 2.0, val histogramWasserstein8: Double = 2.0,
        val meanError8: Double = 2.0, val meanLuminanceError8: Double = 2.0)
    data class Channel(val blurredMae8: Double, val histogramWasserstein8: Double, val meanError8: Double) {
        fun passes(limits: Limits) = blurredMae8 <= limits.blurredMae8 && histogramWasserstein8 <= limits.histogramWasserstein8 && meanError8 <= limits.meanError8
        val candidatePassed get() = passes(Limits())
    }
    data class Result(val channels: List<Channel>, val meanLuminanceReference8: Double, val meanLuminanceActual8: Double) {
        val meanLuminanceError8 get() = abs(meanLuminanceReference8 - meanLuminanceActual8)
        fun passes(limits: Limits) = channels.all { it.passes(limits) } && meanLuminanceError8 <= limits.meanLuminanceError8
        val candidatePassed get() = passes(Limits())
    }

    /** Sigma 8 px, radius 24 (3 sigma), edge clamp; no scaling, alignment or integer rounding. */
    fun compare(reference: BufferedImage, actual: BufferedImage, heatmap: Path? = null, sigma: Double = 8.0): Result {
        require(reference.width == actual.width && reference.height == actual.height) { "Golden dimensions differ" }
        require(sigma > 0 && sigma.isFinite())
        val width = reference.width; val height = reference.height; val count = width * height
        val shifts = intArrayOf(16, 8, 0, 24)
        val r = Array(4) { DoubleArray(count) }; val a = Array(4) { DoubleArray(count) }
        val rh = Array(4) { IntArray(256) }; val ah = Array(4) { IntArray(256) }
        val rm = DoubleArray(4); val am = DoubleArray(4)
        for (y in 0 until height) for (x in 0 until width) {
            val rp = reference.getRGB(x, y); val ap = actual.getRGB(x, y); val index = y * width + x
            for (c in 0..3) {
                val rv = (rp ushr shifts[c]) and 255; val av = (ap ushr shifts[c]) and 255
                r[c][index] = rv.toDouble(); a[c][index] = av.toDouble()
                rh[c][rv]++; ah[c][av]++; rm[c] += rv; am[c] += av
            }
        }
        val radius = ceil(sigma * 3).toInt()
        val kernel = DoubleArray(radius * 2 + 1) { offset -> exp(-.5 * (offset - radius) * (offset - radius) / (sigma * sigma)) }
        val norm = kernel.sum(); kernel.indices.forEach { kernel[it] /= norm }
        fun blur(input: DoubleArray): DoubleArray {
            if (input.all { it == input[0] }) return input // Exact constant alpha/colour, identical to convolution.
            val horizontal = DoubleArray(count); val result = DoubleArray(count)
            for (y in 0 until height) for (x in 0 until width) {
                var sum = 0.0
                for (i in kernel.indices) sum += input[y * width + (x + i - radius).coerceIn(0, width - 1)] * kernel[i]
                horizontal[y * width + x] = sum
            }
            for (y in 0 until height) for (x in 0 until width) {
                var sum = 0.0
                for (i in kernel.indices) sum += horizontal[(y + i - radius).coerceIn(0, height - 1) * width + x] * kernel[i]
                result[y * width + x] = sum
            }
            return result
        }
        val maximum = DoubleArray(count)
        val channels = (0..3).map { c ->
            val rb = blur(r[c]); val ab = blur(a[c]); var sum = 0.0
            for (i in 0 until count) { val error = abs(rb[i] - ab[i]); sum += error; maximum[i] = maxOf(maximum[i], error) }
            var rcdf = 0L; var acdf = 0L; var transport = 0L
            for (i in 0..255) { rcdf += rh[c][i]; acdf += ah[c][i]; transport += abs(rcdf - acdf) }
            Channel(sum / count, transport.toDouble() / count, abs(rm[c] - am[c]) / count)
        }
        heatmap?.let { path ->
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until height) for (x in 0 until width) image.setRGB(x, y,
                -0x1000000 or (minOf(maximum[y * width + x] * 16, 255.0).toInt() shl 16))
            path.parent?.let(Files::createDirectories); check(ImageIO.write(image, "png", path.toFile()))
        }
        val weights = doubleArrayOf(.2126, .7152, .0722)
        return Result(channels, weights.indices.sumOf { weights[it] * rm[it] / count }, weights.indices.sumOf { weights[it] * am[it] / count })
    }
}
