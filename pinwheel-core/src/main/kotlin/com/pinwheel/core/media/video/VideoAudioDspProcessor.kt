package com.pinwheel.core.media.video

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

/** Pitch shift preserves duration; the remaining presets use actual PCM filtering/modulation. */
/**
 * Streaming PCM effects shared by CompositionPlayer and Transformer. This handles both PCM16 and
 * float, keeps filter/delay state between input buffers, and resets it on seeks/flushes.
 */
class VideoAudioDspProcessor(
    private val voiceEffect: String,
    private val reduceNoise: Boolean,
    private val volume: Float,
) {
    private lateinit var inputAudioFormat: PcmFormat
    private var outputBuffer = ByteBuffer.allocate(0)
    private fun replaceOutputBuffer(size: Int): ByteBuffer = ByteBuffer.allocateDirect(size).also { outputBuffer = it }
    fun getOutput(): ByteBuffer = outputBuffer.also { outputBuffer = ByteBuffer.allocate(0) }

    private data class ChannelState(var previousInput: Float = 0f, var highPass: Float = 0f,
        var lowPass: Float = 0f, var secondLowPass: Float = 0f, var envelope: Float = 0f,
        var gate: Float = 1f)

    private var channels = emptyArray<ChannelState>()
    private var echoes = emptyArray<FloatArray>()
    private var echoIndex = 0
    private var phase = 0.0
    private var randomState = 0x2173b85
    private var frameIndex = 0L

    fun configure(inputAudioFormat: PcmFormat): PcmFormat {
        if (inputAudioFormat.encoding != PcmEncoding.PCM16 && inputAudioFormat.encoding != PcmEncoding.FLOAT32)
            error("Voice effects require PCM16 or float PCM")
        require(inputAudioFormat.sampleRate > 0 && inputAudioFormat.channelCount in 1..8)
        this.inputAudioFormat = inputAudioFormat
        return inputAudioFormat
    }

    fun flush() {
        channels = Array(inputAudioFormat.channelCount) { ChannelState() }
        val delay = (inputAudioFormat.sampleRate * .22f).toInt().coerceAtLeast(1)
        echoes = if (voiceEffect == "Echo") Array(inputAudioFormat.channelCount) { FloatArray(delay) } else emptyArray()
        echoIndex = 0; phase = 0.0; randomState = 0x2173b85; frameIndex = 0L
    }

    fun reset() {
        channels = emptyArray(); echoes = emptyArray(); echoIndex = 0; frameIndex = 0L
    }

    fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val input = inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val output = replaceOutputBuffer(input.remaining()).order(ByteOrder.LITTLE_ENDIAN)
        val floatPcm = inputAudioFormat.encoding == PcmEncoding.FLOAT32
        val rate = inputAudioFormat.sampleRate.toFloat()
        val hp90 = exp((-2.0 * PI * 90.0 / rate).toFloat())
        val lp700 = 1f - exp((-2.0 * PI * 700.0 / rate).toFloat())
        val lp2000 = 1f - exp((-2.0 * PI * 2000.0 / rate).toFloat())
        val lp3600 = 1f - exp((-2.0 * PI * 3600.0 / rate).toFloat())
        val highPassEnabled = reduceNoise || voiceEffect in listOf("Radio", "Telephone", "Megaphone", "Whisper")
        while (input.hasRemaining()) {
            for (channel in channels.indices) {
                val state = channels[channel]
                var sample = if (floatPcm) input.getFloat() else input.getShort() / 32768f
                // High-pass cuts rumble; the smoothed gate attenuates steady low-level noise.
                if (highPassEnabled) {
                    state.highPass = hp90 * (state.highPass + sample - state.previousInput)
                    state.previousInput = sample
                    sample = state.highPass
                }
                if (reduceNoise) {
                    state.envelope = maxOf(abs(sample), state.envelope * .9985f)
                    val wanted = ((state.envelope - .009f) / .027f).coerceIn(.12f, 1f)
                    state.gate += (wanted - state.gate) * if (wanted > state.gate) .08f else .003f
                    sample *= state.gate
                }
                sample = when (voiceEffect) {
                    "Deep" -> lowPass(state, sample, lp2000)
                    "Chipmunk" -> sample * 1.08f
                    "Robot" -> sample * (if (sin(phase * 34.0) > 0) 1f else .18f)
                    "Radio" -> softDrive(lowPass(state, sample, lp3600) * 1.8f)
                    "Telephone" -> softDrive(lowPass(state, sample, lp2000) * 1.5f)
                    "Megaphone" -> softDrive(lowPass(state, sample, lp3600) * 2.4f)
                    "Alien" -> sample * (.58f + .42f * sin(phase * 23.0).toFloat())
                    "Whisper" -> {
                        state.envelope = maxOf(abs(sample), state.envelope * .991f)
                        randomState = randomState xor (randomState shl 13)
                        randomState = randomState xor (randomState ushr 17)
                        randomState = randomState xor (randomState shl 5)
                        val noise = (randomState / Int.MAX_VALUE.toFloat()).coerceIn(-1f, 1f)
                        .55f * sample + .8f * state.envelope * noise
                    }
                    "Echo" -> {
                        val delay = echoes[channel][echoIndex]
                        echoes[channel][echoIndex] = (sample + delay * .23f).coerceIn(-1f, 1f)
                        sample + .45f * delay
                    }
                    "Underwater" -> {
                        state.secondLowPass += lp700 * (lowPass(state, sample, lp700) - state.secondLowPass)
                        state.secondLowPass *
                        (.83f + .17f * sin(phase * 2.1).toFloat())
                    }
                    else -> sample
                }
                sample = limit(sample * volume)
                if (floatPcm) output.putFloat(sample) else output.putShort((sample * 32767f).toInt().coerceIn(-32768, 32767).toShort())
            }
            frameIndex++
            phase = frameIndex * (2.0 * PI / rate)
            if (echoes.isNotEmpty()) echoIndex = (echoIndex + 1) % echoes[0].size
        }
        output.flip()
    }

    private fun lowPass(state: ChannelState, value: Float, alpha: Float): Float {
        state.lowPass += alpha * (value - state.lowPass)
        return state.lowPass
    }

    private fun softDrive(value: Float): Float = tanh(value.toDouble()).toFloat()

    /** Leaves ordinary level unchanged, smoothly limits peaks, and never wraps a PCM sample. */
    private fun limit(value: Float): Float {
        val magnitude = abs(value)
        return if (magnitude <= .9f) value else {
            val limited = .9f + .1f * tanh(((magnitude - .9f) / .1f).toDouble()).toFloat()
            if (value < 0f) -limited else limited
        }
    }
}

enum class PcmEncoding { PCM16, FLOAT32 }
data class PcmFormat(val sampleRate: Int, val channelCount: Int, val encoding: PcmEncoding)
