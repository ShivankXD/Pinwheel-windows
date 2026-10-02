package com.pinwheel.core.media

import com.pinwheel.core.model.Adjustments
import com.pinwheel.core.model.CutoutOp
import com.pinwheel.core.model.PhotoCutout
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Keep-mask for the Background tool, in the source photo's normalized space. 1 keeps a pixel, 0 removes
 * it. Built from plain pixels so preview, export and tests share one implementation.
 */
class CutoutMask(val width: Int, val height: Int, val alpha: FloatArray) {
    /** Bilinear keep value at a normalized source position. */
    fun at(nx: Float, ny: Float): Float {
        val x = (nx * width - .5f).coerceIn(0f, width - 1f); val y = (ny * height - .5f).coerceIn(0f, height - 1f)
        val x0 = x.toInt(); val y0 = y.toInt(); val x1 = min(x0 + 1, width - 1); val y1 = min(y0 + 1, height - 1)
        val fx = x - x0; val fy = y - y0
        val top = alpha[y0 * width + x0] * (1 - fx) + alpha[y0 * width + x1] * fx
        val bottom = alpha[y1 * width + x0] * (1 - fx) + alpha[y1 * width + x1] * fx
        return top * (1 - fy) + bottom * fy
    }

    companion object {
        const val EDGE = 900

        /** [pixels] is the source photo (any size), already scaled so its long edge is at most [EDGE]. */
        fun build(pixels: IntArray, w: Int, h: Int, cutout: PhotoCutout, cancel: () -> Unit = {}): CutoutMask {
            val alpha = FloatArray(w * h) { 1f }
            val cover = FloatArray(w * h)
            val short = min(w, h).toFloat()
            fun dist(p: Int, q: Int): Float {
                val dr = ((p shr 16) and 255) - ((q shr 16) and 255); val dg = ((p shr 8) and 255) - ((q shr 8) and 255); val db = (p and 255) - (q and 255)
                // Weighted RGB distance, 0..1.
                return sqrt((2f * dr * dr + 4f * dg * dg + 3f * db * db) / 9f) / 255f
            }
            fun seed(x: Int, y: Int, r: Int): Int {
                var sr = 0; var sg = 0; var sb = 0; var n = 0
                for (yy in max(0, y - r)..min(h - 1, y + r)) for (xx in max(0, x - r)..min(w - 1, x + r)) {
                    val p = pixels[yy * w + xx]; sr += (p shr 16) and 255; sg += (p shr 8) and 255; sb += p and 255; n++
                }
                return if (n == 0) 0 else (-0x1000000 or ((sr / n) shl 16) or ((sg / n) shl 8) or (sb / n))
            }
            for (op in cutout.ops) {
                cancel()
                java.util.Arrays.fill(cover, 0f)
                val first = op.points.first()
                val sx = (first.x * w).toInt().coerceIn(0, w - 1); val sy = (first.y * h).toInt().coerceIn(0, h - 1)
                if (op.fill) floodFill(pixels, w, h, sx, sy, op.tolerance, cover, ::dist)
                else {
                    val r = (op.radius * short).coerceAtLeast(1.5f)
                    fun stamp(cx: Float, cy: Float) {
                        // Smart strokes sample the colour under the brush centre at every step (like a background
                        // eraser): keep the centre on the background and the brush stops at the subject's edge.
                        val seedColour = if (op.smart) seed(cx.toInt().coerceIn(0, w - 1), cy.toInt().coerceIn(0, h - 1), max(1, (r * .15f).toInt())) else 0
                        val x0 = max(0, (cx - r).toInt()); val x1 = min(w - 1, (cx + r).toInt() + 1)
                        val y0 = max(0, (cy - r).toInt()); val y1 = min(h - 1, (cy + r).toInt() + 1)
                        for (y in y0..y1) for (x in x0..x1) {
                            val d = hypot(x + .5f - cx, y + .5f - cy)
                            if (d > r) continue
                            var wgt = ((r - d) / (r * .35f)).coerceIn(0f, 1f)
                            if (op.smart) {
                                val cd = dist(pixels[y * w + x], seedColour)
                                wgt *= (1f - ((cd - op.tolerance * .55f) / (op.tolerance * .45f)).coerceIn(0f, 1f))
                            }
                            val i = y * w + x
                            if (wgt > cover[i]) cover[i] = wgt
                        }
                    }
                    var px = first.x * w; var py = first.y * h
                    stamp(px, py)
                    for (k in 1 until op.points.size) {
                        val nx = op.points[k].x * w; val ny = op.points[k].y * h
                        val len = hypot(nx - px, ny - py); val steps = max(1, (len / (r * .3f)).toInt())
                        for (s in 1..steps) stamp(px + (nx - px) * s / steps, py + (ny - py) * s / steps)
                        px = nx; py = ny
                    }
                }
                for (i in alpha.indices) {
                    val c = cover[i]; if (c <= 0f) continue
                    alpha[i] = if (op.restore) max(alpha[i], c) else min(alpha[i], 1f - c)
                }
            }
            if (cutout.feather > 0f) soften(alpha, w, h, max(1, (cutout.feather * short / 300f).roundToInt()))
            return CutoutMask(w, h, alpha)
        }

        private fun floodFill(pixels: IntArray, w: Int, h: Int, sx: Int, sy: Int, tolerance: Float, cover: FloatArray, dist: (Int, Int) -> Float) {
            val seedColour = pixels[sy * w + sx]
            val visited = BooleanArray(w * h)
            val queue = IntArray(w * h); var head = 0; var tail = 0
            queue[tail++] = sy * w + sx; visited[sy * w + sx] = true
            while (head < tail) {
                val i = queue[head++]
                val d = dist(pixels[i], seedColour)
                cover[i] = (1f - ((d - tolerance * .6f) / (tolerance * .4f)).coerceIn(0f, 1f))
                val x = i % w; val y = i / w
                fun visit(n: Int) { if (!visited[n]) { visited[n] = true; if (dist(pixels[n], seedColour) < tolerance) queue[tail++] = n } }
                if (x > 0) visit(i - 1); if (x < w - 1) visit(i + 1); if (y > 0) visit(i - w); if (y < h - 1) visit(i + w)
            }
            // Pull in the one-pixel rim so selections do not leave halos.
            val grown = cover.copyOf()
            for (y in 1 until h - 1) for (x in 1 until w - 1) {
                val i = y * w + x
                if (cover[i] == 0f) grown[i] = maxOf(cover[i - 1], cover[i + 1], cover[i - w], cover[i + w]) * .7f
            }
            System.arraycopy(grown, 0, cover, 0, cover.size)
        }

        private fun soften(a: FloatArray, w: Int, h: Int, r: Int) {
            val t = FloatArray(a.size)
            for (y in 0 until h) { var s = 0f; var n = 0
                for (x in -r until w + r) {
                    if (x + r < w && x + r >= 0) { s += a[y * w + x + r]; n++ }
                    if (x - r - 1 >= 0 && x - r - 1 < w) { s -= a[y * w + x - r - 1]; n-- }
                    if (x in 0 until w) t[y * w + x] = s / n
                } }
            for (x in 0 until w) { var s = 0f; var n = 0
                for (y in -r until h + r) {
                    if (y + r < h && y + r >= 0) { s += t[(y + r) * w + x]; n++ }
                    if (y - r - 1 >= 0 && y - r - 1 < h) { s -= t[(y - r - 1) * w + x]; n-- }
                    if (y in 0 until h) a[y * w + x] = s / n
                } }
        }

    }
}
