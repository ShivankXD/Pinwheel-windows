package com.pinwheel.render

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.media.video.*
import org.jetbrains.skia.*
import java.nio.file.Files
import java.nio.file.Path

/** Mobile's preview passes, sample crop, float timing, flip and RGB565 conversion. Thread confined. */
class PreviewTileRenderer(nativeDirectory: Path, private val assets: Path, softwareDiagnostic: Boolean = false) : AutoCloseable {
    private val resources = GpuResources()
    internal val device = resources.own { AngleDevice(nativeDirectory, softwareDiagnostic) }
    val renderer get() = device.renderer
    val version get() = device.version
    private val programs = HashMap<String, FxProgram>()
    private val samples = HashMap<String, GpuTexture>()
    private val sway = resources.own { FxProgram(FxProgram.SWAY) }
    private val work = Array(4) { resources.own { GpuTarget(device, VideoPreviewRecipe.WIDTH, VideoPreviewRecipe.HEIGHT) } }
    private val cache = object : LinkedHashMap<String, Array<RgbaFrame?>>(32, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Array<RgbaFrame?>>?) = size > 40
    }
    val cachedSpecs get() = cache.size
    init { resources.initialized() }

    fun compileAll(): Map<String, String> {
        device.checkThread(); val failures = linkedMapOf<String, String>()
        catalogSpecs().forEach { spec -> try { program(spec) } catch (e: Exception) { failures[spec.id] = e.stackTraceToString() } }
        return failures
    }
    private fun program(spec: VideoFxSpec) = programs.getOrPut(spec.id) { resources.own { FxProgram(FxProgram.source(spec)) } }
    fun frame(spec: VideoFxSpec, index: Int): RgbaFrame {
        device.checkThread(); val slot = index.mod(VideoPreviewRecipe.FRAMES)
        val frames = cache.getOrPut(spec.id) { arrayOfNulls(VideoPreviewRecipe.FRAMES) }
        return frames[slot] ?: renderAt(spec, slot * VideoPreviewRecipe.LOOP_SECONDS / VideoPreviewRecipe.FRAMES)
            .also { frames[slot] = it }
    }
    fun renderAt(spec: VideoFxSpec, seconds: Float, values: Map<String, Float> = emptyMap()): RgbaFrame {
        drawTo(work[3], spec, seconds, values)
        // Mobile directly copies glReadPixels into a top-down Bitmap after uFlipY=-1.
        return rgb565(work[3].read(topDown = false))
    }
    /** QA can isolate attachment conversion while keeping sample preparation and shader inputs identical. */
    internal fun drawTo(output: GpuTarget, spec: VideoFxSpec, seconds: Float, values: Map<String, Float> = emptyMap()) {
        device.checkThread(); require(seconds.isFinite())
        require(output.texture.device === device && output.texture.width == VideoPreviewRecipe.WIDTH && output.texture.height == VideoPreviewRecipe.HEIGHT)
        val input = VideoPreviewRecipe.inputs(spec, seconds.toDouble())
        val sample = samples.getOrPut(input.sample) { resources.own { uploadSample(input.sample) } }
        fun swayTo(target: Int, motion: VideoPreviewRecipe.Sway) {
            work[target].bind(); sway.use(); sway.float("uFlipY", 1f); sway.texture("uTexture", sample.id, 0)
            sway.float("uBody", motion.body); sway.float("uZoom", motion.zoom); sway.vec2("uSway", motion.x, motion.y); sway.draw()
        }
        swayTo(0, input.current)
        if (spec.history) { swayTo(1, input.previous); swayTo(2, input.trail) }
        output.bind()
        val p = program(spec); p.use(); p.float("uFlipY", -1f)
        p.texture("uTexture", work[0].texture.id, 0)
        p.texture("uPrev", work[if (spec.history) 1 else 0].texture.id, 1)
        p.texture("uTrail", work[if (spec.history) 2 else 0].texture.id, 2)
        p.vec2("uSize", VideoPreviewRecipe.WIDTH.toFloat(), VideoPreviewRecipe.HEIGHT.toFloat())
        p.float("uAspect", VideoPreviewRecipe.WIDTH.toFloat() / VideoPreviewRecipe.HEIGHT)
        p.float("uTime", seconds); p.float("uDuration", VideoPreviewRecipe.LOOP_SECONDS); p.float("uProgress", input.progress)
        val uniforms = spec.uniforms(values); p.vec4("uP0", uniforms); p.vec4("uP1", uniforms, 4); p.draw()
        checkGl("${spec.id} preview")
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
                        val texture = GpuTexture(device, width, height)
                        try { texture.upload(RgbaFrame(width, height, bytes), topDown = false); return texture }
                        catch (failure: Throwable) { texture.close(); throw failure }
                    }
                }
            }
        }
    }
    override fun close() {
        device.checkThread(); cache.clear(); resources.close()
    }
    companion object {
        fun catalogSpecs() = VideoFxCatalog.effects + VideoFxCatalog.transitions + VideoFxCatalog.overlays + VideoFxCatalog.legacyOverlays
        /** Bitmap.copy quantization followed by the normalized RGB565 expansion used by Android PNG export. */
        fun rgb565(frame: RgbaFrame): RgbaFrame {
            Bitmap().use { original ->
                check(original.installPixels(ImageInfo(frame.width, frame.height, ColorType.RGBA_8888, ColorAlphaType.OPAQUE), frame.pixels, frame.width * 4))
                val info = ImageInfo(frame.width, frame.height, ColorType.RGB_565, ColorAlphaType.OPAQUE)
                val packed = requireNotNull(original.readPixels(info, frame.width * 2, 0, 0))
                val rgba = ByteArray(frame.pixels.size)
                for (pixel in 0 until frame.width * frame.height) {
                    // Windows x64 RGB565 is little-endian. Keep Skia's packed values;
                    // its low-precision 565 -> 8888 readPixels repeats bits, whereas
                    // Android's skcms PNG encoder rounds normalized 5/6-bit channels.
                    val colour = (packed[pixel * 2].toInt() and 255) or ((packed[pixel * 2 + 1].toInt() and 255) shl 8)
                    val r = (colour ushr 11) and 31; val g = (colour ushr 5) and 63; val b = colour and 31
                    rgba[pixel * 4] = ((r * 255 + 15) / 31).toByte()
                    rgba[pixel * 4 + 1] = ((g * 255 + 31) / 63).toByte()
                    rgba[pixel * 4 + 2] = ((b * 255 + 15) / 31).toByte()
                    rgba[pixel * 4 + 3] = 255.toByte()
                }
                return RgbaFrame(frame.width, frame.height, rgba)
            }
        }
    }
}
