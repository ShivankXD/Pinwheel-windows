package com.pinwheel.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class VideoAudioTrackTest {
    @Test fun speedChangesPlaybackDurationWithoutChangingSourceTrim() {
        val track = VideoAudioTrack(uri = "file:///test.ogg", name = "Test", sourceDurationMs = 60_000,
            sourceStartMs = 10_000, sourceEndMs = 40_000, speed = 2f)
        assertEquals(30_000L, track.sourceSpanMs)
        assertEquals(15_000L, track.durationMs)
        assertEquals(30_000L, track.sanitized().sourceSpanMs)
    }

    @Test fun sanitizesExtremeAudioControlsAndFadeInPlaybackTime() {
        val track = VideoAudioTrack(uri = "file:///test.ogg", name = "Test", sourceDurationMs = 10_000,
            speed = 20f, volume = 99f, fadeInMs = 50_000, fadeOutMs = 50_000,
            voiceEffect = "not-a-preset").sanitized()
        assertEquals(10f, track.speed)
        assertEquals(8f, track.volume)
        assertEquals(1_000L, track.durationMs)
        assertEquals(1_000L, track.fadeInMs)
        assertEquals(1_000L, track.fadeOutMs)
        assertEquals("None", track.voiceEffect)
    }

    @Test fun shorteningMovieKeepsExtractedSourceBounds() {
        val track = VideoAudioTrack(uri = "file:///test.ogg", name = "Extracted", sourceDurationMs = 61_000,
            sourceEndMs = 61_000, extendsVideo = true)
        val shortened = VideoProjectEdits(audio = listOf(track)).clampedToDuration(5_000)
        assertEquals(61_000L, shortened.audio.single().sourceEndMs)
        assertEquals(61_000L, shortened.audio.single().durationMs)
        assertFalse(shortened.audio.isEmpty())
    }
}
