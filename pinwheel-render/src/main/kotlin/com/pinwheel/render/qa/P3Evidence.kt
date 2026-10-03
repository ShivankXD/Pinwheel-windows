package com.pinwheel.render.qa

import com.pinwheel.core.model.*
import com.pinwheel.render.GpuFrameEvaluator
import org.json.JSONArray
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path

/** Known-colour mobile PIP fixtures, not substitutes for owner P2 golden references. */
fun main(args: Array<String>) {
    val root = Path.of(args[0]); val output = root.resolve("evidence/p3")
    val project = StudioProject(name = "Mobile PIP fixture", kind = ProjectKind.VIDEO,
        clips = listOf(Clip(uri = root.resolve("assets/p3/pip-base.mp4").toUri().toString(), name = "Blue", sourceDurationMs = 4000, muted = true)),
        video = VideoProjectEdits(images = listOf(VideoImageOverlay(uri = root.resolve("assets/p3/pip-layer.mp4").toUri().toString(), name = "PIP",
            startMs = 1000, endMs = 3000, x = .7f, y = .4f, width = .5f, mask = "Circle", video = VideoOverlaySource(3000, startMs = 500, width = 160, height = 120)))))
    val frames = JSONArray(); val stages=JSONArray(); var currentTime=0.0;val openedAt = System.nanoTime()
    GpuFrameEvaluator(root.resolve("native/windows-x64"), project, 320, 240,onTiming={ stage,ms->stages.put(JSONObject().put("seconds",currentTime).put("stage",stage).put("milliseconds",ms)) }).use { evaluator ->
        for ((seconds, expected) in listOf(.4 to 2, 1.4 to 0, 2.6 to 1, 3.5 to 2, 1.4 to 0)) {
            currentTime=seconds;val start = System.nanoTime(); val frame = evaluator.render((seconds * 1e6).toLong())
            val elapsed = (System.nanoTime() - start) / 1e6
            val center = (0..2).map { frame.pixels[(96 * 320 + 224) * 4 + it].toInt() and 255 }
            check(center[expected] > 190 && center.filterIndexed { index, _ -> index != expected }.all { it < 65 }) { "PIP $seconds: $center" }
            val corner = (0..2).map { frame.pixels[(42 * 320 + 150) * 4 + it].toInt() and 255 }
            check(corner[2] > 190 && corner[0] < 65 && corner[1] < 65) { "Mask corner $seconds: $corner" }
            FrameImages.write(frame, output.resolve("frames/pip/$seconds.png"))
            frames.put(JSONObject().put("seconds", seconds).put("centerRgb", center).put("maskCornerRgb", corner).put("evaluationMs", elapsed))
        }
        val report = JSONObject().put("renderer", evaluator.renderer).put("scope", "Mobile PIP known-colour assertions; no P2 golden or P4 UI replacement")
            .put("openAndFiveFramesMs", (System.nanoTime() - openedAt) / 1e6).put("frames", frames).put("stageTimings",stages)
            .put("decoders", JSONArray(evaluator.decodedSources.values.map { description ->
                JSONObject().put("backend", description.backend.name).put("width", description.width).put("height", description.height).put("note", description.backendNote)
            }))
        Files.writeString(output.resolve("engine-frames.json"), report.toString(2) + "\n")
    }
    println("PASS P3 PIP 0.4/1.4/2.6/3.5 s and repeated 1.4 s seek, unchanged mobile channel/mask assertions")
}
