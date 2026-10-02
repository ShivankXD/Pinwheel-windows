package com.pinwheel.media

import com.pinwheel.core.RgbaFrame
import java.nio.file.Path
import java.time.Duration

enum class DecodeBackend { LIBAV_D3D11VA, LIBAV_SOFTWARE }
data class DecodeConfig(val backend: DecodeBackend = DecodeBackend.LIBAV_D3D11VA,
    val videoQueueFrames: Int = 6, val audioQueueBlocks: Int = 12,
    val maxQueuedBytes: Long = 64L * 1024 * 1024) {
    init { require(videoQueueFrames in 1..32 && audioQueueBlocks in 1..64 && maxQueuedBytes in 1024..(512L * 1024 * 1024)) }
}
data class MediaDescription(val durationUs: Long, val width: Int, val height: Int,
    val audioSampleRate: Int?, val audioChannels: Int?, val still: Boolean)
data class DecodedVideo(val presentationTimeUs: Long, val generation: Long, val pixels: RgbaFrame)
data class DecodedAudio(val presentationTimeUs: Long, val generation: Long,
    val sampleRate: Int, val channels: Int, val interleavedPcm: FloatArray)

/** In-process factories only. P0 FfmpegDecoder deliberately does not implement this interface. */
interface MediaDecoderFactory { fun open(source: Path, config: DecodeConfig): MediaDecoder }
interface MediaDecoder : AutoCloseable {
    val description: MediaDescription
    val config: DecodeConfig
    /** Flush bounded queues, increment generation, seek to previous keyframe and decode to target.
     * Returned frames must be tagged with this generation. No stale delivery after a seek.
     * Stills decode once and reuse pixels independently of source trim/split time.
     */
    fun seek(targetUs: Long): Long
    /** Bounded wait. Audio consumers never wait on a video queue. Null means no frame available. */
    fun pollVideo(timeout: Duration): DecodedVideo?
    fun pollAudio(timeout: Duration): DecodedAudio?
    val videoEnded: Boolean
    val audioEnded: Boolean
    /** Cancels workers, releases queued buffers and persistent demux/codec/hardware contexts. */
    override fun close()
}
