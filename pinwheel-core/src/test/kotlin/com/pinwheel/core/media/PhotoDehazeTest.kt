package com.pinwheel.core.media

import com.pinwheel.core.model.Adjustments
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class PhotoDehazeTest {
    private fun channels(pixel: Int) = floatArrayOf(((pixel ushr 16) and 255) / 255f, ((pixel ushr 8) and 255) / 255f, (pixel and 255) / 255f)

    @Test fun zeroStrengthIsAnExactIdentityAndInvalidStrengthIsNeutral() {
        for (amount in listOf(0f, Float.NaN, Float.POSITIVE_INFINITY)) {
            val transform = PhotoColorTransform(Adjustments(dehaze = amount))
            for (r in 0..255 step 17) for (g in 0..255 step 17) for (b in 0..255 step 17) {
                val pixel = (123 shl 24) or (r shl 16) or (g shl 8) or b
                assertEquals(pixel, transform.pixel(pixel, .2f, .8f))
            }
        }
    }

    @Test fun removalRestoresColourSeparationWithoutChangingHueOrAlpha() {
        val source = 0x79a0c0e0
        val input = channels(source)
        val outputPixel = PhotoColorTransform(Adjustments(dehaze = .8f)).pixel(source, .4f, .6f)
        val output = channels(outputPixel)
        assertEquals(source ushr 24, outputPixel ushr 24)
        assertTrue(output[2] - output[0] > input[2] - input[0])
        assertTrue(output.all { it > 0f && it < 1f })
        val initialRatio = (input[1] - input[0]) / (input[2] - input[0])
        val finalRatio = (output[1] - output[0]) / (output[2] - output[0])
        assertEquals(initialRatio, finalRatio, .015f)
    }

    @Test fun grayscaleRemainsOrderedAndNeutralWithProtectedEndpoints() {
        for (amount in listOf(.25f, .7f, 1f)) {
            val transform = PhotoColorTransform(Adjustments(dehaze = amount))
            assertEquals(0xff000000.toInt(), transform.pixel(0xff000000.toInt(), 0f, 0f))
            assertEquals(-1, transform.pixel(-1, 0f, 0f))
            var previous = -1
            for (v in 0..255) {
                val value = transform.pixel(0xff000000.toInt() or (v * 0x010101), .5f, .5f)
                val r = (value ushr 16) and 255
                assertEquals(r, (value ushr 8) and 255); assertEquals(r, value and 255)
                assertTrue("No tone reversals at $v", r >= previous)
                previous = r
            }
        }
    }

    @Test fun negativeStrengthAddsVeilAndRetainsWhite() {
        val soft = PhotoColorTransform(Adjustments(dehaze = -.8f))
        val input = channels(0xff304080.toInt())
        val output = channels(soft.pixel(0xff304080.toInt(), .5f, .5f))
        assertTrue(output.indices.all { output[it] > input[it] })
        assertTrue(output[2] - output[0] < input[2] - input[0])
        assertEquals(-1, soft.pixel(-1, .5f, .5f))
    }

    @Test fun whiteBalanceSamplingIncludesTheSameDehazeStage() {
        for (amount in listOf(-.8f, .35f, 1f)) {
            val transform = PhotoColorTransform(Adjustments(dehaze = amount, exposure = .25f, contrast = 1.1f))
            val sampled = transform.beforeWhiteBalance(120f, 150f, 190f)
            val rendered = channels(transform.pixel(0xff7896be.toInt(), .5f, .5f))
            assertTrue(sampled.indices.all { abs(sampled[it] - rendered[it] * 255f) <= 1f })
        }
    }

    @Test fun correctionIsIndependentOfImagePositionAndTargetSamplingKeepsIt() {
        val a = Adjustments(dehaze = .6f)
        val transform = PhotoColorTransform(a)
        assertEquals(transform.pixel(0xff96b4d2.toInt(), 0f, 0f), transform.pixel(0xff96b4d2.toInt(), 1f, 1f))
        assertEquals(.6f, PhotoColorTarget.samplingRecipe(a).dehaze, 0f)
    }
}
