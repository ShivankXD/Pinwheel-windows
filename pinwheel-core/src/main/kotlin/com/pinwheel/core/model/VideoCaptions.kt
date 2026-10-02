package com.pinwheel.core.model

import java.util.UUID
import java.text.BreakIterator
import java.util.Locale

/** Composition-clock caption; end is exclusive. Cues remain independent of the 32 title-layer limit. */
data class VideoCaptionCue(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val startMs: Long = 0,
    val endMs: Long = 3000,
    /** Recognizer word starts relative to this cue. Empty for manually written/imported captions. */
    val wordOffsetsMs: List<Long> = emptyList(),
) {
    fun sanitized(): VideoCaptionCue {
        val start = startMs.coerceIn(0, MAX_VIDEO_CAPTION_TIME_MS - 1)
        val safeText = text.replace("\r\n", "\n").replace('\r', '\n')
            .filter { it >= ' ' || it == '\n' || it == '\t' }.captionTextLimit()
        val end = endMs.coerceIn(start + 1, MAX_VIDEO_CAPTION_TIME_MS)
        val words = safeText.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val safeOffsets = wordOffsetsMs.takeIf { offsets ->
            offsets.size == words.size && offsets.size <= 80 && offsets.firstOrNull() == 0L &&
                offsets.zipWithNext().all { (a, b) -> a <= b } && offsets.all { it in 0 until end - start }
        } ?: emptyList()
        return copy(id = id.take(100).ifBlank { UUID.randomUUID().toString() },
            text = safeText, startMs = start, endMs = end, wordOffsetsMs = safeOffsets)
    }
}

/** Size is relative to the short canvas edge; y is the center anchor measured from the top. */
data class VideoCaptionStyle(
    val preset: String = "Outline",
    val size: Float = .055f,
    val y: Float = .84f,
    val color: Long = 0xffffffffL,
    val background: Long = 0L,
    val bold: Boolean = true,
    val fontFamily: String = "sans-serif",
    val alignment: String = "Center",
    /** Recovered package identity. Empty means a built-in editable recipe. */
    val templateId: String = "",
    /** Pinwheel animated caption template id. Empty uses the static preset or recovered package. */
    val motion: String = "",
    /** Emphasise likely keywords in the template accent colour (Auto highlight). */
    val keywordHighlight: Boolean = false,
) {
    fun sanitized() = copy(
        preset = preset.takeIf { it in VIDEO_CAPTION_PRESETS } ?: "Outline",
        size = if (size.isFinite()) size.coerceIn(.025f, .12f) else .055f,
        y = if (y.isFinite()) y.coerceIn(.05f, .95f) else .84f,
        color = color and 0xffffffffL, background = background and 0xffffffffL,
        fontFamily = fontFamily.takeIf { it in VIDEO_CAPTION_FONTS } ?: "sans-serif",
        alignment = alignment.takeIf { it in VIDEO_CAPTION_ALIGNMENTS } ?: "Center",
        templateId = templateId.takeIf { it.matches(Regex("[0-9]{1,24}")) } ?: "",
        motion = motion.takeIf { it.matches(Regex("[a-z0-9-]{1,40}")) } ?: "",
    )
}

fun captionOverlaps(cues: List<VideoCaptionCue>, candidate: VideoCaptionCue): Boolean = cues.any {
    it.id != candidate.id && candidate.startMs < it.endMs && candidate.endMs > it.startMs
}

/** Find a free region at or after the playhead; never push or overwrite another caption. */
fun newCaptionAt(cues: List<VideoCaptionCue>, durationMs: Long, atMs: Long): VideoCaptionCue? {
    if (cues.size >= MAX_VIDEO_CAPTIONS) return null
    val duration = durationMs.coerceIn(0, MAX_VIDEO_CAPTION_TIME_MS)
    var cursor = atMs.coerceIn(0, duration)
    for (cue in cues.sortedBy { it.startMs }) {
        if (cue.endMs <= cursor) continue
        val gapEnd = cue.startMs.coerceIn(0, duration)
        if (gapEnd - cursor >= 100) return VideoCaptionCue(startMs = cursor, endMs = minOf(cursor + 3000, gapEnd))
        cursor = maxOf(cursor, cue.endMs.coerceIn(0, duration))
        if (duration - cursor < 100) return null
    }
    return if (duration - cursor >= 100) VideoCaptionCue(startMs = cursor, endMs = minOf(cursor + 3000, duration)) else null
}

/** Split text at a character boundary and timing at the global playhead, keeping both parts useful. */
fun splitCaption(cue: VideoCaptionCue, atMs: Long, textOffset: Int): Pair<VideoCaptionCue, VideoCaptionCue>? {
    if (atMs <= cue.startMs || atMs >= cue.endMs || atMs - cue.startMs < 100 || cue.endMs - atMs < 100) return null
    var offset = textOffset.coerceIn(0, cue.text.length)
    val characters = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(cue.text) }
    if (!characters.isBoundary(offset)) offset = characters.preceding(offset).coerceAtLeast(0)
    if (offset > 0 && offset < cue.text.length && cue.text[offset].isLowSurrogate() && cue.text[offset - 1].isHighSurrogate()) offset--
    val left = cue.text.substring(0, offset).trim()
    val right = cue.text.substring(offset).trim()
    if (left.isEmpty() || right.isEmpty()) return null
    return cue.copy(text = left, endMs = atMs, wordOffsetsMs = emptyList()) to cue.copy(id = UUID.randomUUID().toString(), text = right, startMs = atMs, wordOffsetsMs = emptyList())
}

