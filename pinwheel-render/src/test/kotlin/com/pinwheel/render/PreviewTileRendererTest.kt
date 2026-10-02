package com.pinwheel.render

import com.pinwheel.core.media.video.*
import java.nio.file.Path
import kotlin.test.*

class PreviewTileRendererTest {
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
