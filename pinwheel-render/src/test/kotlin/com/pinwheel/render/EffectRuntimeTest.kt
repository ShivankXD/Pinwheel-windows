package com.pinwheel.render

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.media.video.*
import com.pinwheel.core.model.*
import java.nio.file.Path
import java.util.concurrent.Executors
import kotlin.test.*

class EffectRuntimeTest {
    private val native = Path.of("native/windows-x64")
    private fun picture(width: Int = 64, height: Int = 80, seed: Int = 0) = RgbaFrame(width, height,
        ByteArray(width * height * 4) { at -> val pixel = at / 4; val x = pixel % width; val y = pixel / width
            when (at % 4) { 0 -> ((x * 3 + seed) % 256).toByte(); 1 -> ((y * 3 + seed) % 256).toByte(); 2 -> 45; else -> -1 } })
    private fun project(effects: List<VideoTimedEffect> = emptyList(), images: List<VideoImageOverlay> = emptyList()) =
        StudioProject(name = "GPU contract", kind = ProjectKind.VIDEO, clips = listOf(Clip(uri = "asset:demo", name = "demo", sourceDurationMs = 8000)),
            video = VideoProjectEdits(effects = effects, images = images))

    @Test fun textureCopyKeepsAsymmetricTopDownRowsAndExactPixels() {
        AngleDevice(native).use { device ->
            GpuTexture(device, 64, 80).use { input ->
                input.upload(picture())
                FxChain(device, 64, 80, emptyList()).use { chain -> assertContentEquals(picture().pixels, chain.render(input, 0).read().pixels) }
            }
        }
    }
    @Test fun activeChainUsesListOrderAndStartInclusiveEndExclusive() {
        val effects = listOf(VideoTimedEffect(kind = "fx-fade-out", startMs = 1000, endMs = 3000),
            VideoTimedEffect(kind = "fx-ct-negative", startMs = 1000, endMs = 3000))
        AngleDevice(native).use { device -> GpuTexture(device, 64, 80).use { input ->
            input.upload(picture())
            FxChain(device, 64, 80, effects).use { chain ->
                assertContentEquals(picture().pixels, chain.render(input, 999_999).read().pixels)
                // Negative bursts only when fract(localTime * .8) is .20..27.
                val actual = chain.render(input, 2_550_000).read().pixels
                FxChain(device, 64, 80, effects.take(1)).use { first -> FxChain(device, 64, 80, effects.drop(1)).use { second ->
                    assertContentEquals(actual, second.render(first.render(input, 2_550_000).texture, 2_550_000).read().pixels)
                } }
                FxChain(device, 64, 80, effects.reversed()).use { reversed -> assertFalse(actual.contentEquals(reversed.render(input, 2_550_000).read().pixels)) }
                assertContentEquals(picture().pixels, chain.render(input, 3_000_000).read().pixels)
            }
        } }
    }
    @Test fun historyResetsOnBackwardSeekAndJumpsOver250ms() {
        val effect = VideoTimedEffect(kind = "fx-datamosh", endMs = 8000)
        AngleDevice(native).use { device -> GpuTexture(device, 64, 80).use { input ->
            input.upload(picture())
            FxChain(device, 64, 80, listOf(effect)).use { chain ->
                chain.render(input, 0); assertEquals(1, chain.historyResets)
                input.upload(picture(seed = 30)); chain.render(input, 250_000); assertEquals(1, chain.historyResets)
                val actual = chain.render(input, 500_001).read().pixels; assertEquals(2, chain.historyResets)
                FxChain(device, 64, 80, listOf(effect)).use { fresh -> assertContentEquals(actual, fresh.render(input, 500_001).read().pixels) }
                chain.render(input, 200_000); assertEquals(3, chain.historyResets)
                chain.resetHistory(); chain.render(input, 200_000); assertEquals(4, chain.historyResets)
            }
        } }
    }
    @Test fun transitionsUseClipDurationsSpeedAndClippedHalfWindows() {
        val clips = listOf(Clip(uri = "a", name = "a", sourceDurationMs = 4000, video = VideoClipEdits(speed = 2f, transition = "fx-tr-pull-in", transitionDurationMs = 3000, transitionSpeed = .9f)),
            Clip(uri = "b", name = "b", sourceDurationMs = 800))
        val plan = EffectPlan.from(project().copy(clips = clips)); val cut = plan.transitions.single()
        assertEquals(1600, cut.startMs); assertEquals(2400, cut.endMs); assertEquals(.9f, cut.params["speed"])
        assertTrue(EffectPlan.from(project().copy(clips = clips.map { it.copy(endMs = 40) })).transitions.isEmpty())
    }
    @Test fun overlayIntensityAndOrphanTargetMatchMobileGraph() {
        val layer = VideoImageOverlay(id = "ov", uri = "overlay:fx-ov-glitter-rain", name = "Glitter", opacity = .4f)
        val fx = VideoTimedEffect(kind = "fx-fade-out", target = "deleted")
        val plan = EffectPlan.from(project(listOf(fx), listOf(layer)))
        assertEquals(listOf(fx), plan.main)
        val defaults = VideoFxCatalog.find("fx-ov-glitter-rain")!!.defaults()
        assertEquals((defaults["intensity"] ?: .85f) * .4f, plan.overlays.single().params["intensity"])
    }
    @Test fun layerFadeChangesCoverageAndLeavesBlueMainUntouched() {
        val layer = VideoImageOverlay(id = "img", uri = "white.png", name = "White", endMs = 4000)
        val fx = VideoTimedEffect(kind = "fx-fade-out", target = layer.id, endMs = 4000)
        val blue = RgbaFrame(64, 80, ByteArray(64 * 80 * 4) { if (it % 4 >= 2) -1 else 0 })
        val white = RgbaFrame(64, 80, ByteArray(64 * 80 * 4) { at -> val x = at / 4 % 64; val y = at / 4 / 64
            if (x in 20..44 && y in 25..55) -1 else 0 })
        EffectRuntime(native, 64, 80, project(listOf(fx), listOf(layer))).use { runtime ->
            val early = runtime.render(blue, 300_000, mapOf(layer.id to white))
            val late = runtime.render(blue, 3_600_000, mapOf(layer.id to white))
            fun c(f: RgbaFrame, x: Int, y: Int, channel: Int) = f.pixels[(y * 64 + x) * 4 + channel].toInt() and 255
            assertTrue(c(early, 32, 40, 0) > 190); assertTrue(c(early, 32, 40, 1) > 190)
            assertTrue(c(late, 32, 40, 2) > 190); assertTrue(c(late, 32, 40, 0) < 70); assertTrue(c(late, 32, 40, 1) < 70)
            assertEquals(255, c(late, 2, 2, 2)); assertEquals(0, c(late, 2, 2, 0))
            assertContentEquals(blue.pixels, runtime.render(blue, 4_000_000, mapOf(layer.id to white)).pixels)
        }
    }
    @Test fun allLegacyKindsDrawAndAnimateWithCompositionTime() {
        for (kind in VIDEO_EFFECT_KINDS) {
            EffectRuntime(native, 64, 80, project(listOf(VideoTimedEffect(kind = kind, intensity = .8f)))).use { runtime ->
                val a = runtime.render(picture(), 300_000); val b = runtime.render(picture(), 900_000)
                assertFalse(a.pixels.contentEquals(picture().pixels), "$kind did not style the image")
                assertContentEquals(a.pixels, runtime.render(picture(), 300_000).pixels, "$kind depends on call order")
                if (kind in listOf("Grain", "3D Tilt")) assertFalse(a.pixels.contentEquals(b.pixels), "$kind does not animate")
            }
        }
    }
    @Test fun contextRejectsOtherThreadsAndOneContextClosingDoesNotTerminateAnother() {
        val worker = Executors.newSingleThreadExecutor()
        try {
            AngleDevice(native).use { device ->
                assertTrue(worker.submit<Boolean> { runCatching { device.checkThread() }.isFailure }.get())
                val ready = java.util.concurrent.CountDownLatch(1); val keep = java.util.concurrent.CountDownLatch(1)
                val result = worker.submit<Boolean> { AngleDevice(native).use { other ->
                    GpuTexture(other, 64, 80).use { input -> input.upload(picture()); ready.countDown(); keep.await()
                        FxChain(other, 64, 80, emptyList()).use { it.render(input, 0).read().pixels.contentEquals(picture().pixels) }
                    }
                } }
                ready.await(); device.close(); keep.countDown(); assertTrue(result.get())
            }
        } finally { worker.shutdownNow() }
    }
}
