package com.pinwheel.render.qa

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.media.video.*
import com.pinwheel.render.*
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import javax.imageio.ImageIO
import org.json.JSONArray
import org.json.JSONObject
import org.lwjgl.BufferUtils
import org.lwjgl.opengles.GLES20.*

private data class Storage8(val name: String, val internalFormat: Int, val externalFormat: Int)
private const val BGRA = 0x80E1
private fun digest8(pixels: ByteArray) = MessageDigest.getInstance("SHA-256").digest(pixels)
    .joinToString("") { "%02x".format(it.toInt() and 255) }
private fun RgbaFrame.redLevels8() = pixels.indices.step(4).map { pixels[it].toInt() and 255 }.toSet().sorted()

private fun upload8(target: GpuTarget, format: Storage8, rgba: List<Int>) {
    val ordered = if (format.externalFormat == BGRA) listOf(rgba[2], rgba[1], rgba[0], rgba[3]) else rgba
    val bytes = BufferUtils.createByteBuffer(target.texture.width * target.texture.height * 4)
    repeat(target.texture.width * target.texture.height) { ordered.forEach { bytes.put(it.toByte()) } }
    bytes.flip(); target.bind(); glBindTexture(GL_TEXTURE_2D, target.texture.id)
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, target.texture.width, target.texture.height,
        format.externalFormat, GL_UNSIGNED_BYTE, bytes)
    checkGl("${format.name} control upload")
}

/** Checks channel order independently, then observes stored half values using endpoint colours. */
private fun controls8(device: AngleDevice, target: GpuTarget, format: Storage8): JSONObject {
    val distinct = listOf(17, 83, 149, 211)
    upload8(target, format, distinct)
    val uploaded = target.read(topDown = false)
    check(uploaded.pixels.indices.all { (uploaded.pixels[it].toInt() and 255) == distinct[it % 4] }) {
        "${format.name} changed RGBA channel order or byte storage"
    }
    val report = JSONObject().put("distinctUploadRGBA", JSONArray(distinct))
        .put("distinctUploadVerifiedPixels", target.texture.width * target.texture.height)
    FxProgram("""
precision highp float;
varying highp vec2 vUv;
uniform float uValue;
void main() { gl_FragColor = vec4(uValue, uValue, uValue, 1.0); }
""").use { writer ->
        val constants = JSONArray()
        for (value in listOf(.25f, .5f, .65f, .92f)) {
            target.bind(); writer.use(); writer.float("uFlipY", -1f); writer.float("uValue", value); writer.draw()
            constants.put(JSONObject().put("value", value).put("rgba8Levels", JSONArray(target.read(false).redLevels8())))
        }
        report.put("constants", constants)
        val clearColour = BufferUtils.createFloatBuffer(4); glGetFloatv(GL_COLOR_CLEAR_VALUE, clearColour)
        try {
            GpuTarget(device, target.texture.width, target.texture.height).use { observation ->
                FxProgram("""
precision highp float;
varying highp vec2 vUv;
uniform sampler2D uTexture;
void main() {
    float stored = texture2D(uTexture, vUv).r;
    gl_FragColor = vec4(stored < 0.5 ? 1.0 : 0.0, stored > 0.5 ? 1.0 : 0.0, stored == 0.5 ? 1.0 : 0.0, 1.0);
}
""").use { reader ->
                    val storage = JSONArray()
                    for (origin in listOf("upload-127", "upload-128", "clear-half", "draw-half")) {
                        target.bind()
                        when (origin) {
                            "upload-127", "upload-128" -> {
                                val level = if (origin == "upload-127") 127 else 128
                                upload8(target, format, listOf(level, level, level, 255))
                                check(target.read(false).redLevels8() == listOf(level))
                            }
                            "clear-half" -> { glClearColor(.5f, .5f, .5f, 1f); glClear(GL_COLOR_BUFFER_BIT) }
                            "draw-half" -> { writer.use(); writer.float("uFlipY", -1f); writer.float("uValue", .5f); writer.draw() }
                        }
                        val levels = target.read(false).redLevels8()
                        observation.bind(); reader.use(); reader.float("uFlipY", -1f)
                        reader.texture("uTexture", target.texture.id, 0); reader.draw()
                        val endpoints = observation.read(false); val counts = IntArray(3)
                        for (at in endpoints.pixels.indices step 4) {
                            val rgb = (0..2).map { endpoints.pixels[at + it].toInt() and 255 }
                            check(rgb.all { it == 0 || it == 255 } && rgb.count { it == 255 } == 1)
                            counts[rgb.indexOf(255)]++
                        }
                        storage.put(JSONObject().put("origin", origin).put("rgba8Levels", JSONArray(levels))
                            .put("storedBelowHalfPixels", counts[0]).put("storedAboveHalfPixels", counts[1])
                            .put("storedExactlyHalfPixels", counts[2]))
                    }
                    report.put("halfValueStorage", storage)
                }
            }
        } finally { glClearColor(clearColour[0], clearColour[1], clearColour[2], clearColour[3]) }
    }
    return report
}

