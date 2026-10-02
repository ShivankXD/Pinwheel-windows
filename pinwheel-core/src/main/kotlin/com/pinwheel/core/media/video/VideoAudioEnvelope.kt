package com.pinwheel.core.media.video


/** Fades refer to the trimmed sound, not the original file or the project's leading silence. */
class VideoAudioEnvelope(durationMs: Long, fadeInMs: Long, fadeOutMs: Long) {
    private val durationUs = durationMs.coerceAtLeast(1) * 1000
    private val fadeInUs = fadeInMs.coerceIn(0, durationMs.coerceAtLeast(1)) * 1000
    private val fadeOutUs = fadeOutMs.coerceIn(0, durationMs.coerceAtLeast(1)) * 1000

    fun getGainFactorAtSamplePosition(samplePosition: Long, sampleRate: Int): Float {
        val timeUs = samplePosition * 1_000_000.0 / sampleRate
        if (timeUs >= durationUs) return 0f
        val incoming = if (fadeInUs == 0L) 1.0 else (timeUs / fadeInUs).coerceIn(0.0, 1.0)
        val outgoing = if (fadeOutUs == 0L) 1.0 else ((durationUs - timeUs) / fadeOutUs).coerceIn(0.0, 1.0)
        return minOf(incoming, outgoing).toFloat()
    }

    fun isUnityUntil(samplePosition: Long, sampleRate: Int): Long {
        // GainProcessor treats TIME_UNSET as a permanently inactive processor. During a fade
        // return the current sample, so it evaluates the changing gain instead of bypassing us.
        if (getGainFactorAtSamplePosition(samplePosition, sampleRate) != 1f) return samplePosition
        // Round up: the first attenuated sample lies at/after the continuous envelope edge.
        val edgeUs = durationUs - fadeOutUs
        return ((edgeUs * sampleRate + 999_999) / 1_000_000).coerceAtLeast(samplePosition + 1)
    }
}
