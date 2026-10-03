package com.pinwheel.media

import com.pinwheel.core.RgbaFrame
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.math.ceil

internal data class NativeVideo(val timeUs: Long, val width: Int, val height: Int, val bytes: ByteArray)
internal data class NativeAudio(val timeUs: Long, val samples: FloatArray)

internal object LibavNative {
    private var loaded: Path? = null
    @Synchronized fun load(directory: Path) {
        val path = directory.toAbsolutePath().normalize()
        loaded?.let { check(it == path) { "A different libav runtime is already loaded" }; return }
        for (name in listOf("avutil-60", "swresample-6", "swscale-9", "avcodec-62", "avformat-62", "pinwheel_media")) {
            val file = path.resolve("$name.dll"); check(Files.isRegularFile(file)) { "Missing native runtime: $file" }
            System.load(file.toString())
        }
        val flags = configuration()
        check("--enable-shared" in flags && listOf("--enable-gpl", "--enable-nonfree", "--enable-libx264", "--enable-libx265").none { it in flags }) { "Playback requires the pinned shared LGPL build" }
        loaded = path
    }
    external fun configuration(): String
    external fun open(source: String, track: Int, backend: Int, maxEdge: Int, fallback: Boolean): Long
    external fun description(handle: Long): LongArray
    external fun note(handle: Long): String
    external fun seek(handle: Long, targetUs: Long)
    external fun video(handle: Long): NativeVideo?
    external fun audio(handle: Long): NativeAudio?
    external fun cancel(handle: Long)
    external fun close(handle: Long)
}

class LibavDecoderFactory(private val nativeDirectory: Path) : MediaDecoderFactory {
    override fun open(source: Path, config: DecodeConfig): MediaDecoder {
        require(Files.isRegularFile(source)) { "Missing media: $source" }
        LibavNative.load(nativeDirectory)
        return LibavDecoder(source.toAbsolutePath().normalize(), config)
    }
}

/** Independent persistent demuxers keep audio runnable while the video queue is full. */
private class LibavDecoder(source: Path, override val config: DecodeConfig) : MediaDecoder {
    private val lock = ReentrantLock()
    private val changed = lock.newCondition()
    private val videos = ArrayDeque<DecodedVideo>()
    private val audios = ArrayDeque<DecodedAudio>()
    private var videoHandle = 0L
    private var audioHandle = 0L
    private var closed = false
    private var failure: Throwable? = null
    private var generation = 0L
    private var targetUs = 0L
    private var videoDone = false
    private var audioDone = false
    private var videoBytes = 0L
    private var audioBytes = 0L
    private var peak = 0L
    private var decodedVideo = 0L
    private var decodedAudio = 0L
    private var seekCount = 0L
    private val workers = mutableListOf<Thread>()
    override val description: MediaDescription
    private val videoBudget: Long
    private val audioBudget: Long

