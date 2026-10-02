package com.pinwheel.render.qa

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.media.video.*
import com.pinwheel.render.*
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import javax.imageio.ImageIO
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

internal val deterministicProbeIds = listOf("fx-ov-tr-diamond", "fx-cmyk-print", "fx-mosaic-pulse",
    "fx-pb-pixel-creation", "fx-carousel", "fx-ne-edges", "fx-cc-dance-flash")

private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
    .joinToString("") { "%02x".format(it.toInt() and 255) }

/** Quantized error signatures are observations, not reconstructed phone RGBA8 values. */
private fun errorSignature(reference: BufferedImage, actual: RgbaFrame, raw: RgbaFrame): JSONArray {
    require(reference.width == actual.width && reference.height == actual.height)
    val shifts = intArrayOf(16, 8, 0, 24)
    return JSONArray((0..3).map { c ->
        val maximum = if (c == 1) 63 else if (c == 3) 255 else 31
        val indices = IntArray(256) { -1 }
        for (i in 0..maximum) indices[(i * 255 + maximum / 2) / maximum] = i
        val differences = sortedMapOf<Int, Int>(); val rawLevelsAtErrors = sortedMapOf<Int, Int>()
        var multipleCells = 0; var halfBelowNextCell = 0; var halfAbovePreviousCell = 0
        for (y in 0 until actual.height) for (x in 0 until actual.width) {
            val at = (y * actual.width + x) * 4 + c
            val expected = (reference.getRGB(x, y) ushr shifts[c]) and 255
            val level = actual.pixels[at].toInt() and 255
            check(indices[expected] >= 0 && indices[level] >= 0) { "Unexpected normalized RGB565 level" }
            val difference = indices[level] - indices[expected]
            differences[difference] = (differences[difference] ?: 0) + 1
            if (difference != 0) {
                val rawLevel = raw.pixels[at].toInt() and 255
                rawLevelsAtErrors[rawLevel] = (rawLevelsAtErrors[rawLevel] ?: 0) + 1
                if (abs(difference) > 1) multipleCells++
                if (rawLevel == 127 && difference == -1) halfBelowNextCell++
                if (rawLevel == 128 && difference == 1) halfAbovePreviousCell++
            }
        }
        JSONObject().put("channel", "RGBA"[c].toString()).put("pixels", actual.width * actual.height)
            .put("multipleQuantizationCellErrors", multipleCells)
            .put("raw127AndPhoneNextCellPixels", halfBelowNextCell)
            .put("raw128AndPhonePreviousCellPixels", halfAbovePreviousCell)
            .put("quantizationCellDeltas", JSONArray(differences.map { JSONObject().put("actualMinusPhone", it.key).put("pixels", it.value) }))
            .put("rawLevelsAtErrors", JSONArray(rawLevelsAtErrors.map { JSONObject().put("rgba8", it.key).put("pixels", it.value) }))
    })
}

/** Same unchanged shaders on two implementations. No diagnostic replaces production evidence. */
fun main(args: Array<String>) {
    val root = Path.of(args[0]); val refs = Path.of(args[1])
    val output = root.resolve("evidence/p2/diagnostics/deterministic"); Files.createDirectories(output)
    val classification = JSONArray(Files.readString(root.resolve("docs/p2-noise-classification.json")))
    val classes = (0 until classification.length()).associate {
        val item = classification.getJSONObject(it); item.getString("id") to item.getString("class")
    }
    check(deterministicProbeIds.all { classes[it] == "deterministic" })
    val baseline = JSONObject(Files.readString(root.resolve("evidence/p2/frame-status.json"))).getJSONArray("cases")
    val baselineHashes = (0 until baseline.length()).associate {
        val item = baseline.getJSONObject(it)
        "${item.getString("id")}/${item.getDouble("time")}" to item.getString("rgbaSha256")
    }
    val reports = JSONArray()
    for (software in listOf(false, true)) {
        val backend = if (software) "warp" else "hardware"
        PreviewTileRenderer(root.resolve("native/windows-x64"), root.resolve("assets"), software).use { renderer ->
            val cases = JSONArray(); var passed = 0
            GpuTarget(renderer.device, VideoPreviewRecipe.WIDTH, VideoPreviewRecipe.HEIGHT).use { target ->
                for (id in deterministicProbeIds) {
                    val spec = requireNotNull(VideoFxCatalog.find(id))
                    for (time in VideoPreviewRecipe.goldenTimes) {
                        val frame = renderer.renderAt(spec, time.toFloat())
                        val productionHash = sha256(frame.pixels)
                        if (!software) check(productionHash == baselineHashes["$id/$time"]) { "$id/$time changed production pixels" }
                        renderer.drawTo(target, spec, time.toFloat())
                        val raw = target.read(topDown = false)
                        check(PreviewTileRenderer.rgb565(raw).pixels.contentEquals(frame.pixels)) { "Raw probe differs from default path" }
                        val folder = output.resolve("$backend/$id")
                        FrameImages.write(frame, folder.resolve("$time.png"))
                        FrameImages.write(raw, folder.resolve("$time-raw-rgba8.png"))
                        val reference = requireNotNull(ImageIO.read(refs.resolve("golden/$id/$time.png").toFile()))
                        val comparison = GoldenImages.compare(reference, FrameImages.buffered(frame), folder.resolve("$time-diff.png"))
                        if (comparison.passed) passed++
                        val input = VideoPreviewRecipe.inputs(spec, time)
                        cases.put(JSONObject().put("id", id).put("time", time).put("sample", input.sample)
                            .put("shaderSourceSha256", sha256(FxProgram.source(spec).toByteArray(Charsets.UTF_8)))
                            .put("rgbaSha256", productionHash).put("rawRgbaSha256", sha256(raw.pixels))
                            .put("productionHashVerified", !software).put("strictPassed", comparison.passed)
                            .put("channelsRGBA", JSONArray(comparison.channels.map { JSONObject()
                                .put("mae8", it.meanAbsoluteError).put("p99_8", it.percentile99) }))
                            .put("errorSignaturesRGBA", errorSignature(reference, frame, raw)))
                    }
                }
            }
            reports.put(JSONObject().put("backend", backend).put("renderer", renderer.renderer).put("version", renderer.version)
                .put("status", "DIAGNOSTIC_ONLY").put("strictPassed", passed).put("strictFailed", cases.length() - passed).put("cases", cases))
            println("$backend deterministic probe: $passed passed, ${cases.length() - passed} failed; original strict limits")
        }
    }
    Files.writeString(output.resolve("deterministic-probe.json"), reports.toString(2))
    println("Deterministic probe: $output; production frames unchanged")
}
