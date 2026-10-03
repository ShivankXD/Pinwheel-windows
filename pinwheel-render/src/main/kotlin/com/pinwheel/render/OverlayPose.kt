package com.pinwheel.render

import com.pinwheel.core.model.VideoImageOverlay

/** [alpha, dx (fraction of width), scale, rotation] for an overlay's entrance and exit animation. */
internal fun overlayPose(image: VideoImageOverlay, timeUs: Long): FloatArray {
    val t = timeUs / 1000
    val sinceIn = (t - image.startMs).toFloat(); val beforeOut = (image.endMs - t).toFloat()
    val pose = floatArrayOf(1f, 0f, 1f, 0f)
    fun apply(kind: String, p: Float, entering: Boolean) {
        if (p >= 1f) return
        val e = 1f - (1f - p) * (1f - p) * (1f - p)
        when (kind) {
            "Fade" -> pose[0] *= p
            "Zoom" -> { pose[0] *= p; pose[2] *= .4f + .6f * e }
            "Slide" -> { pose[0] *= p; pose[1] += (1f - e) * (if (entering) -.35f else .35f) }
            "Spin" -> { pose[0] *= p; pose[3] += (1f - e) * (if (entering) -180f else 180f); pose[2] *= .5f + .5f * e }
            "Pop" -> { val c1 = 1.70158f; val y = p - 1f; pose[2] *= (1f + (c1 + 1f) * y * y * y + c1 * y * y).coerceAtLeast(0f); pose[0] *= (p * 3f).coerceAtMost(1f) }
        }
    }
    if (image.animIn != "None") apply(image.animIn, (sinceIn / 450f).coerceIn(0f, 1f), true)
    if (image.animOut != "None") apply(image.animOut, (beforeOut / 450f).coerceIn(0f, 1f), false)
    return pose
}
