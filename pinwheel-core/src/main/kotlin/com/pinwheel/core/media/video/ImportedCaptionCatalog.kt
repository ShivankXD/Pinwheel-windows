package com.pinwheel.core.media.video

import java.nio.file.Path
import java.nio.file.Files
import org.json.JSONArray
import org.json.JSONObject
import com.pinwheel.core.model.VideoCaptionStyle

/** Declarative, non-executable subset of recovered caption packages. No downloaded script runs. */
data class ImportedCaptionPaint(val color: Int, val gradient: IntArray = intArrayOf(), val angle: Float = 90f)
data class ImportedCaptionStroke(val paint: ImportedCaptionPaint, val width: Float)
data class ImportedCaptionShadow(val paint: ImportedCaptionPaint, val dx: Float, val dy: Float, val blur: Float)
data class ImportedCaptionTemplate(
    val id: String, val name: String, val originalName: String, val categories: List<String>,
    val fill: ImportedCaptionPaint, val strokes: List<ImportedCaptionStroke>, val shadows: List<ImportedCaptionShadow>,
    val bold: Boolean, val italic: Boolean, val underline: Boolean, val capital: String,
    val letterSpacing: Float, val lineSpacing: Float, val background: Long,
    val rotation: Float, val scaleX: Float, val scaleY: Float, val issues: List<String>,
    val motions: List<CaptionMotionChannel>,
) {
    val canRender: Boolean get() = fill.visible() || strokes.any { it.paint.visible() } || shadows.any { it.paint.visible() }
    private fun ImportedCaptionPaint.visible() = if (gradient.isNotEmpty()) gradient.any { it ushr 24 != 0 } else color ushr 24 != 0
    fun apply(base: VideoCaptionStyle): VideoCaptionStyle {
        require(canRender) { "This package requires its native text material." }
        return base.copy(templateId = id, motion = "", preset = "Clean", color = fill.color.toLong() and 0xffffffffL,
            background = background, bold = bold, fontFamily = "sans-serif").sanitized()
    }
    val hasSourceMotion: Boolean get() = motions.any { it.selector in setOf("selector_page", "selector_line", "selector_line_unread", "selector_word", "selector_word_unread") }
    val limitation: String get() = if (hasSourceMotion) "Base style + source keyframes · native animation/font pending" else "Base style · native animation/font pending"
}

object ImportedCaptionCatalog {
    @Volatile private var loaded: List<ImportedCaptionTemplate>? = null
    val templates: List<ImportedCaptionTemplate> get() = loaded.orEmpty()
    fun find(id: String): ImportedCaptionTemplate? = loaded?.firstOrNull { it.id == id }
    /** Caller supplies an IO dispatcher. Data is published atomically, then reads are allocation-free. */
    @Synchronized fun load(assets: Path): List<ImportedCaptionTemplate> {
        loaded?.let { return it }
        val array = Files.newInputStream(assets.resolve("caption_packages/catalog.json")).bufferedReader().use { JSONArray(it.readText()) }
        val result = (0 until array.length()).map { index ->
            val j = array.getJSONObject(index)
            ImportedCaptionTemplate(j.getString("id"), j.getString("name"), j.getString("originalName"), j.getJSONArray("categories").strings(),
                j.getJSONObject("fill").paint(), j.getJSONArray("strokes").objects().map { ImportedCaptionStroke(it.paint(), it.optDouble("width").toFloat().coerceIn(0f, .5f)) },
                j.getJSONArray("shadows").objects().map { ImportedCaptionShadow(it.paint(), it.optDouble("dx").toFloat().coerceIn(-1f, 1f), it.optDouble("dy").toFloat().coerceIn(-1f, 1f), it.optDouble("blur").toFloat().coerceIn(0f, 1f)) },
                j.optBoolean("bold"), j.optBoolean("italic"), j.optBoolean("underline"), j.optString("capital"),
                j.optDouble("letterSpacing", 0.0).toFloat().coerceIn(-.1f, 1f), j.optDouble("lineSpacing", .12).toFloat().coerceIn(0f, 1f),
                j.optLong("background"), j.optDouble("rotation", 0.0).toFloat(), j.optDouble("scaleX", 1.0).toFloat().coerceIn(.1f, 3f), j.optDouble("scaleY", 1.0).toFloat().coerceIn(.1f, 3f), j.getJSONArray("issues").strings(),
                ImportedCaptionMotion.parse(j.optJSONArray("motions")))
        }
        loaded = result
        return result
    }
    private fun JSONArray.strings() = (0 until length()).map { getString(it) }
    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
    private fun JSONObject.paint(): ImportedCaptionPaint {
        val g = optJSONArray("gradient")
        return ImportedCaptionPaint(optLong("color", 0xffffffffL).toInt(), if (g != null && g.length() > 1) IntArray(g.length()) { g.getLong(it).toInt() } else intArrayOf(), optDouble("angle", 90.0).toFloat())
    }
}

fun resolveCaptionStyleSelection(selection: String, base: VideoCaptionStyle): VideoCaptionStyle =
    if (selection.startsWith("motion:")) CaptionMotionCatalog.find(selection.removePrefix("motion:"))
        ?.let { base.copy(motion = it.id, templateId = "", y = if (base.y > .8f) .72f else base.y,
            size = if (base.size < .07f) .075f else base.size).sanitized() } ?: base
    else if (selection.startsWith("imported:")) ImportedCaptionCatalog.find(selection.removePrefix("imported:"))?.takeIf { it.canRender }?.apply(base) ?: base
    else com.pinwheel.core.model.captionPresetStyle(selection, base)
