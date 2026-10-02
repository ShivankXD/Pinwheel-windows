package com.pinwheel.core.media

import kotlin.math.*

/**
 * A manual, monotone radial lens warp, centred on the original image.
 * The inverse map is r_source = r_output * (1+r_output²)^k / cover.
 * Radius uses the half diagonal, so the correction remains circular on every
 * aspect ratio. Automatic cover keeps every output sample inside the source.
 * An optional calibrated PTLens warp composes before this manual correction.
 */
class PhotoLens(private val width: Int, private val height: Int, amount: Float, profileId:String?=null, profileEnabled:Boolean=false, profileCrop:Float=1f,tcaEnabled:Boolean=false) {
    private val k = (amount.takeIf { it.isFinite() } ?: 0f).coerceIn(-1f,1f) * .32f
    private val profile=if(profileEnabled && abs(max(width,height).toFloat()/min(width,height)-1.5f)<.015f)PhotoLensProfiles.byId(profileId)?.let{LensProfileWarp(width,height,it,profileCrop)}else null
    private val tca=PhotoTca(width,height,profileId,tcaEnabled,profileCrop)
    val tcaActive:Boolean get()=tca.active
    val active = k != 0f || profile!=null || tca.active
    private val diagonalSquared = .25f * (width.toFloat()*width + height.toFloat()*height)
    private val cover = max(1f, 2f.pow(k))
    private val scales = if(k!=0f)FloatArray(2049){(1f+it/2048f).pow(k)/cover}else null

    private fun scale(radiusSquared:Float):Float {
        val table=scales?:return 1f
        if(radiusSquared>1f)return (1f+radiusSquared).pow(k)/cover
        val p=radiusSquared.coerceAtLeast(0f)*2048f;val i=p.toInt().coerceAtMost(2047)
        return table[i]+(table[i+1]-table[i])*(p-i)
    }

    fun inverse(x:Float,y:Float):PhotoPoint {
        val result=FloatArray(2);mapInverse(x,y,result,0);return PhotoPoint(result[0],result[1])
    }

    fun mapInverse(x:Float,y:Float,output:FloatArray,index:Int) {
        val dx=(x-.5f)*width;val dy=(y-.5f)*height
        val factor=scale((dx*dx+dy*dy)/diagonalSquared)
        output[index]=.5f+(x-.5f)*factor;output[index+1]=.5f+(y-.5f)*factor
        profile?.mapInverse(output[index],output[index+1],output,index)
    }

    fun mapChannelInverse(x:Float,y:Float,channel:Int,output:FloatArray,index:Int=0) {
        mapInverse(x,y,output,index)
        tca.mapInverse(output[index],output[index+1],channel,output,index)
    }

    fun forward(x:Float,y:Float):PhotoPoint {
        val point=profile?.forward(x,y)?:PhotoPoint(x,y)
        if(k==0f)return point
        val dx=(point.x-.5f)*width;val dy=(point.y-.5f)*height
        val radius=sqrt((dx*dx+dy*dy)/diagonalSquared)
        if(radius<.000001f)return point
        var r=radius*cover
        repeat(7) {
            val square=r*r;val power=(1f+square).pow(k)
            val mapped=r*power/cover
            val derivative=power/cover*(1f+2f*k*square/(1f+square))
            r=(r-(mapped-radius)/derivative).coerceAtLeast(0f)
        }
        val factor=r/radius
        return PhotoPoint(.5f+(point.x-.5f)*factor,.5f+(point.y-.5f)*factor)
    }
}
