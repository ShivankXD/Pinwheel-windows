package com.pinwheel.render.qa

import com.pinwheel.core.media.video.FxShaderSources
import com.pinwheel.render.PreviewTileRenderer
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject
import javax.imageio.ImageIO

/** Separate owner-review report. Never changes frames, shared shaders or strict historical metrics. */
fun main(args: Array<String>) {
    val root = Path.of(args[0]); val refs = Path.of(args[1]); val output = root.resolve("evidence/p2")
    val policy = JSONObject(Files.readString(root.resolve("docs/p2-noise-policy.json")))
    val limits = StructuralGoldens.Limits(policy.getDouble("perChannelBlurredMae8Max"), policy.getDouble("perChannelHistogramWasserstein8Max"),
        policy.getDouble("perChannelMeanError8Max"), policy.getDouble("meanLuminanceError8Max"))
    val provisional = policy.getString("limitsStatus") != "OWNER_APPROVED"
    val classes = JSONArray(Files.readString(root.resolve("docs/p2-noise-classification.json")))
    val byId = (0 until classes.length()).associate { classes.getJSONObject(it).let { row -> row.getString("id") to row } }
    val specs = PreviewTileRenderer.catalogSpecs()
    check(byId.keys == specs.map { it.id }.toSet()) { "Noise classification does not cover the exact catalog" }
    for (spec in specs) {
        val hash = MessageDigest.getInstance("SHA-256").digest(FxShaderSources.source(spec).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        check(byId.getValue(spec.id).getString("sourceSha256") == hash) { "Noise classification source drift: ${spec.id}" }
    }
    val strict = JSONObject(Files.readString(root.resolve("evidence/p1/runtime/golden-audit-status.json")))
    val cases = strict.getJSONArray("goldenCases"); var noisePassed = 0; var noiseFailed = 0; var deterministicPassed = 0; var deterministicFailed = 0
    for (i in 0 until cases.length()) {
        val item = cases.getJSONObject(i); val id = item.getString("id"); val noise = byId.getValue(id).getString("class") == "noise-driven"
        item.put("class", if (noise) "noise-driven" else "deterministic").put("strictStatus", item.getString("status"))
        if (noise && item.has("channelsRGBA")) {
            val reference = requireNotNull(ImageIO.read(Path.of(item.getString("reference")).toFile()))
            val actual = requireNotNull(ImageIO.read(Path.of(item.getString("actual")).toFile()))
            val diff = output.resolve("structural-heatmaps/$id/${item.get("time")}.png")
            val result = StructuralGoldens.compare(reference, actual, diff, policy.getDouble("gaussianSigmaPx"))
            val passed = result.passes(limits)
            item.put("structuralHeatmap", diff.toString()).put("structuralCandidatePassed", passed)
                .put("structuralChannelsRGBA", JSONArray(result.channels.map { c -> JSONObject().put("blurredMae8", c.blurredMae8)
                    .put("histogramWasserstein8", c.histogramWasserstein8).put("meanError8", c.meanError8)
                    .put("blurredMaePassed", c.blurredMae8 <= limits.blurredMae8).put("histogramPassed", c.histogramWasserstein8 <= limits.histogramWasserstein8).put("meanPassed", c.meanError8 <= limits.meanError8) }))
                .put("meanLuminanceReference8", result.meanLuminanceReference8).put("meanLuminanceActual8", result.meanLuminanceActual8)
                .put("meanLuminanceError8", result.meanLuminanceError8).put("luminancePassed", result.meanLuminanceError8 <= limits.meanLuminanceError8)
                .put("reviewStatus", if (!passed) "FAIL" else if (provisional) "PROVISIONAL_PASS" else "PASS")
            if (passed) noisePassed++ else noiseFailed++
            if ((noisePassed + noiseFailed) % 100 == 0) println("Structural comparisons: ${noisePassed + noiseFailed} noise frames")
        } else if (noise) {
            noiseFailed++; item.put("reviewStatus", "FAIL").put("structuralCandidatePassed", false)
        } else {
            if (item.getString("status") == "PASS") deterministicPassed++ else deterministicFailed++
            item.put("reviewStatus", item.getString("status"))
        }
    }
    strict.put("noisePolicy", policy).put("mode", "owner-noise-policy")
        .put("noiseCandidatePassed", noisePassed).put("noiseCandidateFailed", noiseFailed)
        .put("deterministicPassed", deterministicPassed).put("deterministicFailed", deterministicFailed)
    Files.writeString(output.resolve("structural-status.json"), strict.toString(2))
    println("Deterministic strict frames: $deterministicPassed passed, $deterministicFailed failed")
    println("Noise structural frames: $noisePassed ${if (provisional) "provisional passes" else "passes"}, $noiseFailed failures")
    println("Structural report: ${output.resolve("structural-status.json")}")
    if (args.getOrNull(2) == "check") {
        check(strict.getInt("goldenMissing") == 0) { "Golden inputs missing; inspect structural-status.json" }
        check(deterministicFailed == 0 && noiseFailed == 0) { "$deterministicFailed deterministic strict failures and $noiseFailed noise structural failures; inspect structural-status.json and heatmaps" }
        check(!provisional) { "Noise structural limits still require owner review; deterministic thresholds are unchanged" }
    }
}
