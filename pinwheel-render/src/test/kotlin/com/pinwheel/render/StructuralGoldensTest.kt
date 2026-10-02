package com.pinwheel.render

import com.pinwheel.render.qa.GoldenImages
import com.pinwheel.render.qa.StructuralGoldens
import java.awt.image.BufferedImage
import kotlin.test.*
import org.junit.jupiter.api.Test

class StructuralGoldensTest {
    private fun image(value: (Int, Int) -> Int) = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB).apply {
        for (y in 0 until height) for (x in 0 until width) { val c = value(x, y); setRGB(x, y, -0x1000000 or (c shl 16) or (c shl 8) or c) }
    }
    @Test fun relocatedFineNoiseCanPassStructureWhileStrictPixelsFail() {
        val reference = image { x, y -> if ((x + y) % 2 == 0) 0 else 100 }
        val actual = image { x, y -> if ((x + y) % 2 == 0) 100 else 0 }
        assertFalse(GoldenImages.compare(reference, actual).passed)
        val metrics = StructuralGoldens.compare(reference, actual)
        assertTrue(metrics.candidatePassed)
        assertEquals(0.0, metrics.channels[0].histogramWasserstein8)
        assertEquals(0.0, metrics.meanLuminanceError8)
    }
    @Test fun blurCannotHideAGlobalColourOrBranchError() {
        val metrics = StructuralGoldens.compare(image { _, _ -> 50 }, image { _, _ -> 53 })
        assertFalse(metrics.candidatePassed)
        for (channel in metrics.channels.take(3)) {
            assertEquals(3.0, channel.blurredMae8)
            assertEquals(3.0, channel.histogramWasserstein8)
            assertEquals(3.0, channel.meanError8)
        }
        assertEquals(3.0, metrics.meanLuminanceError8, 1e-10)
    }
    @Test fun histogramTransportUsesChannelUnitsAndDimensionsStayStrict() {
        val metrics = StructuralGoldens.compare(image { x, _ -> if (x < 32) 0 else 100 }, image { _, _ -> 100 })
        assertEquals(50.0, metrics.channels[0].histogramWasserstein8)
        assertEquals(50.0, metrics.channels[0].meanError8)
        assertFailsWith<IllegalArgumentException> { StructuralGoldens.compare(image { _, _ -> 0 }, BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)) }
    }
}
