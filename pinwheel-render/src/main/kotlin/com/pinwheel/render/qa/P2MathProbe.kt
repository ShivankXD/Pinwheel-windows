package com.pinwheel.render.qa

import com.pinwheel.core.media.video.*
import com.pinwheel.render.*
import java.nio.file.Files
import java.nio.file.Path
import org.json.JSONArray
import org.json.JSONObject
import javax.imageio.ImageIO

/** Identical GLSL on two D3D11 implementations; diagnostic frames never replace golden actuals. */
fun main(args: Array<String>) {
    val root = Path.of(args[0]); val refs = Path.of(args[1]); val output = root.resolve("evidence/p2/diagnostics")
    Files.createDirectories(output); val report = JSONArray()
    for (software in listOf(false, true)) {
        val backend = if (software) "warp" else "hardware"
        AngleDevice(root.resolve("native/windows-x64"), software).use { device ->
            val math = JSONArray()
            GpuTarget(device, 1, 1).use { target -> FxProgram("""
precision highp float;
varying highp vec2 vUv;
uniform float uTime;
float hash1(float n) { return fract(sin(n * 12.9898) * 43758.5453); }
void main() {
  float k = floor(uTime * 5.0);
  float h = hash1(k * 1.3 + 0.7);
  gl_FragColor = vec4(h, h, h, 1.0);
}
""").use { p ->
                for (t in VideoPreviewRecipe.goldenTimes) {
                    target.bind(); p.use(); p.float("uFlipY", 1f); p.float("uTime", t.toFloat()); p.draw()
                    val result = target.read(); math.put(JSONObject().put("time", t).put("hash1_sRGB8", result.pixels[0].toInt() and 255))
                }
            } }
            report.put(JSONObject().put("backend", backend).put("renderer", device.renderer).put("sceneCutMath", math))
        }
        PreviewTileRenderer(root.resolve("native/windows-x64"), root.resolve("assets"), software).use { renderer ->
            val frames = JSONArray()
            for (id in listOf("fx-ct-scene-cut", "fx-film-grain", "fx-cc-wiggle-flicker", "fx-fade-in", "fx-split-3")) {
                val spec = requireNotNull(VideoFxCatalog.find(id))
                for (t in VideoPreviewRecipe.goldenTimes) {
                    val frame = renderer.renderAt(spec, t.toFloat())
                    val path = output.resolve("$backend/$id/$t.png"); FrameImages.write(frame, path)
                    val diff = output.resolve("$backend/$id/$t-diff.png")
                    val reference = requireNotNull(ImageIO.read(refs.resolve("golden/$id/$t.png").toFile()))
                    val metrics = GoldenImages.compare(reference, FrameImages.buffered(frame), diff)
                    frames.put(JSONObject().put("id", id).put("time", t).put("passed", metrics.passed)
                        .put("channelsRGBA", JSONArray(metrics.channels.map { JSONObject().put("mae8", it.meanAbsoluteError).put("p99_8", it.percentile99) })))
                }
            }
            report.getJSONObject(report.length() - 1).put("frames", frames)
        }
    }
    Files.writeString(output.resolve("math-probe.json"), report.toString(2)); println("P2 math diagnostic: ${output.resolve("math-probe.json")}")
}
