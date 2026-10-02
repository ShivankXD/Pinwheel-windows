package com.pinwheel.render.qa

import com.pinwheel.core.data.*
import com.pinwheel.core.media.video.*
import java.nio.file.Files
import java.nio.file.Path
import org.json.JSONArray
import org.json.JSONObject
import javax.imageio.ImageIO

/** Read-only reference inputs; every report, imported package and diff stays in Windows output. */
fun main(args: Array<String>) {
    val root = Path.of(args[0]); val refs = Path.of(args[1]); val mode = args.getOrElse(2) { "inventory" }
    val output = root.resolve("evidence/p1/runtime"); Files.createDirectories(output)
    val compareGoldens = mode == "golden" || mode == "golden-audit" || mode == "inventory"
    val compareProjects = mode != "golden" && mode != "golden-audit"
    val specs = VideoFxCatalog.effects + VideoFxCatalog.transitions + VideoFxCatalog.overlays + VideoFxCatalog.legacyOverlays
    val cases = JSONArray(); var goldenPassed = 0; var goldenFailed = 0; var missing = 0
    if (compareGoldens) for (spec in specs) for (t in VideoPreviewRecipe.goldenTimes) {
        val relative = "golden/${spec.id}/$t.png"; val reference = refs.resolve(relative)
        val actual = root.resolve("evidence/p2/frames/${spec.id}/$t.png")
        val item = JSONObject().put("id", spec.id).put("time", t).put("reference", reference.toString()).put("actual", actual.toString())
            .put("sample", VideoPreviewRecipe.inputs(spec, t).sample).put("params", JSONObject(spec.defaults()))
        if (!Files.isRegularFile(reference) || !Files.isRegularFile(actual)) {
            missing++; item.put("status", "MISSING").put("referencePresent", Files.isRegularFile(reference)).put("actualPresent", Files.isRegularFile(actual))
        } else {
            try {
                val r = requireNotNull(ImageIO.read(reference.toFile())); val a = requireNotNull(ImageIO.read(actual.toFile()))
                require(r.width == VideoPreviewRecipe.WIDTH && r.height == VideoPreviewRecipe.HEIGHT) { "Expected 192x240 mobile preview tile" }
                val diff = output.resolve("diffs/${spec.id}/$t.png")
                val result = GoldenImages.compare(r, a, diff)
                if (result.passed) goldenPassed++ else goldenFailed++
                item.put("status", if (result.passed) "PASS" else "FAIL").put("diff", diff.toString())
                    .put("channelsRGBA", JSONArray(result.channels.map { JSONObject().put("mae8", it.meanAbsoluteError).put("p99_8", it.percentile99) }))
            } catch (failure: Exception) { goldenFailed++; item.put("status", "FAIL").put("error", failure.stackTraceToString()) }
        }
        cases.put(item)
    }
    val projects = JSONArray(); var projectsPassed = 0; var projectsFailed = 0
    val projectFolder = refs.resolve("projects")
    val files = if (compareProjects && Files.isDirectory(projectFolder)) Files.walk(projectFolder).use { paths -> paths.filter { Files.isRegularFile(it) && (it.toString().endsWith(".json") || it.toString().endsWith(".pinwheel")) }.sorted().toList() } else emptyList()
    for ((index, file) in files.withIndex()) {
        val item = JSONObject().put("source", file.toString())
        try {
            val decoded = if (file.toString().endsWith(".pinwheel")) {
                val work = root.resolve("pinwheel-core/build/reference-work/${java.util.UUID.randomUUID()}-$index")
                ProjectPackage.import(file, work, ProjectStore(work.toFile()))
            } else ProjectCodec.decode(Files.readString(file), strictVideoHistory = true)
            val again = ProjectCodec.decode(ProjectCodec.encode(decoded), strictVideoHistory = true)
            check(decoded == again) { "Mobile project round-trip changed its normalized recipe" }
            projectsPassed++; item.put("status", "PASS").put("id", decoded.id)
        } catch (failure: Exception) { projectsFailed++; item.put("status", "FAIL").put("error", failure.stackTraceToString()) }
        projects.put(item)
    }
    val report = JSONObject().put("mode", mode).put("referenceRoot", refs.toString()).put("referenceRootPresent", Files.isDirectory(refs))
        .put("effects", VideoFxCatalog.effects.size).put("transitions", VideoFxCatalog.transitions.size).put("overlays", VideoFxCatalog.overlays.size + VideoFxCatalog.legacyOverlays.size)
        .put("activeOverlays", VideoFxCatalog.overlays.size).put("legacyOverlays", VideoFxCatalog.legacyOverlays.size)
        .put("goldenPassed", goldenPassed).put("goldenFailed", goldenFailed).put("goldenMissing", missing)
        .put("projectsPassed", projectsPassed).put("projectsFailed", projectsFailed).put("projectsStatus", if (!compareProjects) "NOT_CHECKED" else if (files.isEmpty()) "MISSING" else if (projectsFailed > 0) "FAIL" else "PASS")
        .put("goldenCases", cases).put("projects", projects)
    Files.writeString(output.resolve("reference-status.json"), report.toString(2))
    Files.writeString(output.resolve("$mode-status.json"), report.toString(2))
    if (compareGoldens) println("Golden references: $goldenPassed passed, $goldenFailed failed, $missing missing")
    if (compareProjects) println("Real mobile projects: $projectsPassed passed, $projectsFailed failed, ${if (files.isEmpty()) "MISSING" else "provided"}")
    println("Report: ${output.resolve("reference-status.json")}")
    if (mode == "inventory") check(projectsFailed == 0 && goldenFailed == 0) { "Provided references failed; inspect reference-status.json" }
    if (mode == "project-inventory") check(projectsFailed == 0) { "Provided mobile projects failed; inspect reference-status.json" }
    if (mode == "golden") {
        check(goldenFailed == 0) { "$goldenFailed provided golden comparisons failed; inspect golden-status.json and its heatmaps. Fixed limits: per-channel MAE8 <= 2, p99 <= 8." }
        check(missing == 0) { "$missing golden comparisons missing: provide mobile PNGs in $refs/golden and P2 frames in evidence/p2/frames" }
    }
    if (mode == "projects") check(files.isNotEmpty() && projectsFailed == 0) { "Real mobile package check incomplete: provide projects in $projectFolder" }
}
