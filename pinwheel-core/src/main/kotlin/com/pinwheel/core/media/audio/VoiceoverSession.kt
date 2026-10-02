package com.pinwheel.core.media.audio

import java.io.File
import java.util.UUID

const val MAX_VOICEOVER_DURATION_MS = 20L * 60 * 1000
const val MIN_VOICEOVER_DURATION_MS = 250L

data class VoiceoverTake(val file: File, val durationMs: Long)
enum class VoiceoverPhase { READY, RECORDING, PAUSED, REVIEW, ERROR, CLOSED }
data class VoiceoverState(val phase: VoiceoverPhase = VoiceoverPhase.READY, val elapsedMs: Long = 0,
    val amplitude: Float = 0f, val take: VoiceoverTake? = null, val error: String? = null)

/** Backend is replaceable so lifetime, pause timing and cleanup can be tested without a microphone. */
interface VoiceoverBackend {
    fun start(file: File, maximumMs: Long, onLimit: () -> Unit, onError: () -> Unit)
    fun pause()
    fun resume()
    fun stop()
    fun amplitude(): Int
    fun release()
}

/** Only direct files created inside our private take directory may be adopted or removed. */
object VoiceoverFiles {
    private val leased = mutableSetOf<String>()
    fun directory(filesDir: File): File = File(filesDir, "voiceover-takes").apply { mkdirs() }
    fun isOwned(filesDir: File, file: File): Boolean = runCatching {
        val folder = File(filesDir, "voiceover-takes")
        folder.canonicalFile.parentFile == filesDir.canonicalFile &&
            file.canonicalFile.parentFile == folder.canonicalFile && file.name.matches(Regex("take-[a-zA-Z0-9-]+\\.m4a"))
    }.getOrDefault(false)
    @Synchronized fun create(filesDir: File): File {
        val folder = directory(filesDir)
        check(folder.canonicalFile.parentFile == filesDir.canonicalFile) { "Recording storage is unavailable." }
        return File(folder, "take-${UUID.randomUUID()}.m4a").also { leased += it.canonicalPath }
    }
    @Synchronized fun release(file: File) { leased -= file.canonicalPath }
    @Synchronized fun cleanAbandoned(filesDir: File) {
        File(filesDir, "voiceover-takes").listFiles().orEmpty().forEach { file ->
            if (file.isFile && isOwned(filesDir, file) && file.canonicalPath !in leased) file.delete()
        }
    }
}

/** Main-thread session. Backgrounding stops into review; closing or rerecording deletes an unsaved take. */
class VoiceoverSession(private val filesDir: File, requestedMaximumMs: Long,
    private val backendFactory: () -> VoiceoverBackend, private val durationOf: (File) -> Long,
    private val now: () -> Long) : AutoCloseable {
    val maximumMs = requestedMaximumMs.coerceIn(0, MAX_VOICEOVER_DURATION_MS)
    var state = VoiceoverState(); private set
    private var backend: VoiceoverBackend? = null
    private var file: File? = null
    private var segmentStarted = 0L
    private var accumulated = 0L
    private var generation = 0L

    init { VoiceoverFiles.cleanAbandoned(filesDir) }

    fun start(): Boolean {
        if (state.phase == VoiceoverPhase.CLOSED || state.phase == VoiceoverPhase.RECORDING || state.phase == VoiceoverPhase.PAUSED) return false
        discardTake()
        if (maximumMs < MIN_VOICEOVER_DURATION_MS) { state = VoiceoverState(VoiceoverPhase.ERROR, error = "Move the playhead earlier to leave room for a recording."); return false }
        return try {
            val target = VoiceoverFiles.create(filesDir); file = target
            val recorder = backendFactory(); backend = recorder
            val token = ++generation
            accumulated = 0; segmentStarted = now()
            state = VoiceoverState(VoiceoverPhase.RECORDING)
            recorder.start(target, maximumMs,
                { if (token == generation) stop() },
                { if (token == generation && backend != null) fail("The microphone stopped unexpectedly. Try recording again.") })
            segmentStarted = now()
            true
        } catch (e: Exception) { fail(e.message ?: "The microphone could not be started."); false }
    }

    fun tick(): VoiceoverState {
        if (state.phase == VoiceoverPhase.RECORDING) {
            val elapsed = (accumulated + (now() - segmentStarted).coerceAtLeast(0)).coerceAtMost(maximumMs)
            if (elapsed >= maximumMs) stop()
            else state = state.copy(elapsedMs = elapsed, amplitude = (runCatching { backend?.amplitude() ?: 0 }.getOrDefault(0) / 32767f).coerceIn(0f, 1f))
        }
        return state
    }

    fun pause() {
        if (state.phase != VoiceoverPhase.RECORDING) return
        try {
            backend?.pause()
            accumulated = (accumulated + (now() - segmentStarted).coerceAtLeast(0)).coerceAtMost(maximumMs)
            state = state.copy(phase = VoiceoverPhase.PAUSED, elapsedMs = accumulated, amplitude = 0f)
        } catch (e: Exception) { fail("The recording could not pause. Please try again.") }
    }

    fun resume() {
        if (state.phase != VoiceoverPhase.PAUSED) return
        try { backend?.resume(); segmentStarted = now(); state = state.copy(phase = VoiceoverPhase.RECORDING) }
        catch (e: Exception) { fail("The recording could not resume. Please try again.") }
    }

    fun stop(): VoiceoverTake? {
        if (state.phase != VoiceoverPhase.RECORDING && state.phase != VoiceoverPhase.PAUSED) return state.take
        val target = file
        val recorder = backend
        backend = null
        generation++
        // A platform duration limit may already have stopped the recorder. Validate the completed file either way.
        try { runCatching { recorder?.stop() } } finally { runCatching { recorder?.release() } }
        val actualDuration = target?.takeIf { it.isFile && it.length() > 0 }?.let { runCatching { durationOf(it) }.getOrDefault(0L) } ?: 0L
        if (target == null || actualDuration < MIN_VOICEOVER_DURATION_MS) {
            fail("That take was too short or could not be recorded. Record for at least a moment and try again.")
            return null
        }
        val take = VoiceoverTake(target, actualDuration.coerceAtMost(maximumMs))
        state = VoiceoverState(VoiceoverPhase.REVIEW, elapsedMs = take.durationMs, take = take)
        return take
    }

    fun backgrounded() { if (state.phase == VoiceoverPhase.RECORDING || state.phase == VoiceoverPhase.PAUSED) stop() }
    fun discard() { if (state.phase != VoiceoverPhase.CLOSED) { releaseRecorder(); discardTake(); state = VoiceoverState() } }
    fun markAdopted() {
        // Caller has atomically moved the file into the project library; do not retain ownership.
        file?.let(VoiceoverFiles::release); file = null
        state = VoiceoverState(VoiceoverPhase.CLOSED)
    }
    private fun fail(message: String) { releaseRecorder(); discardTake(); state = VoiceoverState(VoiceoverPhase.ERROR, error = message) }
    private fun releaseRecorder() { generation++; val active = backend; backend = null; runCatching { active?.release() } }
    private fun discardTake() { file?.let { if (VoiceoverFiles.isOwned(filesDir, it)) it.delete(); VoiceoverFiles.release(it) }; file = null }
    override fun close() { releaseRecorder(); discardTake(); state = VoiceoverState(VoiceoverPhase.CLOSED) }
}
