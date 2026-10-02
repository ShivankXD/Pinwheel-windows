package com.pinwheel.core.media

import com.pinwheel.core.model.MaskStroke
import kotlin.math.*

/** Spatial bins avoid checking the entire paint history at every output pixel. */
class BrushMaskIndex(strokes: List<MaskStroke>, feather: Float) {
    private data class Segment(val stroke: Int, val erase: Boolean, val x: Float, val y: Float,
        val dx: Float, val dy: Float, val rx: Float, val ry: Float, val lengthSquared: Float)
    private val cells = Array(16*16) { ArrayList<Segment>() }
    private val feather = feather.coerceAtLeast(.001f)

    init {
        strokes.forEachIndexed { index, stroke ->
            val rx=stroke.radiusX.coerceAtLeast(.005f);val ry=stroke.radiusY.coerceAtLeast(.005f)
            stroke.points.forEachIndexed { i, point ->
                val previous=stroke.points[(i-1).coerceAtLeast(0)]
                val dx=(point.x-previous.x)/rx;val dy=(point.y-previous.y)/ry
                val segment=Segment(index,stroke.erase,previous.x,previous.y,dx,dy,rx,ry,dx*dx+dy*dy)
                val left=floor((min(point.x,previous.x)-rx)*16).toInt().coerceIn(0,15)
                val right=floor((max(point.x,previous.x)+rx)*16).toInt().coerceIn(0,15)
                val top=floor((min(point.y,previous.y)-ry)*16).toInt().coerceIn(0,15)
                val bottom=floor((max(point.y,previous.y)+ry)*16).toInt().coerceIn(0,15)
                for(y in top..bottom)for(x in left..right)cells[y*16+x].add(segment)
            }
        }
    }

    fun at(x: Float, y: Float): Float {
        if(x !in 0f..1f || y !in 0f..1f)return 0f
        val list=cells[(y*16).toInt().coerceIn(0,15)*16+(x*16).toInt().coerceIn(0,15)]
        var current=-1;var erase=false;var strongest=0f;var alpha=0f
        fun composite() { alpha=if(erase)alpha*(1f-strongest) else max(alpha,strongest) }
        for(segment in list) {
            if(segment.stroke!=current) {
                if(current>=0)composite()
                current=segment.stroke;erase=segment.erase;strongest=0f
            }
            if(strongest>=1f)continue
            val px=(x-segment.x)/segment.rx;val py=(y-segment.y)/segment.ry
            val t=if(segment.lengthSquared<.000001f)0f else ((px*segment.dx+py*segment.dy)/segment.lengthSquared).coerceIn(0f,1f)
            val dx=px-t*segment.dx;val dy=py-t*segment.dy
            val distanceSquared=dx*dx+dy*dy
            if(distanceSquared>=1f)continue
            val weight=((1f-sqrt(distanceSquared))/feather).coerceIn(0f,1f)
            strongest=max(strongest,weight*weight*(3f-2f*weight))
        }
        if(current>=0)composite()
        return alpha
    }
}
