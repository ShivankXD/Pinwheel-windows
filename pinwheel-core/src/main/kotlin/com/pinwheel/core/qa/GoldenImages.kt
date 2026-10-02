package com.pinwheel.core.qa

import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.ceil

/** Compare unpremultiplied sRGB8 channels. No resampling, alignment or tolerance inflation. */
object GoldenImages {
    data class Channel(val meanAbsoluteError: Double, val percentile99: Int) {
        val passed get() = meanAbsoluteError <= 2.0 && percentile99 <= 8
    }
    data class Result(val channels: List<Channel>) { val passed get() = channels.all { it.passed } }
    fun compare(reference: BufferedImage, actual: BufferedImage, difference: Path? = null): Result {
        require(reference.width == actual.width && reference.height == actual.height) { "Golden dimensions differ" }
        val histograms = Array(4) { IntArray(256) }; val sums = LongArray(4)
        val heatmap = BufferedImage(reference.width, reference.height, BufferedImage.TYPE_INT_ARGB)
        val shifts = intArrayOf(16, 8, 0, 24)
        for (y in 0 until reference.height) for (x in 0 until reference.width) {
            val r = reference.getRGB(x, y); val a = actual.getRGB(x, y); var maximum = 0
            for (c in 0..3) {
                val error = kotlin.math.abs(((r ushr shifts[c]) and 255) - ((a ushr shifts[c]) and 255))
                histograms[c][error]++; sums[c] += error; maximum = maxOf(maximum, error)
            }
            heatmap.setRGB(x, y, -0x1000000 or (minOf(maximum * 16, 255) shl 16))
        }
        difference?.let { it.parent?.let(Files::createDirectories); check(ImageIO.write(heatmap, "png", it.toFile())) }
        val count = reference.width.toLong() * reference.height
        return Result(histograms.mapIndexed { c, hist ->
            val rank = ceil(count * .99).toLong(); var total = 0L
            val p99 = hist.indices.first { total += hist[it]; total >= rank }
            Channel(sums[c].toDouble() / count, p99)
        })
    }
}
