package com.pinwheel.render.qa

import com.pinwheel.core.media.video.VideoPreviewRecipe
import com.pinwheel.render.*
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import org.json.JSONArray
import org.json.JSONObject
import org.lwjgl.BufferUtils
import org.lwjgl.opengles.GLES20.*

/** Experimental conversion evidence only. Never writes authoritative frames or changes golden policy. */
fun main(args: Array<String>) {
    val root = Path.of(args[0]); val refs = Path.of(args[1])
    val output = root.resolve("evidence/p2/diagnostics/framebuffer"); Files.createDirectories(output)
    val baseline = Files.readAllLines(root.resolve("evidence/p2/golden-metrics.csv")).drop(1).associate { line ->
        val columns = line.split(','); "${columns[0]}/${columns[1]}" to (columns[2] == "PASS")
    }
    val classes = JSONArray(Files.readString(root.resolve("docs/p2-noise-classification.json")))
    val deterministic = (0 until classes.length()).map { classes.getJSONObject(it) }
        .filter { it.getString("class") == "deterministic" }.map { it.getString("id") }.toSet()
    check(deterministic.size == 229)
    val reports = JSONArray()
    PreviewTileRenderer(root.resolve("native/windows-x64"), root.resolve("assets")).use { renderer ->
        val extensions = requireNotNull(glGetString(GL_EXTENSIONS)).split(' ').filter { "float" in it }
        for ((name, type, internalFormat) in listOf(Triple("float32-unsized", GL_FLOAT, GL_RGBA),
            Triple("float32-sized", GL_FLOAT, 0x8814), Triple("float16", 0x8D61, GL_RGBA))) {
            val report = JSONObject().put("storage", name).put("renderer", renderer.renderer).put("floatExtensions", JSONArray(extensions))
                .put("internalFormat", internalFormat).put("type", type)
                .put("status", "DIAGNOSTIC_ONLY").put("conversion", "clamp [0,1], floor(x * 255 + 0.5) / 255 in a separate GPU pass")
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
                    FxProgram("""
precision highp float;
varying highp vec2 vUv;
uniform highp sampler2D uTexture;
void main() {
    vec4 value = clamp(texture2D(uTexture, vUv), 0.0, 1.0);
    gl_FragColor = floor(value * 255.0 + 0.5) / 255.0;
}
""").use { conversion ->
                        fun convert() {
                            quantized.bind(); conversion.use(); conversion.float("uFlipY", 1f)
                            conversion.texture("uTexture", floating.texture.id, 0); conversion.draw()
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
                        }
                        val cases = JSONArray(); var rendered = 0; var passed = 0; var improved = 0; var regressed = 0
                        var deterministicPassed = 0; var deterministicImproved = 0; var deterministicRegressed = 0
                        for (spec in PreviewTileRenderer.catalogSpecs()) for (time in VideoPreviewRecipe.goldenTimes) {
                            renderer.drawTo(floating, spec, time.toFloat()); convert()
                            val frame = PreviewTileRenderer.rgb565(quantized.read(topDown = false))
                            val reference = requireNotNull(ImageIO.read(refs.resolve("golden/${spec.id}/$time.png").toFile()))
                            val selected = spec.id in setOf("fx-ov-tr-diamond", "fx-cmyk-print", "fx-mosaic-pulse", "fx-pb-pixel-creation")
                            if (selected) FrameImages.write(frame, output.resolve("$name/${spec.id}/$time.png"))
                            val result = GoldenImages.compare(reference, FrameImages.buffered(frame),
                                if (selected) output.resolve("$name/${spec.id}/$time-diff.png") else null)
                            val before = requireNotNull(baseline["${spec.id}/$time"]); val after = result.passed
                            rendered++; if (after) passed++; if (!before && after) improved++; if (before && !after) regressed++
                            if (spec.id in deterministic) {
                                if (after) deterministicPassed++; if (!before && after) deterministicImproved++; if (before && !after) deterministicRegressed++
                            }
                            cases.put(JSONObject().put("id", spec.id).put("time", time).put("deterministic", spec.id in deterministic)
                                .put("baselineStrictPassed", before).put("diagnosticStrictPassed", after)
                                .put("channelsRGBA", JSONArray(result.channels.map {
                                    JSONObject().put("mae8", it.meanAbsoluteError).put("p99_8", it.percentile99)
                                })))
                            if (rendered % 200 == 0) println("$name diagnostic frames: $rendered")
                        }
                        report.put("rendered", rendered).put("strictPassed", passed).put("strictFailed", rendered - passed)
                            .put("newStrictPasses", improved).put("strictRegressions", regressed)
                            .put("deterministicPassed", deterministicPassed).put("deterministicFailed", 916 - deterministicPassed)
                            .put("newDeterministicPasses", deterministicImproved).put("deterministicRegressions", deterministicRegressed).put("cases", cases)
                        println("$name diagnostic: $passed strict passes; $improved new passes; $regressed regressions; deterministic ${916 - deterministicPassed} failures / $deterministicRegressed regressions")
                    }
                }
            }
            reports.put(report)
        }
    }
    Files.writeString(output.resolve("framebuffer-probe.json"), reports.toString(2))
    println("P2 framebuffer diagnostic: ${output.resolve("framebuffer-probe.json")}")
}
