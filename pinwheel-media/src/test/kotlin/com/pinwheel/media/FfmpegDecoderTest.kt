package com.pinwheel.media

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.*

class FfmpegDecoderTest {
    private val decoder = FfmpegDecoder(Path.of("native/windows-x64"))
    @Test fun bundledRuntimeIsSharedLgpl() {
        val config = decoder.configuration()
        assertTrue(config.contains("--enable-shared"))
        for (flag in listOf("--enable-gpl", "--enable-nonfree", "--enable-libx264", "--enable-libx265")) assertFalse(config.contains(flag), flag)
    }
    @Test fun decodesRealDemoAtTwoTimesWithoutChangingSource() {
        val source = Path.of("assets/demo.mp4")
        fun hash() = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source))
        val before = hash()
        val first = decoder.frame(source, 0)
        val second = decoder.frame(source, 1_000_000)
        assertEquals(960, first.width); assertEquals(720, first.height)
        assertEquals(960 * 720 * 4, first.pixels.size)
        assertTrue(first.pixels.asSequence().filterIndexed { index, _ -> index % 4 != 3 }.any { (it.toInt() and 255) > 32 })
        assertFalse(first.pixels.contentEquals(second.pixels), "Demo must move")
        assertContentEquals(before, hash())
        println("PASS FFmpeg demo decode at 0 and 1 s: 960x720 RGBA; original SHA-256 unchanged")
    }
    @Test fun rejectsMissingMediaAndInvalidTimes() {
        assertFailsWith<IllegalArgumentException> { decoder.frame(Path.of("assets/missing.mp4")) }
        assertFailsWith<IllegalArgumentException> { decoder.frame(Path.of("assets/demo.mp4"), -1) }
    }
}
