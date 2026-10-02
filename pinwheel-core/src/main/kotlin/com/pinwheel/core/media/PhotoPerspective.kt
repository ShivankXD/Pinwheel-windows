package com.pinwheel.core.media

import kotlin.math.abs

/** A bounded, invertible keystone transform. Cover prevents transparent wedges at the edges. */
class PhotoPerspective(horizontal:Float,vertical:Float) {
    val horizontal=horizontal.coerceIn(-1f,1f)*.65f
    val vertical=vertical.coerceIn(-1f,1f)*.65f
    val cover=1f+(abs(this.horizontal)+abs(this.vertical))*.5f
    val active get()=horizontal!=0f || vertical!=0f

    fun forward(x:Float,y:Float):PhotoPoint {
        val dx=x-.5f;val dy=y-.5f;val denominator=1f+horizontal*dx+vertical*dy
        return PhotoPoint(.5f+cover*dx/denominator,.5f+cover*dy/denominator)
    }
    fun inverse(x:Float,y:Float):PhotoPoint {
        val dx=x-.5f;val dy=y-.5f;val denominator=cover-horizontal*dx-vertical*dy
        return PhotoPoint(.5f+dx/denominator,.5f+dy/denominator)
    }
    fun matrixValues(width:Float,height:Float)=floatArrayOf(
        cover+.5f*horizontal, .5f*vertical*width/height, width*(.5f-.5f*cover-.25f*horizontal-.25f*vertical),
        .5f*horizontal*height/width, cover+.5f*vertical, height*(.5f-.5f*cover-.25f*horizontal-.25f*vertical),
        horizontal/width, vertical/height, 1f-.5f*horizontal-.5f*vertical
    )
}
