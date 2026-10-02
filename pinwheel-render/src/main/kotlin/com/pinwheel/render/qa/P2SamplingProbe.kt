package com.pinwheel.render.qa

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.media.video.*
import com.pinwheel.render.*
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import javax.imageio.ImageIO
import org.json.JSONArray
import org.json.JSONObject
import org.lwjgl.BufferUtils
import org.lwjgl.opengles.GLES20.*
import org.lwjgl.opengles.ANGLETranslatedShaderSource.*

private fun precisionMetadata(): JSONArray = JSONArray(listOf(GL_HIGH_FLOAT, GL_MEDIUM_FLOAT, GL_LOW_FLOAT).map { type ->
    // LWJGL 3.3.6's generated guard requires two slots for this output; GLES writes slot zero.
    val range = BufferUtils.createIntBuffer(2); val precision = BufferUtils.createIntBuffer(2)
    glGetShaderPrecisionFormat(GL_FRAGMENT_SHADER, type, range, precision)
    checkGl("fragment precision query")
    JSONObject().put("type", type).put("rangeMin", range[0]).put("rangeMax", range[1]).put("precisionBits", precision[0])
})

/** Read-only compiler evidence; never replaces the original GLSL with translated text. */
private fun recordTranslation(output: Path, backend: String, id: String): JSONObject {
    val result = JSONObject().put("available", requireNotNull(glGetString(GL_EXTENSIONS)).split(' ').contains("GL_ANGLE_translated_shader_source"))
    if (!result.getBoolean("available")) return result
    val count = BufferUtils.createIntBuffer(1); val shaders = BufferUtils.createIntBuffer(2)
    glGetAttachedShaders(glGetInteger(GL_CURRENT_PROGRAM), count, shaders)
    val paths = JSONArray()
    for (index in 0 until count[0]) {
        val shader = shaders[index]
        val stage = if (glGetShaderi(shader, GL_SHADER_TYPE) == GL_VERTEX_SHADER) "vertex" else "fragment"
        val source = glGetTranslatedShaderSourceANGLE(shader)
        if (source.isNotEmpty()) {
            val path = output.resolve("translated/$backend/$id-$stage.hlsl"); Files.createDirectories(path.parent)
            Files.writeString(path, source); paths.put(path.fileName.toString())
        }
    }
    checkGl("translated shader query")
    return result.put("files", paths)
}

private fun samplingHash(frame: RgbaFrame) = MessageDigest.getInstance("SHA-256").digest(frame.pixels)
    .joinToString("") { "%02x".format(it.toInt() and 255) }

/** Read the already-prepared input texture, preserving its stored row order and GL bindings. */
private fun capturePrepared(device: AngleDevice): RgbaFrame {
    val program = glGetInteger(GL_CURRENT_PROGRAM)
    val location = glGetUniformLocation(program, "uTexture"); check(location >= 0)
    val unit = BufferUtils.createIntBuffer(1); glGetUniformiv(program, location, unit)
    val active = glGetInteger(GL_ACTIVE_TEXTURE); val activeBinding = glGetInteger(GL_TEXTURE_BINDING_2D)
    glActiveTexture(GL_TEXTURE0 + unit[0])
    val source = glGetInteger(GL_TEXTURE_BINDING_2D); check(source != 0)
    glActiveTexture(active)
    val framebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING)
    try {
        GpuTarget(device, VideoPreviewRecipe.WIDTH, VideoPreviewRecipe.HEIGHT).use { capture ->
            capture.bind()
            // The temporary FBO borrows this attachment; only its own unused texture is disposed.
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, source, 0)
            check(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE)
            return capture.read(topDown = false)
        }
    } finally {
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer)
        glActiveTexture(active); glBindTexture(GL_TEXTURE_2D, activeBinding)
    }
}

/** Execute the preserved catalog shader with supplied prepared bytes, without running SWAY again. */
private fun drawPrepared(device: AngleDevice, spec: VideoFxSpec, time: Double, frame: RgbaFrame): RgbaFrame {
    check(!spec.history)
    GpuTexture(device, frame.width, frame.height).use { texture ->
        texture.upload(frame, topDown = false)
        GpuTarget(device, VideoPreviewRecipe.WIDTH, VideoPreviewRecipe.HEIGHT).use { target ->
            FxProgram(FxProgram.source(spec)).use { program ->
                target.bind(); program.use(); program.float("uFlipY", -1f)
                program.texture("uTexture", texture.id, 0); program.texture("uPrev", texture.id, 1); program.texture("uTrail", texture.id, 2)
                program.vec2("uSize", VideoPreviewRecipe.WIDTH.toFloat(), VideoPreviewRecipe.HEIGHT.toFloat())
                program.float("uAspect", VideoPreviewRecipe.WIDTH.toFloat() / VideoPreviewRecipe.HEIGHT)
                program.float("uTime", time.toFloat()); program.float("uDuration", VideoPreviewRecipe.LOOP_SECONDS)
                program.float("uProgress", VideoPreviewRecipe.inputs(spec, time).progress)
                val uniforms = spec.uniforms(emptyMap()); program.vec4("uP0", uniforms); program.vec4("uP1", uniforms, 4)
                program.draw(); checkGl("prepared input diagnostic")
                return target.read(topDown = false)
            }
        }
    }
}

