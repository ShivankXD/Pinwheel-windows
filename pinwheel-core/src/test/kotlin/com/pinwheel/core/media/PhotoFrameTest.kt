package com.pinwheel.core.media

import com.pinwheel.core.model.Adjustments
import com.pinwheel.core.model.PhotoFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PhotoFrameTest {
    @Test fun canvasHasTheChosenShapeAndHoldsThePhotoUnscaled() {
        for (ratio in PhotoFrameRenderer.ratios.filter { it != "None" }) for ((w, h) in listOf(4000 to 3000, 3000 to 4000, 2000 to 2000)) {
            val frame = PhotoFrame(ratio, "White", .06f)
            val l = PhotoFrameRenderer.layout(w, h, frame)
            val want = PhotoFrameRenderer.ratio(frame, w.toFloat() / h)
            if (ratio != "Even") assertTrue("$ratio $w x $h", abs(l.width.toFloat() / l.height - want) < .01f)
            assertEquals(w.toFloat(), l.photoWidth, .5f); assertEquals(h.toFloat(), l.photoHeight, .5f)
            assertTrue(l.left >= -.5f && l.top >= -.5f && l.left + l.photoWidth <= l.width + .5f && l.top + l.photoHeight <= l.height + .5f)
        }
    }

    @Test fun evenBorderIsTheSameOnEverySide() {
        val l = PhotoFrameRenderer.layout(3000, 2000, PhotoFrame("Even", "Black", .05f))
        assertEquals(l.left, l.height - (l.top + l.photoHeight), 1.5f)
        assertEquals(l.top, l.left, 1.5f)
    }

    @Test fun frameSurvivesSaveAndLooks() {
        val framed = Adjustments(exposure = .2f, frame = PhotoFrame("4:5", "Blur", .08f))
        val look = PhotoLooks.builtIn.first { it.name == "Portrait 400" }
        assertEquals(framed.frame, look.adjustments.keepGeometryFrom(framed).frame)
    }
}
