package com.pinwheel.core.data

import com.pinwheel.core.model.*
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.*

class StorageContractTest {
    @TempDir lateinit var directory: Path
    private fun movie(uri: String) = StudioProject(id = "fixture", name = "Synthetic fixture", kind = ProjectKind.VIDEO,
        modifiedAt = 123, clips = listOf(Clip(id = "clip", uri = uri, name = "sample", sourceDurationMs = 5000)))
    @Test fun acceptsVersionsOneThroughTenAndRejectsFutureSchema() {
        for (version in 1..10) {
            val json = JSONObject(ProjectCodec.encode(movie("file:///a.mp4"))).put("version", version)
            val decoded = ProjectCodec.decode(json.toString())
            assertEquals(decoded, ProjectCodec.decode(ProjectCodec.encode(decoded)))
        }
        assertFails { ProjectCodec.decode(JSONObject(ProjectCodec.encode(movie("file:///a.mp4"))).put("version", 11).toString()) }
    }
    @Test fun legacyMissingKeysUseMobileDefaultsAndRetiredEffectsDrop() {
        val json = JSONObject(ProjectCodec.encode(movie("file:///a.mp4")))
        json.put("adjustments", JSONObject()); json.remove("video"); json.remove("videoHistory")
        val p = ProjectCodec.decode(json.toString())
        assertEquals(Adjustments(), p.adjustments); assertEquals(VideoProjectEdits(), p.video)
        val retired = com.pinwheel.core.media.video.VideoFxCatalog.retired.first()
        val v = VideoEditCodec.decodeProject(VideoEditCodec.encodeProject(VideoProjectEdits()).put("effects",
            org.json.JSONArray().put(JSONObject().put("id", "old").put("kind", retired))))
        assertTrue(v.effects.isEmpty())
    }
    @Test fun allRecipeFieldsAndBothHistoriesRoundTrip() {
        val background = "file:///background.png"
        val a = Adjustments(exposure = .4f, masks = listOf(PhotoMask()), frame = PhotoFrame("4:5", "Blur", .1f), cutout = PhotoCutout(ops = listOf(CutoutOp(listOf(BrushPoint(.5f, .5f)))), imageUri = background), raw = RawDevelopment(.7f, .2f))
        val original = movie("file:///a.mp4").copy(adjustments = a, video = VideoProjectEdits(
            effects = listOf(VideoTimedEffect(kind = "fx-shake", params = mapOf("speed" to .25f))),
            images = listOf(VideoImageOverlay(uri = "file:///pip.mp4", name = "PIP", video = VideoOverlaySource(5000))),
            texts = listOf(VideoTextOverlay(text = "Title")), captions = listOf(VideoCaptionCue(text = "Caption")),
            audio = listOf(VideoAudioTrack(uri = "file:///a.wav", name = "Sound", sourceDurationMs = 5000))))
        val video = original.copy(videoHistory = VideoHistory(past = listOf(VideoEdit.of(original))))
        assertEquals(video, ProjectCodec.decode(ProjectCodec.encode(video)))
        val photo = original.copy(kind = ProjectKind.PHOTO, video = VideoProjectEdits(), photoHistory = PhotoHistory(past = listOf(PhotoEdit.of(original))))
        assertEquals(photo, ProjectCodec.decode(ProjectCodec.encode(photo)))
    }
    @Test fun failedAtomicSaveRetainsPriorFileAndBackupRecoveryMatchesMobile() {
        val path = directory.resolve("project.json").toFile(); path.writeText("old")
        val atomic = AtomicFile(path)
        val stream = atomic.startWrite(); stream.write("partial".toByteArray()); atomic.failWrite(stream)
        assertEquals("old", atomic.openRead().bufferedReader().use { it.readText() })
        directory.resolve("project.json.bak").toFile().writeText("recovered")
        assertEquals("recovered", atomic.openRead().bufferedReader().use { it.readText() })
        val good = atomic.startWrite(); good.write("new".toByteArray()); atomic.finishWrite(good)
        assertEquals("new", path.readText()); assertFalse(directory.resolve("project.json.new").toFile().exists())
    }
    @Test fun packageRemapsCurrentAndHistoryMediaAndPhotoBackgrounds() {
        val a = Files.write(directory.resolve("video & space.mp4"), byteArrayOf(1, 2, 3))
        val b = Files.write(directory.resolve("background.png"), byteArrayOf(4, 5))
        val historyOnly = Files.write(directory.resolve("history.wav"), byteArrayOf(6))
        val previous = movie(a.toUri().toString()).copy(adjustments = Adjustments(cutout = PhotoCutout(ops = listOf(CutoutOp(listOf(BrushPoint(.5f, .5f)))), imageUri = b.toUri().toString())),
            video = VideoProjectEdits(audio = listOf(VideoAudioTrack(uri = historyOnly.toUri().toString(), name = "Old sound", sourceDurationMs = 1000))))
        val p = previous.copy(video = VideoProjectEdits(), videoHistory = VideoHistory(past = listOf(VideoEdit.of(previous))))
        val archive = directory.resolve("fixture.pinwheel"); ProjectPackage.export(p, archive)
        val files = directory.resolve("import"); val store = ProjectStore(files.toFile())
        val loaded = ProjectPackage.import(archive, files, store)
        assertEquals(loaded, store.load(loaded.id)); assertEquals(p.durationMs, loaded.durationMs)
        assertContentEquals(byteArrayOf(1, 2, 3), Files.readAllBytes(Path.of(java.net.URI(loaded.clips.single().uri))))
        assertTrue(loaded.adjustments.cutout.imageUri!!.startsWith(files.toUri().toString()))
        assertTrue(loaded.videoHistory.past.single().video.audio.single().uri.startsWith(files.toUri().toString()))
        assertFails { ProjectPackage.import(archive, files, store) }
        assertFails { ProjectPackage.export(p, archive) }
    }
    private fun zip(name: String, entries: Map<String, ByteArray>): Path {
        val path = directory.resolve(name)
        ZipOutputStream(Files.newOutputStream(path)).use { z -> entries.forEach { (key, bytes) -> z.putNextEntry(ZipEntry(key)); z.write(bytes); z.closeEntry() } }
        return path
    }
    @Test fun zipTraversalAbsoluteUrisAndOversizedMediaFailWithoutOrphans() {
        val files = directory.resolve("import"); val store = ProjectStore(files.toFile())
        val badPath = zip("traversal.pinwheel", mapOf("../outside" to byteArrayOf(1), "project.json" to ProjectCodec.encode(movie("media/a")).toByteArray()))
        assertFails { ProjectPackage.import(badPath, files, store) }; assertFalse(directory.resolve("outside").toFile().exists())
        val absolute = zip("absolute.pinwheel", mapOf("project.json" to ProjectCodec.encode(movie("file:///outside.mp4")).toByteArray()))
        assertFails { ProjectPackage.import(absolute, files, store) }
        val huge = zip("huge.pinwheel", mapOf("project.json" to ProjectCodec.encode(movie("media/a")).toByteArray(), "media/a" to ByteArray(100)))
        assertFails { ProjectPackage.import(huge, files, store, ProjectPackage.Limits(mediaBytes = 10)) }
        assertTrue(store.list().isEmpty()); assertEquals(0, files.resolve("originals").toFile().listFiles().orEmpty().size)
    }
    @Test fun deletionPreservesSharedAndUndoReferencedOriginals() {
        val originals = Files.createDirectories(directory.resolve("originals"))
        val source = Files.write(originals.resolve("shared.mp4"), byteArrayOf(1))
        val store = ProjectStore(directory.toFile()); val a = movie(source.toUri().toString()); val b = a.copy(id = "other")
        store.save(a); store.save(b); store.delete(a.id); assertTrue(Files.exists(source))
        store.delete(b.id); assertFalse(Files.exists(source))
    }
}
