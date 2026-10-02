package com.pinwheel.core.media

import kotlin.math.*

data class PerspectiveGuide(val start: PhotoPoint, val end: PhotoPoint)

data class GuidedPerspectiveResult(val straighten: Float, val vertical: Float)

/** Solves two real-world vertical lines using their homogeneous vanishing point.
 * Coordinates belong to the full, rotated/flipped/lens-corrected frame, before straighten.
 * Produces the same bounded rotation and keystone parameters used by PhotoEngine.
 */
object PhotoGuidedPerspective {
    fun solve(guides: List<PerspectiveGuide>, aspect: Float): GuidedPerspectiveResult? {
        if (guides.size != 2 || !aspect.isFinite() || aspect <= 0f) return null
        if (guides.any { g ->
            listOf(g.start.x, g.start.y, g.end.x, g.end.y).any { !it.isFinite() || it !in 0f..1f } ||
                abs(g.end.y - g.start.y) < .15f
        }) return null
        // Nearly coincident guides cannot constrain a useful vanishing point reliably.
        fun middleX(g: PerspectiveGuide) = g.start.x + (.5f - g.start.y) * (g.end.x - g.start.x) / (g.end.y - g.start.y)
        if (abs(middleX(guides[0]) - middleX(guides[1])) < .08f) return null
        fun line(g: PerspectiveGuide): DoubleArray {
            val x1 = (g.start.x.toDouble() - .5) * aspect
            val y1 = g.start.y.toDouble() - .5
            val x2 = (g.end.x.toDouble() - .5) * aspect
            val y2 = g.end.y.toDouble() - .5
            return doubleArrayOf(y1 - y2, x2 - x1, x1 * y2 - y1 * x2)
        }
        val a = line(guides[0]); val b = line(guides[1])
        val vx = a[1] * b[2] - a[2] * b[1]
        val vy = a[2] * b[0] - a[0] * b[2]
        val vw = a[0] * b[1] - a[1] * b[0]
        if (hypot(vx, vy) < 1e-9) return null
        var radians = atan2(vx, vy)
        while (radians > PI / 2) radians -= PI
        while (radians < -PI / 2) radians += PI
        val degrees = Math.toDegrees(radians)
        if (abs(degrees) > 30.0001) return null
        val c = cos(radians); val s = sin(radians)
        val cover = max(abs(c) + abs(s) / aspect, abs(c) + abs(s) * aspect)
        val vertical = -vw / (cover * (s * vx + c * vy)) / .65
        if (!vertical.isFinite() || abs(vertical) > 1.000001) return null
        return GuidedPerspectiveResult(degrees.toFloat().coerceIn(-30f, 30f), vertical.toFloat().coerceIn(-1f, 1f))
    }
}
