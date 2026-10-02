package com.pinwheel.core.model

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class VideoSpeedAudioTest {
    private fun clip(speed: Float) = Clip(uri = "file:///movie.mp4", name = "Movie", sourceDurationMs = 20000,
        startMs = 2000, endMs = 12000, video = VideoClipEdits(speed = speed, transition = "Dip black", transitionDurationMs = 800))

    @Test fun speedControlsPlaybackDurationWithoutChangingSourceBounds() {
        assertEquals(20000L, clip(.5f).durationMs)
        assertEquals(5000L, clip(2f).durationMs)
        assertEquals(10000L, clip(Float.NaN).durationMs)
        assertEquals(40000L, clip(0f).durationMs)
        assertEquals(2500L, clip(Float.MAX_VALUE).durationMs)
        assertEquals(10000L, clip(2f).sourceSpanMs)
        assertEquals(2000L, clip(2f).startMs)
        val huge = Clip(uri = "file:///huge", name = "Huge", sourceDurationMs = Long.MAX_VALUE,
            video = VideoClipEdits(speed = .25f))
        assertEquals(Long.MAX_VALUE, huge.durationMs)
        val project = StudioProject(name = "Huge", kind = ProjectKind.VIDEO, clips = listOf(huge, huge))
        assertEquals(Long.MAX_VALUE, project.durationMs)
        assertEquals(Long.MAX_VALUE, clipToGlobal(project, 1, Long.MAX_VALUE))
    }

    @Test fun splittingPlaybackTimeMapsToSourceAndLeavesOutgoingTransitionOnlyOnRight() {
        val original = clip(2f)
        val (left, right) = original.split(1500)!!
        assertEquals(5000L, left.endMs); assertEquals(left.endMs, right.startMs)
        assertEquals(1500L, left.durationMs); assertEquals(3500L, right.durationMs)
        assertEquals(original.startMs, left.startMs); assertEquals(original.endMs, right.endMs)
        assertEquals("Cut", left.video.transition); assertEquals("Dip black", right.video.transition)
        assertEquals(800L, right.video.transitionDurationMs)
        assertEquals(2f, left.video.speed); assertNotEquals(left.id, right.id)
        assertNull(original.split(99)); assertNull(original.split(original.durationMs - 99))
    }

    @Test fun fractionalSpeedSplitPreservesExactSourceAndAtMostOneMillisecondOfRounding() {
        val original = clip(1.3f)
        for (position in listOf(101L, 957L, 3001L, 6000L)) {
            val (left, right) = original.split(position)!!
            assertEquals(original.sourceSpanMs, left.sourceSpanMs + right.sourceSpanMs)
            assertEquals(left.endMs, right.startMs)
            assertTrue(abs(original.durationMs - left.durationMs - right.durationMs) <= 1)
        }
        val project = StudioProject(name = "Speed", kind = ProjectKind.VIDEO, clips = listOf(clip(.5f), clip(2f)))
        assertEquals(1 to 2000L, globalToClip(project, 22000))
        assertEquals(22000L, clipToGlobal(project, 1, 2000))
    }

    @Test fun malformedAudioMetadataCannotCreateNegativeRangesOrInfiniteGain() {
        val track = VideoAudioTrack(uri = "file:///music.m4a", name = "Music", sourceDurationMs = 10000,
            sourceStartMs = Long.MAX_VALUE, sourceEndMs = -1, startMs = -1,
            volume = Float.NaN, fadeInMs = Long.MAX_VALUE, fadeOutMs = -40).sanitized()
        assertEquals(9999L, track.sourceStartMs); assertEquals(10000L, track.sourceEndMs)
        assertEquals(1L, track.durationMs); assertEquals(0L, track.startMs)
        assertEquals(.7f, track.volume); assertEquals(1L, track.fadeInMs); assertEquals(0L, track.fadeOutMs)
        assertEquals(track, track.sanitized())
        val zero = track.copy(sourceDurationMs = -20).sanitized()
        assertEquals(0L, zero.durationMs)
        assertTrue(VideoProjectEdits(audio = listOf(zero)).sanitized().audio.isEmpty())
    }

    @Test fun audioOutsideShortenedTimelineKeepsItsSourceTrim() {
        val track = VideoAudioTrack(uri = "file:///music.m4a", name = "Music", sourceDurationMs = 20000,
            sourceStartMs = 5000, sourceEndMs = 15000, startMs = 3000, fadeInMs = 7000, fadeOutMs = 7000)
        val edits = VideoProjectEdits(audio = listOf(track))
        val clamped = edits.clampedToDuration(6000).audio.single()
        assertEquals(5000L, clamped.sourceStartMs); assertEquals(15000L, clamped.sourceEndMs)
        assertEquals(3000L, clamped.startMs); assertEquals(10000L, clamped.durationMs)
        assertEquals(7000L, clamped.fadeInMs); assertEquals(7000L, clamped.fadeOutMs)
        assertTrue(edits.clampedToDuration(3000).audio.isEmpty())
        assertTrue(edits.clampedToDuration(-1).audio.isEmpty())
    }

    @Test fun transitionAndAudioCountBoundsAreExplicit() {
        val edit = VideoClipEdits(speed = Float.POSITIVE_INFINITY, transition = "Unknown", transitionDurationMs = -20).sanitized()
        assertEquals(1f, edit.speed); assertEquals("Cut", edit.transition); assertEquals(100L, edit.transitionDurationMs)
        assertEquals(3000L, edit.copy(transitionDurationMs = Long.MAX_VALUE).sanitized().transitionDurationMs)
        val audio = List(20) { VideoAudioTrack(id = "track$it", uri = "file:///music$it.wav", name = "Music", sourceDurationMs = 1000, volume = 5f) }
        val safe = VideoProjectEdits(audio = audio).sanitized()
        assertEquals(MAX_VIDEO_AUDIO_TRACKS, safe.audio.size); assertTrue(safe.audio.all { it.volume == 5f })
        assertEquals(safe, safe.sanitized())
    }
}
