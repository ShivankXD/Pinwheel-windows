package com.pinwheel.core.model

import org.junit.Assert.*
import org.junit.Test

class VideoTimelineTest {
    private val project = StudioProject(name = "Timeline", kind = ProjectKind.VIDEO, clips = listOf(
        Clip(uri = "a", name = "A", sourceDurationMs = 5000, startMs = 2000, endMs = 3000),
        Clip(uri = "b", name = "B", sourceDurationMs = 8000, startMs = 1000, endMs = 4000),
        Clip(uri = "c", name = "C", sourceDurationMs = 2000),
    ))

    @Test fun playheadMappingUsesTrimmedDurationsAndChoosesNextClipAtBoundary() {
        assertEquals(0 to 0L, globalToClip(project, -1))
        assertEquals(0 to 999L, globalToClip(project, 999))
        assertEquals(1 to 0L, globalToClip(project, 1000))
        assertEquals(1 to 2500L, globalToClip(project, 3500))
        assertEquals(2 to 0L, globalToClip(project, 4000))
        assertEquals(2 to 2000L, globalToClip(project, Long.MAX_VALUE))
        for (time in listOf(0L, 200L, 1000L, 1500L, 4000L, 6000L)) {
            val (index, local) = globalToClip(project, time)
            assertEquals(time, clipToGlobal(project, index, local))
        }
    }

    @Test fun emptyAndZeroLengthTimelinesAreSafe() {
        val empty = project.copy(clips = emptyList())
        assertEquals(0 to 0L, globalToClip(empty, 2000)); assertEquals(0L, clipToGlobal(empty, 50, -30))
        val zero = project.copy(clips = listOf(Clip(uri = "a", name = "A", sourceDurationMs = 0), project.clips[0]))
        assertEquals(1 to 0L, globalToClip(zero, 0))
    }

    @Test fun shorteningPreservesEarlierLayersAndClipsTailWithoutRelocatingOutsideLayers() {
        val first = VideoTextOverlay(startMs = 0, endMs = 1000)
        val tail = VideoTextOverlay(startMs = 1500, endMs = 5000)
        val outside = VideoTextOverlay(startMs = 3000, endMs = 5000)
        val edits = VideoProjectEdits(texts = listOf(first, tail, outside),
            images = listOf(VideoImageOverlay(uri = "file:///logo.png", name = "Logo", startMs = 2500, endMs = 4000)))
        val clamped = edits.clampedToDuration(3000)
        assertEquals(listOf(first, tail.copy(endMs = 3000)), clamped.texts)
        assertEquals(3000, clamped.images.single().endMs)
        assertTrue(edits.clampedToDuration(0).texts.isEmpty()); assertTrue(edits.clampedToDuration(-50).images.isEmpty())
        assertEquals(clamped, clamped.clampedToDuration(3000))
    }
}
