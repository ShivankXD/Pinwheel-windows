package com.pinwheel.core.media

import kotlin.math.abs

/** Piecewise linear curves preserve legacy output exactly and never overshoot control points. */
object PhotoCurve {
    val neutral = listOf(0f, .25f, .5f, .75f, 1f)
    const val maxPoints = 32

    fun positions(values: List<Float>, positions: List<Float>): List<Float> =
        if (valid(values, positions)) positions else List(values.size) { it.toFloat() / (values.size - 1).coerceAtLeast(1) }

    fun valid(values: List<Float>, positions: List<Float>): Boolean =
        values.size in 2..maxPoints && positions.size == values.size &&
            values.all { it.isFinite() && it in 0f..1f } &&
            positions.first() == 0f && positions.last() == 1f &&
            positions.all { it.isFinite() && it in 0f..1f } &&
            positions.zipWithNext().all { (a,b) -> b-a >= .0001f }

    class Sampler(private val values:List<Float>, positions:List<Float>) {
        private val xs=positions(values,positions)
        /** Continue the endpoint segment for scene-referred HDR values above SDR white. */
        fun extended(value:Float):Float {
            if(value<=1f || values.size<2)return at(value)
            val slope=(values.last()-values[values.lastIndex-1])/(xs.last()-xs[xs.lastIndex-1]).coerceAtLeast(.0001f)
            return (values.last()+(value-1f)*slope).coerceIn(0f,16f)
        }
        fun at(value:Float):Float {
            val x=value.coerceIn(0f,1f)
            if(values.size<2)return x
            var right=xs.binarySearch(x)
            if(right>=0)return values[right].coerceIn(0f,1f)
            right=(-right-1).coerceIn(1,xs.lastIndex)
            val left=right-1
            return (values[left]+(values[right]-values[left])*(x-xs[left])/(xs[right]-xs[left])).coerceIn(0f,1f)
        }
    }
    fun evaluate(value:Float,values:List<Float>,positions:List<Float> = neutral):Float = Sampler(values,positions).at(value)

    /** Preserve both shapes when blending presets with independently positioned nodes. */
    fun blend(av:List<Float>, ax:List<Float>, bv:List<Float>, bx:List<Float>, amount:Float):Pair<List<Float>,List<Float>> {
        if (amount<=0f) return av to positions(av,ax)
        if (amount>=1f) return bv to positions(bv,bx)
        var xs=(positions(av,ax)+positions(bv,bx)).distinct().sorted()
        xs=xs.filterIndexed { index,x -> index==0 || abs(x-xs[index-1])>=.001f }
        if(xs.last()!=1f)xs=xs.dropLast(1)+1f
        if(xs.size>maxPoints)xs=List(maxPoints){it.toFloat()/(maxPoints-1)}
        return xs.map {x->val a=evaluate(x,av,ax);a+(evaluate(x,bv,bx)-a)*amount} to xs
    }
}