    init {
        try {
            if (config.decodeVideo) videoHandle = LibavNative.open(source.toString(), 0, config.backend.ordinal, config.maxVideoEdge, config.allowSoftwareFallback)
            if (config.decodeAudio) audioHandle = LibavNative.open(source.toString(), 1, 1, 0, true)
            check(videoHandle != 0L || audioHandle != 0L) { "No requested playable stream: $source" }
            val v = if (videoHandle != 0L) LibavNative.description(videoHandle) else LongArray(8)
            val a = if (audioHandle != 0L) LibavNative.description(audioHandle) else LongArray(8)
            description = MediaDescription(maxOf(v[0], a[0]), v[1].toInt(), v[2].toInt(), a[3].toInt().takeIf { it > 0 },
                a[4].toInt().takeIf { it > 0 }, v[5] == 1L, if (v[6] == 1L) DecodeBackend.LIBAV_D3D11VA else DecodeBackend.LIBAV_SOFTWARE,
                if (videoHandle != 0L) LibavNative.note(videoHandle) else "Audio-only libav", v[7].toInt())
            videoBudget = if (audioHandle == 0L) config.maxQueuedBytes else config.maxQueuedBytes * 3 / 4
            audioBudget = if (videoHandle == 0L) config.maxQueuedBytes else config.maxQueuedBytes - videoBudget
            videoDone = videoHandle == 0L; audioDone = audioHandle == 0L
            if (videoHandle != 0L) workers += thread(start = false, isDaemon = true, name = "pinwheel-libav-video") { guarded { videoWorker() } }
            if (audioHandle != 0L) workers += thread(start = false, isDaemon = true, name = "pinwheel-libav-audio") { guarded { audioWorker() } }
            workers.forEach { it.start() }
        } catch (error: Throwable) { LibavNative.close(videoHandle); LibavNative.close(audioHandle); throw error }
    }
    override val statistics get() = lock.withLock { DecodeStatistics(generation, videos.size, audios.size, videoBytes + audioBytes, peak,
        decodedVideo, decodedAudio, seekCount, (if (videoHandle != 0L) 1 else 0) + (if (audioHandle != 0L) 1 else 0)) }
    override val videoEnded get() = lock.withLock { videoDone && videos.isEmpty() }
    override val audioEnded get() = lock.withLock { audioDone && audios.isEmpty() }
    override fun seek(targetUs: Long): Long = lock.withLock {
        require(targetUs >= 0); checkOpen(); this.targetUs = if (description.still) 0 else targetUs
        generation++; seekCount++; videos.clear(); audios.clear(); videoBytes = 0; audioBytes = 0
        videoDone = videoHandle == 0L; audioDone = audioHandle == 0L; changed.signalAll(); generation
    }
    override fun pollVideo(timeout: Duration): DecodedVideo? = poll(timeout, videos, { videoDone }) {
        videoBytes -= it.pixels.pixels.size
    }
    override fun pollAudio(timeout: Duration): DecodedAudio? = poll(timeout, audios, { audioDone }) {
        audioBytes -= it.interleavedPcm.size * 4L
    }
    private fun <T> poll(timeout: Duration, queue: ArrayDeque<T>, ended: () -> Boolean, consumed: (T) -> Unit): T? = lock.withLock {
        require(!timeout.isNegative); checkOpen()
        var left = timeout.coerceAtMost(Duration.ofSeconds(5)).toNanos()
        while (queue.isEmpty() && !ended() && left > 0 && !closed && failure == null) left = changed.awaitNanos(left)
        checkOpen(); queue.removeFirstOrNull()?.also { consumed(it); changed.signalAll() }
    }
    private fun checkOpen() { check(!closed) { "Decoder is closed" }; failure?.let { throw IllegalStateException("Decode worker failed", it) } }
    private fun guarded(block: () -> Unit) { try { block() } catch (error: Throwable) { lock.withLock { if (!closed) failure = error; changed.signalAll() } } }
    private fun request(previous: Long, ended: () -> Boolean): Pair<Long, Long>? = lock.withLock {
        while (!closed && generation == previous && ended()) changed.await()
        if (closed) null else generation to targetUs
    }
    private fun videoWorker() {
        var local = -1L; var cached: NativeVideo? = null
        while (true) {
            val (g, target) = request(local) { videoDone } ?: return
            if (g != local) { if (!description.still) LibavNative.seek(videoHandle, target); local = g }
            val frame = if (description.still) cached ?: LibavNative.video(videoHandle)?.also { cached = it } else LibavNative.video(videoHandle)
            if (frame == null) { lock.withLock { if (g == generation) { videoDone = true; changed.signalAll() } }; continue }
            if (!description.still && frame.timeUs < target) continue
            val size = frame.bytes.size.toLong(); check(size <= videoBudget) { "Video frame exceeds queue byte budget" }
            lock.withLock {
                while (!closed && g == generation && (videos.size >= config.videoQueueFrames || videoBytes + size > videoBudget)) changed.await()
                if (closed) return
                if (g == generation) {
                    videos += DecodedVideo(if (description.still) 0 else frame.timeUs, g, RgbaFrame(frame.width, frame.height, frame.bytes))
                    videoBytes += size; decodedVideo++; peak = maxOf(peak, videoBytes + audioBytes)
                    if (description.still) videoDone = true
                    changed.signalAll()
                }
            }
        }
    }
    private fun audioWorker() {
        var local = -1L
        while (true) {
            val (g, target) = request(local) { audioDone } ?: return
            if (g != local) { LibavNative.seek(audioHandle, target); local = g }
            val frame = LibavNative.audio(audioHandle)
            if (frame == null) { lock.withLock { if (g == generation) { audioDone = true; changed.signalAll() } }; continue }
            val drop = ceil((target - frame.timeUs).coerceAtLeast(0) * .048).toInt().coerceAtMost(frame.samples.size / 2)
            if (drop == frame.samples.size / 2) continue
            val pcm = if (drop == 0) frame.samples else frame.samples.copyOfRange(drop * 2, frame.samples.size)
            val time = frame.timeUs + drop * 1_000_000L / 48000
            val size = pcm.size * 4L; check(size <= audioBudget) { "Audio block exceeds queue byte budget" }
            lock.withLock {
                while (!closed && g == generation && (audios.size >= config.audioQueueBlocks || audioBytes + size > audioBudget)) changed.await()
                if (closed) return
                if (g == generation) {
                    audios += DecodedAudio(time, g, 48000, 2, pcm); audioBytes += size; decodedAudio++
                    peak = maxOf(peak, videoBytes + audioBytes); changed.signalAll()
                }
            }
        }
    }
    override fun close() {
        lock.withLock { if (closed) return; closed = true; changed.signalAll() }
        LibavNative.cancel(videoHandle); LibavNative.cancel(audioHandle)
        workers.forEach { it.join(5000) }
        check(workers.none { it.isAlive }) { "Decode cancellation timed out; contexts retained until workers stop" }
        LibavNative.close(videoHandle); LibavNative.close(audioHandle)
    }
}
