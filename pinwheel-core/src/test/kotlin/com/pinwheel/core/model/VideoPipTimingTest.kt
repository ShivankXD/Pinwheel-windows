package com.pinwheel.core.model

import org.junit.Assert.*
import org.junit.Test

class VideoPipTimingTest {
    private val layer = VideoImageOverlay(uri = "video.mp4", name = "PiP", startMs = 1000, endMs = 3000,
        video = VideoOverlaySource(5000, startMs = 500))
    private fun drag(edge: TimelineEdge, delta: Long) = VideoProjectEdits(images = listOf(layer))
        .dragLayer("Image", layer.id, edge, delta, 6000).images.single()
    @Test fun trimmingStartChangesSourceInPointAndKeepsEnd() {
        val moved = drag(TimelineEdge.START, 400)
        assertEquals(1400L, moved.startMs); assertEquals(900L, moved.video!!.startMs); assertEquals(3000L, moved.endMs)
        val extended = drag(TimelineEdge.START, -900)
        assertEquals(500L, extended.startMs); assertEquals(0L, extended.video!!.startMs)
    }
    @Test fun trimEndCannotReadPastSourceAndMoveDoesNotSlipSource() {
        assertEquals(5500L, drag(TimelineEdge.END, 10000).endMs)
        val moved = drag(TimelineEdge.MOVE, 9000)
        assertEquals(4000L, moved.startMs); assertEquals(6000L, moved.endMs); assertEquals(500L, moved.video!!.startMs)
    }
    @Test fun sanitationAndProjectShorteningRetainPlayableSource() {
        assertEquals(5500L, layer.copy(endMs = Long.MAX_VALUE).sanitized().endMs)
        val trimmed = VideoProjectEdits(images = listOf(layer)).clampedToDuration(2000).images.single()
        assertEquals(2000L, trimmed.endMs); assertEquals(500L, trimmed.video!!.startMs)
        assertTrue(VideoProjectEdits(images = listOf(layer)).clampedToDuration(900).images.isEmpty())
    }
}
