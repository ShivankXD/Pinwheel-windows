package com.pinwheel.render.qa

import com.pinwheel.core.media.video.VideoPreviewRecipe
import com.pinwheel.render.PreviewTileRenderer
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

fun main(args: Array<String>) {
    val root = Path.of(args[0]); val output = root.resolve("evidence/p2"); Files.createDirectories(output)
    val cases = JSONArray(); var failed = 0; var rendered = 0
    PreviewTileRenderer(root.resolve("native/windows-x64"), root.resolve("assets")).use { renderer ->
        val failures = renderer.compileAll()
        val report = JSONObject().put("renderer", renderer.renderer).put("version", renderer.version)
            .put("specs", PreviewTileRenderer.catalogSpecs().size).put("compileFailures", JSONObject(failures))
        Files.writeString(output.resolve("shader-compile.json"), report.toString(2))
        println("Catalog shaders: ${PreviewTileRenderer.catalogSpecs().size - failures.size} passed, ${failures.size} failed")
        failures.forEach { (id, error) -> System.err.println("$id\n$error") }
        for (spec in PreviewTileRenderer.catalogSpecs()) {
            var previous: ByteArray? = null; var moved = false
            for (t in VideoPreviewRecipe.goldenTimes) {
                val item = JSONObject().put("id", spec.id).put("time", t)
                try {
                    val frame = renderer.renderAt(spec, t.toFloat())
                    FrameImages.write(frame, output.resolve("frames/${spec.id}/$t.png"))
                    val hash = MessageDigest.getInstance("SHA-256").digest(frame.pixels).joinToString("") { "%02x".format(it) }
                    item.put("status", "PASS").put("rgbaSha256", hash)
                    if (previous != null && !previous.contentEquals(frame.pixels)) moved = true
                    previous = frame.pixels; rendered++
                } catch (e: Exception) { failed++; item.put("status", "FAIL").put("error", e.stackTraceToString()); System.err.println("${spec.id} $t\n${e.stackTraceToString()}") }
                cases.put(item)
            }
            if (!moved && spec.id !in failures) { failed++; System.err.println("${spec.id}: no animation across the four golden times") }
            if (rendered % 100 == 0) println("Rendered $rendered frames")
        }
        report.put("rendered", rendered).put("renderFailures", failed).put("cases", cases)
        Files.writeString(output.resolve("frame-status.json"), report.toString(2))
        println("P2 frames: $rendered rendered, $failed failures")
        check(failures.isEmpty() && failed == 0) { "P2 runtime failures; inspect evidence/p2 reports" }
    }
}
