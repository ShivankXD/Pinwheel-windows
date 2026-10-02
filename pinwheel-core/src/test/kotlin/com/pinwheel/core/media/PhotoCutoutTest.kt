package com.pinwheel.core.media

import com.pinwheel.core.model.BrushPoint
import com.pinwheel.core.model.CutoutOp
import com.pinwheel.core.model.PhotoCutout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoCutoutTest {
    // 200 x 200: light grey studio background with a dark red subject square in the middle.
    private val w = 200; private val h = 200
    private val px = IntArray(w * h) { i -> val x = i % w; val y = i / w; if (x in 60..139 && y in 60..139) 0xFF8A1F24.toInt() else 0xFFE4E4E2.toInt() }
    private fun sweep(smart: Boolean) = CutoutOp((0..20).flatMap { r -> listOf(BrushPoint(0f, r / 20f), BrushPoint(1f, r / 20f)) }, radius = .08f, smart = smart)
    /** A ring hugging the subject: the brush centre stays on the background but its edge overlaps the subject. */
    private fun ring() = CutoutOp(listOf(BrushPoint(.22f, .22f), BrushPoint(.78f, .22f), BrushPoint(.78f, .78f), BrushPoint(.22f, .78f), BrushPoint(.22f, .22f)), radius = .12f, smart = true)

    @Test fun smartBrushStopsAtTheSubject() {
        val m = CutoutMask.build(px, w, h, PhotoCutout(listOf(ring()), feather = 0f))
        assertTrue("background under the brush removed", m.at(.22f, .5f) < .05f)
        assertTrue("subject edge kept", m.at(.32f, .5f) > .95f)
        assertTrue("subject kept", m.at(.5f, .5f) > .95f)
    }

    @Test fun plainBrushRemovesEverythingItTouches() {
        val m = CutoutMask.build(px, w, h, PhotoCutout(listOf(sweep(false)), feather = 0f))
        assertTrue(m.at(.5f, .5f) < .05f)
    }

    @Test fun tapSelectTakesTheConnectedBackgroundAndRestoreBringsItBack() {
        val select = CutoutOp(listOf(BrushPoint(.05f, .05f)), fill = true, tolerance = .1f)
        val m = CutoutMask.build(px, w, h, PhotoCutout(listOf(select), feather = 0f))
        assertTrue(m.at(.95f, .95f) < .05f); assertTrue(m.at(.5f, .5f) > .95f)
        val restored = CutoutMask.build(px, w, h, PhotoCutout(listOf(select, CutoutOp(listOf(BrushPoint(.1f, .1f)), radius = .05f, restore = true, smart = false)), feather = 0f))
        assertEquals(1f, restored.at(.1f, .1f), .05f)
    }
}
