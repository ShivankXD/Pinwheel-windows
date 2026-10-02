package com.pinwheel.core.media

import com.pinwheel.core.model.Adjustments
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class PhotoPackedColorTest {
    @Test fun packedChannelsMatchGeneralPipelineAcrossToneCurvesWhiteBalanceFadeAndAlpha() {
        val recipes=listOf(
            Adjustments(),
            Adjustments(exposure=1.3f,contrast=1.35f,highlights=-.7f,shadows=.4f,whites=.2f,blacks=-.15f),
            Adjustments(exposure=-1.7f,contrast=.45f,highlights=.6f,shadows=-.5f,whites=-.8f,blacks=.5f),
            Adjustments(warmth=.8f,tint=-.5f,fade=.7f),
            Adjustments(warmth=-.9f,tint=.8f,fade=1f,contrast=0f),
            Adjustments(curve=listOf(0f,.1f,.65f,.9f,1f),redCurve=listOf(.05f,.35f,.55f,.8f,1f),greenCurve=listOf(0f,.15f,.4f,.85f,.98f),blueCurve=listOf(.08f,.3f,.6f,.8f,.94f)),
        )
        for(recipe in recipes) {
            val packed=PhotoColorTransform(recipe)
            // A nonzero vignette disables the packed path; at the image centre
            // its spatial multiplier is exactly one, giving a general reference.
            val reference=PhotoColorTransform(recipe.copy(vignette=Float.MIN_VALUE))
            var state=0x01234567
            repeat(4096) {
                state=state*1664525+1013904223
                val actual=packed.pixel(state,.5f,.5f);val expected=reference.pixel(state,.5f,.5f)
                assertEquals(state ushr 24,actual ushr 24)
                for(shift in listOf(0,8,16)) {
                    // The general saturation identity adds/subtracts luma and
                    // can differ by one code value at a floating-point tie.
                    assertTrue("Packed channel must preserve colour precision",abs(((actual ushr shift)and 255)-((expected ushr shift)and 255))<=1)
                }
            }
        }
    }

    @Test fun neutralPackedPathPreservesEveryEightBitChannelAndAllAlphaValues() {
        val transform=PhotoColorTransform(Adjustments())
        for(value in 0..255)for(alpha in listOf(0,1,64,128,254,255)) {
            val pixel=(alpha shl 24)or(value shl 16)or((255-value)shl 8)or((value*71)%256)
            assertEquals(pixel,transform.pixel(pixel,.1f,.9f))
        }
    }

    @Test fun spatialEditsNeverUseThePositionIndependentPackedPath() {
        val plain=PhotoColorTransform(Adjustments(exposure=.2f,fade=.2f))
        val vignette=PhotoColorTransform(Adjustments(exposure=.2f,fade=.2f,vignette=.9f))
        val pixel=0xff8a7e72.toInt()
        val centre=plain.pixel(pixel,.5f,.5f);val reference=vignette.pixel(pixel,.5f,.5f)
        for(shift in listOf(0,8,16))assertTrue(abs(((centre ushr shift)and 255)-((reference ushr shift)and 255))<=1)
        assertTrue((vignette.pixel(pixel,0f,0f)and 255)<(plain.pixel(pixel,0f,0f)and 255))
    }
}
