package com.pinwheel.core.media
import com.pinwheel.core.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class PhotoColorTest {
 @Test fun neutralRecipePreservesPixels(){
  val transform=PhotoColorTransform(Adjustments())
  for(r in 0..255 step 17)for(g in 0..255 step 23)for(b in 0..255 step 29){
   val p=-0x1000000 or (r shl 16) or(g shl 8)or b;val q=transform.pixel(p,.25f,.75f)
   assertTrue(abs(r-((q ushr 16)and 255))<=1);assertTrue(abs(g-((q ushr 8)and 255))<=1);assertTrue(abs(b-(q and 255))<=1)
  }
 }
 @Test fun exposureAndShadowRecoveryIncreaseDarkTones(){
  val p=0xff303030.toInt();val neutral=PhotoColorTransform(Adjustments()).pixel(p,.5f,.5f)and 255
  assertTrue((PhotoColorTransform(Adjustments(exposure=1f)).pixel(p,.5f,.5f)and 255)>neutral)
  assertTrue((PhotoColorTransform(Adjustments(shadows=.8f)).pixel(p,.5f,.5f)and 255)>neutral)
 }
 @Test fun monochromeHasEqualChannels(){
  val q=PhotoColorTransform(Adjustments(saturation=0f,warmth=.4f)).pixel(0xff3a80d2.toInt(),.5f,.5f)
  assertEquals((q ushr 16)and 255,(q ushr 8)and 255);assertEquals(q and 255,(q ushr 8)and 255)
 }
 @Test fun colourMixPreservesNeutralAndCanDesaturateRed(){
  val a=Adjustments(mix=List(8){if(it==0)ColorBand(saturation=-1f)else ColorBand()})
  val q=PhotoColorTransform(a).pixel(0xffff0000.toInt(),.5f,.5f)
  assertEquals((q ushr 16)and 255,(q ushr 8)and 255)
  assertEquals(0xff808080.toInt(),PhotoColorTransform(a).pixel(0xff808080.toInt(),.5f,.5f))
 }
 @Test fun curvesAffectOnlySelectedChannel(){
  val q=PhotoColorTransform(Adjustments(redCurve=listOf(0f,.4f,.7f,.9f,1f))).pixel(0xff808080.toInt(),.5f,.5f)
  assertTrue(((q ushr 16)and 255)>((q ushr 8)and 255));assertEquals(128,q and 255)
 }
 @Test fun grainIsDeterministicAndVignetteDarkensCorners(){
  val t=PhotoColorTransform(Adjustments(grain=.8f));assertEquals(t.pixel(-1,.23f,.67f),t.pixel(-1,.23f,.67f))
  val v=PhotoColorTransform(Adjustments(vignette=1f));assertTrue((v.pixel(-1,0f,0f)and 255)<(v.pixel(-1,.5f,.5f)and 255))
 }
 @Test fun autoIsBoundedForBlackAndWhiteImages(){
  for(bin in listOf(0,1,127,254,255)){
   val h=IntArray(256);h[bin]=1000;val a=AutoTone.suggest(h,0.0,0.0,0.0,Adjustments(rotation=90,crop="1:1"))
   assertTrue(a.exposure.isFinite());assertTrue(a.exposure in -.85f.. .85f);assertEquals(90,a.rotation);assertEquals("1:1",a.crop)
  }
 }
}
