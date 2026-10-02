package com.pinwheel.core.media

import com.pinwheel.core.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class PhotoPrecisionColorTest {
    @Test fun identityPreservesSubEightBitDifferencesAndHdrHeadroom() {
        val transform=PhotoColorTransform(Adjustments())
        for(hdr in listOf(false,true))for(v in listOf(.0001f,.20001f,.20002f,.67891f,1f)) {
            val rgb=floatArrayOf(v,v,v);transform.precise(rgb,.5f,.5f,hdr)
            assertEquals(v,rgb[0],.000001f)
        }
        val extended=floatArrayOf(1.1f,2f,4f)
        transform.precise(extended,.5f,.5f,true)
        assertArrayEquals(floatArrayOf(1.1f,2f,4f),extended,.00001f)
    }

    @Test fun hdrLightExposureAndCurveContinueAboveWhiteWithoutClipping() {
        val rgb=floatArrayOf(2f,2f,2f)
        PhotoColorTransform(Adjustments(exposure=-1f)).precise(rgb,.5f,.5f,true)
        assertEquals((2.0*2.0.pow(-1/2.2)).toFloat(),rgb[0],.00001f)
        val compressed=floatArrayOf(2f,2f,2f)
        PhotoColorTransform(Adjustments(curve=listOf(0f,.25f,.5f,.7f,.8f))).precise(compressed,.5f,.5f,true)
        assertEquals(1.2f,compressed[0],.00001f)
    }

    @Test fun floatAndDisplayColorAgreeWithinQuantizationForRepresentativeRecipes() {
        val recipes=listOf(Adjustments(exposure=.2f,contrast=1.1f,shadows=.2f,highlights=-.3f),
            Adjustments(warmth=.3f,tint=-.1f,saturation=1.2f,vibrance=.2f),
            Adjustments(mix=List(8){ColorBand(.1f,-.1f,.1f)},dehaze=.2f),
            Adjustments(grading=listOf(ToneGrade(30f,.3f),ToneGrade(210f,.2f),ToneGrade(160f,.1f)),fade=.2f,vignette=.3f))
        val random=java.util.Random(123)
        for(a in recipes) {
            val t=PhotoColorTransform(a)
            repeat(200) {
                val p=0xff000000.toInt() or random.nextInt(0x1000000)
                val rgb=FloatArray(3){c->((p ushr (16-c*8)) and 255)/255f}
                t.precise(rgb,.37f,.61f)
                val display=t.pixel(p,.37f,.61f)
                for(c in 0..2)assertEquals(((display ushr (16-c*8)) and 255).toFloat(),rgb[c]*255,1.1f)
            }
        }
    }
}
