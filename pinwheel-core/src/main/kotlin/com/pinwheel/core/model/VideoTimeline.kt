package com.pinwheel.core.model

import kotlin.math.roundToLong

enum class TimelineEdge { MOVE, START, END }

/** One drag changes one layer; audio edges change source trim as well as timeline bounds. */
fun VideoProjectEdits.dragLayer(kind: String, id: String, edge: TimelineEdge, deltaMs: Long, durationMs: Long): VideoProjectEdits {
    val limit = durationMs.coerceAtLeast(100)
    fun range(start: Long, end: Long): Pair<Long, Long> = when (edge) {
        TimelineEdge.MOVE -> {
            val next = (start + deltaMs).coerceIn(0, (limit - (end - start)).coerceAtLeast(0))
            next to next + end - start
        }
        TimelineEdge.START -> (start + deltaMs).coerceIn(0, (end - minOf(100, end - start)).coerceAtLeast(0)) to end
        TimelineEdge.END -> start to (end + deltaMs).coerceIn(start + minOf(100, end - start), maxOf(limit, end))
    }
    return when (kind) {
        "Text" -> copy(texts = texts.map { item -> if (item.id != id) item else {
            val (start, end) = range(item.startMs, item.endMs); item.copy(startMs = start, endMs = end)
        } })
        "Image" -> copy(images = images.map { item -> if (item.id != id) item else {
            val source = item.video
            if (source == null || edge == TimelineEdge.MOVE) {
                val (start, end) = range(item.startMs, item.endMs); item.copy(startMs = start, endMs = end)
            } else if (edge == TimelineEdge.START) {
                val shift = deltaMs.coerceIn(-minOf(item.startMs, source.startMs),
                    (item.endMs - item.startMs - minOf(100, item.endMs - item.startMs)).coerceAtLeast(0))
                item.copy(startMs = item.startMs + shift, video = source.copy(startMs = source.startMs + shift))
            } else {
                val maxEnd = minOf(limit, item.startMs + source.durationMs - source.startMs).coerceAtLeast(item.endMs)
                item.copy(endMs = (item.endMs + deltaMs).coerceIn(item.startMs + minOf(100, item.endMs - item.startMs), maxEnd))
            }
        } })
        "Effect" -> copy(effects = effects.map { item -> if (item.id != id) item else {
            val (start, end) = range(item.startMs, item.endMs); item.copy(startMs = start, endMs = end)
        } })
        "Caption" -> copy(captions = captions.map { item -> if (item.id != id) item else {
            val (start, end) = range(item.startMs, item.endMs)
            val candidate = item.copy(startMs = start, endMs = end,
                wordOffsetsMs = if (edge == TimelineEdge.MOVE) item.wordOffsetsMs else emptyList())
            if (captionOverlaps(captions, candidate)) item else candidate
        } })
        "Audio" -> copy(audio = audio.map { item -> if (item.id != id) item else when (edge) {
            TimelineEdge.MOVE -> item.copy(startMs = (item.startMs + deltaMs).coerceIn(0,
                if (item.extendsVideo) Long.MAX_VALUE / 2 else (limit - item.durationMs).coerceAtLeast(0)))
            TimelineEdge.START -> {
                val shift = deltaMs.coerceAtLeast(-item.startMs)
                val source = (item.sourceStartMs + (shift * item.speed).roundToLong())
                    .coerceIn(0, (item.sourceEndMs - 100).coerceAtLeast(0))
                item.copy(sourceStartMs = source, startMs = (item.startMs +
                    ((source - item.sourceStartMs) / item.speed).roundToLong()).coerceAtLeast(0))
            }
            TimelineEdge.END -> {
                val maxEnd = if (item.extendsVideo) item.sourceDurationMs else
                    minOf(item.sourceDurationMs, item.sourceEndMs +
                        ((limit - item.startMs - item.durationMs).coerceAtLeast(0) * item.speed).roundToLong())
                item.copy(sourceEndMs = (item.sourceEndMs + (deltaMs * item.speed).roundToLong())
                    .coerceIn((item.sourceStartMs + 100).coerceAtMost(maxEnd), maxEnd))
            }
        } })
        else -> this
    }
}

