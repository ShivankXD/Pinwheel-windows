package com.pinwheel.core.media.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class VideoAudioEnvelopeTest {
    @Test fun fadeInAndOutFollowTrimmedPlaybackClock() {
        val envelope = VideoAudioEnvelope(4_000, 1_000, 1_000)
        val sampleRate = 1_000
        assertEquals(0f, envelope.getGainFactorAtSamplePosition(0, sampleRate), .001f)
        assertEquals(.5f, envelope.getGainFactorAtSamplePosition(500, sampleRate), .001f)
        assertEquals(1f, envelope.getGainFactorAtSamplePosition(2_000, sampleRate), .001f)
        assertEquals(.5f, envelope.getGainFactorAtSamplePosition(3_500, sampleRate), .001f)
        assertEquals(0f, envelope.getGainFactorAtSamplePosition(4_000, sampleRate), .001f)
    }

    @Test fun gainProcessorRemainsActiveWhenFadeStartsAtSilence() {
        val envelope = VideoAudioEnvelope(4_000, 1_000, 1_000)
        assertNotEquals((Long.MIN_VALUE + 1), envelope.isUnityUntil(0, 48_000))
    }
}
