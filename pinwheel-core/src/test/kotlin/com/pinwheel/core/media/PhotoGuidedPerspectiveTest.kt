package com.pinwheel.core.media

import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class PhotoGuidedPerspectiveTest {
    private fun inputGuide(x: Float, angle: Float, vertical: Float, horizontal: Float, aspect: Float): PerspectiveGuide {
        val r=Math.toRadians(angle.toDouble()); val c=cos(r); val s=sin(r)
        val cover=max(abs(c)+abs(s)/aspect,abs(c)+abs(s)*aspect)
        val perspective=PhotoPerspective(horizontal,vertical)
        fun input(y: Float): PhotoPoint {
            val p=perspective.inverse(x,y)
            val dx=(p.x-.5)*aspect; val dy=p.y-.5
            return PhotoPoint((.5+(c*dx+s*dy)/cover/aspect).toFloat(),(.5+(-s*dx+c*dy)/cover).toFloat())
        }
        return PerspectiveGuide(input(.15f),input(.85f))
    }

    @Test fun recoversRotationAndKeystoneAcrossPortraitLandscapeAndExistingHorizontal() {
        for(aspect in listOf(.5f,1f,1.5f,2f)) for(angle in listOf(-22f,0f,19f)) for(vertical in listOf(-.7f,0f,.6f)) {
            val guides=listOf(inputGuide(.2f,angle,vertical,.35f,aspect),inputGuide(.8f,angle,vertical,.35f,aspect))
            val result=PhotoGuidedPerspective.solve(guides,aspect)
            assertNotNull("aspect=$aspect angle=$angle vertical=$vertical",result)
            assertEquals(angle,result!!.straighten,.0002f)
            assertEquals(vertical,result.vertical,.00002f)
            // Independently project the traced edges: both endpoints must share x.
            val radians=Math.toRadians(result.straighten.toDouble()); val c=cos(radians); val s=sin(radians)
            val cover=max(abs(c)+abs(s)/aspect,abs(c)+abs(s)*aspect)
            val p=PhotoPerspective(.35f,result.vertical)
            fun output(input: PhotoPoint): PhotoPoint {
                val x=(input.x-.5)*aspect; val y=input.y-.5
                return p.forward((.5+cover*(c*x-s*y)/aspect).toFloat(),(.5+cover*(s*x+c*y)).toFloat())
            }
            guides.forEach { assertEquals(output(it.start).x,output(it.end).x,.00001f) }
        }
    }

    @Test fun reversingGuideDirectionsAndOrderDoesNotChangeCorrection() {
        val guides=listOf(inputGuide(.2f,12f,.4f,0f,1.5f),inputGuide(.8f,12f,.4f,0f,1.5f))
        val variants=listOf(guides,guides.reversed(),guides.map { PerspectiveGuide(it.end,it.start) },listOf(guides[0],PerspectiveGuide(guides[1].end,guides[1].start)))
        variants.forEach { val result=PhotoGuidedPerspective.solve(it,1.5f)!!; assertEquals(12f,result.straighten,.0002f);assertEquals(.4f,result.vertical,.00002f) }
    }

    @Test fun rejectsDegenerateShortCoincidentHorizontalAndNonFiniteGuides() {
        val good=PerspectiveGuide(PhotoPoint(.2f,.1f),PhotoPoint(.2f,.9f))
        val other=PerspectiveGuide(PhotoPoint(.8f,.1f),PhotoPoint(.8f,.9f))
        assertNull(PhotoGuidedPerspective.solve(listOf(good),1f))
        assertNull(PhotoGuidedPerspective.solve(listOf(good,good),1f))
        assertNull(PhotoGuidedPerspective.solve(listOf(good,other.copy(end=PhotoPoint(.8f,.15f))),1f))
        assertNull(PhotoGuidedPerspective.solve(listOf(good,other.copy(start=PhotoPoint(Float.NaN,.1f))),1f))
        assertNull(PhotoGuidedPerspective.solve(listOf(good,other),0f))
        assertNull(PhotoGuidedPerspective.solve(listOf(good,other),Float.POSITIVE_INFINITY))
    }

    @Test fun refusesCorrectionsBeyondExistingGeometryBoundsInsteadOfClampingBadResults() {
        val rolled=listOf(inputGuide(.2f,40f,0f,0f,1f),inputGuide(.8f,40f,0f,0f,1f))
        assertNull(PhotoGuidedPerspective.solve(rolled,1f))
        val converging=listOf(PerspectiveGuide(PhotoPoint(.4f,.1f),PhotoPoint(.1f,.9f)),PerspectiveGuide(PhotoPoint(.6f,.1f),PhotoPoint(.9f,.9f)))
        assertNull(PhotoGuidedPerspective.solve(converging,1f))
    }
}
