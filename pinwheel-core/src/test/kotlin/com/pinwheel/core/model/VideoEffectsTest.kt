package com.pinwheel.core.model

import org.junit.Assert.*
import org.junit.Test

class VideoEffectsTest {
    @Test fun effectTimesAndInvalidValuesAreBounded() {
        val value = VideoTimedEffect(id = "", kind = "missing", startMs = Long.MAX_VALUE,
            endMs = -1, intensity = Float.NaN).sanitized()
        assertTrue(value.id.isNotBlank())
        assertEquals("Vignette", value.kind)
        assertEquals(.5f, value.intensity, 0f)
        assertEquals(value.startMs + 1, value.endMs)
        assertEquals(86400000L, value.endMs)
        assertEquals("Solid", VideoProjectEdits(backgroundMode = "invalid").sanitized().backgroundMode)
        assertEquals(.5f, VideoProjectEdits(backgroundBlur = Float.NaN).sanitized().backgroundBlur, 0f)
        assertEquals(1f, VideoProjectEdits(backgroundBlur = 20f).sanitized().backgroundBlur, 0f)
        assertEquals(0xff123456L, VideoProjectEdits(backgroundColor = 0x00123456).sanitized().backgroundColor)
    }

    @Test fun activeIntensityIsEndExclusiveAndOverlapsAreCapped() {
        val values = listOf(VideoTimedEffect(startMs = 100, endMs = 200, intensity = .6f),
            VideoTimedEffect(startMs = 150, endMs = 250, intensity = .7f),
            VideoTimedEffect(kind = "Grain", startMs = 100, endMs = 300, enabled = false))
        val reused = FloatArray(4)
        assertEquals(0f, activeVideoEffectIntensities(values, 99_999, reused)[0], 0f)
        assertEquals(.6f, activeVideoEffectIntensities(values, 100_000, reused)[0], 0f)
        assertEquals(1f, activeVideoEffectIntensities(values, 150_000, reused)[0], 0f)
        assertEquals(.7f, activeVideoEffectIntensities(values, 200_000, reused)[0], 0f)
        assertArrayEquals(FloatArray(4), activeVideoEffectIntensities(values, 250_000, reused), 0f)
        assertArrayEquals(FloatArray(4), activeVideoEffectIntensities(values, -1, reused), 0f)
    }

    @Test fun effectCountDeduplicationAndShortenedTimelineRemainSafe() {
        val effects = List(MAX_VIDEO_EFFECTS + 10) { VideoTimedEffect(id = "effect-$it", startMs = it * 100L, endMs = it * 100L + 500) }
        val capped = VideoProjectEdits(effects = effects + effects).sanitized()
        assertEquals(MAX_VIDEO_EFFECTS, capped.effects.size)
        val shortened = capped.clampedToDuration(250)
        assertEquals(3, shortened.effects.size)
        assertTrue(shortened.effects.all { it.endMs == 250L })
        assertTrue(capped.clampedToDuration(0).effects.isEmpty())
    }

    @Test fun threeDTiltUsesLocalEffectTimeAndExclusiveEnd() {
        val effect = VideoTimedEffect(kind = "3D Tilt", startMs = 1000, endMs = 2800, intensity = 1f)
        assertArrayEquals(floatArrayOf(0f, 0f), activeVideo3DTilt(listOf(effect), 999_999), 0f)
        val start = activeVideo3DTilt(listOf(effect), 1_000_000)
        assertEquals(0f, start[0], .0001f)
        assertEquals(.3f, start[1], .0001f)
        assertTrue(activeVideo3DTilt(listOf(effect), 1_450_000)[0] > .7f)
        assertArrayEquals(floatArrayOf(0f, 0f), activeVideo3DTilt(listOf(effect), 2_800_000), 0f)
        assertArrayEquals(start, activeVideo3DTilt(listOf(effect), 1_000_000), 0f)
    }

    @Test fun softGlowSumsOnlyActiveFiniteEffects() {
        val effects = listOf(
            VideoTimedEffect(kind = "Soft Glow", startMs = 500, endMs = 1500, intensity = .6f),
            VideoTimedEffect(kind = "Soft Glow", startMs = 500, endMs = 1000, intensity = .7f),
            VideoTimedEffect(kind = "Soft Glow", startMs = 500, endMs = 1500, intensity = Float.NaN),
        )
        assertEquals(0f, activeVideoSoftGlow(effects, 499_999), 0f)
        assertEquals(1f, activeVideoSoftGlow(effects, 800_000), 0f)
        assertEquals(.6f, activeVideoSoftGlow(effects, 1_000_000), 0f)
        assertEquals(0f, activeVideoSoftGlow(effects, 1_500_000), 0f)
    }
}
