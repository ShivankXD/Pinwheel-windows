package com.pinwheel.core

import com.pinwheel.core.media.RawLinearTransform
import com.pinwheel.core.model.RawDevelopment
import org.junit.Assert.*
import org.junit.Test

class RawLinearTransformTest {
    @Test fun exposureIsLinearBeforeDisplayTransferAndDoesNotClampHdr() {
        val transform=RawLinearTransform(RawDevelopment(exposure=2f))
        assertEquals(.4,transform.channel(.1,0),1e-12)
        assertEquals(3.6,transform.channel(.9,0),1e-12)
        assertEquals(.735356983,RawLinearTransform.toSrgb(.5),1e-8)
    }
    @Test fun highlightCompressionLeavesShadowsAndRemainsContinuousAtKnee() {
        val transform=RawLinearTransform(RawDevelopment(highlights=1f))
        assertEquals(.3,transform.channel(.3,1),1e-12)
        assertEquals(.6,transform.channel(.6,1),1e-12)
        assertEquals(.6000001,transform.channel(.6000001,1),1e-12)
        assertTrue(transform.channel(3.0,1)<1.0)
    }
    @Test fun relativeWhiteBalanceAndMalformedInputAreFinite() {
        val warm=RawLinearTransform(RawDevelopment(temperature=1f))
        assertTrue(warm.channel(.4,0)>.4);assertTrue(warm.channel(.4,2)<.4)
        val malformed=RawLinearTransform(RawDevelopment(Float.NaN,Float.POSITIVE_INFINITY,Float.NaN,Float.NEGATIVE_INFINITY))
        for(c in 0..2)assertEquals(.4,malformed.channel(.4,c),1e-12)
    }
}
