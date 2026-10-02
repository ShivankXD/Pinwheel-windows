package com.pinwheel.core.data

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class CaptionFileTextTest {
    @Test fun unicodeTextAndBomEncodingsRoundTripWithoutReplacement() {
        val text = "नमस्ते दुनिया\nمرحبا بالعالم\nHello 🌊"
        assertEquals(text, CaptionFileText.decode(text.toByteArray(Charsets.UTF_8)))
        assertEquals(text, CaptionFileText.decode(byteArrayOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte()) + text.toByteArray(Charsets.UTF_8)))
        assertEquals(text, CaptionFileText.decode(byteArrayOf(0xff.toByte(), 0xfe.toByte()) + text.toByteArray(Charsets.UTF_16LE)))
        assertEquals(text, CaptionFileText.decode(byteArrayOf(0xfe.toByte(), 0xff.toByte()) + text.toByteArray(Charsets.UTF_16BE)))
    }

    @Test fun malformedBytesAreRejectedRatherThanCorruptingCaptions() {
        assertThrows(IllegalArgumentException::class.java) { CaptionFileText.decode(byteArrayOf(0xc3.toByte(), 0x28)) }
        assertThrows(IllegalArgumentException::class.java) { CaptionFileText.decode(byteArrayOf(0xff.toByte(), 0xfe.toByte(), 0x00)) }
    }

    @Test fun oversizedInputStopsReadingAtOneBytePastLimit() {
        var reads = 0
        val source = object : InputStream() { override fun read(): Int { reads++; return 65 } }
        assertThrows(IllegalArgumentException::class.java) { CaptionFileText.read(source) }
        assertEquals(CaptionFileText.MAX_BYTES + 1, reads)
        val exact = ByteArray(CaptionFileText.MAX_BYTES) { 65 }
        assertEquals(exact.size, CaptionFileText.read(ByteArrayInputStream(exact)).length)
    }

    @Test fun zeroLengthReadFallsBackWithoutSpinning() {
        val source = object : InputStream() {
            var index = 0
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = 0
            override fun read(): Int = if (index++ < 3) 'A'.code else -1
        }
        assertEquals("AAA", CaptionFileText.read(source))
    }
}
