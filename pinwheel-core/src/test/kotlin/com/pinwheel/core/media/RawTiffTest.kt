package com.pinwheel.core.media

import com.pinwheel.core.model.Adjustments
import com.pinwheel.core.model.RawDevelopment
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CancellationException
import kotlin.math.pow
import kotlin.math.roundToInt

class RawTiffTest {
    private fun profile(): ByteArray {
        val relative = "licenses/mobile/third_party/icc/sRGB2014.icc"
        return listOf(File(relative), File("app/$relative")).first { it.isFile }.readBytes()
    }
    private data class Tag(val type: Int, val count: Int, val valueOffset: Int)
    private class Parsed(val bytes: ByteArray) {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val tags: Map<Int, Tag>
        init {
            assertEquals('I'.code, bytes[0].toInt()); assertEquals('I'.code, bytes[1].toInt())
            assertEquals(42, buffer.getShort(2).toInt())
            val start = buffer.getInt(4)
            val count = buffer.getShort(start).toInt() and 65535
            tags = (0 until count).associate { i ->
                val offset = start + 2 + i * 12
                val tag = buffer.getShort(offset).toInt() and 65535
                val type = buffer.getShort(offset + 2).toInt() and 65535
                val length = buffer.getInt(offset + 4)
                val itemSize = when(type) { 3 -> 2; 4 -> 4; 5 -> 8; else -> 1 }
                tag to Tag(type, length, if(length.toLong() * itemSize <= 4) offset + 8 else buffer.getInt(offset + 8))
            }
            assertEquals(0, buffer.getInt(start + 2 + count * 12))
        }
        fun long(tag: Int) = buffer.getInt(tags.getValue(tag).valueOffset)
        fun short(tag: Int, index: Int = 0) = buffer.getShort(tags.getValue(tag).valueOffset + index * 2).toInt() and 65535
        fun pixel(x: Int, y: Int, c: Int) = buffer.getShort(long(273) + (y * long(256) + x) * 6 + c * 2).toInt() and 65535
    }
    private fun encode(master: RawLinearImage, development: RawDevelopment = RawDevelopment()): Parsed {
        val stream = ByteArrayOutputStream()
        Tiff16Writer.write(master, development, profile(), stream)
        return Parsed(stream.toByteArray())
    }

    @Test fun genuine16BitRampRetainsFarMoreThan256LevelsAndEmbedsValidProfile() {
        val width = 4096
        val raw = RawLinearImage(width, 1, ShortArray(width * 3) { ((it / 3) * 16).toShort() })
        val tiff = encode(raw)
        // Also available to an independent TIFF decoder during release qualification.
        File("build/test-artifacts/tiff16-ramp.tif").apply { checkNotNull(parentFile).mkdirs(); writeBytes(tiff.bytes) }
        assertEquals(width, tiff.long(256)); assertEquals(1, tiff.long(257))
        assertEquals(1, tiff.short(259)); assertEquals(2, tiff.short(262))
        assertEquals(3, tiff.short(277)); assertEquals(1, tiff.short(284))
        for (c in 0..2) { assertEquals(16, tiff.short(258, c)); assertEquals(1, tiff.short(339, c)) }
        assertEquals(width * 6, tiff.long(279))
        assertEquals(tiff.bytes.size, tiff.long(273) + tiff.long(279))
        val values = (0 until width).map { tiff.pixel(it, 0, 0) }
        assertTrue(values.toSet().size > 4000)
        assertTrue(values.zipWithNext().all { (a, b) -> a <= b })
        assertTrue(values.any { it % 257 != 0 })
        val icc = tiff.tags.getValue(34675)
        val embedded = tiff.bytes.copyOfRange(icc.valueOffset, icc.valueOffset + icc.count)
        assertArrayEquals(profile(), embedded)
        val profileHeader = ByteBuffer.wrap(embedded).order(ByteOrder.BIG_ENDIAN)
        assertEquals(embedded.size, profileHeader.getInt(0))
        assertEquals("RGB ", String(embedded, 16, 4, Charsets.US_ASCII))
        assertEquals("XYZ ", String(embedded, 20, 4, Charsets.US_ASCII))
        assertEquals("acsp", String(embedded, 36, 4, Charsets.US_ASCII))
    }

    @Test fun samplesMatchIndependentSrgbFormulaAfterLinearExposureAndPreserveRowOrder() {
        val samples = intArrayOf(0, 127, 3000, 16000, 32000, 65535, 17, 27, 39, 1234, 4567, 8910)
        val tiff = encode(RawLinearImage(2, 2, ShortArray(samples.size) { samples[it].toShort() }), RawDevelopment(exposure = -1f))
        for (y in 0..1) for (x in 0..1) for (c in 0..2) {
            val linear = samples[(y * 2 + x) * 3 + c] / 65535.0 * .5
            val expected = ((if (linear <= .0031308) 12.92 * linear else 1.055 * linear.pow(1.0 / 2.4) - .055) * 65535).roundToInt()
            assertEquals(expected, tiff.pixel(x, y, c))
        }
    }

    @Test fun hdrValuesClipExplicitlyAtSdrWhiteAndNegativeExposureRetainsHighlights() {
        val master = RawLinearImage(1, 1, shortArrayOf(65535.toShort(), 32768.toShort(), 0))
        val bright = encode(master, RawDevelopment(exposure = 2f))
        assertEquals(65535, bright.pixel(0, 0, 0)); assertEquals(65535, bright.pixel(0, 0, 1))
        val dark = encode(master, RawDevelopment(exposure = -2f))
        assertTrue(dark.pixel(0, 0, 0) > dark.pixel(0, 0, 1)); assertEquals(0, dark.pixel(0, 0, 2))
    }

    @Test fun allPhotoEditsHaveAPrecisionPath() {
        assertNull(RawTiff.eligibilityIssue(Adjustments(raw = RawDevelopment(exposure = .5f))))
        assertNull(RawTiff.eligibilityIssue(Adjustments(exposure = .1f)))
        assertNull(RawTiff.eligibilityIssue(Adjustments(crop = "1:1")))
        assertNull(RawTiff.eligibilityIssue(Adjustments(lensDistortion = .1f)))
    }

    @Test fun cancellationStopsBeforeWritingAndDuringRows() {
        val master = RawLinearImage(4, 4, ShortArray(48))
        val output = ByteArrayOutputStream()
        try {
            Tiff16Writer.write(master, RawDevelopment(), profile(), output, checkCancellation = { throw CancellationException() })
            fail("Expected cancellation")
        } catch (_: CancellationException) { assertEquals(0, output.size()) }
        var cancel = false
        try {
            Tiff16Writer.write(master, RawDevelopment(), profile(), output,
                onProgress = { cancel = true }, checkCancellation = { if (cancel) throw CancellationException() })
            fail("Expected cancellation during rows")
        } catch (_: CancellationException) {
            val complete = encode(master).bytes.size
            assertTrue(output.size() < complete)
        }
    }
}