const val MAX_VIDEO_CAPTIONS = 1000
const val MAX_VIDEO_CAPTION_TEXT_LENGTH = 500
const val MAX_VIDEO_CAPTION_TIME_MS = 24L * 60 * 60 * 1000
val VIDEO_CAPTION_PRESETS = listOf("Outline", "Clean", "Box", "Highlight", "Film", "Typewriter", "Neon", "Broadcast", "Chalk", "Pop",
    "Mono", "Ivory", "Ember", "Ocean", "Crimson", "Mint", "Shadow", "Block", "Serif", "Midnight")
val VIDEO_CAPTION_FONTS = listOf("sans-serif", "serif", "monospace", "sans-serif-condensed", "cursive", "sans-serif-black", "casual")
val VIDEO_CAPTION_ALIGNMENTS = listOf("Left", "Center", "Right")

/** Built-in, editable style recipes; all use the same timestamped preview/export painter. */
fun captionPresetStyle(name: String, base: VideoCaptionStyle): VideoCaptionStyle = when (name) {
    "Clean" -> base.copy(preset = name, color = 0xffffffffL, background = 0L, fontFamily = "sans-serif", bold = false)
    "Box" -> base.copy(preset = name, color = 0xffffffffL, background = 0xe6000000L, fontFamily = "sans-serif", bold = true)
    "Highlight" -> base.copy(preset = name, color = 0xff101215L, background = 0xfff6cf5bL, fontFamily = "sans-serif", bold = true)
    "Film" -> base.copy(preset = name, color = 0xfff6eee0L, background = 0L, fontFamily = "serif", bold = false)
    "Typewriter" -> base.copy(preset = name, color = 0xffffffffL, background = 0xd9212326L, fontFamily = "monospace", bold = false)
    "Neon" -> base.copy(preset = name, color = 0xffff8ed4L, background = 0L, fontFamily = "sans-serif-black", bold = true)
    "Broadcast" -> base.copy(preset = name, color = 0xffffffffL, background = 0xe61d3557L, fontFamily = "sans-serif-condensed", bold = true)
    "Chalk" -> base.copy(preset = name, color = 0xfffaf3dfL, background = 0L, fontFamily = "casual", bold = false)
    "Pop" -> base.copy(preset = name, color = 0xff15212dL, background = 0xffffd445L, fontFamily = "sans-serif-black", bold = true)
    "Mono" -> base.copy(preset = name, color = 0xffffffffL, background = 0L, fontFamily = "monospace", bold = true)
    "Ivory" -> base.copy(preset = name, color = 0xffffeed9L, background = 0xb326221fL, fontFamily = "serif", bold = false)
    "Ember" -> base.copy(preset = name, color = 0xffffa05cL, background = 0L, fontFamily = "sans-serif-black", bold = true)
    "Ocean" -> base.copy(preset = name, color = 0xff8fe7efL, background = 0xd91a3440L, fontFamily = "sans-serif", bold = true)
    "Crimson" -> base.copy(preset = name, color = 0xffffffffL, background = 0xe6a82d43L, fontFamily = "sans-serif-black", bold = true)
    "Mint" -> base.copy(preset = name, color = 0xff18352eL, background = 0xff9de6bdL, fontFamily = "sans-serif", bold = true)
    "Shadow" -> base.copy(preset = name, color = 0xffffffffL, background = 0L, fontFamily = "sans-serif-black", bold = true)
    "Block" -> base.copy(preset = name, color = 0xff14191fL, background = 0xffffffffL, fontFamily = "sans-serif-black", bold = true)
    "Serif" -> base.copy(preset = name, color = 0xfff7eddaL, background = 0L, fontFamily = "serif", bold = true)
    "Midnight" -> base.copy(preset = name, color = 0xffdbcaffL, background = 0xd92b1e45L, fontFamily = "serif", bold = true)
    else -> base.copy(preset = "Outline", color = 0xffffffffL, background = 0L, fontFamily = "sans-serif", bold = true)
}.copy(templateId = "", motion = "").sanitized()

private fun String.captionTextLimit(): String {
    if (length <= MAX_VIDEO_CAPTION_TEXT_LENGTH) return this
    val end = if (this[MAX_VIDEO_CAPTION_TEXT_LENGTH - 1].isHighSurrogate() && this[MAX_VIDEO_CAPTION_TEXT_LENGTH].isLowSurrogate())
        MAX_VIDEO_CAPTION_TEXT_LENGTH - 1 else MAX_VIDEO_CAPTION_TEXT_LENGTH
    return substring(0, end)
}
