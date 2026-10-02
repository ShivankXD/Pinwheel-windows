package com.pinwheel.core.media.video

import org.json.JSONArray

/** Numeric keyframes read from recovered content.json. Amaz programs and materials are never executed. */
data class CaptionMotionPoint(
    val timeUs: Long, val value: Float, val interpolation: String,
    val inValue: Float = 0f, val outValue: Float = 0f, val inTime: Float = 0f, val outTime: Float = 0f,
)

data class CaptionMotionChannel(
    val selector: String, val property: String, val mode: String, val startUs: Long,
    val endUs: Long, val points: List<CaptionMotionPoint>,
) {
    fun valueAt(progress: Float): Float {
        if (points.isEmpty()) return 0f
        val t = startUs + progress.coerceIn(0f, 1f) * (endUs - startUs).coerceAtLeast(1)
        if (t <= points.first().timeUs) return points.first().value
        if (t >= points.last().timeUs) return points.last().value
        val rightIndex = points.indexOfFirst { it.timeUs >= t }.coerceAtLeast(1)
        val a = points[rightIndex - 1]; val b = points[rightIndex]
        val linear = ((t - a.timeUs) / (b.timeUs - a.timeUs).coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
        if (a.interpolation != "cubic" || (a.outTime == 0f && b.inTime == 0f))
            return a.value + (b.value - a.value) * linear
        val t0 = a.timeUs.toFloat(); val t1 = b.timeUs.toFloat()
        fun bezier(p0: Float, p1: Float, p2: Float, p3: Float, u: Float): Float {
            val v = 1f - u
            return v * v * v * p0 + 3f * v * v * u * p1 + 3f * v * u * u * p2 + u * u * u * p3
        }
        val c1t = (t0 + a.outTime).coerceIn(t0, t1)
        val c2t = (t1 + b.inTime).coerceIn(t0, t1)
        var low = 0f; var high = 1f
        repeat(12) {
            val middle = (low + high) / 2f
            if (bezier(t0, c1t, c2t, t1, middle) < t) low = middle else high = middle
        }
        val u = (low + high) / 2f
        return bezier(a.value, a.value + a.outValue, b.value + b.inValue, b.value, u)
    }
}

data class CaptionMotionState(
    val x: Float = 0f, val y: Float = 0f, val scale: Float = 1f,
    val rotation: Float = 0f, val opacity: Float = 1f,
)

object ImportedCaptionMotion {
    fun parse(array: JSONArray?): List<CaptionMotionChannel> = if (array == null) emptyList() else (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        val frames = item.optJSONArray("frames") ?: return@mapNotNull null
        val points = (0 until frames.length()).mapNotNull { n -> frames.optJSONObject(n)?.let { p ->
            val value = p.optDouble("v", Double.NaN).toFloat()
            if (!value.isFinite()) null else CaptionMotionPoint(p.optLong("t"), value.coerceIn(-10000f, 10000f), p.optString("it"),
                p.optDouble("vi", 0.0).toFloat(), p.optDouble("vo", 0.0).toFloat(),
                p.optDouble("vti", 0.0).toFloat(), p.optDouble("vto", 0.0).toFloat())
        } }.sortedBy { it.timeUs }
        if (points.isEmpty()) null else CaptionMotionChannel(item.optString("selector"), item.optString("property"), item.optString("mode"),
            item.optLong("startUs"), item.optLong("endUs", 1_600_000), points)
    }

    /** Sample each channel on its authored microsecond clock. */
    fun state(channels: List<CaptionMotionChannel>, selectors: Set<String>, elapsedUs: Long): CaptionMotionState {
        var x = 0f; var y = 0f; var scale = 1f; var rotation = 0f; var opacity = 1f
        for (channel in channels) {
            if (channel.selector !in selectors) continue
            val span = (channel.endUs - channel.startUs).coerceAtLeast(1)
            val progress = (elapsedUs - channel.startUs).toFloat() / span
            val value = channel.valueAt(progress)
            when (channel.property) {
                "px" -> x += value
                "py" -> y += value
                "s", "sxy" -> scale *= value.coerceIn(.05f, 3f)
                "rz" -> rotation += value
                "ti" -> opacity *= value.coerceIn(0f, 1f)
            }
        }
        // Some source scenes animate an alternate text layer a whole screen away. We
        // render a single caption layer, so keep its translated glyphs in the safe area.
        return CaptionMotionState(x.coerceIn(-.3f, .3f), y.coerceIn(-.25f, .25f),
            scale.coerceIn(.05f, 4f), rotation.coerceIn(-360f, 360f), opacity.coerceIn(0f, 1f))
    }
}
