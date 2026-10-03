package com.pinwheel.render.qa

import com.pinwheel.core.media.video.VideoPreviewRecipe
import com.pinwheel.render.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import javax.imageio.ImageIO
import org.json.JSONArray
import org.json.JSONObject
import org.lwjgl.BufferUtils
import org.lwjgl.opengles.GLES20.*
import kotlin.math.round

/** Experimental conversion evidence only. Never writes authoritative frames or changes golden policy. */
fun main(args: Array<String>) {
    val root = Path.of(args[0]); val refs = Path.of(args[1])
    val even = args.getOrNull(4) == "even"
    val staged = args.getOrNull(5) == "staged"
    val folder = args.getOrElse(2) { "framebuffer" }
    require(folder.matches(Regex("[a-z0-9-]+"))) { "Diagnostic folder must be a simple name" }
    val output = root.resolve("evidence/p2/diagnostics/$folder"); Files.createDirectories(output)
    val metricsPath = root.resolve("evidence/p2/golden-metrics.csv")
    val frameStatusPath = root.resolve("evidence/p2/frame-status.json")
    val structuralPath = root.resolve("evidence/p2/structural-status.json")
    val policyPath = root.resolve("docs/p2-noise-policy.json")
    fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }
    fun sha256(path: Path) = digest(Files.readAllBytes(path))
    val metricsHash = sha256(metricsPath); val frameStatusHash = sha256(frameStatusPath)
    val reviewNoise = args.getOrNull(3) == "structural"
    val structuralHash = sha256(structuralPath); val policyHash = sha256(policyPath)
    val policy = JSONObject(Files.readString(policyPath))
    val limits = StructuralGoldens.Limits(policy.getDouble("perChannelBlurredMae8Max"), policy.getDouble("perChannelHistogramWasserstein8Max"),
        policy.getDouble("perChannelMeanError8Max"), policy.getDouble("meanLuminanceError8Max"))
    val structuralBaseline = JSONObject(Files.readString(structuralPath)).getJSONArray("goldenCases")
    val priorNoise = (0 until structuralBaseline.length()).map { structuralBaseline.getJSONObject(it) }
        .filter { it.getString("class") == "noise-driven" }.associate {
            "${it.getString("id")}/${it.getDouble("time")}" to it.getBoolean("structuralCandidatePassed")
        }
    val baseline = Files.readAllLines(metricsPath).drop(1).associate { line ->
        val columns = line.split(','); "${columns[0]}/${columns[1]}" to (columns[2] == "PASS")
    }
    val manifest = JSONObject(Files.readString(frameStatusPath)).getJSONArray("cases")
    val productionHashes = (0 until manifest.length()).associate {
        val c = manifest.getJSONObject(it); "${c.getString("id")}/${c.getDouble("time")}" to c.getString("rgbaSha256")
    }
    val classes = JSONArray(Files.readString(root.resolve("docs/p2-noise-classification.json")))
    val deterministic = (0 until classes.length()).map { classes.getJSONObject(it) }
        .filter { it.getString("class") == "deterministic" }.map { it.getString("id") }.toSet()
    check(deterministic.size == 229)
    val reports = JSONArray()
    PreviewTileRenderer(root.resolve("native/windows-x64"), root.resolve("assets")).use { renderer ->
        val extensions = requireNotNull(glGetString(GL_EXTENSIONS)).split(' ').filter { "float" in it }
        val formats = if (even) listOf(Triple(if (staged) "float32-round-even-staged" else "float32-round-even", GL_FLOAT, 0x8814)) else
            listOf(Triple("float32-unsized", GL_FLOAT, GL_RGBA), Triple("float32-sized", GL_FLOAT, 0x8814), Triple("float16", 0x8D61, GL_RGBA))
        val priorRegressions = if (even) JSONArray(Files.readString(root.resolve("evidence/p2/diagnostics/framebuffer-current/framebuffer-probe.json")))
            .let { rows -> (0 until rows.length()).map { rows.getJSONObject(it) }.first { it.getString("storage") == "float32-sized" }
                .getJSONArray("cases") }.let { rows -> (0 until rows.length()).map { rows.getJSONObject(it) }
                .filter { it.getBoolean("baselineStrictPassed") && !it.getBoolean("diagnosticStrictPassed") }
                .map { "${it.getString("id")}/${it.getDouble("time")}" }.toSet() } else emptySet()
        for ((name, type, internalFormat) in formats) {
            val report = JSONObject().put("storage", name).put("renderer", renderer.renderer).put("floatExtensions", JSONArray(extensions))
                .put("internalFormat", internalFormat).put("type", type)
                .put("baselineMetricsSha256", metricsHash).put("baselineFrameStatusSha256", frameStatusHash)
                .put("noiseReviewEnabled", reviewNoise).put("baselineStructuralSha256", structuralHash).put("noisePolicySha256", policyHash)
                .put("scaledValueEvaluation", if (staged) "separate RGBA32F scaling attachment" else "single conversion shader")
                .put("baselineStrictPassed", baseline.values.count { it }).put("baselineStrictFailed", baseline.values.count { !it })
                .put("status", "DIAGNOSTIC_ONLY").put("conversion", if (even) "clamp [0,1], round float32 x * 255 to nearest with even ties / 255 in a separate GPU pass"
                    else "clamp [0,1], floor(x * 255 + 0.5) / 255 in a separate GPU pass")
            GpuTarget(renderer.device, VideoPreviewRecipe.WIDTH, VideoPreviewRecipe.HEIGHT).use { floating ->
                floating.bind(); glBindTexture(GL_TEXTURE_2D, floating.texture.id)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
                glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, VideoPreviewRecipe.WIDTH, VideoPreviewRecipe.HEIGHT, 0, GL_RGBA, type, null as ByteBuffer?)
                val allocationError = glGetError(); val complete = glCheckFramebufferStatus(GL_FRAMEBUFFER)
                report.put("allocationError", allocationError).put("framebufferStatus", complete)
                if (allocationError != GL_NO_ERROR || complete != GL_FRAMEBUFFER_COMPLETE) {
                    report.put("status", "UNSUPPORTED"); reports.put(report); continue
                }
                GpuTarget(renderer.device, VideoPreviewRecipe.WIDTH, VideoPreviewRecipe.HEIGHT).use { quantized ->
                    GpuTarget(renderer.device, VideoPreviewRecipe.WIDTH, VideoPreviewRecipe.HEIGHT).use { direct ->
                    GpuResources().use { conversionResources ->
                    val scaled = if (staged) conversionResources.own { GpuTarget(renderer.device, floating.texture.width, floating.texture.height).also {
                        it.bind(); glBindTexture(GL_TEXTURE_2D, it.texture.id)
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
                        glTexImage2D(GL_TEXTURE_2D, 0, 0x8814, it.texture.width, it.texture.height, 0, GL_RGBA, GL_FLOAT, null as ByteBuffer?)
                        check(glGetError() == GL_NO_ERROR && glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE)
                    } } else null
                    val scaling = if (staged) conversionResources.own { FxProgram("""
precision highp float;
varying highp vec2 vUv;
uniform highp sampler2D uTexture;
void main() { gl_FragColor = clamp(texture2D(uTexture, vUv), 0.0, 1.0) * 255.0; }
""") } else null
                    conversionResources.initialized()
                    val scalingExpression = if (staged) "value" else "clamp(value, 0.0, 1.0) * 255.0"
                    val converter = if (even) """
precision highp float;
varying highp vec2 vUv;
uniform highp sampler2D uTexture;
float nearestEven(float value) {
    float scaled = $scalingExpression;
    float lower = floor(scaled);
    float fraction = scaled - lower;
    bool up = fraction > 0.5 || (fraction == 0.5 && mod(lower, 2.0) > 0.5);
    return (lower + (up ? 1.0 : 0.0)) / 255.0;
}
void main() {
    vec4 value = texture2D(uTexture, vUv);
    gl_FragColor = vec4(nearestEven(value.r), nearestEven(value.g), nearestEven(value.b), nearestEven(value.a));
}
""" else """
precision highp float;
varying highp vec2 vUv;
uniform highp sampler2D uTexture;
void main() {
    vec4 value = clamp(texture2D(uTexture, vUv), 0.0, 1.0);
    gl_FragColor = floor(value * 255.0 + 0.5) / 255.0;
}
"""
                    FxProgram(converter).use { conversion ->
                        fun convert() {
                            if (scaled != null) {
                                scaled.bind(); scaling!!.use(); scaling.float("uFlipY", 1f)
                                scaling.texture("uTexture", floating.texture.id, 0); scaling.draw()
                            }
                            quantized.bind(); conversion.use(); conversion.float("uFlipY", 1f)
                            conversion.texture("uTexture", scaled?.texture?.id ?: floating.texture.id, 0); conversion.draw()
                        }
                        FxProgram("""
precision highp float;
varying highp vec2 vUv;
uniform float uValue;
void main() { gl_FragColor = vec4(uValue, uValue, uValue, 1.0); }
""").use { constant ->
                            val constants = JSONArray()
                            for (value in listOf(.25f, .5f, .65f, .92f)) {
                                floating.bind(); constant.use(); constant.float("uFlipY", -1f); constant.float("uValue", value); constant.draw()
                                val pixels = BufferUtils.createFloatBuffer(VideoPreviewRecipe.WIDTH * VideoPreviewRecipe.HEIGHT * 4)
                                glReadPixels(0, 0, VideoPreviewRecipe.WIDTH, VideoPreviewRecipe.HEIGHT, GL_RGBA, GL_FLOAT, pixels)
                                check(glGetError() == GL_NO_ERROR) { "$name float readback failed" }
                                convert(); val raw = quantized.read(topDown = false)
                                val levels = raw.pixels.indices.step(4).map { raw.pixels[it].toInt() and 255 }.toSet().sorted()
                                constants.put(JSONObject().put("value", value).put("storedFloatFirstPixel", pixels[0]).put("rgba8Levels", JSONArray(levels)))
                            }
                            report.put("constants", constants)
                            if (even) {
                                val controls = JSONArray(); var failed = 0
                                val stored = BufferUtils.createFloatBuffer(4); val bytes = BufferUtils.createByteBuffer(4)
                                for (boundary in listOf(false, true)) for (level in 0..(if (boundary) 254 else 255)) {
                                    val value = (level + if (boundary) .5f else 0f) / 255f
                                    floating.bind(); glViewport(0, 0, 1, 1); constant.use()
                                    constant.float("uFlipY", -1f); constant.float("uValue", value); constant.draw()
                                    stored.clear(); glReadPixels(0, 0, 1, 1, GL_RGBA, GL_FLOAT, stored)
                                    check(glGetError() == GL_NO_ERROR && stored[0] == value)
                                    convert(); bytes.clear(); glReadPixels(0, 0, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, bytes)
                                    check(glGetError() == GL_NO_ERROR)
                                    val scaled = value * 255f
                                    val expected = if (boundary) round(scaled.toDouble()).toInt() else level
                                    val actual = bytes[0].toInt() and 255
                                    val pass = (0..2).all { (bytes[it].toInt() and 255) == expected } && (bytes[3].toInt() and 255) == 255
                                    if (!pass) failed++
                                    controls.put(JSONObject().put("boundary", boundary).put("level", level).put("inputF32", value)
                                        .put("scaledF32", scaled).put("expectedByte", expected).put("actualByte", actual).put("passed", pass))
                                }
                                report.put("roundingControls", controls).put("roundingControlFailures", failed)
                            }
                        }
                        val cases = JSONArray(); var rendered = 0; var passed = 0; var improved = 0; var regressed = 0
                        var deterministicPassed = 0; var deterministicImproved = 0; var deterministicRegressed = 0
                        var noisePassed = 0; var noiseImproved = 0; var noiseRegressed = 0
                        for (spec in PreviewTileRenderer.catalogSpecs()) for (time in VideoPreviewRecipe.goldenTimes) {
                            renderer.drawTo(floating, spec, time.toFloat()); convert()
                            val raw = quantized.read(topDown = false)
                            val frame = PreviewTileRenderer.rgb565(raw)
                            val reference = requireNotNull(ImageIO.read(refs.resolve("golden/${spec.id}/$time.png").toFile()))
                            val selected = spec.id in setOf("fx-ov-tr-diamond", "fx-cmyk-print", "fx-mosaic-pulse", "fx-pb-pixel-creation")
                            if (selected) FrameImages.write(frame, output.resolve("$name/${spec.id}/$time.png"))
                            val result = GoldenImages.compare(reference, FrameImages.buffered(frame),
                                if (selected) output.resolve("$name/${spec.id}/$time-diff.png") else null)
                            val before = requireNotNull(baseline["${spec.id}/$time"]); val after = result.passed
                            val noise = if (reviewNoise && spec.id !in deterministic)
                                StructuralGoldens.compare(reference, FrameImages.buffered(frame), sigma = policy.getDouble("gaussianSigmaPx")) else null
                            val noisePass = noise?.passes(limits)
                            val noiseBefore = if (noise != null) priorNoise.getValue("${spec.id}/$time") else null
                            if (noisePass == true) noisePassed++
                            if (noisePass == true && noiseBefore == false) noiseImproved++
                            if (noisePass == false && noiseBefore == true) noiseRegressed++
                            if (before != after || (noise != null && noiseBefore != noisePass) || "${spec.id}/$time" in priorRegressions) {
                                val folder = output.resolve("$name/${spec.id}")
                                FrameImages.write(frame, folder.resolve("$time.png"))
                                FrameImages.write(raw, folder.resolve("$time-converted-raw.png"))
                                GoldenImages.compare(reference, FrameImages.buffered(frame), folder.resolve("$time-diff.png"))
                                floating.bind()
                                val stored = BufferUtils.createFloatBuffer(raw.pixels.size)
                                glReadPixels(0, 0, raw.width, raw.height, GL_RGBA, GL_FLOAT, stored)
                                check(glGetError() == GL_NO_ERROR) { "$name stored-float capture failed" }
                                val values = FloatArray(stored.capacity()); stored.get(values)
                                val bytes = ByteArray(values.size * 4)
                                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().put(values)
                                Files.write(folder.resolve("$time-stored-floats.f32le"), bytes)
                                renderer.drawTo(direct, spec, time.toFloat())
                                val directRaw = direct.read(topDown = false)
                                check(digest(PreviewTileRenderer.rgb565(directRaw).pixels) == productionHashes["${spec.id}/$time"]) {
                                    "${spec.id}/$time direct control differs from production"
                                }
                                FrameImages.write(directRaw, folder.resolve("$time-default-raw.png"))
                            }
                            rendered++; if (after) passed++; if (!before && after) improved++; if (before && !after) regressed++
                            if (spec.id in deterministic) {
                                if (after) deterministicPassed++; if (!before && after) deterministicImproved++; if (before && !after) deterministicRegressed++
                            }
                            val item = JSONObject().put("id", spec.id).put("time", time).put("deterministic", spec.id in deterministic)
                                .put("rgbaSha256", digest(frame.pixels))
                                .put("baselineStrictPassed", before).put("diagnosticStrictPassed", after)
                                .put("channelsRGBA", JSONArray(result.channels.map {
                                    JSONObject().put("mae8", it.meanAbsoluteError).put("p99_8", it.percentile99)
                                }))
                            if (noise != null) item.put("baselineNoiseCandidatePassed", noiseBefore).put("noiseCandidatePassed", noisePass)
                                .put("structuralChannelsRGBA", JSONArray(noise.channels.map { JSONObject().put("blurredMae8", it.blurredMae8)
                                    .put("histogramWasserstein8", it.histogramWasserstein8).put("meanError8", it.meanError8) }))
                                .put("meanLuminanceError8", noise.meanLuminanceError8)
                            cases.put(item)
                            if (rendered % 200 == 0) println("$name diagnostic frames: $rendered")
                        }
                        report.put("rendered", rendered).put("strictPassed", passed).put("strictFailed", rendered - passed)
                            .put("newStrictPasses", improved).put("strictRegressions", regressed)
                            .put("deterministicPassed", deterministicPassed).put("deterministicFailed", 916 - deterministicPassed)
                            .put("newDeterministicPasses", deterministicImproved).put("deterministicRegressions", deterministicRegressed).put("cases", cases)
                        if (reviewNoise) report.put("noiseCandidatePassed", noisePassed).put("noiseCandidateFailed", priorNoise.size - noisePassed)
                            .put("newNoiseCandidatePasses", noiseImproved).put("noiseCandidateRegressions", noiseRegressed).put("noisePolicy", policy)
                        val rows = (0 until cases.length()).map { cases.getJSONObject(it) }
                        fun worst(channelKey: String, valueKey: String, selectedRows: List<JSONObject>) = selectedRows.sortedByDescending { row ->
                            val channels = row.getJSONArray(channelKey)
                            (0 until channels.length()).maxOf { channels.getJSONObject(it).getDouble(valueKey) }
                        }.take(20)
                        val strictWorst = worst("channelsRGBA", "mae8", rows)
                        val noiseWorst = if (reviewNoise) worst("structuralChannelsRGBA", "blurredMae8", rows.filter { it.has("structuralChannelsRGBA") }) else emptyList()
                        report.put("worst20Strict", JSONArray(strictWorst.map { "${it.getString("id")}/${it.getDouble("time")}" }))
                            .put("worst20Noise", JSONArray(noiseWorst.map { "${it.getString("id")}/${it.getDouble("time")}" }))
                        for (row in (strictWorst + noiseWorst).distinctBy { "${it.getString("id")}/${it.getDouble("time")}" }) {
                            val id = row.getString("id"); val time = row.getDouble("time")
                            renderer.drawTo(floating, requireNotNull(com.pinwheel.core.media.video.VideoFxCatalog.find(id)), time.toFloat()); convert()
                            val frame = PreviewTileRenderer.rgb565(quantized.read(topDown = false))
                            check(digest(frame.pixels) == row.getString("rgbaSha256")) { "$name/$id/$time worst-case replay changed pixels" }
                            val reference = requireNotNull(ImageIO.read(refs.resolve("golden/$id/$time.png").toFile()))
                            val folder = output.resolve("$name/$id"); FrameImages.write(frame, folder.resolve("$time.png"))
                            GoldenImages.compare(reference, FrameImages.buffered(frame), folder.resolve("$time-diff.png"))
                            if (row.has("structuralChannelsRGBA")) StructuralGoldens.compare(reference, FrameImages.buffered(frame),
                                folder.resolve("$time-structural-diff.png"), policy.getDouble("gaussianSigmaPx"))
                        }
                        println("$name diagnostic: $passed strict passes; $improved new passes; $regressed regressions; deterministic ${916 - deterministicPassed} failures / $deterministicRegressed regressions")
                        if (reviewNoise) println("$name noise: $noisePassed provisional passes; ${priorNoise.size - noisePassed} failures; $noiseImproved new passes / $noiseRegressed regressions")
                    }
                    }
                    }
                }
            }
            reports.put(report)
        }
    }
    Files.writeString(output.resolve("framebuffer-probe.json"), reports.toString(2))
    check(sha256(metricsPath) == metricsHash && sha256(frameStatusPath) == frameStatusHash) { "Production baseline changed during probe" }
    check(sha256(structuralPath) == structuralHash && sha256(policyPath) == policyHash) { "Noise baseline or policy changed during probe" }
    if (even) check((0 until reports.length()).all { reports.getJSONObject(it).optInt("roundingControlFailures", -1) == 0 }) {
        "Round-to-even synthetic controls failed; inspect the full diagnostic"
    }
    println("P2 framebuffer diagnostic: ${output.resolve("framebuffer-probe.json")}")
}