/** Alternative 8-bit attachments only. Never changes production pixels, shaders or acceptance gates. */
fun main(args: Array<String>) {
    val root = Path.of(args[0]); val refs = Path.of(args[1])
    val output = root.resolve("evidence/p2/diagnostics/storage8"); Files.createDirectories(output)
    val formats = listOf(Storage8("rgba-unsized", GL_RGBA, GL_RGBA), Storage8("rgba8-sized", 0x8058, GL_RGBA),
        Storage8("bgra-unsized", BGRA, BGRA), Storage8("bgra8-sized", 0x93A1, BGRA))
    val manifest = JSONObject(Files.readString(root.resolve("evidence/p2/frame-status.json"))).getJSONArray("cases")
    val production = (0 until manifest.length()).associate {
        val c = manifest.getJSONObject(it); "${c.getString("id")}/${c.getDouble("time")}" to c.getString("rgbaSha256")
    }
    val prior = JSONArray(Files.readString(root.resolve("evidence/p2/diagnostics/deterministic/deterministic-probe.json")))
    val priorRaw = (0 until prior.length()).associate { b ->
        val backend = prior.getJSONObject(b); val cases = backend.getJSONArray("cases")
        backend.getString("backend") to (0 until cases.length()).associate {
            val c = cases.getJSONObject(it); "${c.getString("id")}/${c.getDouble("time")}" to c.getString("rawRgbaSha256")
        }
    }
    val reports = JSONArray()
    for (software in listOf(false, true)) {
        val backend = if (software) "warp" else "hardware"
        PreviewTileRenderer(root.resolve("native/windows-x64"), root.resolve("assets"), software).use { renderer ->
            val extension = glGetString(GL_EXTENSIONS)?.split(' ')?.contains("GL_EXT_texture_format_BGRA8888") == true
            val baselineRaw = mutableMapOf<String, String>(); val baselineCooked = mutableMapOf<String, String>()
            for (format in formats) {
                val report = JSONObject().put("backend", backend).put("renderer", renderer.renderer).put("version", renderer.version)
                    .put("storage", format.name).put("internalFormat", format.internalFormat).put("externalFormat", format.externalFormat)
                    .put("type", GL_UNSIGNED_BYTE).put("bgraExtensionAdvertised", extension).put("status", "DIAGNOSTIC_ONLY")
                if (format.externalFormat == BGRA && !extension) {
                    report.put("status", "UNSUPPORTED_EXTENSION"); reports.put(report); continue
                }
                GpuTarget(renderer.device, VideoPreviewRecipe.WIDTH, VideoPreviewRecipe.HEIGHT).use { target ->
                    target.bind(); glBindTexture(GL_TEXTURE_2D, target.texture.id)
                    glTexImage2D(GL_TEXTURE_2D, 0, format.internalFormat, target.texture.width, target.texture.height,
                        0, format.externalFormat, GL_UNSIGNED_BYTE, null as ByteBuffer?)
                    val allocationError = glGetError(); val complete = glCheckFramebufferStatus(GL_FRAMEBUFFER)
                    report.put("allocationError", allocationError).put("framebufferStatus", complete)
                    if (allocationError != GL_NO_ERROR || complete != GL_FRAMEBUFFER_COMPLETE) {
                        report.put("status", "UNSUPPORTED_ALLOCATION"); reports.put(report); continue
                    }
                    val bits = listOf(GL_RED_BITS, GL_GREEN_BITS, GL_BLUE_BITS, GL_ALPHA_BITS).map { glGetInteger(it) }
                    checkGl("${format.name} channel precision query"); check(bits == listOf(8, 8, 8, 8))
                    report.put("channelBitsRGBA", JSONArray(bits)).put("controls", controls8(renderer.device, target, format))
                    val cases = JSONArray(); var passed = 0; var rawChanged = 0; var cookedChanged = 0
                    for (id in deterministicProbeIds) for (time in VideoPreviewRecipe.goldenTimes) {
                        val key = "$id/$time"; renderer.drawTo(target, requireNotNull(VideoFxCatalog.find(id)), time.toFloat())
                        val raw = target.read(false); val cooked = PreviewTileRenderer.rgb565(raw)
                        val rawHash = digest8(raw.pixels); val cookedHash = digest8(cooked.pixels)
                        if (format == formats.first()) {
                            check(rawHash == priorRaw.getValue(backend).getValue(key)) { "$backend/$key differs from stored-input control" }
                            if (!software) check(cookedHash == production.getValue(key)) { "$key changed production pixels" }
                            baselineRaw[key] = rawHash; baselineCooked[key] = cookedHash
                        }
                        val rawSame = rawHash == baselineRaw.getValue(key); val cookedSame = cookedHash == baselineCooked.getValue(key)
                        if (!rawSame) rawChanged++; if (!cookedSame) cookedChanged++
                        val folder = output.resolve("$backend/${format.name}/$id")
                        if (!rawSame || !cookedSame) {
                            FrameImages.write(raw, folder.resolve("$time-raw.png")); FrameImages.write(cooked, folder.resolve("$time.png"))
                        }
                        val reference = requireNotNull(ImageIO.read(refs.resolve("golden/$id/$time.png").toFile()))
                        val result = GoldenImages.compare(reference, FrameImages.buffered(cooked),
                            if (!rawSame || !cookedSame) folder.resolve("$time-diff.png") else null)
                        if (result.passed) passed++
                        cases.put(JSONObject().put("id", id).put("time", time).put("rawRgbaSha256", rawHash).put("rgbaSha256", cookedHash)
                            .put("rawMatchesDefault", rawSame).put("matchesDefault", cookedSame).put("strictPassed", result.passed)
                            .put("channelsRGBA", JSONArray(result.channels.map { JSONObject().put("mae8", it.meanAbsoluteError).put("p99_8", it.percentile99) })))
                    }
                    report.put("cases", cases).put("strictPassed", passed).put("strictFailed", cases.length() - passed)
                        .put("rawChangedFrames", rawChanged).put("changedFrames", cookedChanged)
                    println("$backend ${format.name}: bits $bits; $passed strict passes, ${cases.length() - passed} failures; $rawChanged raw / $cookedChanged RGB565 changed frames")
                }
                reports.put(report)
            }
        }
    }
    Files.writeString(output.resolve("storage8-probe.json"), reports.toString(2))
    println("8-bit storage controls complete: $output; production evidence unchanged")
}
