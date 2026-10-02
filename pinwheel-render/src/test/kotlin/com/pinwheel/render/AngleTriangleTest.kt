package com.pinwheel.render

import java.nio.file.Path
import kotlin.test.*

class AngleTriangleTest {
    @Test fun d3d11TriangleHasColourAndTopDownOrientation() {
        val result = AngleTriangle.render(Path.of("native/windows-x64"))
        val frame = result.frame
        assertTrue(result.renderer.contains("ANGLE") && result.renderer.contains("Direct3D11"))
        fun channel(x: Int, y: Int, c: Int) = frame.pixels[(y * frame.width + x) * 4 + c].toInt() and 255
        assertEquals(7, channel(0, 0, 0))
        assertEquals(255, channel(160, 120, 3))
        assertTrue(channel(160, 50, 1) > 180, "The green apex must be near the top")
        assertTrue(channel(160, 180, 1) < 100, "The base must have lower green")
        assertTrue(channel(160, 120, 0) > 100, "Triangle must contain actual shader pixels")
        println("PASS ANGLE D3D11 triangle, shader compile/link, RGBA readback and Y orientation: ${result.renderer}")
    }
    @Test fun freshContextCanRenderAfterRelease() {
        val first = AngleTriangle.render(Path.of("native/windows-x64"), 64, 48)
        val second = AngleTriangle.render(Path.of("native/windows-x64"), 64, 48)
        assertContentEquals(first.frame.pixels, second.frame.pixels)
    }
}
