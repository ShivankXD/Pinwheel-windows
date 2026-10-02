package com.pinwheel.core.media.captions

import com.pinwheel.core.model.VideoCaptionCue
import kotlin.math.roundToLong

data class CaptionWord(val text: String, val startMs: Long)

/** Recognition supplies word starts, so display ends use the next group or a bounded final hold. */
fun captionWordsToCues(words: List<CaptionWord>, sourceDurationMs: Long, speed: Float, projectStartMs: Long): List<VideoCaptionCue> {
    require(speed.isFinite() && speed in .1f..10f && sourceDurationMs > 0)
    require(words.zipWithNext().all { (a,b) -> a.startMs <= b.startMs }) { "Speech timing was not chronological." }
    val valid = words.filter { it.text.isNotBlank() && it.startMs in 0 until sourceDurationMs }
    val groups = mutableListOf<List<CaptionWord>>()
    var group = mutableListOf<CaptionWord>()
    for (word in valid) {
        if (group.isNotEmpty() && (group.size >= 8 || group.sumOf { it.text.length + 1 } + word.text.length > 70 ||
            word.startMs - group.first().startMs > 3000 || word.startMs - group.last().startMs > 900)) {
            groups += group; group = mutableListOf()
        }
        group += word
    }
    if (group.isNotEmpty()) groups += group
    return groups.mapIndexedNotNull { index, values ->
        val sourceEnd = minOf(sourceDurationMs, groups.getOrNull(index + 1)?.first()?.startMs ?: (values.last().startMs + 1200))
        val start = projectStartMs + (values.first().startMs / speed.toDouble()).roundToLong()
        val end = projectStartMs + (sourceEnd / speed.toDouble()).roundToLong()
        if (end <= start) null else VideoCaptionCue(text = values.joinToString(" ") { it.text.trim() }, startMs = start, endMs = end,
            wordOffsetsMs = values.map { ((it.startMs - values.first().startMs) / speed.toDouble()).roundToLong().coerceAtLeast(0) }).sanitized()
    }
}

private val FILLERS = setOf("um", "umm", "uh", "uhh", "uhm", "erm", "er", "ah", "ahh", "hmm", "hm", "mm", "mhm")

/** Drops hesitation sounds ("um", "uh") while keeping every other recognised word and its timing. */
fun removeFillerWords(words: List<CaptionWord>): List<CaptionWord> =
    words.filterNot { it.text.trim { c -> !c.isLetter() }.lowercase(java.util.Locale.ROOT) in FILLERS }
