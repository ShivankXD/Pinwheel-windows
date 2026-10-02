package com.pinwheel.core.model

import org.junit.Assert.*
import org.junit.Test

class VideoCaptionsTest {
    @Test fun sanitizerBoundsNumbersWithoutBreakingSurrogatePair() {
        val cue = VideoCaptionCue(text = "a".repeat(499) + "😀end", startMs = Long.MAX_VALUE, endMs = Long.MIN_VALUE).sanitized()
        assertEquals(499, cue.text.length); assertEquals(MAX_VIDEO_CAPTION_TIME_MS, cue.endMs)
        assertEquals(cue.startMs + 1, cue.endMs)
        val style = VideoCaptionStyle(preset = "bad", size = Float.NaN, y = Float.POSITIVE_INFINITY,
            fontFamily = "bad", alignment = "bad", color = -1L).sanitized()
        assertEquals(VideoCaptionStyle(), style)
    }

    @Test fun newCaptionFindsActualFreeGapRatherThanOverwritingExistingCue() {
        val cues = listOf(VideoCaptionCue(startMs = 1000, endMs = 3000), VideoCaptionCue(startMs = 3050, endMs = 4000))
        val before = newCaptionAt(cues, 8000, 400)!!
        assertEquals(400L, before.startMs); assertEquals(1000L, before.endMs)
        val after = newCaptionAt(cues, 8000, 1500)!!
        assertEquals(4000L, after.startMs); assertEquals(7000L, after.endMs)
        assertNull(newCaptionAt(cues, 4000, 1500))
        assertNull(newCaptionAt(emptyList(), 99, 0))
        assertNull(newCaptionAt(List(MAX_VIDEO_CAPTIONS) { VideoCaptionCue() }, 60000, 0))
    }

    @Test fun overlapsUseExclusiveEndAndIgnoreTheEditedCueId() {
        val first = VideoCaptionCue(id = "first", startMs = 1000, endMs = 2000)
        assertFalse(captionOverlaps(listOf(first), first.copy(endMs = 2500)))
        assertFalse(captionOverlaps(listOf(first), VideoCaptionCue(startMs = 2000, endMs = 3000)))
        assertTrue(captionOverlaps(listOf(first), VideoCaptionCue(startMs = 1999, endMs = 3000)))
    }

    @Test fun splitUsesTextCaretAndPlayheadWithoutSplittingEmoji() {
        val cue = VideoCaptionCue(text = "Hello 😀 world", startMs = 1000, endMs = 5000)
        val (left, right) = splitCaption(cue, 2500, 7)!! // Inside the emoji's UTF-16 pair.
        assertEquals("Hello", left.text); assertEquals("😀 world", right.text)
        assertEquals(cue.id, left.id); assertNotEquals(cue.id, right.id)
        assertEquals(2500L, left.endMs); assertEquals(2500L, right.startMs)
        assertEquals(1000L, left.startMs); assertEquals(5000L, right.endMs)
        assertNull(splitCaption(cue, 1050, 6)); assertNull(splitCaption(cue, 2500, 0))
        assertNull(splitCaption(cue, 2500, cue.text.length))
    }

    @Test fun shortenProjectClampsCaptionTailAndPreservesStyle() {
        val cue = VideoCaptionCue(text = "Caption", startMs = 1000, endMs = 3000)
        val style = VideoCaptionStyle(preset = "Highlight", alignment = "Left")
        val edits = VideoProjectEdits(captions = listOf(cue, cue.copy(id = "later", startMs = 3000, endMs = 5000)), captionStyle = style)
        val clamped = edits.clampedToDuration(2000)
        assertEquals(listOf(cue.copy(endMs = 2000)), clamped.captions); assertEquals(style, clamped.captionStyle)
        assertTrue(edits.clampedToDuration(0).captions.isEmpty())
    }
}
