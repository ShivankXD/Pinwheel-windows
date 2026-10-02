package com.pinwheel.core.media

import kotlin.math.*

/**
 * Maps original green-channel coordinates to calibrated red/blue sample locations.
 * Apply after inverse distortion/geometry, before sampling. No change to green,
 * alpha, mask positions or original pixels; out-of-image taps use edge extension.
 */
class PhotoTca(private val width:Int,private val height:Int,profileId:String?,enabled:Boolean,crop:Float=1f) {
    private val calibration=if(enabled && width>0 && height>0 &&
        abs(max(width,height).toFloat()/min(width,height)-1.5f)<.015f)
        PhotoLensProfiles.byId(profileId)?.tca else null
    val active:Boolean get()=calibration!=null
    private val radiusScale=2f/min(width,height).coerceAtLeast(1)/(crop.takeIf{it.isFinite()}?:1f).coerceIn(.95f,1.05f)
    fun mapInverse(nx:Float,ny:Float,channel:Int,output:FloatArray,index:Int=0) {
        val coefficients=calibration
        if(coefficients==null || channel==1) {
            output[index]=nx;output[index+1]=ny;return
        }
        val x=nx-.5f;val y=ny-.5f
        val radius=sqrt(x*x*width*width+y*y*height*height)*radiusScale
        val factor=coefficients.factor(radius,channel)
        output[index]=if(factor==1f)nx else .5f+x*factor
        output[index+1]=if(factor==1f)ny else .5f+y*factor
    }
}
