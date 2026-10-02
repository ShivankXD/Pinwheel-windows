package com.pinwheel.core.media

import org.junit.Assert.*
import org.junit.Test

class PhotoLensTest {
    @Test fun neutralAndInvalidStrengthDoNotMoveCoordinates() {
        for(amount in listOf(0f,Float.NaN,Float.POSITIVE_INFINITY)) {
            val mapping=PhotoLens(6000,4000,amount)
            assertFalse(mapping.active)
            val forward=mapping.forward(.13f,.91f);val inverse=mapping.inverse(.13f,.91f)
            assertEquals(.13f,forward.x,0f);assertEquals(.91f,forward.y,0f)
            assertEquals(.13f,inverse.x,.000001f);assertEquals(.91f,inverse.y,.000001f)
        }
    }
    @Test fun forwardInverseRoundTripAcrossStrengthAndAspectRatio() {
        for((width,height) in listOf(6000 to 4000,4000 to 6000,10000 to 200,1 to 1000))for(amount in listOf(-1f,-.5f,.5f,1f)) {
            val mapping=PhotoLens(width,height,amount)
            for(x in 0..10)for(y in 0..10) {
                val frame=mapping.forward(x/10f,y/10f);val original=mapping.inverse(frame.x,frame.y)
                assertEquals("x round trip $width/$height $amount",x/10f,original.x,.00001f)
                assertEquals("y round trip $width/$height $amount",y/10f,original.y,.00001f)
            }
        }
    }
    @Test fun everyOutputPointHasAValidSourceAtBothExtremes() {
        for(amount in listOf(-1f,1f)) {
            val mapping=PhotoLens(4032,3024,amount)
            for(x in 0..100)for(y in 0..100) {
                val p=mapping.inverse(x/100f,y/100f)
                assertTrue("No blank wedges",p.x in 0f..1f && p.y in 0f..1f)
            }
            assertEquals(PhotoPoint(.5f,.5f),mapping.inverse(.5f,.5f))
        }
    }
    @Test fun bothDirectionsBendStraightLinesDifferently() {
        val negative=PhotoLens(1200,800,-1f);val positive=PhotoLens(1200,800,1f)
        val negBend=negative.forward(.5f,.25f).y-negative.forward(.1f,.25f).y
        val posBend=positive.forward(.5f,.25f).y-positive.forward(.1f,.25f).y
        assertTrue(negBend*posBend<0f)
    }
}
