package com.pinwheel.core.media

import com.pinwheel.core.model.PhotoFrame
import kotlin.math.*

data class FrameLayout(val width: Int, val height: Int, val left: Float, val top: Float, val photoWidth: Float, val photoHeight: Float) {
}

object PhotoFrameRenderer {
    val ratios = listOf("None", "Even", "1:1", "4:5", "9:16", "3:4", "16:9", "3:2")
    val styles = listOf("White", "Black", "Blur", "Cream", "Blush", "Sage", "Sky", "Charcoal")

    fun ratio(frame: PhotoFrame, imageAspect: Float): Float = when (frame.ratio) {
        "1:1" -> 1f; "4:5" -> .8f; "9:16" -> 9f / 16f; "3:4" -> .75f; "16:9" -> 16f / 9f; "3:2" -> 1.5f
        else -> imageAspect
    }

    fun color(style: String): Int = when (style) {
        "Black" -> 0xFF000000.toInt(); "Cream" -> 0xFFF3EBDD.toInt(); "Blush" -> 0xFFF4D9D6.toInt()
        "Sage" -> 0xFFC9D3BF.toInt(); "Sky" -> 0xFFD3E2EF.toInt(); "Charcoal" -> 0xFF2A2B2E.toInt()
        else -> 0xFFFFFFFF.toInt()
    }

    /** Canvas size for an image of w x h, and where the image sits inside it. */
    fun layout(w: Int, h: Int, frame: PhotoFrame): FrameLayout {
        val s = frame.border.coerceIn(0f, .3f)
        if (frame.ratio == "Even") {
            // Same margin on every side: m is s of the canvas short edge, which itself includes 2m.
            val m = ceil(s * minOf(w, h) / (1 - 2 * s)).toInt()
            return FrameLayout(w + 2 * m, h + 2 * m, m.toFloat(), m.toFloat(), w.toFloat(), h.toFloat())
        }
        val r = ratio(frame, w.toFloat() / h)
        val canvasH: Float; val canvasW: Float
        if (r >= 1f) {
            canvasH = max(w / (r - 2 * s), h / (1 - 2 * s)); canvasW = canvasH * r
        } else {
            canvasW = max(w / (1 - 2 * s), h / (1 / r - 2 * s)); canvasH = canvasW / r
        }
        val cw = ceil(canvasW).toInt(); val ch = ceil(canvasH).toInt()
        return FrameLayout(cw, ch, (cw - w) / 2f, (ch - h) / 2f, w.toFloat(), h.toFloat())
    }

}
