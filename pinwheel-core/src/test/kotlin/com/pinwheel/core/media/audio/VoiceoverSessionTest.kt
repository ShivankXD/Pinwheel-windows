package com.pinwheel.core.media.audio

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class VoiceoverSessionTest {
    private class Backend : VoiceoverBackend {
        var released = 0; var pauses = 0; var resumes = 0; var stops = 0
        var failStart = false; var failStop = false
        var onLimit: () -> Unit = {}; var onError: () -> Unit = {}
        override fun start(file: File, maximumMs: Long, onLimit: () -> Unit, onError: () -> Unit) {
            this.onLimit = onLimit; this.onError = onError
            file.writeBytes(byteArrayOf(1, 2, 3))
            if (failStart) error("Microphone denied")
        }
        override fun pause() { pauses++ }
        override fun resume() { resumes++ }
        override fun stop() { stops++; if (failStop) error("Already stopped") }
        override fun amplitude() = 16384
        override fun release() { released++ }
    }
    private fun isolated(block: (File) -> Unit) {
        val root = Files.createTempDirectory("voiceover-test-").toFile()
        try { block(root) } finally { root.deleteRecursively() }
    }

    @Test fun pauseTimeDoesNotConsumeRecordingBudgetAndBackgroundStopsForReview() = isolated { root ->
        var clock = 0L; val backend = Backend()
        val session = VoiceoverSession(root, 10000, { backend }, { 1200 }, { clock })
        assertTrue(session.start()); clock = 800; assertEquals(800L, session.tick().elapsedMs)
        assertTrue(session.state.amplitude in .49f.. .51f)
        session.pause(); clock = 5000; assertEquals(800L, session.tick().elapsedMs)
        session.resume(); clock = 5400; assertEquals(1200L, session.tick().elapsedMs)
        session.backgrounded()
        assertEquals(VoiceoverPhase.REVIEW, session.state.phase); assertEquals(1200L, session.state.take!!.durationMs)
        assertEquals(1, backend.pauses); assertEquals(1, backend.resumes); assertEquals(1, backend.stops); assertEquals(1, backend.released)
        val file = session.state.take!!.file; assertTrue(file.isFile)
        session.close(); assertFalse(file.exists())
    }

    @Test fun maximumDurationStopsExactlyOnceAndAcceptsAlreadyStoppedValidFile() = isolated { root ->
        var clock = 0L; val backend = Backend().apply { failStop = true }
        val session = VoiceoverSession(root, 1000, { backend }, { 1045 }, { clock })
        session.start(); clock = 1000; session.tick()
        assertEquals(VoiceoverPhase.REVIEW, session.state.phase)
        assertEquals(1000L, session.state.take!!.durationMs)
        backend.onLimit(); backend.onError(); session.stop()
        assertEquals(VoiceoverPhase.REVIEW, session.state.phase); assertEquals(1, backend.stops)
        session.close()
    }

    @Test fun failureAndTooShortTakeDeleteOwnedFilesAndReleaseRecorder() = isolated { root ->
        val failed = Backend().apply { failStart = true }
        val session = VoiceoverSession(root, 1000, { failed }, { 0 }, { 0 })
        assertFalse(session.start()); assertEquals(VoiceoverPhase.ERROR, session.state.phase)
        assertEquals(1, failed.released); assertTrue(VoiceoverFiles.directory(root).listFiles()!!.isEmpty())
        session.close()
        val backend = Backend(); val short = VoiceoverSession(root, 1000, { backend }, { 50 }, { 0 })
        assertTrue(short.start()); assertNull(short.stop())
        assertEquals(VoiceoverPhase.ERROR, short.state.phase); assertEquals(1, backend.released)
        assertTrue(VoiceoverFiles.directory(root).listFiles()!!.isEmpty()); short.close()
    }

    @Test fun staleCallbacksCannotKillNewTakeOrResurrectClosedSession() = isolated { root ->
        val backends = mutableListOf<Backend>()
        val session = VoiceoverSession(root, 1000, { Backend().also(backends::add) }, { 700 }, { 0 })
        session.start(); val old = backends.single(); session.discard(); session.start()
        old.onError(); old.onLimit(); assertEquals(VoiceoverPhase.RECORDING, session.state.phase)
        val current = backends.last(); session.close(); current.onError(); current.onLimit()
        assertEquals(VoiceoverPhase.CLOSED, session.state.phase)
        assertTrue(VoiceoverFiles.directory(root).listFiles()!!.isEmpty())
    }

    @Test fun cleanupKeepsLeasedTakesAndOnlyDeletesOwnedAbandonedNames() = isolated { root ->
        val abandoned = File(VoiceoverFiles.directory(root), "take-abandoned.m4a").apply { writeText("old") }
        val unrelated = File(VoiceoverFiles.directory(root), "other.txt").apply { writeText("keep") }
        val active = VoiceoverFiles.create(root).apply { writeText("active") }
        VoiceoverFiles.cleanAbandoned(root)
        assertFalse(abandoned.exists()); assertTrue(active.exists()); assertTrue(unrelated.exists())
        assertFalse(VoiceoverFiles.isOwned(root, File(root, "take-external.m4a")))
        VoiceoverFiles.release(active); VoiceoverFiles.cleanAbandoned(root)
        assertFalse(active.exists()); assertTrue(unrelated.exists())
    }

    @Test fun successfulAdoptionSurvivesSessionCloseAndSmallBudgetDoesNotStart() = isolated { root ->
        val backend = Backend(); val session = VoiceoverSession(root, Long.MAX_VALUE, { backend }, { 800 }, { 0 })
        assertEquals(MAX_VOICEOVER_DURATION_MS, session.maximumMs)
        session.start(); val take = session.stop()!!
        val adopted = File(root, "originals/saved.m4a").apply { parentFile!!.mkdirs() }
        assertTrue(take.file.renameTo(adopted)); session.markAdopted(); session.close()
        assertTrue(adopted.exists())
        val short = VoiceoverSession(root, 100, { error("Must not start") }, { 0 }, { 0 })
        assertFalse(short.start()); short.close()
    }
}
