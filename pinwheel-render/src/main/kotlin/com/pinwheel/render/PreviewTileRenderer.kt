package com.pinwheel.render

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.media.video.*
import org.jetbrains.skia.*
import java.nio.file.Files
import java.nio.file.Path

/** Mobile's preview passes, sample crop, float timing, flip and RGB565 conversion. Thread confined. */
class PreviewTileRenderer(nativeDirectory: Path, private val assets: Path, softwareDiagnostic: Boolean = false) : AutoCloseable {
    private val device = AngleDevice(nativeDirectory, softwareDiagnostic)
    val renderer get() = device.renderer
    val version get() = device.version
    private val programs = HashMap<String, FxProgram>()
    private val samples = HashMap<String, GpuTexture>()
    private val sway = FxProgram(FxProgram.SWAY)
    private val work = Array(4) { GpuTarget(device, VideoPreviewRecipe.WIDTH, VideoPreviewRecipe.HEIGHT) }
    private val cache = object : LinkedHashMap<String, Array<RgbaFrame?>>(32, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Array<RgbaFrame?>>?) = size > 40
    }
    val cachedSpecs get() = cache.size

    fun compileAll(): Map<String, String> {
        device.checkThread(); val failures = linkedMapOf<String, String>()
        catalogSpecs().forEach { spec -> try { program(spec) } catch (e: Exception) { failures[spec.id] = e.stackTraceToString() } }
        return failures
    }
    private fun program(spec: VideoFxSpec) = programs.getOrPut(spec.id) { FxProgram(FxProgram.source(spec)) }
    fun frame(spec: VideoFxSpec, index: Int): RgbaFrame {
        device.checkThread(); val slot = index.mod(VideoPreviewRecipe.FRAMES)
        val frames = cache.getOrPut(spec.id) { arrayOfNulls(VideoPreviewRecipe.FRAMES) }
        return frames[slot] ?: renderAt(spec, slot * VideoPreviewRecipe.LOOP_SECONDS / VideoPreviewRecipe.FRAMES)
            .also { frames[slot] = it }
    }
    fun renderAt(spec: VideoFxSpec, seconds: Float, values: Map<String, Float> = emptyMap()): RgbaFrame {
        device.checkThread(); require(seconds.isFinite())
        val input = VideoPreviewRecipe.inputs(spec, seconds.toDouble())
        val sample = samples.getOrPut(input.sample) { uploadSample(input.sample) }
        fun swayTo(target: Int, motion: VideoPreviewRecipe.Sway) {
            work[target].bind(); sway.use(); sway.float("uFlipY", 1f); sway.texture("uTexture", sample.id, 0)
            sway.float("uBody", motion.body); sway.float("uZoom", motion.zoom); sway.vec2("uSway", motion.x, motion.y); sway.draw()
        }
        swayTo(0, input.current)
        if (spec.history) { swayTo(1, input.previous); swayTo(2, input.trail) }
        work[3].bind()
        val p = program(spec); p.use(); p.float("uFlipY", -1f)
        p.texture("uTexture", work[0].texture.id, 0)
        p.texture("uPrev", work[if (spec.history) 1 else 0].texture.id, 1)
        p.texture("uTrail", work[if (spec.history) 2 else 0].texture.id, 2)
        p.vec2("uSize", VideoPreviewRecipe.WIDTH.toFloat(), VideoPreviewRecipe.HEIGHT.toFloat())
        p.float("uAspect", VideoPreviewRecipe.WIDTH.toFloat() / VideoPreviewRecipe.HEIGHT)
        p.float("uTime", seconds); p.float("uDuration", VideoPreviewRecipe.LOOP_SECONDS); p.float("uProgress", input.progress)
        val uniforms = spec.uniforms(values); p.vec4("uP0", uniforms); p.vec4("uP1", uniforms, 4); p.draw()
        checkGl("${spec.id} preview")
        // Mobile directly copies glReadPixels into a top-down Bitmap after uFlipY=-1.
        return rgb565(work[3].read(topDown = false))
    }
    private fun uploadSample(name: String): GpuTexture {
        Image.makeFromEncoded(Files.readAllBytes(assets.resolve(name))).use { decoded ->
            val crop = VideoPreviewRecipe.crop(decoded.width, decoded.height)
            val width = VideoPreviewRecipe.WIDTH * 2; val height = VideoPreviewRecipe.HEIGHT * 2
            Surface.makeRasterN32Premul(width, height).use { surface ->
                surface.canvas.translate(0f, height.toFloat()); surface.canvas.scale(1f, -1f)
                surface.canvas.drawImageRect(decoded,
                    Rect.makeXYWH(crop.x.toFloat(), crop.y.toFloat(), crop.width.toFloat(), crop.height.toFloat()),
                    Rect.makeWH(width.toFloat(), height.toFloat()), FilterMipmap(FilterMode.LINEAR, MipmapMode.NONE), null, true)
                surface.makeImageSnapshot().use { tile ->
                    Bitmap().use { pixels ->
                        check(pixels.allocPixels(ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL)))
                        check(tile.readPixels(pixels))
                        val bytes = requireNotNull(pixels.readPixels(pixels.imageInfo, width * 4, 0, 0))
                        return GpuTexture(device, width, height).also { it.upload(RgbaFrame(width, height, bytes), topDown = false) }
                    }
                }
            }
        }
    }
    override fun close() {
        device.checkThread(); cache.clear(); programs.values.forEach { it.close() }
        samples.values.forEach { it.close() }; work.forEach { it.close() }; sway.close(); device.close()
    }
    companion object {
        fun catalogSpecs() = VideoFxCatalog.effects + VideoFxCatalog.transitions + VideoFxCatalog.overlays + VideoFxCatalog.legacyOverlays
        /** Use Skia's actual RGB565 conversion, as Bitmap.copy does, including 5/6-bit expansion. */
        fun rgb565(frame: RgbaFrame): RgbaFrame {
            Bitmap().use { original ->
                check(original.installPixels(ImageInfo(frame.width, frame.height, ColorType.RGBA_8888, ColorAlphaType.OPAQUE), frame.pixels, frame.width * 4))
                val info = ImageInfo(frame.width, frame.height, ColorType.RGB_565, ColorAlphaType.OPAQUE)
                val packed = requireNotNull(original.readPixels(info, frame.width * 2, 0, 0))
                Bitmap().use { converted ->
                    check(converted.installPixels(info, packed, frame.width * 2))
                    return RgbaFrame(frame.width, frame.height, requireNotNull(converted.readPixels(original.imageInfo, frame.width * 4, 0, 0)))
                }
            }
        }
    }
}