/** One layer on a timeline group; [lane] is the row the user last left it on. */
data class TimelineSpan(val id: String, val startMs: Long, val endMs: Long, val lane: Int = 0)

/**
 * CapCut-style rows for one layer group. Each layer keeps its preferred row while that
 * row is free for its time range; otherwise it drops to the next free row below.
 */
fun packTimelineLanes(items: List<TimelineSpan>): Map<String, Int> {
    val rows = mutableListOf<MutableList<LongRange>>()
    val result = LinkedHashMap<String, Int>()
    for (item in items.sortedWith(compareBy({ it.lane }, { it.startMs }))) {
        val span = item.startMs until item.endMs.coerceAtLeast(item.startMs + 1)
        var lane = item.lane.coerceIn(0, MAX_TIMELINE_LANE)
        while (true) {
            while (rows.size <= lane) rows += mutableListOf<LongRange>()
            if (rows[lane].none { it.first < span.last + 1 && span.first < it.last + 1 }) break
            lane++
        }
        rows[lane] += span; result[item.id] = lane
    }
    return result
}

fun VideoProjectEdits.withLane(kind: String, id: String, lane: Int): VideoProjectEdits {
    val row = lane.coerceIn(0, MAX_TIMELINE_LANE)
    return when (kind) {
        "Text" -> copy(texts = texts.map { if (it.id == id) it.copy(lane = row) else it })
        "Image" -> copy(images = images.map { if (it.id == id) it.copy(lane = row) else it })
        "Effect" -> copy(effects = effects.map { if (it.id == id) it.copy(lane = row) else it })
        "Audio" -> copy(audio = audio.map { if (it.id == id) it.copy(lane = row) else it })
        else -> this
    }
}

/** Nearest snap target within [thresholdMs] of [valueMs], or null when nothing is close enough. */
fun snapTimelineMs(valueMs: Long, targets: Collection<Long>, thresholdMs: Long): Long? =
    targets.minByOrNull { kotlin.math.abs(it - valueMs) }?.takeIf { kotlin.math.abs(it - valueMs) <= thresholdMs }

/** Convert the composition playhead to clip index and offset within that clip's trimmed segment. */
fun globalToClip(project: StudioProject, totalMs: Long): Pair<Int, Long> {
    if (project.clips.isEmpty()) return 0 to 0L
    var remaining = totalMs.coerceAtLeast(0)
    project.clips.forEachIndexed { index, clip ->
        if (remaining < clip.durationMs || index == project.clips.lastIndex) {
            return index to remaining.coerceAtMost(clip.durationMs)
        }
        remaining -= clip.durationMs
    }
    return 0 to 0L
}

/** Offset is in playback time, not absolute source-media time. */
fun clipToGlobal(project: StudioProject, clipIndex: Int, positionMs: Long): Long {
    if (project.clips.isEmpty()) return 0
    val index = clipIndex.coerceIn(0, project.clips.lastIndex)
    val start = project.clips.take(index).fold(0L) { total, clip -> total + clip.durationMs.coerceAtMost(Long.MAX_VALUE - total) }
    return start + positionMs.coerceIn(0, project.clips[index].durationMs).coerceAtMost(Long.MAX_VALUE - start)
}

/** Keep layers on the composition clock after shortening it: trim overlaps and drop layers outside it. */
fun VideoProjectEdits.clampedToDuration(durationMs: Long): VideoProjectEdits {
    val safe = sanitized()
    val duration = durationMs.coerceAtLeast(0)
    return safe.copy(
        texts = safe.texts.filter { it.startMs < duration }.map { it.copy(endMs = it.endMs.coerceAtMost(duration)) },
        images = safe.images.filter { it.startMs < duration }.map { it.copy(endMs = it.endMs.coerceAtMost(duration)) },
        captions = safe.captions.filter { it.startMs < duration }.map { it.copy(endMs = it.endMs.coerceAtMost(duration)) },
        effects = safe.effects.filter { it.startMs < duration }.map { it.copy(endMs = it.endMs.coerceAtMost(duration)) },
        // Keep the source trim when the project is shortened. Playback clips the audible
        // portion to the movie; lengthening the movie again can reveal the remaining sound.
        audio = safe.audio.filter { it.startMs < duration },
    )
}