/** Cross prepared inputs independently of backend execution. Acceptance frames remain untouched. */
fun main(args: Array<String>) {
    val root = Path.of(args[0]); val refs = Path.of(args[1])
    val output = root.resolve("evidence/p2/diagnostics/sampling"); Files.createDirectories(output)
    val inputs = HashMap<String, RgbaFrame>(); val originals = HashMap<String, RgbaFrame>()
    val reports = JSONArray(); val inputComparisons = JSONArray(); val constantComparisons = JSONArray()
    val width = VideoPreviewRecipe.WIDTH; val height = VideoPreviewRecipe.HEIGHT
    for (software in listOf(false, true)) {
        val backend = if (software) "warp" else "hardware"
        PreviewTileRenderer(root.resolve("native/windows-x64"), root.resolve("assets"), software).use { renderer ->
            val cases = JSONArray(); val translations = JSONObject(); val precision = precisionMetadata()
            for (id in deterministicProbeIds) {
                val spec = requireNotNull(VideoFxCatalog.find(id)); check(!spec.history)
                for (time in VideoPreviewRecipe.goldenTimes) {
                    val original = renderer.renderAt(spec, time.toFloat())
                    if (time == VideoPreviewRecipe.goldenTimes.first()) translations.put(id, recordTranslation(output, backend, id))
                    val prepared = capturePrepared(renderer.device)
                    inputs["$backend/$id/$time"] = prepared; originals["$backend/$id/$time"] = original
                    FrameImages.write(prepared, output.resolve("inputs/$backend/$id/$time.png"))
                    val replay = PreviewTileRenderer.rgb565(drawPrepared(renderer.device, spec, time, prepared))
                    check(replay.pixels.contentEquals(original.pixels)) { "$backend $id/$time prepared round trip changed pixels" }
                    val reference = requireNotNull(ImageIO.read(refs.resolve("golden/$id/$time.png").toFile()))
                    val comparison = GoldenImages.compare(reference, FrameImages.buffered(original))
                    cases.put(JSONObject().put("id", id).put("time", time).put("inputBackend", backend)
                        .put("inputRgbaSha256", samplingHash(prepared)).put("preparedRoundTripExact", true)
                        .put("strictPassed", comparison.passed).put("channelsRGBA", JSONArray(comparison.channels.map {
                            JSONObject().put("mae8", it.meanAbsoluteError).put("p99_8", it.percentile99)
                        })))
                    if (software) {
                        val hardware = inputs.getValue("hardware/$id/$time")
                        val difference = GoldenImages.compare(FrameImages.buffered(hardware), FrameImages.buffered(prepared),
                            output.resolve("inputs/warp/$id/$time-vs-hardware-diff.png"))
                        inputComparisons.put(JSONObject().put("id", id).put("time", time).put("role", "BACKEND_INPUT_COMPARISON_NOT_MOBILE_GOLDEN")
                            .put("channelsRGBA", JSONArray(difference.channels.map { JSONObject().put("mae8", it.meanAbsoluteError).put("p99_8", it.percentile99) })))
                    }
                }
            }
            reports.put(JSONObject().put("shaderBackend", backend).put("inputBackend", backend).put("renderer", renderer.renderer)
                .put("fragmentPrecision", precision).put("translatedShaders", translations).put("cases", cases))
        }
    }
    for (software in listOf(false, true)) {
        val backend = if (software) "warp" else "hardware"; val other = if (software) "hardware" else "warp"
        AngleDevice(root.resolve("native/windows-x64"), software).use { device ->
            val cases = JSONArray()
            for (id in deterministicProbeIds) {
                val spec = requireNotNull(VideoFxCatalog.find(id))
                for (time in VideoPreviewRecipe.goldenTimes) {
                    val raw = drawPrepared(device, spec, time, inputs.getValue("$other/$id/$time"))
                    val actual = PreviewTileRenderer.rgb565(raw)
                    val folder = output.resolve("$backend-shader-$other-input/$id")
                    FrameImages.write(actual, folder.resolve("$time.png"))
                    val reference = requireNotNull(ImageIO.read(refs.resolve("golden/$id/$time.png").toFile()))
                    val comparison = GoldenImages.compare(reference, FrameImages.buffered(actual), folder.resolve("$time-diff.png"))
                    cases.put(JSONObject().put("id", id).put("time", time).put("inputBackend", other)
                        .put("strictPassed", comparison.passed).put("channelsRGBA", JSONArray(comparison.channels.map {
                            JSONObject().put("mae8", it.meanAbsoluteError).put("p99_8", it.percentile99)
                        })))
                }
                val constant = RgbaFrame(1, 1, byteArrayOf(137.toByte(), 113, 83, 255.toByte()))
                val frame = drawPrepared(device, spec, .3, constant)
                FrameImages.write(frame, output.resolve("constant/$backend/$id.png"))
                if (!software) originals["constant/$id"] = frame
                else {
                    val comparison = GoldenImages.compare(FrameImages.buffered(originals.getValue("constant/$id")), FrameImages.buffered(frame),
                        output.resolve("constant/warp/$id-vs-hardware-diff.png"))
                    constantComparisons.put(JSONObject().put("id", id).put("time", .3).put("inputRGBA8", JSONArray(listOf(137, 113, 83, 255)))
                        .put("role", "BACKEND_CONSTANT_COMPARISON_NOT_MOBILE_GOLDEN")
                        .put("channelsRGBA", JSONArray(comparison.channels.map { JSONObject().put("mae8", it.meanAbsoluteError).put("p99_8", it.percentile99) })))
                }
            }
            reports.put(JSONObject().put("shaderBackend", backend).put("inputBackend", other).put("renderer", device.renderer).put("cases", cases))
        }
    }
    Files.writeString(output.resolve("sampling-probe.json"), JSONObject().put("status", "DIAGNOSTIC_ONLY")
        .put("cases", reports).put("preparedInputBackendComparisons", inputComparisons).put("constantInputBackendComparisons", constantComparisons).toString(2))
    println("Sampling probe: 56 exact same-backend prepared-input replays; 56 crossed-input comparisons; $output")
}
