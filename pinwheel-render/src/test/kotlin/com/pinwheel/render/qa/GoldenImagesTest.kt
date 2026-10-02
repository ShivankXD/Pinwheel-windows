package com.pinwheel.render.qa

import java.awt.image.BufferedImage
import org.junit.jupiter.api.Test
import kotlin.test.*

class GoldenImagesTest {
    private fun image(channel: Int) = BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB).apply {
        for (y in 0 until height) for (x in 0 until width) setRGB(x, y, -0x1000000 or (channel shl 16))
    }
    @Test fun exactToleranceBoundaryPassesAndLargerMeanFails() {
        assertTrue(GoldenImages.compare(image(0), image(2)).passed)
        assertFalse(GoldenImages.compare(image(0), image(3)).passed)
    }
    @Test fun percentileIsPerChannelAndCannotHideBehindLowMean() {
        val actual = image(0); for (i in 0..199) actual.setRGB(i % 100, i / 100, -0x1000000 or (9 shl 16))
        val result = GoldenImages.compare(image(0), actual)
        assertEquals(.18, result.channels[0].meanAbsoluteError); assertEquals(9, result.channels[0].percentile99); assertFalse(result.passed)
    }
    @Test fun orientationAndDimensionsCannotBeSilentlyAligned() {
        val a = image(0); val b = image(0); a.setRGB(0, 0, -1); b.setRGB(0, 99, -1)
        val result = GoldenImages.compare(a, b); assertTrue(result.channels[0].meanAbsoluteError > 0)
        assertFails { GoldenImages.compare(a, BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)) }
    }
}
