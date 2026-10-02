package com.pinwheel.core.model

/** Motion uses final timeline time, so trimming/speed changes keep endpoints on clip boundaries. */
class VideoClipMotion(clips: List<Clip>) {
    data class Frame(val scale: Float = 1f, val x: Float = 0f, val y: Float = 0f)
    private data class Span(val start: Long, val end: Long, val kind: String, val amount: Float)
    private val spans = buildList {
        var time = 0L
        clips.forEach { clip ->
            val edit = clip.video.sanitized()
            add(Span(time, time + clip.durationMs * 1000, edit.motion, edit.motionAmount))
            time += clip.durationMs * 1000
        }
    }
    fun at(timeUs: Long): Frame {
        val span = spans.firstOrNull { timeUs >= it.start && timeUs < it.end } ?: return Frame()
        if (span.kind == "None" || span.amount <= 0f) return Frame()
        val progress = ((timeUs - span.start).toDouble() / (span.end - span.start).coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
        val ease = progress * progress * (3f - 2f * progress)
        val margin = .35f * span.amount
        val travel = margin * (2f * ease - 1f)
        return when (span.kind) {
            "Zoom in" -> Frame(1f + margin * ease)
            "Zoom out" -> Frame(1f + margin * (1f - ease))
            "Pan left" -> Frame(1f + margin, -travel)
            "Pan right" -> Frame(1f + margin, travel)
            "Pan up" -> Frame(1f + margin, y = travel)
            "Pan down" -> Frame(1f + margin, y = -travel)
            else -> Frame()
        }
    }
}
