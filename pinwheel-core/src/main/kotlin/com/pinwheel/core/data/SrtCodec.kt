package com.pinwheel.core.data

import com.pinwheel.core.model.*
import java.util.Locale

data class SrtParseResult(val cues: List<VideoCaptionCue>, val warnings: List<String>, val skippedCues: Int)

/** Plain-text SubRip interchange. Import issues are reported; no rejected cue is silently discarded. */
object SrtCodec {
    const val MAX_INPUT_CHARACTERS = 2 * 1024 * 1024
    private val time = "(\\d{1,3}):([0-5]\\d):([0-5]\\d)[,.](\\d{1,3})"
    private val timing = Regex("^\\s*$time\\s*-->\\s*$time(?:\\s+.*)?\\s*$")
    private val blocks = Regex("\\n[\\t ]*\\n+")
    private val inlineStyle = Regex("</?(?:b|i|u|s|font)(?:\\s+[^>]*)?>", RegexOption.IGNORE_CASE)
    // Single bounded pass: no XML parser, recursive expansion or external entities.
    private val entity = Regex("&(?:amp|lt|gt|quot|apos|nbsp|#[0-9]{1,8}|#[xX][0-9a-fA-F]{1,6});")

    /** Overlap policy: chronological first cue wins; later intersecting cues are rejected with warnings. */
    fun parse(text: String, durationMs: Long = MAX_VIDEO_CAPTION_TIME_MS): SrtParseResult {
        if (text.length > MAX_INPUT_CHARACTERS) return SrtParseResult(emptyList(), listOf("Subtitle input exceeds the 2 MB text limit."), 0)
        val warnings = mutableListOf<String>()
        var omittedWarnings = 0
        fun warn(message: String) { if (warnings.size < 50) warnings += message else omittedWarnings++ }
        var skipped = 0
        var cueNumber = 0
        val accepted = mutableListOf<VideoCaptionCue>()
        val candidates = mutableListOf<Pair<Int, VideoCaptionCue>>()
        val duration = durationMs.coerceIn(0, MAX_VIDEO_CAPTION_TIME_MS)
        val normalized = text.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n').trim()
        if (normalized.isEmpty()) return SrtParseResult(emptyList(), listOf("The subtitle file is empty."), 0)
        for (block in normalized.split(blocks)) {
            val lines = block.lines()
            val indices = lines.indices.filter { index ->
                val line = lines[index].trimStart()
                "-->" in line && (line.startsWith("-->") ||
                    (line.firstOrNull()?.isDigit() == true && ':' in line.substringBefore("-->")))
            }
            if (indices.isEmpty()) { skipped++; warn("Block ${++cueNumber}: missing subtitle timing; skipped."); continue }
            if (indices.first() > 1 || (indices.first() == 1 && !lines.first().trim().all { it.isDigit() })) {
                warn("Block ${cueNumber + 1}: unexpected text before the timing line was ignored.")
            }
            if (indices.size > 1) warn("Block ${cueNumber + 1}: missing blank separators were recovered.")
            for ((part, index) in indices.withIndex()) {
                cueNumber++
                val match = timing.matchEntire(lines[index])
                val start = match?.let { timestamp(it.groupValues, 1) }
                val end = match?.let { timestamp(it.groupValues, 5) }
                if (start == null || end == null || start < 0 || end <= start || end > MAX_VIDEO_CAPTION_TIME_MS) {
                    skipped++; warn("Cue $cueNumber: invalid or unsupported timing; skipped."); continue
                }
                var bodyEnd = indices.getOrNull(part + 1) ?: lines.size
                if (part + 1 < indices.size && bodyEnd > index + 1 && lines[bodyEnd - 1].trim().all { it.isDigit() }) bodyEnd--
                val originalBody = lines.subList(index + 1, bodyEnd).joinToString("\n").trim()
                val unstyled = originalBody.replace(inlineStyle, "")
                if (unstyled != originalBody) warn("Cue $cueNumber: inline subtitle styling was converted to plain text.")
                val body = decodeEntities(unstyled)
                if (body.isBlank()) { skipped++; warn("Cue $cueNumber: empty caption; skipped."); continue }
                if (start >= duration) { skipped++; warn("Cue $cueNumber: starts outside this video; skipped."); continue }
                if (end > duration) warn("Cue $cueNumber: end trimmed to the video duration.")
                if (body.length > MAX_VIDEO_CAPTION_TEXT_LENGTH) warn("Cue $cueNumber: text shortened to $MAX_VIDEO_CAPTION_TEXT_LENGTH characters.")
                val cue = VideoCaptionCue(text = body, startMs = start, endMs = minOf(end, duration)).sanitized()
                if (cue.text != body && body.length <= MAX_VIDEO_CAPTION_TEXT_LENGTH) warn("Cue $cueNumber: unsupported control characters were removed.")
                if (cue.text.isBlank()) { skipped++; warn("Cue $cueNumber: no displayable text; skipped."); continue }
                candidates += cueNumber to cue
            }
        }
        for ((number, cue) in candidates.sortedBy { it.second.startMs }) {
            if (accepted.size >= MAX_VIDEO_CAPTIONS) { skipped++; warn("Cue $number: the $MAX_VIDEO_CAPTIONS-caption limit was reached; skipped."); continue }
            if (accepted.lastOrNull()?.endMs?.let { cue.startMs < it } == true) {
                skipped++; warn("Cue $number: overlaps an earlier caption; skipped."); continue
            }
            accepted += cue
        }
        if (omittedWarnings > 0) warnings += "$omittedWarnings additional import warnings; $skipped cues skipped in total."
        return SrtParseResult(accepted, warnings, skipped)
    }

