package com.pinwheel.core.media

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class PhotoTcaTest {
    @Test fun publishedPolynomialUsesOriginalGreenCoordinatesAndKeepsGreenIdentity() {
        val p=PhotoLensProfiles.byId("canon-ef20-f28-ff")!!
        val t=p.tca!!
        assertEquals(-.0000976f,t.br,0f);assertEquals(1.0012100f,t.vr,0f)
        val warp=PhotoTca(6000,4000,p.id,true)
        val out=FloatArray(2)
        for(channel in 0..2) {
            warp.mapInverse(.9f,.8f,channel,out)
            val radius=sqrt(2400f*2400f+1200f*1200f)/2000f
            val factor=when(channel){0->-.0000976f*radius*radius+1.0012100f;2->.0002552f*radius*radius+.9997696f;else->1f}
            assertEquals(.5f+.4f*factor,out[0],.0000001f)
            assertEquals(.5f+.3f*factor,out[1],.0000001f)
        }
        warp.mapInverse(.5f,.5f,0,out);assertArrayEquals(floatArrayOf(.5f,.5f),out,0f)
    }
    @Test fun noCalibrationDisabledAndWrongAspectCannotChangeCoordinates() {
        val supported="canon-ef20-f28-ff"
        for(warp in listOf(PhotoTca(600,400,supported,false),PhotoTca(600,400,"nikon-afs50-f18g-ff",true),
            PhotoTca(600,400,"unknown",true),PhotoTca(600,450,supported,true))) {
            assertFalse(warp.active)
            val out=FloatArray(2);for(channel in 0..2){warp.mapInverse(.1f,.9f,channel,out);assertArrayEquals(floatArrayOf(.1f,.9f),out,0f)}
        }
    }
    @Test fun channelMapComposesAfterDistortionAndKeepsExistingGeometryUnchanged() {
        for(profile in PhotoLensProfiles.profiles.filter{it.tca!=null}) {
            val lens=PhotoLens(600,400,.4f,profile.id,true,1.005f,true)
            val old=PhotoLens(600,400,.4f,profile.id,true,1.005f)
            val tca=PhotoTca(600,400,profile.id,true,1.005f)
            for(x in 0..10)for(y in 0..10) {
                val baseline=old.inverse(x/10f,y/10f)
                assertEquals(baseline,lens.inverse(x/10f,y/10f))
                for(channel in 0..2) {
                    val expected=FloatArray(2);val actual=FloatArray(2)
                    tca.mapInverse(baseline.x,baseline.y,channel,expected)
                    lens.mapChannelInverse(x/10f,y/10f,channel,actual)
                    assertArrayEquals(expected,actual,0f)
                }
            }
        }
    }
}
