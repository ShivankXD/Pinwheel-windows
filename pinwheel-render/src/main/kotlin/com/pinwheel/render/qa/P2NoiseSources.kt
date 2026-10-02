package com.pinwheel.render.qa

import com.pinwheel.core.media.video.FxShaderSources
import com.pinwheel.render.PreviewTileRenderer
import java.nio.file.Files
import java.nio.file.Path
import org.json.JSONArray
import org.json.JSONObject

/** Source inventory only. No shader edits, native context or reference writes. */
fun main(args: Array<String>) {
    val output = Path.of(args[0]).resolve("evidence/p2/runtime/noise-sources.json")
    Files.createDirectories(output.parent)
    val specs = JSONArray(PreviewTileRenderer.catalogSpecs().map { spec ->
        JSONObject().put("id", spec.id).put("name", spec.name).put("category", spec.category)
            .put("defines", spec.defines).put("source", FxShaderSources.source(spec))
    })
    Files.writeString(output, specs.toString(2)); println("Noise source inventory: ${specs.length()} unchanged catalog sources")
}
