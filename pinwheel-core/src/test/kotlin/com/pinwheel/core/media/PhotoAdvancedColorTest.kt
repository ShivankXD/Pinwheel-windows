package com.pinwheel.core.media

import com.pinwheel.core.model.*
import org.junit.Assert.*
import org.junit.Test

class PhotoAdvancedColorTest {
    @Test fun brushInterpolatesStrokesAndErasesOnlyPaintedAreas() {
        val paint=MaskStroke(listOf(BrushPoint(.2f,.5f),BrushPoint(.8f,.5f)),.06f,.08f)
        val erase=MaskStroke(listOf(BrushPoint(.5f,.3f),BrushPoint(.5f,.7f)),.04f,.04f,true)
        val weight=MaskWeight(PhotoMask(shape=MaskShape.BRUSH,strokes=listOf(paint,erase),feather=.6f))
        assertTrue(weight.at(.3f,.5f)>.99f)
        assertEquals(0f,weight.at(.5f,.5f),.001f)
        assertEquals(0f,weight.at(.1f,.1f),.001f)
        assertTrue(weight.at(.3f,.56f) in .01f.. .99f)
        val repainted=MaskWeight(PhotoMask(shape=MaskShape.BRUSH,strokes=listOf(paint,erase,paint),feather=.6f))
        assertEquals(1f,repainted.at(.5f,.5f),.001f)
    }
    @Test fun radialMaskIsFeatheredAndInvertIsComplementary() {
        val mask=PhotoMask(centerX=.4f,centerY=.6f,radiusX=.2f,radiusY=.3f,feather=.5f)
        val weight=MaskWeight(mask);val inverse=MaskWeight(mask.copy(inverted=true))
        assertEquals(1f,weight.at(.4f,.6f),.0001f)
        assertEquals(0f,weight.at(.9f,.6f),.0001f)
        assertTrue(weight.at(.55f,.6f) in .01f.. .99f)
        for(x in 0..10)for(y in 0..10)assertEquals(1f,weight.at(x/10f,y/10f)+inverse.at(x/10f,y/10f),.0001f)
        assertEquals(0f,MaskWeight(mask.copy(enabled=false,inverted=true)).at(.9f,.9f),0f)
    }

    @Test fun linearMaskHasDirectionalFalloff() {
        val horizontal=MaskWeight(PhotoMask(shape=MaskShape.LINEAR,feather=1f))
        assertTrue(horizontal.at(.1f,.5f)>.99f)
        assertEquals(.5f,horizontal.at(.5f,.5f),.0001f)
        assertTrue(horizontal.at(.9f,.5f)<.01f)
        val vertical=MaskWeight(PhotoMask(shape=MaskShape.LINEAR,angle=90f,feather=1f))
        assertTrue(vertical.at(.5f,.1f)>.99f)
        assertTrue(vertical.at(.5f,.9f)<.01f)
    }

    @Test fun localExposureLeavesUnselectedPixelsAndAlphaUntouched() {
        val original=0x7f808080
        val renderer=LocalMaskRenderer(listOf(PhotoMask(exposure=1f,feather=.5f)))
        assertEquals(original,renderer.pixel(original,0f,0f))
        val inside=renderer.pixel(original,.5f,.5f)
        assertEquals(0x7f,inside ushr 24)
        assertTrue((inside and 255)>128)
        assertFalse(LocalMaskRenderer(listOf(PhotoMask())).active)
    }

    @Test fun gradingSeparatesShadowsAndHighlightsAndPreservesEndpoints() {
        val transform=PhotoColorTransform(Adjustments(grading=listOf(ToneGrade(0f,1f),ToneGrade(),ToneGrade(240f,1f)),gradingBlending=0f))
        val shadow=transform.pixel(0xff303030.toInt(),.5f,.5f)
        val highlight=transform.pixel(0xffcccccc.toInt(),.5f,.5f)
        assertTrue(((shadow ushr 16) and 255)>(shadow and 255))
        assertTrue((highlight and 255)>((highlight ushr 16) and 255))
        assertEquals(0xff000000.toInt(),transform.pixel(0xff000000.toInt(),.5f,.5f))
        assertEquals(-1,transform.pixel(-1,.5f,.5f))
    }

    @Test fun presetsKeepDestinationMasksAndGeometry() {
        val source=Adjustments(exposure=1f,masks=listOf(PhotoMask(exposure=2f)))
        val destination=Adjustments(rotation=90,crop="1:1",masks=listOf(PhotoMask(name="Destination",exposure=-.5f)))
        val result=source.keepGeometryFrom(destination)
        assertEquals(1f,result.exposure,0f)
        assertEquals(90,result.rotation)
        assertEquals(destination.masks,result.masks)
    }
}
