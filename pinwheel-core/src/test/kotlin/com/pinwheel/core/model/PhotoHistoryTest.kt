package com.pinwheel.core.model

import org.junit.Assert.*
import org.junit.Test

class PhotoHistoryTest {
    private fun project() = StudioProject(name = "Original", kind = ProjectKind.PHOTO, clips = emptyList())
    private fun edit(p: StudioProject, exposure: Float) = p.copy(adjustments = p.adjustments.copy(exposure = exposure), photoHistory = p.photoHistory.record(p))

    @Test fun undoRedoPreserveCropMasksRawAndProjectIdentity() {
        val original = project().copy(adjustments = Adjustments(crop = "2:3", masks = listOf(PhotoMask(exposure = .6f)), raw = RawDevelopment(exposure = -.5f)))
        val edited = edit(original, 1f)
        val undone = edited.photoHistory.undo(edited)!!
        assertEquals(original.adjustments, undone.adjustments)
        assertEquals(original.id, undone.id)
        assertEquals(edited.adjustments, undone.photoHistory.redo(undone)!!.adjustments)
    }

    @Test fun aNewEditDropsTheOldRedoBranch() {
        val first = edit(project(), .5f)
        val second = edit(first, 1f)
        val undone = second.photoHistory.undo(second)!!
        assertEquals(1, undone.photoHistory.future.size)
        val fork = edit(undone, -.8f)
        assertNull(fork.photoHistory.redo(fork))
        assertEquals(first.adjustments, fork.photoHistory.undo(fork)!!.adjustments)
    }

    @Test fun historyIsBoundedAndSupportsMultipleRedoSteps() {
        var p = project()
        repeat(100) { p = edit(p, it / 100f) }
        assertEquals(40, p.photoHistory.past.size)
        val last = p.adjustments
        repeat(40) { p = p.photoHistory.undo(p)!! }
        assertNull(p.photoHistory.undo(p))
        repeat(40) { p = p.photoHistory.redo(p)!! }
        assertEquals(last, p.adjustments)
        assertNull(p.photoHistory.redo(p))
    }
}
