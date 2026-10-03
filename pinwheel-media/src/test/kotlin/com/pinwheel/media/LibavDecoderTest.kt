package com.pinwheel.media

import java.nio.file.Path
import java.time.Duration
import kotlin.test.*

class LibavDecoderTest {
    private val factory = LibavDecoderFactory(Path.of("native/windows-x64"))
    private val source = Path.of("assets/demo.mp4")
    @Test fun persistentContextsDecodeHardwareVideoAndStereoAudio() {
        factory.open(source, DecodeConfig()).use { decoder ->
            val a = assertNotNull(decoder.pollAudio(Duration.ofSeconds(5)))
            val v = assertNotNull(decoder.pollVideo(Duration.ofSeconds(5)))
            assertEquals(960, v.pixels.width); assertEquals(720, v.pixels.height)
            assertEquals(48000, a.sampleRate); assertEquals(2, a.channels)
            assertTrue(a.interleavedPcm.all { it.isFinite() })
            assertEquals(2, decoder.statistics.contextsOpened)
            println("PASS in-process libav: ${decoder.description.backend}, ${decoder.description.backendNote}; video ${v.pixels.width}x${v.pixels.height}, 48 kHz stereo")
        }
    }
    @Test fun decodesKnownPcmToneWithoutAProcess() {
        val file = java.nio.file.Files.createTempFile(Path.of("pinwheel-media/build"), "tone-", ".wav")
        try {
            val samples = 12000
            val buffer = java.nio.ByteBuffer.allocate(44 + samples * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            buffer.put("RIFF".toByteArray()).putInt(36 + samples * 4).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(2)
                .putInt(48000).putInt(192000).putShort(4).putShort(16).put("data".toByteArray()).putInt(samples * 4)
            repeat(samples) { i -> val sample = (kotlin.math.sin(i * 2 * Math.PI * 440 / 48000) * 12000).toInt().toShort(); buffer.putShort(sample).putShort(sample) }
            java.nio.file.Files.write(file, buffer.array())
            factory.open(file, DecodeConfig(decodeVideo = false)).use { decoder ->
                val audio = assertNotNull(decoder.pollAudio(Duration.ofSeconds(5)))
                assertTrue(audio.interleavedPcm.max() > .36f); assertTrue(audio.interleavedPcm.min() < -.36f)
                for (i in audio.interleavedPcm.indices step 2) {
                    assertEquals(audio.interleavedPcm[i], audio.interleavedPcm[i + 1])
                    assertEquals(kotlin.math.sin(i / 2 * 2 * Math.PI * 440 / 48000).toFloat() * (12000f / 32768), audio.interleavedPcm[i], .00004f)
                }
            }
        } finally { java.nio.file.Files.deleteIfExists(file) }
    }
    @Test fun fullVideoQueueCannotBlockAudioAndBothRemainBounded() {
        factory.open(source, DecodeConfig(videoQueueFrames = 1, audioQueueBlocks = 2, maxQueuedBytes = 8L * 1024 * 1024)).use { decoder ->
            repeat(20) { assertNotNull(decoder.pollAudio(Duration.ofSeconds(5))) }
            val stats = decoder.statistics
            assertTrue(stats.videoQueued <= 1); assertTrue(stats.audioQueued <= 2); assertTrue(stats.peakQueuedBytes <= decoder.config.maxQueuedBytes)
            assertTrue(stats.audioDecoded >= 20)
        }
    }
    @Test fun rapidSeeksRejectStaleFramesAndLandBeyondPreviousKeyframe() {
        factory.open(source, DecodeConfig()).use { decoder ->
            repeat(25) { decoder.seek((it % 7) * 110_000L) }
            val generation = decoder.seek(1_230_000)
            val v = assertNotNull(decoder.pollVideo(Duration.ofSeconds(5)))
            val a = assertNotNull(decoder.pollAudio(Duration.ofSeconds(5)))
            assertEquals(generation, v.generation); assertEquals(generation, a.generation)
            assertTrue(v.presentationTimeUs in 1_230_000..1_280_000, "${v.presentationTimeUs}")
            assertTrue(a.presentationTimeUs in 1_230_000..1_255_000, "${a.presentationTimeUs}")
            assertEquals(2, decoder.statistics.contextsOpened); assertEquals(26, decoder.statistics.seeks)
        }
    }
    @Test fun softwareBackendAndDecodeScaleAreExplicit() {
        factory.open(source, DecodeConfig(backend = DecodeBackend.LIBAV_SOFTWARE, maxVideoEdge = 320, decodeAudio = false)).use { decoder ->
            assertEquals(DecodeBackend.LIBAV_SOFTWARE, decoder.description.backend)
            val frame = assertNotNull(decoder.pollVideo(Duration.ofSeconds(5)))
            assertEquals(320, frame.pixels.width); assertEquals(240, frame.pixels.height)
            assertTrue(decoder.audioEnded); assertEquals(1, decoder.statistics.contextsOpened)
        }
    }
    @Test fun stillReusesDecodedPixelsAfterTrimAndSeek() {
        factory.open(Path.of("assets/demo.jpg"), DecodeConfig(decodeAudio = false)).use { decoder ->
            assertTrue(decoder.description.still)
            val a = assertNotNull(decoder.pollVideo(Duration.ofSeconds(5)))
            val g = decoder.seek(1_629_000)
            val b = assertNotNull(decoder.pollVideo(Duration.ofSeconds(5)))
            assertEquals(g, b.generation); assertEquals(0, b.presentationTimeUs)
            assertSame(a.pixels.pixels, b.pixels.pixels)
        }
    }
    @Test fun cancellationWhileQueuesAreFullReleasesWorkers() {
        repeat(3) {
            val decoder = factory.open(source, DecodeConfig(videoQueueFrames = 1, audioQueueBlocks = 1))
            assertNotNull(decoder.pollVideo(Duration.ofSeconds(5))); assertNotNull(decoder.pollAudio(Duration.ofSeconds(5)))
            decoder.close(); decoder.close()
            assertFailsWith<IllegalStateException> { decoder.pollVideo(Duration.ZERO) }
        }
        assertTrue(Thread.getAllStackTraces().keys.none { it.isAlive && it.name.startsWith("pinwheel-libav-") })
    }
}
