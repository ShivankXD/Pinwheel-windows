package com.pinwheel.core.ui
import com.pinwheel.core.model.MAX_VIDEO_CAPTION_TIME_MS

fun captionTime(ms: Long): String = java.lang.String.format(java.util.Locale.ROOT, "%02d:%02d.%03d", ms.coerceAtLeast(0) / 60_000, ms.coerceAtLeast(0) / 1000 % 60, ms.coerceAtLeast(0) % 1000)

fun parseCaptionTime(value: String): Long? {
    val match = Regex("^(?:(\\d{1,2}):)?(\\d{1,4}):([0-5]\\d)(?:[.,](\\d{1,3}))?$").matchEntire(value.trim()) ?: return null
    val hours = match.groupValues[1].toLongOrNull() ?: 0
    val minutes = match.groupValues[2].toLongOrNull() ?: return null
    if (match.groupValues[1].isNotEmpty() && minutes >= 60) return null
    val seconds = match.groupValues[3].toLong()
    val millis = match.groupValues[4].padEnd(3, '0').toLongOrNull() ?: 0
    return (hours * 3_600_000 + minutes * 60_000 + seconds * 1000 + millis).takeIf { it <= MAX_VIDEO_CAPTION_TIME_MS }
}