    /** Canonical chronological numbering, comma milliseconds and LF endings; style is separate from SRT. */
    fun write(cues: List<VideoCaptionCue>): String {
        require(cues.size <= MAX_VIDEO_CAPTIONS) { "At most $MAX_VIDEO_CAPTIONS captions can be exported." }
        val ordered = cues.sortedBy { it.startMs }
        ordered.forEachIndexed { index, cue ->
            require(cue.startMs >= 0 && cue.endMs > cue.startMs && cue.endMs <= MAX_VIDEO_CAPTION_TIME_MS) { "Caption ${index + 1} has invalid timing." }
            require(cue.text.isNotBlank() && cue.text.length <= MAX_VIDEO_CAPTION_TEXT_LENGTH) { "Caption ${index + 1} has empty or oversized text." }
            require(index == 0 || cue.startMs >= ordered[index - 1].endMs) { "Caption ${index + 1} overlaps another caption." }
        }
        return ordered.mapIndexed { index, cue ->
            // Empty text lines are SubRip separators; omit them inside a caption body.
            val body = cue.text.replace("\r\n", "\n").replace('\r', '\n').lines().filter { it.isNotBlank() }.joinToString("\n").trim()
                .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            "${index + 1}\n${format(cue.startMs)} --> ${format(cue.endMs)}\n$body\n"
        }.joinToString("\n")
    }

    private fun timestamp(parts: List<String>, at: Int): Long? {
        val hours = parts[at].toLongOrNull() ?: return null
        val minutes = parts[at + 1].toLongOrNull() ?: return null
        val seconds = parts[at + 2].toLongOrNull() ?: return null
        val millis = parts[at + 3].padEnd(3, '0').toLongOrNull() ?: return null
        return ((hours * 60 + minutes) * 60 + seconds) * 1000 + millis
    }

    private fun decodeEntities(text: String): String = entity.replace(text) { match ->
        val token = match.value.substring(1, match.value.length - 1)
        when (token) {
            "amp" -> "&"; "lt" -> "<"; "gt" -> ">"; "quot" -> "\""; "apos" -> "'"; "nbsp" -> "\u00a0"
            else -> {
                val hex = token.startsWith("#x", true)
                val point = token.substring(if (hex) 2 else 1).toIntOrNull(if (hex) 16 else 10)
                if (point != null && point in 0..0x10ffff && point !in 0xd800..0xdfff)
                    String(Character.toChars(point)) else match.value
            }
        }
    }

    private fun format(ms: Long) = String.format(Locale.ROOT, "%02d:%02d:%02d,%03d", ms / 3600000, ms / 60000 % 60, ms / 1000 % 60, ms % 1000)
}
