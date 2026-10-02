package com.pinwheel.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTimelineLayerEditTest {
    @Test fun tinyCaptionEdgesRemainValidAfterProjectShortening() {
        val edits = VideoProjectEdits(captions = listOf(VideoCaptionCue(id = "short", text = "A", startMs = 0, endMs = 30)))
        for (edge in TimelineEdge.entries) for (delta in listOf(-10000L, -20L, 0L, 20L, 10000L)) {
            val cue = edits.dragLayer("Caption", "short", edge, delta, 50).captions.single()
            assertTrue(cue.startMs >= 0)
            assertTrue(cue.endMs > cue.startMs)
        }
    }

    @Test fun repeatedLayerEditsPreserveBoundsAndAudioSourceAtAllSpeeds() {
        val random = java.util.Random(24)
        for (speed in listOf(.1f, .5f, 1f, 2f, 10f)) {
            var edits = VideoProjectEdits(audio = listOf(VideoAudioTrack(id = "a", uri = "file:///a", name = "A",
                sourceDurationMs = 30000, sourceStartMs = 1000, sourceEndMs = 15000, startMs = 2000, speed = speed, extendsVideo = true)))
            repeat(500) {
                edits = edits.dragLayer("Audio", "a", TimelineEdge.entries[random.nextInt(3)], random.nextInt(8001).toLong() - 4000, 180000)
                val audio = edits.audio.single()
                assertTrue(audio.startMs >= 0)
                assertTrue(audio.sourceStartMs >= 0)
                assertTrue(audio.sourceEndMs <= audio.sourceDurationMs)
                assertTrue(audio.sourceEndMs > audio.sourceStartMs)
                assertTrue(audio.durationMs > 0)
            }
        }
    }
    @Test fun textMoveAndEdgeTrimStayInsideComposition() {
        val text = VideoTextOverlay(id = "title", startMs = 1_000, endMs = 3_000)
        val edits = VideoProjectEdits(texts = listOf(text))
        val moved = edits.dragLayer("Text", "title", TimelineEdge.MOVE, 9_000, 10_000)
        assertEquals(8_000L, moved.texts.single().startMs)
        assertEquals(10_000L, moved.texts.single().endMs)
        val trimmed = moved.dragLayer("Text", "title", TimelineEdge.START, 9_000, 10_000)
        assertEquals(9_900L, trimmed.texts.single().startMs)
        assertEquals(10_000L, trimmed.texts.single().endMs)
    }

    @Test fun audioEdgesChangeSourceRangeAtPlaybackSpeed() {
        val track = VideoAudioTrack(id = "music", uri = "file:///music.mp3", name = "Music",
            sourceDurationMs = 12_000, sourceStartMs = 1_000, sourceEndMs = 9_000,
            startMs = 2_000, speed = 2f)
        val edits = VideoProjectEdits(audio = listOf(track))
        val startTrimmed = edits.dragLayer("Audio", "music", TimelineEdge.START, 500, 10_000).audio.single()
        assertEquals(2_000L, startTrimmed.sourceStartMs)
        assertEquals(2_500L, startTrimmed.startMs)
        assertEquals(3_500L, startTrimmed.durationMs)
        val endTrimmed = edits.dragLayer("Audio", "music", TimelineEdge.END, -500, 10_000).audio.single()
        assertEquals(8_000L, endTrimmed.sourceEndMs)
        assertEquals(3_500L, endTrimmed.durationMs)
    }

    @Test fun captionsCannotBeDraggedOntoAnotherCaption() {
        val edits = VideoProjectEdits(captions = listOf(
            VideoCaptionCue(id = "one", text = "One", startMs = 0, endMs = 1_000),
            VideoCaptionCue(id = "two", text = "Two", startMs = 2_000, endMs = 3_000)))
        val invalid = edits.dragLayer("Caption", "two", TimelineEdge.MOVE, -1_500, 5_000)
        assertEquals(edits.captions, invalid.captions)
        val moved = edits.dragLayer("Caption", "two", TimelineEdge.MOVE, -1_000, 5_000)
        assertEquals(1_000L, moved.captions.last().startMs)
        assertTrue(moved.captions.last().endMs <= 2_000L)
    }
}
