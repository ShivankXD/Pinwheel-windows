package com.pinwheel.core.model

import kotlin.math.ceil
import kotlin.math.roundToLong

/** Timeline gestures use playback time; source boundaries must account for clip speed. */
fun timelineTrim(clip: Clip, startEdge: Boolean, deltaPlaybackMs: Double): Clip {
    if (!deltaPlaybackMs.isFinite() || clip.sourceDurationMs <= 0) return clip
    val delta = (deltaPlaybackMs.coerceIn(-86_400_000.0,86_400_000.0) * clip.playbackSpeed).roundToLong()
    val minimum = maxOf(100L,ceil(100.0 * clip.playbackSpeed).toLong()).coerceAtMost(clip.sourceDurationMs)
    return if (startEdge) clip.trim((clip.startMs+delta).coerceIn(0,(clip.endMs-minimum).coerceAtLeast(0)),clip.endMs)
    else clip.trim(clip.startMs,(clip.endMs+delta).coerceIn((clip.startMs+minimum).coerceAtMost(clip.sourceDurationMs),clip.sourceDurationMs))
}
