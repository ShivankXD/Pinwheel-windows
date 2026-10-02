package com.pinwheel.core.data

import com.pinwheel.core.model.*
import com.pinwheel.core.media.PhotoCurve
import org.json.JSONArray
import org.json.JSONObject

object AdjustmentCodec {
    private fun number(o:JSONObject,key:String,default:Float,min:Float,max:Float):Float =
        o.optDouble(key,default.toDouble()).toFloat().let {if(it.isFinite())it.coerceIn(min,max) else default}

    fun encode(a:Adjustments)=JSONObject().apply {
        put("raw",JSONObject().put("exposure",a.raw.exposure).put("temperature",a.raw.temperature).put("tint",a.raw.tint).put("highlights",a.raw.highlights))
        put("exposure",a.exposure);put("contrast",a.contrast);put("saturation",a.saturation);put("warmth",a.warmth)
        put("rotation",a.rotation);put("crop",a.crop)
        if(a.cutout.active)put("cutout",JSONObject().put("background",a.cutout.background).put("color",a.cutout.color).put("image",a.cutout.imageUri?:"").put("feather",a.cutout.feather.toDouble())
            .put("ops",JSONArray().apply { a.cutout.ops.forEach { op -> put(JSONObject().put("r",op.radius.toDouble()).put("restore",op.restore).put("smart",op.smart).put("fill",op.fill).put("tol",op.tolerance.toDouble())
                .put("p",JSONArray().apply { op.points.forEach { put(it.x.toDouble());put(it.y.toDouble()) } })) } }))
        if(a.frame.active)put("frame",JSONObject().put("ratio",a.frame.ratio).put("style",a.frame.style).put("border",a.frame.border.toDouble()))
        put("highlights",a.highlights);put("shadows",a.shadows);put("whites",a.whites);put("blacks",a.blacks)
        put("tint",a.tint);put("vibrance",a.vibrance);put("fade",a.fade);put("vignette",a.vignette);put("grain",a.grain);put("sharpness",a.sharpness)
        put("straighten",a.straighten);put("flipHorizontal",a.flipHorizontal);put("flipVertical",a.flipVertical)
        put("cropLeft",a.cropLeft);put("cropTop",a.cropTop);put("cropRight",a.cropRight);put("cropBottom",a.cropBottom)
        put("curve",JSONArray(a.curve));put("redCurve",JSONArray(a.redCurve));put("greenCurve",JSONArray(a.greenCurve));put("blueCurve",JSONArray(a.blueCurve))
        put("curveX",JSONArray(a.curveX));put("redCurveX",JSONArray(a.redCurveX));put("greenCurveX",JSONArray(a.greenCurveX));put("blueCurveX",JSONArray(a.blueCurveX))
        put("perspectiveHorizontal",a.perspectiveHorizontal);put("perspectiveVertical",a.perspectiveVertical)
        put("lensDistortion",a.lensDistortion)
        put("lensProfileId",a.lensProfileId);put("lensProfileEnabled",a.lensProfileEnabled);put("lensProfileCrop",a.lensProfileCrop);put("lensTcaEnabled",a.lensTcaEnabled)
        put("gradingBalance",a.gradingBalance);put("gradingBlending",a.gradingBlending)
        put("texture",a.texture);put("clarity",a.clarity);put("dehaze",a.dehaze);put("noiseReduction",a.noiseReduction);put("colorNoiseReduction",a.colorNoiseReduction)
        put("cloneSpots",JSONArray().apply { a.cloneSpots.take(32).forEach { s -> put(JSONObject().put("id",s.id).put("x",s.x).put("y",s.y).put("sourceX",s.sourceX).put("sourceY",s.sourceY).put("radius",s.radius).put("feather",s.feather).put("mode",s.mode.name).put("opacity",s.opacity)) } })
        put("grading",JSONArray().apply {a.grading.forEach {put(JSONObject().put("hue",it.hue).put("saturation",it.saturation).put("luminance",it.luminance))}})
        put("masks",JSONArray().apply {a.masks.forEach {m -> put(JSONObject().apply {
            put("id",m.id);put("name",m.name);put("shape",m.shape.name)
            put("x",m.centerX);put("y",m.centerY);put("rx",m.radiusX);put("ry",m.radiusY);put("angle",m.angle);put("feather",m.feather)
            put("strokes",JSONArray().apply { m.strokes.forEach { stroke -> put(JSONObject().apply {
                put("rx",stroke.radiusX);put("ry",stroke.radiusY);put("erase",stroke.erase)
                put("points",JSONArray().apply {stroke.points.forEach {point -> put(JSONArray().put(point.x).put(point.y))}})
            })}})
            put("highlights",m.highlights);put("shadows",m.shadows);put("tint",m.tint);put("clarity",m.clarity)
            put("inverted",m.inverted);put("enabled",m.enabled);put("exposure",m.exposure);put("contrast",m.contrast);put("warmth",m.warmth);put("saturation",m.saturation)
        })}})
        put("mix",JSONArray().apply {a.mix.forEach {put(JSONObject().put("hue",it.hue).put("saturation",it.saturation).put("luminance",it.luminance))}})
    }
    fun decode(o:JSONObject):Adjustments {
        fun f(key:String,default:Float=0f)=o.optDouble(key,default.toDouble()).toFloat().let {if(it.isFinite())it else default}
        fun curve(key:String):List<Float> {
            val array=o.optJSONArray(key)?:return PhotoCurve.neutral
            if(array.length() !in 2..PhotoCurve.maxPoints)return PhotoCurve.neutral
            return List(array.length()){i->array.optDouble(i,i.toDouble()/(array.length()-1)).toFloat().let{if(it.isFinite())it.coerceIn(0f,1f) else i.toFloat()/(array.length()-1)}}
        }
        fun curveX(key:String):List<Float> {
            val values=curve(key);val array=o.optJSONArray(key+"X")
            val xs=if(array!=null && array.length()==values.size)List(values.size){array.optDouble(it,Double.NaN).toFloat()}else emptyList()
            return PhotoCurve.positions(values,xs)
        }
        return Adjustments(exposure=f("exposure"),contrast=f("contrast",1f),saturation=f("saturation",1f),warmth=f("warmth"),rotation=o.optInt("rotation"),crop=o.optString("crop","Original"),
            raw=o.optJSONObject("raw")?.let { r -> RawDevelopment(number(r,"exposure",0f,-4f,4f),number(r,"temperature",0f,-1f,1f),number(r,"tint",0f,-1f,1f),number(r,"highlights",0f,0f,1f)) }?:RawDevelopment(),
            highlights=f("highlights"),shadows=f("shadows"),whites=f("whites"),blacks=f("blacks"),tint=f("tint"),vibrance=f("vibrance"),fade=f("fade"),vignette=f("vignette"),grain=f("grain"),sharpness=f("sharpness"),
            straighten=f("straighten"),flipHorizontal=o.optBoolean("flipHorizontal"),flipVertical=o.optBoolean("flipVertical"),cropLeft=f("cropLeft"),cropTop=f("cropTop"),cropRight=f("cropRight",1f),cropBottom=f("cropBottom",1f),
            curve=curve("curve"),redCurve=curve("redCurve"),greenCurve=curve("greenCurve"),blueCurve=curve("blueCurve"),
            curveX=curveX("curve"),redCurveX=curveX("redCurve"),greenCurveX=curveX("greenCurve"),blueCurveX=curveX("blueCurve"),
            perspectiveHorizontal=number(o,"perspectiveHorizontal",0f,-1f,1f),perspectiveVertical=number(o,"perspectiveVertical",0f,-1f,1f),
            lensDistortion=number(o,"lensDistortion",0f,-1f,1f),
            lensProfileId=o.optString("lensProfileId","").takeIf{it.isNotBlank()&&it!="null"}?.take(80),lensProfileEnabled=o.optBoolean("lensProfileEnabled",false),lensProfileCrop=number(o,"lensProfileCrop",1f,.95f,1.05f),lensTcaEnabled=o.optBoolean("lensTcaEnabled",false),
            cutout=o.optJSONObject("cutout")?.let { c -> PhotoCutout(
                ops=c.optJSONArray("ops")?.let { list -> (0 until minOf(list.length(),160)).mapNotNull { i -> list.optJSONObject(i)?.let { op ->
                    val p=op.optJSONArray("p")?:return@let null
                    val pts=(0 until minOf(p.length()/2,2000)).map { k -> BrushPoint(p.optDouble(k*2,.5).toFloat().coerceIn(0f,1f),p.optDouble(k*2+1,.5).toFloat().coerceIn(0f,1f)) }
                    if(pts.isEmpty())null else CutoutOp(pts,number(op,"r",.05f,.003f,.4f),op.optBoolean("restore"),op.optBoolean("smart",true),op.optBoolean("fill"),number(op,"tol",.16f,.02f,.6f))
                } } }?:emptyList(),
                background=c.optString("background","Transparent").takeIf { it in setOf("Transparent","Color","Blur","Image") }?:"Transparent",
                color=c.optLong("color",0xFFFFFFFF),imageUri=c.optString("image","").takeIf { it.isNotBlank() }?.take(4096),feather=number(c,"feather",.5f,0f,1f)) }?:PhotoCutout(),
            frame=o.optJSONObject("frame")?.let { fr -> PhotoFrame(fr.optString("ratio","None").takeIf { it in com.pinwheel.core.media.PhotoFrameRenderer.ratios }?:"None",
                fr.optString("style","White").takeIf { it in com.pinwheel.core.media.PhotoFrameRenderer.styles }?:"White",number(fr,"border",.05f,0f,.3f)) }?:PhotoFrame(),
            gradingBalance=f("gradingBalance").coerceIn(-1f,1f),gradingBlending=f("gradingBlending",.5f).coerceIn(0f,1f),
            texture=f("texture").coerceIn(-1f,1f),clarity=f("clarity").coerceIn(-1f,1f),dehaze=number(o,"dehaze",0f,-1f,1f),noiseReduction=f("noiseReduction").coerceIn(0f,1f),colorNoiseReduction=f("colorNoiseReduction").coerceIn(0f,1f),
            cloneSpots=o.optJSONArray("cloneSpots")?.let { list -> (0 until minOf(32,list.length())).mapNotNull { i -> list.optJSONObject(i)?.let { s -> CloneSpot(id=s.optString("id",java.util.UUID.randomUUID().toString()).take(80),x=number(s,"x",.5f,0f,1f),y=number(s,"y",.5f,0f,1f),sourceX=number(s,"sourceX",.3f,0f,1f),sourceY=number(s,"sourceY",.5f,0f,1f),radius=number(s,"radius",.06f,.005f,.3f),feather=number(s,"feather",.65f,0f,1f),mode=runCatching { RetouchMode.valueOf(s.optString("mode","CLONE")) }.getOrDefault(RetouchMode.CLONE),opacity=number(s,"opacity",1f,0f,1f)) } } }?:emptyList(),
            grading=List(3){i->o.optJSONArray("grading")?.optJSONObject(i)?.let {ToneGrade(number(it,"hue",0f,0f,360f),number(it,"saturation",0f,0f,1f),number(it,"luminance",0f,-1f,1f))}?:ToneGrade()},
            masks=o.optJSONArray("masks")?.let {array -> (0 until minOf(array.length(),8)).mapNotNull {i -> array.optJSONObject(i)?.let {m ->
                PhotoMask(id=m.optString("id",java.util.UUID.randomUUID().toString()),name=m.optString("name","Mask").take(60),
                    shape=runCatching {MaskShape.valueOf(m.optString("shape","RADIAL"))}.getOrDefault(MaskShape.RADIAL),
                    centerX=number(m,"x",.5f,0f,1f),centerY=number(m,"y",.5f,0f,1f),radiusX=number(m,"rx",.28f,.015f,2f),radiusY=number(m,"ry",.28f,.005f,1024f),
                    angle=number(m,"angle",0f,-360f,360f),feather=number(m,"feather",.7f,0f,1f),inverted=m.optBoolean("inverted"),enabled=m.optBoolean("enabled",true),
                    highlights=number(m,"highlights",0f,-1f,1f),shadows=number(m,"shadows",0f,-1f,1f),tint=number(m,"tint",0f,-1f,1f),clarity=number(m,"clarity",0f,-1f,1f),
                    exposure=number(m,"exposure",0f,-3f,3f),contrast=number(m,"contrast",0f,-1f,1f),warmth=number(m,"warmth",0f,-1f,1f),saturation=number(m,"saturation",0f,-1f,1f),strokes=m.optJSONArray("strokes")?.let { strokes ->
                        var remaining=4096
                        (0 until minOf(strokes.length(),128)).mapNotNull {index -> strokes.optJSONObject(index)?.let { stroke ->
                            val points=stroke.optJSONArray("points")
                            val count=minOf(points?.length()?:0,512,remaining);remaining-=count
                            if(count==0)null else MaskStroke(List(count) { n ->
                                val point=points!!.optJSONArray(n)
                                fun coordinate(i:Int)=point?.optDouble(i,.5)?.toFloat()?.takeIf{it.isFinite()}?.coerceIn(0f,1f)?:.5f
                                BrushPoint(coordinate(0),coordinate(1))
                            },number(stroke,"rx",.04f,.005f,1f),number(stroke,"ry",.04f,.005f,1024f),stroke.optBoolean("erase"))
                        } }
                    }?:emptyList())
            } } }?:emptyList(),
            mix=List(8){i->o.optJSONArray("mix")?.optJSONObject(i)?.let {ColorBand(it.optDouble("hue",0.0).toFloat(),it.optDouble("saturation",0.0).toFloat(),it.optDouble("luminance",0.0).toFloat())}?:ColorBand()})
    }
}
