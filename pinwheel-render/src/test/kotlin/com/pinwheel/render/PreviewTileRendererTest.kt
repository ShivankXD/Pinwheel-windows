package com.pinwheel.render

import com.pinwheel.core.media.video.*
import com.pinwheel.core.RgbaFrame
import java.nio.file.Path
import kotlin.test.*

class PreviewTileRendererTest {
    @Test fun everyRgb565ColourUsesNormalizedChannelLevels() {
        // Android's skcms PNG export normalizes the packed channel before 8-bit rounding.
        // Fixed channel ramps independently specify the format, including levels where
        // Windows Skia's low-precision bit repetition differs by one.
        val rbLevels = intArrayOf(0, 8, 16, 25, 33, 41, 49, 58, 66, 74, 82, 90, 99, 107, 115, 123,
            132, 140, 148, 156, 165, 173, 181, 189, 197, 206, 214, 222, 230, 239, 247, 255)
        val gLevels = intArrayOf(0, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40, 45, 49, 53, 57, 61,
            65, 69, 73, 77, 81, 85, 89, 93, 97, 101, 105, 109, 113, 117, 121, 125,
            130, 134, 138, 142, 146, 150, 154, 158, 162, 166, 170, 174, 178, 182, 186, 190,
            194, 198, 202, 206, 210, 215, 219, 223, 227, 231, 235, 239, 243, 247, 251, 255)
        val rgba = ByteArray(65536 * 4)
        for (colour in 0..65535) {
            val r = (colour ushr 11) and 31; val g = (colour ushr 5) and 63; val b = colour and 31
            rgba[colour * 4] = ((r shl 3) or (r ushr 2)).toByte()
            rgba[colour * 4 + 1] = ((g shl 2) or (g ushr 4)).toByte()
            rgba[colour * 4 + 2] = ((b shl 3) or (b ushr 2)).toByte()
            rgba[colour * 4 + 3] = colour.toByte() // RGB565 always discards input alpha.
        }
        val actual = PreviewTileRenderer.rgb565(RgbaFrame(256, 256, rgba)).pixels
        for (colour in 0..65535) {
            val expected = intArrayOf(rbLevels[(colour ushr 11) and 31], gLevels[(colour ushr 5) and 63], rbLevels[colour and 31], 255)
            for (channel in 0..3) assertEquals(expected[channel], actual[colour * 4 + channel].toInt() and 255,
                "RGB565 colour $colour, channel $channel differs from normalized PNG export")
        }
    }
    @Test fun everyShaderCompilesAndEveryCatalogEffectDrawsAPictureAndMoves() {
        PreviewTileRenderer(Path.of("native/windows-x64"), Path.of("assets")).use { renderer ->
            val failures = renderer.compileAll(); assertTrue(failures.isEmpty(), failures.toString())
            for (spec in PreviewTileRenderer.catalogSpecs()) {
                val frames = VideoPreviewRecipe.goldenTimes.map { renderer.renderAt(spec, it.toFloat()) }
                val f = frames[1]; val colours = HashSet<Int>()
                for (at in f.pixels.indices step 4) colours += (f.pixels[at].toInt() and 255) / 16 * 256 +
                    (f.pixels[at + 1].toInt() and 255) / 16 * 16 + (f.pixels[at + 2].toInt() and 255) / 16
                val fullFrame = spec.id.startsWith("fx-ov-") && spec.category in setOf("Background", "Scenery", "Transitions")
                assertTrue(colours.size > if (fullFrame) 0 else 12, "${spec.id} renders a flat image (${colours.size} colours)")
                assertTrue(frames.zipWithNext().any { (a, b) -> !a.pixels.contentEquals(b.pixels) }, "${spec.id} is static")
            }
        }
    }
    @Test fun paramsAffectPixelsLoopSlotsCacheAndLruStaysBounded() {
        PreviewTileRenderer(Path.of("native/windows-x64"), Path.of("assets")).use { renderer ->
            val cube = VideoFxCatalog.find("fx-cube-spin")!!
            assertFalse(renderer.renderAt(cube, 1f, mapOf("size" to 0f)).pixels.contentEquals(renderer.renderAt(cube, 1f, mapOf("size" to 1f)).pixels))
            assertSame(renderer.frame(cube, 0), renderer.frame(cube, 16))
            assertSame(renderer.frame(cube, 15), renderer.frame(cube, -1))
            for (spec in PreviewTileRenderer.catalogSpecs().take(45)) renderer.frame(spec, 0)
            assertEquals(40, renderer.cachedSpecs)
        }
    }
}
