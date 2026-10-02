package com.pinwheel.core.media

import com.pinwheel.core.model.RawDevelopment
import kotlin.math.pow
import kotlin.math.roundToInt

/** Unsigned16 linear sRGB-primary RGB. The original sensor file remains the authoritative master. */
class RawLinearImage(val width:Int,val height:Int,private val samples:ShortArray) {
    init { require(width>0 && height>0 && samples.size.toLong()==width.toLong()*height*3) }
    val byteSize:Long get()=samples.size.toLong()*2
    fun sample(x:Int,y:Int,channel:Int):Int {
        require(x in 0 until width && y in 0 until height && channel in 0..2)
        return samples[(y*width+x)*3+channel].toInt() and 65535
    }
}

/** All development mathematics happens before final display/export quantization. */
class RawLinearTransform(development:RawDevelopment) {
    private fun finite(value:Float,min:Float,max:Float)=if(value.isFinite())value.coerceIn(min,max).toDouble() else 0.0
    private val exposure=2.0.pow(finite(development.exposure,-4f,4f))
    private val temperature=finite(development.temperature,-1f,1f)
    private val tint=finite(development.tint,-1f,1f)
    private val compression=finite(development.highlights,0f,1f)
    private val gains=doubleArrayOf(2.0.pow(temperature*.5+tint*.25),2.0.pow(-tint*.5),2.0.pow(-temperature*.5+tint*.25))
    fun channel(linear:Double,channel:Int):Double {
        val value=linear.coerceAtLeast(0.0)*exposure*gains[channel]
        // A continuous shoulder compresses highlights; it cannot recover clipped sensor samples.
        return if(value>.6 && compression>0) .6+(value-.6)/(1+compression*3*(value-.6)) else value
    }
    companion object {
        fun toSrgb(linear:Double):Double=if(linear<=.0031308)linear*12.92 else 1.055*linear.pow(1/2.4)-.055
    }
}
