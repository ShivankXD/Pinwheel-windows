package com.pinwheel.core.media

import org.junit.Assert.*
import org.junit.Test
import com.pinwheel.core.model.Adjustments
import com.pinwheel.core.model.ColorBand

class PhotoColorTargetTest {
    @Test fun samplingTracksChannelCurvesButDoesNotChaseItsOwnHueEdits() {
        val source = 0xff0000ff.toInt()
        val a = Adjustments(greenCurve = List(5) { .6f }, mix = List(8) { ColorBand(hue = 1f) })
        val targetPixel = PhotoColorTransform(PhotoColorTarget.samplingRecipe(a)).pixel(source, .5f, .5f)
        val displayedPixel = PhotoColorTransform(a).pixel(source, .5f, .5f)
        assertEquals(5, PhotoColorTarget.fromArgb(source)?.bandIndex)
        assertEquals(4, PhotoColorTarget.fromArgb(targetPixel)?.bandIndex)
        assertEquals(5, PhotoColorTarget.fromArgb(displayedPixel)?.bandIndex)
    }

    @Test fun primaryAndSecondaryColoursSelectTheExpectedMixerBands() {
        val examples = listOf(
            0xffff0000.toInt() to 0, 0xffff8000.toInt() to 1, 0xffffff00.toInt() to 2,
            0xff00ff00.toInt() to 3, 0xff00ffff.toInt() to 4, 0xff0000ff.toInt() to 5,
            0xffaa00ff.toInt() to 6, 0xffff00aa.toInt() to 7,
        )
        examples.forEach { (argb, band) -> assertEquals("Colour ${argb.toUInt().toString(16)}", band, PhotoColorTarget.fromArgb(argb)?.bandIndex) }
    }

    @Test fun redWrapAndUnequalBandSpacingMatchLargestEngineWeight() {
        assertEquals(0, PhotoColorTarget.nearestBand(359f))
        assertEquals(0, PhotoColorTarget.nearestBand(-8f))
        assertEquals(1, PhotoColorTarget.nearestBand(385f))
        assertEquals(2, PhotoColorTarget.nearestBand(89f))
        assertEquals(3, PhotoColorTarget.nearestBand(91f))
        assertEquals(5, PhotoColorTarget.nearestBand(259f))
        assertEquals(6, PhotoColorTarget.nearestBand(261f))
        assertTrue(runCatching { PhotoColorTarget.nearestBand(Float.NaN) }.isFailure)
    }

    @Test fun neutralTransparentAndClippedSamplesDoNotPretendToHaveAHue() {
        listOf(0xff000000.toInt(), 0xffffffff.toInt(), 0xff808080.toInt(), 0xff808181.toInt(),
            0x80ff0000.toInt(), 0x00ff0000, 0xff020000.toInt(), 0xfffffffc.toInt()).forEach {
            assertNull("Unreliable colour ${it.toUInt().toString(16)}", PhotoColorTarget.fromArgb(it))
        }
        assertNotNull(PhotoColorTarget.fromArgb(0xff89b5d1.toInt()))
        assertNotNull(PhotoColorTarget.fromArgb(0xffd2a782.toInt()))
    }
}
