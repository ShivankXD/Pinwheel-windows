package com.pinwheel.core.model

import org.junit.Assert.*
import org.junit.Test

class ProjectTest {
    @Test fun splitPreservesTrimmedSourceRange() {
        val clip = Clip(uri="file:///a.mp4", name="a", sourceDurationMs=12000, startMs=2000, endMs=9000)
        val (left, right) = clip.split(3000)!!
        assertEquals(2000, left.startMs); assertEquals(5000, left.endMs)
        assertEquals(5000, right.startMs); assertEquals(9000, right.endMs)
        assertEquals(clip.durationMs, left.durationMs + right.durationMs)
        assertNotEquals(left.id, right.id)
    }
    @Test fun splitRejectsUnusableFragments() {
        val c = Clip(uri="a", name="a", sourceDurationMs=1000)
        assertNull(c.split(0)); assertNull(c.split(950)); assertNull(c.split(-5)); assertNull(c.split(2000))
    }
    @Test fun trimClampsToSourceAndPreservesMinimumDuration() {
        val c = Clip(uri="a", name="a", sourceDurationMs=1000).trim(1500, -20)
        assertEquals(900, c.startMs); assertEquals(1000, c.endMs)
    }
    @Test fun newEditAfterUndoInvalidatesRedo() {
        val p = StudioProject(name="a", kind=ProjectKind.VIDEO, clips=emptyList())
        val h = EditHistory(); h.record(p)
        assertEquals(p, h.undo(p.copy(name="b")))
        assertTrue(h.canRedo)
        h.record(p.copy(name="c")); assertFalse(h.canRedo)
    }
}
