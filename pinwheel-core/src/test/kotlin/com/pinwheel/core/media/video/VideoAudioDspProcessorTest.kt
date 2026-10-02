package com.pinwheel.core.media.video

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoAudioDspProcessorTest {
    private fun process(samples: ShortArray, voice: String = "None", noise: Boolean = false,
        volume: Float = 1f): ShortArray {
        val processor = VideoAudioDspProcessor(voice, noise, volume)
        processor.configure(PcmFormat(48_000, 1, PcmEncoding.PCM16))
        processor.flush()
        val input = ByteBuffer.allocateDirect(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { input.putShort(it) }
        input.flip()
        processor.queueInput(input)
        val output = processor.getOutput().order(ByteOrder.LITTLE_ENDIAN)
        return ShortArray(output.remaining() / 2) { output.getShort() }
    }

    @Test fun highGainIsLimitedWithoutPcmWraparound() {
        val samples = ShortArray(1_000) { 12_000 }
        val processed = process(samples, volume = 8f)
        assertTrue(processed.all { it >= 0 })
        assertTrue(processed.any { it > 12_000 })
    }

    @Test fun noiseReductionAttenuatesLowLevelStationarySignal() {
        val samples = ShortArray(4_000) { 100 }
        val processed = process(samples, noise = true)
        assertTrue(kotlin.math.abs(processed.last().toInt()) < 30)
    }

    @Test fun robotPresetChangesActualSamples() {
        val samples = ShortArray(48_000) { 5_000 }
        val processed = process(samples, voice = "Robot")
        assertTrue(processed.any { it < 2_000 })
        assertTrue(processed.any { it > 4_000 })
    }
}
