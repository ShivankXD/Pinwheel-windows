package com.pinwheel.core.media

import com.pinwheel.core.model.Adjustments
import kotlin.math.*

/** SDR colour operations shared by preview and export. No learned models or network calls. */
class PhotoColorTransform(private val a: Adjustments) {
    private val masterCurve=PhotoCurve.Sampler(a.curve,a.curveX)
    private val rCurve=PhotoCurve.Sampler(a.redCurve,a.redCurveX)
    private val gCurve=PhotoCurve.Sampler(a.greenCurve,a.greenCurveX)
    private val bCurve=PhotoCurve.Sampler(a.blueCurve,a.blueCurveX)
    private val grading = PhotoGrading(a)
    private val dehaze = PhotoDehaze(a.dehaze)
    private val lut = FloatArray(1025) { i ->
        val v = i / 1024f
        val exposed = (v.toDouble().pow(2.2) * 2.0.pow(a.exposure.toDouble())).pow(1.0 / 2.2).toFloat()
        var t = (exposed - .5f) * a.contrast + .5f
        t += a.shadows * (1f-v).pow(2) * v * 1.5f
        t += a.highlights * v.pow(2) * (1f-v) * 1.5f
        t += a.whites * v.pow(3) * .22f + a.blacks * (1f-v).pow(3) * .18f
        t = t.coerceIn(0f, 1f)
        masterCurve.at(t)
    }
    private val mixed = a.mix.any { it.hue != 0f || it.saturation != 0f || it.luminance != 0f }
    private val centers = floatArrayOf(0f,30f,60f,120f,180f,240f,280f,320f,360f)
    private fun tone(v: Float): Float {
        val p=v.coerceIn(0f,1f)*1024; val i=p.toInt().coerceAtMost(1023)
        return lut[i]+(lut[i+1]-lut[i])*(p-i)
    }
    // The input is 8-bit, so tone, white balance and RGB curves can be compiled
    // once per edit instead of interpolated six times for every preview pixel.
    private val red = FloatArray(256) { rCurve.at((tone(it/255f)*(1+a.warmth*.16f+a.tint*.06f)).coerceIn(0f,1f)) }
    private val green = FloatArray(256) { gCurve.at((tone(it/255f)*(1-a.tint*.1f)).coerceIn(0f,1f)) }
    private val blue = FloatArray(256) { bCurve.at((tone(it/255f)*(1-a.warmth*.16f+a.tint*.06f)).coerceIn(0f,1f)) }
    // Light, white-balance and curve drags are separable channel operations.
    // Compile their final 8-bit values once instead of repeating floating-point
    // saturation/fade/clamping arithmetic for every pixel in every frame.
    private val separable = !dehaze.active && !mixed && !grading.active &&
        a.saturation == 1f && a.vibrance == 0f && a.vignette == 0f && a.grain <= 0f
    private fun packed(table: FloatArray, shift: Int): IntArray? = if (!separable) null else {
        val fade = a.fade * .18f
        IntArray(256) { (((table[it] * (1f-fade)+fade).coerceIn(0f,1f)*255f+.5f).toInt()) shl shift }
    }
    private val packedRed = packed(red,16)
    private val packedGreen = packed(green,8)
    private val packedBlue = packed(blue,0)
    /** Avoid coordinate divisions and dispatch per pixel for separable Light/curve edits. */
    fun row(input: IntArray, output: IntArray, ny: Float) {
        require(input.size == output.size)
        if (separable) {
            val reds = packedRed!!; val greens = packedGreen!!; val blues = packedBlue!!
            for (x in input.indices) {
                val pixel = input[x]
                output[x] = (pixel and -0x1000000) or reds[(pixel ushr 16) and 255] or
                    greens[(pixel ushr 8) and 255] or blues[pixel and 255]
            }
        } else for (x in input.indices) output[x] = pixel(input[x], (x + .5f) / input.size, ny)
    }
    /** Picker samples the same tone stage as the image, before white balance and artistic colour operations. */
    fun beforeWhiteBalance(red:Float,green:Float,blue:Float):FloatArray {
        val veil=dehaze.veil(red/255f,green/255f,blue/255f)
        return floatArrayOf(tone(dehaze.channel(red/255f,veil))*255,tone(dehaze.channel(green/255f,veil))*255,tone(dehaze.channel(blue/255f,veil))*255)
    }
    private fun lookup(table:FloatArray,value:Float):Float {
        val p=value.coerceIn(0f,1f)*255f;val i=p.toInt().coerceAtMost(254)
        return table[i]+(table[i+1]-table[i])*(p-i)
    }
    fun pixel(pixel: Int, nx: Float, ny: Float): Int {
        if (separable) return (pixel and -0x1000000) or packedRed!![(pixel ushr 16) and 255] or
            packedGreen!![(pixel ushr 8) and 255] or packedBlue!![pixel and 255]
        var r=red[(pixel ushr 16) and 255]
        var g=green[(pixel ushr 8) and 255]
        var b=blue[pixel and 255]
        if (dehaze.active) {
            val inputR=((pixel ushr 16) and 255)/255f
            val inputG=((pixel ushr 8) and 255)/255f
            val inputB=(pixel and 255)/255f
            val veil=dehaze.veil(inputR,inputG,inputB)
            r=lookup(red,dehaze.channel(inputR,veil))
            g=lookup(green,dehaze.channel(inputG,veil))
            b=lookup(blue,dehaze.channel(inputB,veil))
        }
        val l=.2126f*r+.7152f*g+.0722f*b
        val chroma=maxOf(r,g,b)-minOf(r,g,b)
        val sat=a.saturation*(1+a.vibrance*(1-chroma)*.7f)
        r=l+(r-l)*sat; g=l+(g-l)*sat; b=l+(b-l)*sat
        if (mixed) {
            r=r.coerceIn(0f,1f); g=g.coerceIn(0f,1f); b=b.coerceIn(0f,1f)
            val mx=maxOf(r,g,b); val mn=minOf(r,g,b); val d=mx-mn
            if(d>.0001f) {
                var h=when(mx) { r -> 60*((g-b)/d % 6); g -> 60*((b-r)/d+2); else -> 60*((r-g)/d+4) }
                if(h<0) h+=360
                var sector=0; while(sector<7 && h>centers[sector+1]) sector++
                val t=(h-centers[sector])/(centers[sector+1]-centers[sector])
                val left=a.mix.getOrElse(sector){com.pinwheel.core.model.ColorBand()}
                val right=a.mix.getOrElse((sector+1)%8){com.pinwheel.core.model.ColorBand()}
                h=(h+(left.hue*(1-t)+right.hue*t)*30+360)%360
                val light=(mx+mn)/2
                val sl=(d/(1-abs(2*light-1)).coerceAtLeast(.0001f)*(1+left.saturation*(1-t)+right.saturation*t)).coerceIn(0f,1f)
                val ll=(light+(left.luminance*(1-t)+right.luminance*t)*.25f).coerceIn(0f,1f)
                val c=(1-abs(2*ll-1))*sl; val x=c*(1-abs((h/60)%2-1)); val m=ll-c/2
                when(h.toInt()/60) {
                    0 -> {r=c;g=x;b=0f}; 1 -> {r=x;g=c;b=0f}; 2 -> {r=0f;g=c;b=x}
                    3 -> {r=0f;g=x;b=c}; 4 -> {r=x;g=0f;b=c}; else -> {r=c;g=0f;b=x}
                }
                r+=m;g+=m;b+=m
            }
        }
        val vig=if(a.vignette==0f)1f else {val distance=((nx-.5f).pow(2)+(ny-.5f).pow(2))*2;(1-a.vignette*distance.pow(1.4f)*.8f).coerceIn(.1f,1.8f)}
        if (grading.active) {
            val index=(.2126f*r+.7152f*g+.0722f*b).coerceIn(0f,1f).times(255).roundToInt()
            r+=grading.red[index];g+=grading.green[index];b+=grading.blue[index]
        }
        val fade=a.fade*.18f
        var noise=0f
        if(a.grain>0) {
            var hash=(nx*4096).toInt()*374761393+(ny*4096).toInt()*668265263
            hash=(hash xor (hash ushr 13))*1274126177
            noise=(((hash ushr 16) and 255)/255f-.5f)*a.grain*.13f
        }
        fun channel(v:Float)=(((v*(1-fade)+fade)*vig+noise).coerceIn(0f,1f)*255+.5f).toInt()
        return (pixel and -0x1000000) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
    }
    /** Floating-point colour path. No packed pixels or channel quantization before serialization. */
    fun precise(rgb: FloatArray, nx: Float, ny: Float, hdr: Boolean = false) {
        val limit=if(hdr)16f else 1f
        val veil=dehaze.veil(rgb[0],rgb[1],rgb[2])
        fun channel(value:Float):Float = if(!hdr)dehaze.channel(value,veil)
            else (if(veil>=0)(value-veil)/(1-veil) else value*(1+veil)-veil).coerceIn(0f,limit)
        fun preciseTone(value:Float):Float {
            if(!hdr)return tone(value)
            val v=value.coerceIn(0f,1f)
            val exposed=(value.toDouble().coerceAtLeast(0.0).pow(2.2)*2.0.pow(a.exposure.toDouble())).pow(1/2.2).toFloat()
            var t=(exposed-.5f)*a.contrast+.5f
            t+=a.shadows*(1-v).pow(2)*v*1.5f+a.highlights*v.pow(2)*(1-v)*1.5f
            t+=a.whites*v.pow(3)*.22f+a.blacks*(1-v).pow(3)*.18f
            return masterCurve.extended(t.coerceIn(0f,limit))
        }
        fun curve(curve:PhotoCurve.Sampler,value:Float)=if(hdr)curve.extended(value.coerceIn(0f,limit))else curve.at(value.coerceIn(0f,1f))
        var r=curve(rCurve,preciseTone(channel(rgb[0]))*(1+a.warmth*.16f+a.tint*.06f))
        var g=curve(gCurve,preciseTone(channel(rgb[1]))*(1-a.tint*.1f))
        var b=curve(bCurve,preciseTone(channel(rgb[2]))*(1-a.warmth*.16f+a.tint*.06f))
        val l=.2126f*r+.7152f*g+.0722f*b
        val chroma=(maxOf(r,g,b)-minOf(r,g,b)).coerceIn(0f,1f)
        val sat=a.saturation*(1+a.vibrance*(1-chroma)*.7f)
        r=l+(r-l)*sat; g=l+(g-l)*sat; b=l+(b-l)*sat
        if (mixed) {
            val headroom=if(hdr)maxOf(1f,r,g,b)else 1f
            r/=headroom;g/=headroom;b/=headroom
            r=r.coerceIn(0f,1f); g=g.coerceIn(0f,1f); b=b.coerceIn(0f,1f)
            val mx=maxOf(r,g,b); val mn=minOf(r,g,b); val d=mx-mn
            if(d>.0001f) {
                var h=when(mx) { r -> 60*((g-b)/d % 6); g -> 60*((b-r)/d+2); else -> 60*((r-g)/d+4) }
                if(h<0) h+=360
                var sector=0; while(sector<7 && h>centers[sector+1]) sector++
                val t=(h-centers[sector])/(centers[sector+1]-centers[sector])
                val left=a.mix.getOrElse(sector){com.pinwheel.core.model.ColorBand()}
                val right=a.mix.getOrElse((sector+1)%8){com.pinwheel.core.model.ColorBand()}
                h=(h+(left.hue*(1-t)+right.hue*t)*30+360)%360
                val light=(mx+mn)/2
                val sl=(d/(1-abs(2*light-1)).coerceAtLeast(.0001f)*(1+left.saturation*(1-t)+right.saturation*t)).coerceIn(0f,1f)
                val ll=(light+(left.luminance*(1-t)+right.luminance*t)*.25f).coerceIn(0f,1f)
                val c=(1-abs(2*ll-1))*sl; val x=c*(1-abs((h/60)%2-1)); val m=ll-c/2
                when(h.toInt()/60) {
                    0 -> {r=c;g=x;b=0f}; 1 -> {r=x;g=c;b=0f}; 2 -> {r=0f;g=c;b=x}
                    3 -> {r=0f;g=x;b=c}; 4 -> {r=x;g=0f;b=c}; else -> {r=c;g=0f;b=x}
                }
                r+=m;g+=m;b+=m
            }
            r*=headroom;g*=headroom;b*=headroom
        }
        val vig=if(a.vignette==0f)1f else {val distance=((nx-.5f).pow(2)+(ny-.5f).pow(2))*2;(1-a.vignette*distance.pow(1.4f)*.8f).coerceIn(.1f,1.8f)}
        if (grading.active) {
            val luminance=(.2126f*r+.7152f*g+.0722f*b).coerceIn(0f,1f)
            r+=lookup(grading.red,luminance);g+=lookup(grading.green,luminance);b+=lookup(grading.blue,luminance)
        }
        val fade=a.fade*.18f
        var noise=0f
        if(a.grain>0) {
            var hash=(nx*4096).toInt()*374761393+(ny*4096).toInt()*668265263
            hash=(hash xor (hash ushr 13))*1274126177
            noise=(((hash ushr 16) and 255)/255f-.5f)*a.grain*.13f
        }
        rgb[0]=((r*(1-fade)+fade)*vig+noise).coerceIn(0f,limit)
        rgb[1]=((g*(1-fade)+fade)*vig+noise).coerceIn(0f,limit)
        rgb[2]=((b*(1-fade)+fade)*vig+noise).coerceIn(0f,limit)
    }
    companion object {
        fun curve(v:Float, points:List<Float>):Float {
            return PhotoCurve.evaluate(v,points)
        }
    }
}

