package com.pinwheel.core.ui
import com.pinwheel.core.model.Adjustments
import org.junit.Assert.*
import org.junit.Test
class PhotoCropFrameTest {
 @Test fun resizingAnyHandleSelectsFreeCrop(){for(handle in 0..7){val a=Adjustments(crop="1:1",cropLeft=.2f,cropTop=.2f,cropRight=.8f,cropBottom=.8f);val b=resizeCropFrame(a,handle,.1f,.1f);assertEquals("Free",b.crop);assertNotEquals(a,b)}}
 @Test fun movingKeepsRatioAndSizeInsideImage(){val a=Adjustments(crop="1:1",cropLeft=.2f,cropTop=.2f,cropRight=.8f,cropBottom=.8f);val b=resizeCropFrame(a,-1,5f,-5f);assertEquals("1:1",b.crop);assertEquals(1f,b.cropRight,.0001f);assertEquals(0f,b.cropTop,.0001f);assertEquals(.6f,b.cropRight-b.cropLeft,.0001f)}
 @Test fun edgesDoNotMoveUnrelatedCoordinates(){val a=Adjustments(cropLeft=.2f,cropTop=.2f,cropRight=.8f,cropBottom=.8f);val b=resizeCropFrame(a,4,.4f,.1f);assertEquals(a.cropLeft,b.cropLeft,0f);assertEquals(a.cropRight,b.cropRight,0f);assertEquals(.3f,b.cropTop,.0001f)}
 @Test fun crossingHandlesCannotInvertFrame(){val a=Adjustments();for(handle in 0..7){for(delta in listOf(-10f,10f)){val b=resizeCropFrame(a,handle,delta,delta);assertTrue(b.cropLeft>=0 && b.cropRight<=1 && b.cropTop>=0 && b.cropBottom<=1);assertTrue(b.cropRight-b.cropLeft>=.079f && b.cropBottom-b.cropTop>=.079f)}}}
}
