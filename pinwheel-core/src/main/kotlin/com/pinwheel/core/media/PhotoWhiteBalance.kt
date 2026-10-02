package com.pinwheel.core.media

import kotlin.math.abs

object PhotoWhiteBalance {
    data class Balance(val warmth:Float,val tint:Float,val limited:Boolean)

    /** Solve the two chromatic gains used by our SDR engine for a user-picked neutral patch. */
    fun neutral(red:Float,green:Float,blue:Float):Balance? {
        if(listOf(red,green,blue).any{!it.isFinite() || it<8f || it>247f})return null
        val aa=.16f*(red+blue);val ab=.06f*(red-blue);val ac=blue-red
        val ba=.08f*(red-blue);val bb=.03f*(red+blue)+.1f*green;val bc=green-(red+blue)/2
        val determinant=aa*bb-ab*ba
        if(abs(determinant)<.0001f)return null
        val warmth=(ac*bb-ab*bc)/determinant
        val tint=(aa*bc-ac*ba)/determinant
        return Balance(warmth.coerceIn(-1f,1f),tint.coerceIn(-1f,1f),abs(warmth)>1f || abs(tint)>1f)
    }
}
