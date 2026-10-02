package com.pinwheel.render.qa

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.media.video.*
import com.pinwheel.render.*
import java.nio.file.Files
import java.nio.file.Path
import org.json.JSONArray
import org.json.JSONObject
import org.lwjgl.BufferUtils
import org.lwjgl.opengles.GLES20.*

private fun exactGridCell(halfPixel: Int, size: Int, grid: Float): Int {
    val bits = grid.toBits(); val significand = (bits and 0x7fffff) or 0x800000
    val denominatorShift = 150 - ((bits ushr 23) and 255)
    require(denominatorShift in 0..30)
    return (halfPixel.toLong() * significand / ((size.toLong() * 2) shl denominatorShift)).toInt()
}

/** Isolate readback ties and varying/grid boundaries. Diagnostic outputs never replace actual frames. */
fun main(args: Array<String>) {
    val root = Path.of(args[0]); val output = root.resolve("evidence/p2/diagnostics/numeric")
    Files.createDirectories(output); val reports = JSONArray()
    for (software in listOf(false, true)) {
        val backend = if (software) "warp" else "hardware"
        AngleDevice(root.resolve("native/windows-x64"), software).use { device ->
            val report = JSONObject().put("backend", backend).put("renderer", device.renderer)
            val colours = JSONArray()
            GpuTarget(device, 192, 240).use { target ->
                for (value in listOf(.25f, .5f, .65f, .92f)) {
                    FxProgram("""
precision highp float;
varying highp vec2 vUv;
uniform float uValue;
void main() { gl_FragColor = vec4(uValue, uValue, uValue, 1.0); }
""").use { program ->
                        target.bind(); program.use(); program.float("uFlipY", -1f); program.float("uValue", value); program.draw()
                        val frame = target.read(topDown = false)
                        val levels = frame.pixels.indices.step(4).map { frame.pixels[it].toInt() and 255 }.toSet().sorted()
                        colours.put(JSONObject().put("value", value).put("rgba8Levels", JSONArray(levels)))
                    }
                }
                GpuTexture(device, 1, 1).use { input ->
                    input.upload(RgbaFrame(1, 1, byteArrayOf(137.toByte(), 113, 83, 255.toByte())))
                    val spec = requireNotNull(VideoFxCatalog.find("fx-ov-tr-diamond"))
                    FxProgram(FxProgram.source(spec)).use { program ->
                        val uniforms = spec.uniforms(emptyMap()); val t = .3f
                        target.bind(); program.use(); program.float("uFlipY", -1f)
                        program.texture("uTexture", input.id, 0); program.texture("uPrev", input.id, 1); program.texture("uTrail", input.id, 2)
                        program.vec2("uSize", 192f, 240f); program.float("uAspect", 192f / 240)
                        program.float("uTime", t); program.float("uDuration", VideoPreviewRecipe.LOOP_SECONDS)
                        program.float("uProgress", VideoPreviewRecipe.inputs(spec, t.toDouble()).progress)
                        program.vec4("uP0", uniforms); program.vec4("uP1", uniforms, 4); program.draw()
                        val frame = target.read(topDown = false)
                        FrameImages.write(frame, output.resolve("$backend/diamond-raw-rgba8.png"))
                        val counts = mutableMapOf<List<Int>, Int>()
                        for (at in frame.pixels.indices step 4) {
                            val colour = (0..3).map { frame.pixels[at + it].toInt() and 255 }
                            counts[colour] = (counts[colour] ?: 0) + 1
                        }
                        report.put("unchangedDiamondColours", JSONArray(counts.entries.sortedByDescending { it.value }.map {
                            JSONObject().put("rgba8", JSONArray(it.key)).put("pixels", it.value)
                        }))
                    }
                }
                val grids = JSONArray()
                FxProgram("""
precision highp float;
varying highp vec2 vUv;
uniform vec2 uGrid;
void main() { gl_FragColor = vec4(floor(vUv * uGrid) / 255.0, 0.0, 1.0); }
""").use { program ->
                    for (triangle in listOf(false, true)) for (n in listOf(16f, 32f, 64f, 128f)) {
                        val gx = n * (192f / 240); val gy = n
                        target.bind(); program.use(); program.float("uFlipY", -1f); program.vec2("uGrid", gx, gy)
                        if (triangle) {
                            val position = glGetAttribLocation(program.id, "aPosition")
                            val vertices = BufferUtils.createFloatBuffer(6).apply { put(floatArrayOf(-1f, -1f, 3f, -1f, -1f, 3f)); flip() }
                            glEnableVertexAttribArray(position); glVertexAttribPointer(position, 2, GL_FLOAT, false, 0, vertices)
                            glDrawArrays(GL_TRIANGLES, 0, 3); glDisableVertexAttribArray(position)
                        } else program.draw()
                        val frame = target.read(topDown = false); val failures = JSONArray()
                        val heatmap = ByteArray(frame.pixels.size)
                        for (y in 0 until 240) for (x in 0 until 192) {
                            val at = (y * 192 + x) * 4
                            // A rational reference using the exact uploaded Float grid;
                            // this is not an owner/mobile golden or an acceptance assertion.
                            val expectedX = exactGridCell(2 * x + 1, 192, gx)
                            val expectedY = exactGridCell(2 * (240 - y) - 1, 240, gy)
                            val actualX = frame.pixels[at].toInt() and 255; val actualY = frame.pixels[at + 1].toInt() and 255
                            if (actualX != expectedX || actualY != expectedY) {
                                failures.put(JSONObject().put("x", x).put("y", y).put("expectedX", expectedX).put("actualX", actualX)
                                    .put("expectedY", expectedY).put("actualY", actualY))
                                heatmap[at] = if (actualX != expectedX) 255.toByte() else 0
                                heatmap[at + 1] = if (actualY != expectedY) 255.toByte() else 0
                            }
                            heatmap[at + 3] = 255.toByte()
                        }
                        val primitive = if (triangle) "triangle" else "quad"
                        FrameImages.write(frame, output.resolve("$backend/$primitive-grid-${n.toInt()}-cells.png"))
                        FrameImages.write(RgbaFrame(192, 240, heatmap), output.resolve("$backend/$primitive-grid-${n.toInt()}-boundaries.png"))
                        grids.put(JSONObject().put("primitive", primitive).put("gridX", gx).put("gridY", gy)
                            .put("differingPixels", failures.length()).put("cases", failures))
                    }
                }
                report.put("constantColourReadback", colours).put("gridBoundaryComparisons", grids)
            }
            reports.put(report)
        }
    }
    Files.writeString(output.resolve("numeric-probe.json"), reports.toString(2))
    println("P2 numeric diagnostic: ${output.resolve("numeric-probe.json")}")
}
