package com.pinwheel.render

import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.*

class PreviewSamplesTest {
    @Test fun mobileCropFilteringIncludesNeighbouringSourcePixelsAtBothEdges() {
        val input = Path.of("pinwheel-render/build/sample-test-inputs"); Files.createDirectories(input)
        fun colour(pixels: ByteArray, width: Int, x: Int, y: Int) =
            (0..3).map { pixels[(y * width + x) * 4 + it].toInt() and 255 }
        val horizontal = BufferedImage(200, 240, BufferedImage.TYPE_INT_ARGB)
        // Crop x=4..195 at scale 2: the outside neighbour contributes exactly one quarter.
        for (y in 0 until 240) for (x in 0 until 200) horizontal.setRGB(x, y,
            when { x < 4 -> 0xff800000.toInt(); x >= 196 -> 0xff008000.toInt(); else -> 0xff000080.toInt() })
        check(ImageIO.write(horizontal, "png", input.resolve("crop-border-horizontal.png").toFile()))
        val h = PreviewSamples.read(input, "crop-border-horizontal.png")
        // Quarter-pixel centres and 128-valued colours avoid fractional filter-rounding ambiguity.
        assertEquals(listOf(32, 0, 96, 255), colour(h.pixels, h.width, 0, h.height / 2))
        assertEquals(listOf(0, 32, 96, 255), colour(h.pixels, h.width, h.width - 1, h.height / 2))
        assertEquals(listOf(0, 0, 128, 255), colour(h.pixels, h.width, h.width / 2, h.height / 2))

        val vertical = BufferedImage(192, 248, BufferedImage.TYPE_INT_ARGB)
        // Third-height positioning selects y=2..241; the result flips top/bottom.
        for (y in 0 until 248) for (x in 0 until 192) vertical.setRGB(x, y,
            when { y < 2 -> 0xff800000.toInt(); y >= 242 -> 0xff008000.toInt(); else -> 0xff000080.toInt() })
        check(ImageIO.write(vertical, "png", input.resolve("crop-border-vertical.png").toFile()))
        val v = PreviewSamples.read(input, "crop-border-vertical.png")
        assertEquals(listOf(0, 32, 96, 255), colour(v.pixels, v.width, v.width / 2, 0))
        assertEquals(listOf(32, 0, 96, 255), colour(v.pixels, v.width, v.width / 2, v.height - 1))
        assertEquals(listOf(0, 0, 128, 255), colour(v.pixels, v.width, v.width / 2, v.height / 2))
    }

    @Test fun mobileMatrixKeepsMappedFloatBoundsBeforeRoundingBitmapSize() {
        val portrait = PreviewSamples.mapping(750, 900)
        assertEquals(15, portrait.crop.x); assertEquals(0, portrait.crop.y)
        assertEquals(384.00003f, portrait.mappedWidth)
        assertEquals(480.00003f, portrait.mappedHeight)
        assertEquals(384, portrait.width); assertEquals(480, portrait.height)
        val landscape = PreviewSamples.mapping(1280, 853)
        assertEquals(299, landscape.crop.x); assertEquals(0, landscape.crop.y)
        assertEquals(384f, landscape.mappedWidth); assertEquals(480.00003f, landscape.mappedHeight)
        assertEquals(384, landscape.width); assertEquals(480, landscape.height)
    }

    @Test fun preparedSampleUsesThirdHeightCropAndFlipsRowsWithoutChangingOpaqueColours() {
        val input = Path.of("pinwheel-render/build/sample-test-inputs"); Files.createDirectories(input)
        val image = BufferedImage(64, 160, BufferedImage.TYPE_INT_ARGB)
        // Crop is y=26..105. Its upper and lower solid halves remain well away from filter edges.
        for (y in 0 until 160) for (x in 0 until 64) image.setRGB(x, y,
            if (y < 66) 0xffff0000.toInt() else 0xff0000ff.toInt())
        check(ImageIO.write(image, "png", input.resolve("asymmetric.png").toFile()))
        val result = PreviewSamples.read(input, "asymmetric.png")
        assertEquals(384, result.width); assertEquals(480, result.height)
        fun colour(y: Int) = (0..3).map { result.pixels[(y * result.width + result.width / 2) * 4 + it].toInt() and 255 }
        assertEquals(listOf(0, 0, 255, 255), colour(10))
        assertEquals(listOf(255, 0, 0, 255), colour(470))
    }
}
