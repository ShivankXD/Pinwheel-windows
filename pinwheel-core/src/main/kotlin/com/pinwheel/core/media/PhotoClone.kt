package com.pinwheel.core.media
import kotlin.math.roundToInt

object PhotoClone {
    fun blend(a: Int, b: Int, amount: Float): Int {
        if (amount <= 0f) return a
        if (amount >= 1f) return b
        val alphaA = (a ushr 24).toFloat(); val alphaB = (b ushr 24).toFloat()
        val wa = alphaA * (1f - amount); val wb = alphaB * amount; val alpha = wa + wb
        if (alpha <= 0f) return 0
        fun channel(shift: Int) = ((((a ushr shift) and 255) * wa + ((b ushr shift) and 255) * wb) / alpha).roundToInt().coerceIn(0, 255)
        return (alpha.roundToInt().coerceIn(0, 255) shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}
