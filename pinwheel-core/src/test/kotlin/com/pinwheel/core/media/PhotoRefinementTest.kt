package com.pinwheel.core.media

import com.pinwheel.core.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class PhotoRefinementTest {
    @Test fun legacyCurveOutputIsUnchangedAtEveryInputLevel() {
        val ys=listOf(.03f,.31f,.54f,.72f,.97f)
        for(i in 0..255) {
            val x=i/255f*4;val left=x.toInt().coerceAtMost(3)
            val expected=ys[left]+(ys[left+1]-ys[left])*(x-left)
            assertEquals(expected,PhotoCurve.evaluate(i/255f,ys),.000001f)
        }
    }
    @Test fun arbitraryCurvesHitNodesAndDoNotOvershoot() {
        val xs=listOf(0f,.13f,.42f,.8f,1f);val ys=listOf(.1f,.6f,.2f,.9f,.95f)
        for(i in xs.indices)assertEquals(ys[i],PhotoCurve.evaluate(xs[i],ys,xs),.000001f)
        for(i in 0..1000)assertTrue(PhotoCurve.evaluate(i/1000f,ys,xs) in .1f.. .95f)
        val blended=PhotoCurve.blend(ys,xs,listOf(0f,.3f,1f),listOf(0f,.6f,1f),.4f)
        assertTrue(PhotoCurve.valid(blended.first,blended.second))
        for(i in 0..100) {
            val x=i/100f;val expected=.6f*PhotoCurve.evaluate(x,ys,xs)+.4f*PhotoCurve.evaluate(x,listOf(0f,.3f,1f),listOf(0f,.6f,1f))
            assertEquals(expected,PhotoCurve.evaluate(x,blended.first,blended.second),.00001f)
        }
    }
    @Test fun neutralPickerCorrectsWarmAndGreenCastsWithoutChangingNeutralGreys() {
        for(rgb in listOf(listOf(132f,120f,112f),listOf(115f,130f,120f),listOf(70f,70f,70f))) {
            val result=PhotoWhiteBalance.neutral(rgb[0],rgb[1],rgb[2])!!
            assertFalse(result.limited)
            val r=rgb[0]*(1+.16f*result.warmth+.06f*result.tint)
            val g=rgb[1]*(1-.1f*result.tint)
            val b=rgb[2]*(1-.16f*result.warmth+.06f*result.tint)
            assertEquals(r,g,.001f);assertEquals(g,b,.001f)
        }
        assertNull(PhotoWhiteBalance.neutral(255f,255f,255f))
        assertNull(PhotoWhiteBalance.neutral(1f,2f,1f))
        assertNull(PhotoWhiteBalance.neutral(Float.NaN,100f,100f))
        assertTrue(PhotoWhiteBalance.neutral(180f,100f,30f)!!.limited)
    }
    @Test fun perspectiveIsInvertibleAndCoverAlwaysKeepsOutputInsideSource() {
        for(h in listOf(-1f,-.4f,0f,.3f,1f))for(v in listOf(-1f,-.3f,0f,.5f,1f)) {
            val warp=PhotoPerspective(h,v)
            for(x in 0..10)for(y in 0..10) {
                val p=warp.forward(x/10f,y/10f);val back=warp.inverse(p.x,p.y)
                assertEquals(x/10f,back.x,.00001f);assertEquals(y/10f,back.y,.00001f)
                val covered=warp.inverse(x/10f,y/10f)
                assertTrue(covered.x in -.00001f..1.00001f);assertTrue(covered.y in -.00001f..1.00001f)
            }
        }
    }
    @Test fun neutralPickerIncludesExistingLightEdits() {
        val base=Adjustments(contrast=1.8f,shadows=.2f,highlights=-.1f)
        val channels=PhotoColorTransform(base).beforeWhiteBalance(110f,100f,95f)
        val picked=PhotoWhiteBalance.neutral(channels[0],channels[1],channels[2])!!
        assertFalse(picked.limited)
        val out=PhotoColorTransform(base.copy(warmth=picked.warmth,tint=picked.tint)).pixel(0xff6e645f.toInt(),.5f,.5f)
        val r=(out ushr 16)and 255;val g=(out ushr 8)and 255;val b=out and 255
        assertTrue(abs(r-g)<=1 && abs(g-b)<=1)
    }
    @Test fun newLocalAdjustmentsAffectOnlyTheirSelection() {
        val neutral=0xff808080.toInt()
        for(mask in listOf(PhotoMask(highlights=.8f),PhotoMask(shadows=.8f),PhotoMask(tint=.5f))) {
            val renderer=LocalMaskRenderer(listOf(mask))
            assertEquals(neutral,renderer.pixel(neutral,0f,0f))
            assertNotEquals(neutral,renderer.pixel(neutral,.5f,.5f))
            assertEquals(neutral,LocalMaskRenderer(listOf(mask.copy(enabled=false))).pixel(neutral,.5f,.5f))
        }
    }
}