object AutoTone {
    /** Percentile analysis avoids basing exposure on a single bright/dark pixel. */
    fun suggest(histogram: IntArray, red: Double, green: Double, blue: Double, base: Adjustments): Adjustments {
        val count=histogram.sum(); if(count==0) return base
        fun percentile(f:Double):Float { var sum=0; for(i in histogram.indices) {sum+=histogram[i];if(sum>=count*f)return i/255f};return 1f }
        val median=percentile(.5).coerceAtLeast(.025f)
        val low=percentile(.02);val high=percentile(.98)
        val exposure=(ln(.44/median)/ln(2.0)*1.15).toFloat().coerceIn(-.85f,.85f)
        val spread=high-low
        // Deliberately conservative: coloured scenes should not be forced to neutral grey.
        val warmth=((blue-red)/(red+blue).coerceAtLeast(1.0)*.5).toFloat().coerceIn(-.18f,.18f)
        val tint=((green-(red+blue)/2)/(red+green+blue).coerceAtLeast(1.0)*.7).toFloat().coerceIn(-.12f,.12f)
        return base.copy(exposure=exposure,contrast=if(spread<.6f)1.08f else 1f,
            shadows=if(low<.12f).22f else .05f,highlights=if(high>.9f)-.22f else 0f,
            whites=0f,blacks=0f,warmth=warmth,tint=tint,vibrance=.12f)
    }
}
