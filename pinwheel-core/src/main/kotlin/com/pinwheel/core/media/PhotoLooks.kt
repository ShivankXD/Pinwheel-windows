package com.pinwheel.core.media
import com.pinwheel.core.data.AtomicFile
import com.pinwheel.core.data.AdjustmentCodec
import com.pinwheel.core.model.Adjustments
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class PhotoLook(val name:String,val description:String,val adjustments:Adjustments,val category:String="Essentials")
fun Adjustments.keepGeometryFrom(a:Adjustments)=copy(raw=a.raw,lensProfileId=a.lensProfileId,lensProfileEnabled=a.lensProfileEnabled,lensProfileCrop=a.lensProfileCrop,lensTcaEnabled=a.lensTcaEnabled,lensDistortion=a.lensDistortion,cloneSpots=a.cloneSpots,perspectiveHorizontal=a.perspectiveHorizontal,perspectiveVertical=a.perspectiveVertical,masks=a.masks,frame=a.frame,cutout=a.cutout,rotation=a.rotation,crop=a.crop,straighten=a.straighten,flipHorizontal=a.flipHorizontal,flipVertical=a.flipVertical,cropLeft=a.cropLeft,cropTop=a.cropTop,cropRight=a.cropRight,cropBottom=a.cropBottom)
object PhotoLooks {
 val builtIn=listOf(
 PhotoLook("Original","A clean start",Adjustments()),
 PhotoLook("Daylight","Clear and balanced",Adjustments(exposure=.12f,contrast=1.06f,vibrance=.18f,highlights=-.12f)),
 PhotoLook("Golden","Soft evening warmth",Adjustments(exposure=.12f,contrast=1.04f,warmth=.45f,highlights=-.18f,shadows=.12f,vibrance=.1f)),
 PhotoLook("Coast","Cool, open shadows",Adjustments(contrast=1.08f,saturation=.9f,warmth=-.22f,shadows=.24f,highlights=-.16f),"Landscape"),
 PhotoLook("Alpine","Crisp mountain light",Adjustments(contrast=1.1f,highlights=-.2f,shadows=.15f,warmth=-.08f,vibrance=.22f,dehaze=.14f),"Landscape"),
 PhotoLook("Woodland","Deep greens and warm light",Adjustments(contrast=1.08f,warmth=.15f,vibrance=.15f,shadows=.08f,blacks=-.08f),"Landscape"),
 PhotoLook("Silver","Tonal monochrome",Adjustments(contrast=1.15f,saturation=0f,highlights=-.12f,shadows=.15f,grain=.12f),"Monochrome"),
 PhotoLook("Graphite","Rich monochrome shadows",Adjustments(contrast=1.25f,saturation=0f,blacks=-.12f,highlights=-.18f,vignette=.12f),"Monochrome"),
 PhotoLook("Paper","Soft matte monochrome",Adjustments(contrast=.94f,saturation=0f,shadows=.2f,fade=.14f,grain=.08f),"Monochrome"),
 PhotoLook("Soft film","Faded and understated",Adjustments(exposure=.08f,contrast=.94f,saturation=.8f,warmth=.15f,fade=.2f,grain=.1f),"Film"),
 PhotoLook("Amber","Warm print tones",Adjustments(warmth=.28f,contrast=1.1f,saturation=.88f,highlights=-.16f,fade=.08f,grain=.16f),"Film"),
 PhotoLook("Cool print","Quiet blues and lifted blacks",Adjustments(warmth=-.18f,saturation=.82f,shadows=.14f,fade=.12f,grain=.08f),"Film"),
 PhotoLook("Nightfall","Deep cinematic colour",Adjustments(exposure=-.15f,contrast=1.18f,saturation=.8f,warmth=-.12f,highlights=-.2f,vignette=.24f),"Film"),
 PhotoLook("Portrait","Gentle warm light",Adjustments(exposure=.16f,contrast=.96f,warmth=.12f,shadows=.16f,highlights=-.1f,vibrance=.08f),"Portrait"),
 PhotoLook("Window","Bright natural skin tones",Adjustments(exposure=.22f,contrast=.95f,highlights=-.2f,shadows=.18f,saturation=.96f),"Portrait"),
 PhotoLook("Soft glow","Low contrast and warm highlights",Adjustments(exposure=.14f,contrast=.92f,warmth=.18f,highlights=-.15f,fade=.06f),"Portrait"))+PhotoLookLibrary.looks
 private fun read(filesDir:File):List<PhotoLook> {
 val file=File(filesDir,"photo-looks.json")
 val text=if(file.exists() || File(file.path+".bak").exists())AtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use{it.readText()}
 else "[]"
 val array=JSONArray(text)
 return (0 until array.length()).map{val o=array.getJSONObject(it);PhotoLook(o.getString("name"),"Your preset",AdjustmentCodec.decode(o.getJSONObject("adjustments")))}
 }
 @Synchronized fun saved(filesDir:File):List<PhotoLook> = runCatching {read(filesDir)}.getOrDefault(emptyList())
 @Synchronized fun save(filesDir:File,name:String,a:Adjustments){
 // A damaged existing collection must never be silently replaced by an empty one.
 val looks=(read(filesDir).filterNot{it.name==name}+PhotoLook(name,"Your preset",a)).takeLast(40)
 val data=JSONArray().apply{looks.forEach{put(JSONObject().put("name",it.name).put("adjustments",AdjustmentCodec.encode(it.adjustments)))}}
 val text=data.toString();val atomic=AtomicFile(File(filesDir,"photo-looks.json"))
 val output=atomic.startWrite()
 try {output.write(text.toByteArray(Charsets.UTF_8));output.fd.sync();atomic.finishWrite(output)}
 catch(error:Exception){atomic.failWrite(output);throw error}
 // AtomicFile can log a failed rename without throwing. Verify before reporting success.
 check(atomic.openRead().bufferedReader(Charsets.UTF_8).use{it.readText()}==text){"Could not save preset"}
 }
}
