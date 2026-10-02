package com.pinwheel.core.media
import com.pinwheel.core.model.Adjustments
import kotlin.math.*

data class CropRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun width() = right - left
    fun height() = bottom - top
}

object PhotoEngine {
    fun aspect(name:String):Float=when(name){"1:1"->1f;"4:5"->.8f;"3:2"->1.5f;"2:3"->2f/3;"4:3"->4f/3;"16:9"->16f/9;"9:16"->9f/16;else->0f}
    fun cropBounds(width:Int,height:Int,a:Adjustments):CropRect {
        var l=a.cropLeft.coerceIn(0f,.98f);var t=a.cropTop.coerceIn(0f,.98f)
        var r=a.cropRight.coerceIn(l+.01f,1f);var b=a.cropBottom.coerceIn(t+.01f,1f)
        // Version-one projects stored only a centred aspect ratio.
        if(l==0f && t==0f && r==1f && b==1f && aspect(a.crop)>0) {
            val target=aspect(a.crop); val source=width.toFloat()/height
            if(source>target) {val w=target/source;l=(1-w)/2;r=1-l} else {val h=source/target;t=(1-h)/2;b=1-t}
        }
        val x=(l*width).roundToInt().coerceIn(0,width-1);val y=(t*height).roundToInt().coerceIn(0,height-1)
        val rect=CropRect(x,y,(r*width).roundToInt().coerceIn(x+1,width),(b*height).roundToInt().coerceIn(y+1,height))
        val ratio=aspect(a.crop)
        if(ratio>0) {
            // Independently rounded edges can make a square one pixel wider.
            // Preserve the selected aspect after rounding to integer pixels.
            val cw=min(rect.width(),(rect.height()*ratio).roundToInt().coerceAtLeast(1))
            val ch=min(rect.height(),(cw/ratio).roundToInt().coerceAtLeast(1))
            val left=rect.left+(rect.width()-cw)/2;val top=rect.top+(rect.height()-ch)/2
            return CropRect(left,top,left+cw,top+ch)
        }
        return rect
    }
}
