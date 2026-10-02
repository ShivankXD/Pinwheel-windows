package com.pinwheel.core.media

import java.util.Locale
import kotlin.math.*

data class LensCalibration(val id:String,val name:String,val maker:String,val focal:Float,val a:Float,val b:Float,val c:Float,val aliases:List<String>,val tca:LensTcaCalibration?=null) {
    /** Lensfun PTLens: distorted radius / ideal radius, with r=1 at the short-edge half-height. */
    fun factor(radius:Float)=a*radius*radius*radius+b*radius*radius+c*radius+1f-a-b-c
    fun derivative(radius:Float)=4*a*radius*radius*radius+3*b*radius*radius+2*c*radius+1f-a-b-c
}
/** Lensfun poly3 TCA, relative to the green channel, in short-edge half-height units. */
data class LensTcaCalibration(val br:Float=0f,val cr:Float=0f,val vr:Float=1f,
    val bb:Float=0f,val cb:Float=0f,val vb:Float=1f) {
    fun factor(radius:Float,channel:Int)=when(channel) {
        0->br*radius*radius+cr*radius+vr
        2->bb*radius*radius+cb*radius+vb
        else->1f
    }
}
data class LensProfileMatch(val profile:LensCalibration,val cameraCrop:Float)

object PhotoLensProfiles {
    val profiles:List<LensCalibration> get()=LensfunCalibrations.lenses
    fun byId(id:String?)=profiles.firstOrNull{it.id==id}
    private fun normalized(value:String)=value.uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9.]"),"")
    private fun cameraName(value:String):String {
        var name=normalized(value).replace("NIKONCORPORATION","NIKON")
        while(name.startsWith("CANONCANON"))name=name.removePrefix("CANON")
        while(name.startsWith("NIKONNIKON"))name=name.removePrefix("NIKON")
        while(name.startsWith("SONYSONY"))name=name.removePrefix("SONY")
        return name
    }
    fun focalLength(value:String?):Float? {
        val text=value?.trim()?.removeSuffix("mm")?.trim()?:return null
        val parts=text.split('/')
        val result=if(parts.size==2){val n=parts[0].toFloatOrNull();val d=parts[1].toFloatOrNull();if(n==null||d==null||d==0f)null else n/d}else text.toFloatOrNull()
        return result?.takeIf{it.isFinite()&&it>0f}
    }
    /** Conservative metadata matching: no fuzzy lens guessing, camera crop guessing, or focal extrapolation. */
    fun match(camera:String?,lens:String?,focal:String?,width:Int,height:Int,focal35mm:String?=null):LensProfileMatch? {
        if(camera.isNullOrBlank()||lens.isNullOrBlank()||width<=0||height<=0)return null
        val aspect=max(width,height).toFloat()/min(width,height)
        if(abs(aspect-1.5f)>.012f)return null
        val crop=LensfunCalibrations.cameraCrops.entries.firstOrNull{cameraName(it.key)==cameraName(camera)}?:return null
        val mm=focalLength(focal)?:return null
        // FX bodies can capture a DX crop with the same aspect ratio. Missing equivalent
        // focal length cannot establish sensor coverage; fail closed instead of guessing.
        val equivalent=focalLength(focal35mm)?:return null
        if(abs(equivalent/mm-1f)>.04f)return null
        val name=normalized(lens)
        val candidates=profiles.filter{profile->crop.key.startsWith(profile.maker)&&abs(mm-profile.focal)<=.05f&&profile.aliases.any{normalized(it)==name}}
        return candidates.singleOrNull()?.let{LensProfileMatch(it,crop.value)}
    }
}

/** Calibrated inverse sampling with automatic zoom before distortion; coefficients remain unchanged. */
class LensProfileWarp(private val width:Int,private val height:Int,private val profile:LensCalibration,crop:Float) {
    private val radiusScale=2f/min(width,height)/(crop.takeIf{it.isFinite()}?:1f).coerceIn(.95f,1.05f)
    private val corner=sqrt(width.toFloat()*width+height.toFloat()*height)*.5f*radiusScale
    private val cover=run {
        val candidates=mutableListOf(0f,corner)
        val qa=3f*profile.a;val qb=2f*profile.b;val qc=profile.c
        if(abs(qa)<.0000001f) {if(abs(qb)>.0000001f)candidates+=-qc/qb}
        else {val discriminant=qb*qb-4f*qa*qc;if(discriminant>=0f){val root=sqrt(discriminant);candidates+=(-qb+root)/(2f*qa);candidates+=(-qb-root)/(2f*qa)}}
        candidates.filter{it in 0f..corner}.maxOf{profile.factor(it)}.coerceAtLeast(1f)*1.000001f
    }
    fun mapInverse(x:Float,y:Float,output:FloatArray,index:Int) {
        val dx=(x-.5f)*width/cover;val dy=(y-.5f)*height/cover
        val radius=sqrt(dx*dx+dy*dy)*radiusScale
        val factor=profile.factor(radius)/cover
        output[index]=.5f+(x-.5f)*factor;output[index+1]=.5f+(y-.5f)*factor
    }
    fun forward(x:Float,y:Float):PhotoPoint {
        val dx=(x-.5f)*width;val dy=(y-.5f)*height
        val radius=sqrt(dx*dx+dy*dy)*radiusScale
        if(radius<.000001f)return PhotoPoint(x,y)
        var r=radius
        repeat(8){r=(r-(r*profile.factor(r)-radius)/profile.derivative(r).coerceAtLeast(.1f)).coerceAtLeast(0f)}
        val factor=r/radius*cover
        return PhotoPoint(.5f+(x-.5f)*factor,.5f+(y-.5f)*factor)
    }
}
