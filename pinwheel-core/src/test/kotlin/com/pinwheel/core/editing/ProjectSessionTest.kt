package com.pinwheel.core.editing

import com.pinwheel.core.model.*
import com.pinwheel.core.data.*
import com.pinwheel.core.plans.*
import com.pinwheel.core.media.video.VideoFxCatalog
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.*

class ProjectSessionTest {
    @TempDir lateinit var directory: Path
    private val plus = object : Entitlement { override val tier = PlanTier.PLUS }
    private fun movie() = StudioProject(id = "test", name = "Test", kind = ProjectKind.VIDEO, modifiedAt = 1,
        clips = listOf(Clip(id = "one", uri = "file:///one.mp4", name = "one.mp4", sourceDurationMs = 8000),
            Clip(id = "two", uri = "file:///two.mp4", name = "two.mp4", sourceDurationMs = 4000)))
    @Test fun gestureAutosavesOnceAndHistorySurvivesReload() {
        val store = ProjectStore(directory.toFile()); var saves = 0
        val session = ProjectSession(movie(), { saves++; store.save(it) }, plus) { 123 }
        repeat(30) { session.updateDraft(SetClipEdits("one", VideoClipEdits(exposure = it / 30f))) }
        assertEquals(0, saves); assertFalse(session.canUndo)
        session.commitDraft(); assertEquals(1, saves); assertEquals(1, session.project.videoHistory.past.size)
        val loaded = ProjectSession(store.load("test"), store::save, plus)
        loaded.apply(Undo); assertEquals(0f, loaded.project.clips.first().video.exposure)
        loaded.apply(Redo); assertEquals(29 / 30f, loaded.project.clips.first().video.exposure)
    }
    @Test fun failedSaveDoesNotPublishStateOrLoseDraft() {
        var fail = true; val session = ProjectSession(movie(), { if (fail) error("Disk full") }, plus)
        assertFailsWith<IllegalStateException> { session.apply(RenameProject("changed")) }
        assertEquals("Test", session.project.name); assertFalse(session.canUndo)
        session.updateDraft(SetClipEdits("one", VideoClipEdits(speed = 2f)))
        assertFailsWith<IllegalStateException> { session.commitDraft() }
        assertEquals(2f, session.project.clips.first().video.speed); assertFalse(session.canUndo)
        fail = false; session.commitDraft(); assertTrue(session.canUndo)
    }
    @Test fun cancelledGestureAndNoOpLeaveHistoryAndAutosaveAlone() {
        var saves = 0; val session = ProjectSession(movie(), { saves++ }, plus)
        session.updateDraft(SetClipEdits("one", VideoClipEdits(speed = 4f))); session.cancelDraft()
        session.apply(SetClipEdits("one", VideoClipEdits()))
        assertEquals(0, saves); assertFalse(session.canUndo); assertEquals(1f, session.project.clips.first().playbackSpeed)
    }
    @Test fun splitKeepsSpeedAndTrimAndUndoRestoresOriginal() {
        val p = movie().copy(clips = listOf(movie().clips.first().copy(startMs = 2000, endMs = 6000, video = VideoClipEdits(speed = 2f, transition = "Dip black"))))
        val session = ProjectSession(p, {}, plus)
        session.apply(SplitClip("one", 1000)); val halves = session.project.clips
        assertEquals(4000, halves[0].endMs); assertEquals(4000, halves[1].startMs)
        assertEquals("Cut", halves[0].video.transition); assertEquals(2000, session.project.durationMs)
        session.apply(Undo); assertEquals(p.clips, session.project.clips)
        assertFailsWith<IllegalArgumentException> { session.apply(DeleteClip("one")) }
    }
    @Test fun effectPickerSwapsSelectedLayerAndUsesMobileWindows() {
        val session = ProjectSession(movie(), {}, plus)
        val a = VideoFxCatalog.effects.first(); val b = VideoFxCatalog.effects.first { it.transitionLike }
        session.apply(PickEffect(null, a.id, 900)); val id = session.project.video.effects.single().id
        session.apply(PickEffect(id, b.id, 3000))
        val swapped = session.project.video.effects.single()
        assertEquals(id, swapped.id); assertEquals(900, swapped.startMs); assertEquals(3900, swapped.endMs); assertEquals(b.defaults(), swapped.params)
        session.apply(PickEffect(null, b.id, 11000)); assertEquals(12000, session.project.video.effects.last().endMs)
        session.apply(PickEffect(id, null, 0)); assertEquals(1, session.project.video.effects.size)
    }
    @Test fun freeGateAlsoCatchesCanvasAndDraftBypasses() {
        val session = ProjectSession(movie(), {})
        val effect = VideoTimedEffect(kind = VideoFxCatalog.effects.first().id)
        assertFailsWith<IllegalStateException> { session.apply(AddEffect(effect.kind, 0)) }
        assertFailsWith<IllegalStateException> { session.apply(SetCanvas(VideoProjectEdits(effects = listOf(effect)))) }
        assertFailsWith<IllegalStateException> { session.updateDraft(SetCanvas(VideoProjectEdits(effects = listOf(effect)))) }
        assertFalse(session.canUndo)
    }
    @Test fun layerLimitsRejectBeforeSanitizationCanDropUserData() {
        val session = ProjectSession(movie(), {}, plus)
        repeat(40) { session.apply(AddEffect(VideoFxCatalog.effects.first().id, 0)) }
        val before = session.project
        assertFailsWith<IllegalArgumentException> { session.apply(AddEffect(VideoFxCatalog.effects.first().id, 0)) }
        assertEquals(before, session.project)
        repeat(16) { session.apply(AddAudio(VideoAudioTrack(uri = "file:///a.wav", name = "a", sourceDurationMs = 2000))) }
        assertFailsWith<IllegalArgumentException> { session.apply(AddAudio(VideoAudioTrack(uri = "file:///b.wav", name = "b", sourceDurationMs = 2000))) }
    }
    @Test fun audioExtractionUsesClipPositionTrimSpeedAndMutesSource() {
        val p = movie().copy(clips = movie().clips.map { if (it.id == "two") it.copy(startMs = 1000, endMs = 4000, video = VideoClipEdits(speed = 2f, volume = .7f)) else it })
        val session = ProjectSession(p, {}, plus); session.apply(ExtractClipAudio("two", true))
        val track = session.project.video.audio.single()
        assertEquals(8000, track.startMs); assertEquals(1000, track.sourceStartMs); assertEquals(4000, track.sourceEndMs)
        assertEquals(2f, track.speed); assertEquals(.7f, track.volume); assertTrue(session.project.clips.last().muted)
        session.apply(Undo); assertTrue(session.project.video.audio.isEmpty()); assertFalse(session.project.clips.last().muted)
    }
    @Test fun captionsRejectOverlapAndSplitWithSingleUndoStep() {
        val session = ProjectSession(movie(), {}, plus)
        session.apply(SaveCaption(VideoCaptionCue(id = "cue", text = "Hello world", startMs = 0, endMs = 2000)))
        assertFailsWith<IllegalArgumentException> { session.apply(SaveCaption(VideoCaptionCue(text = "bad", startMs = 1000, endMs = 3000))) }
        session.apply(SplitCaption("cue", 1000, 5)); assertEquals(listOf("Hello", "world"), session.project.video.captions.map { it.text })
        session.apply(Undo); assertEquals(1, session.project.video.captions.size)
    }
    @Test fun branchingEditClearsRedoAndHistoryRemainsBounded() {
        val session = ProjectSession(movie(), {}, plus)
        repeat(50) { session.apply(RenameProject("Name $it")) }
        assertEquals(40, session.project.videoHistory.past.size)
        session.apply(Undo); assertTrue(session.canRedo)
        session.apply(RenameProject("branch")); assertFalse(session.canRedo)
    }
    @Test fun applyAllGradeDoesNotChangeGeometryTimingOrAudio() {
        val session = ProjectSession(movie(), {}, plus)
        session.apply(ApplyGradeToAll(VideoClipEdits(exposure = .5f, speed = 4f, mirrored = true, volume = 0f)))
        session.project.clips.forEach { assertEquals(.5f, it.video.exposure); assertEquals(1f, it.video.speed); assertFalse(it.video.mirrored); assertEquals(1f, it.video.volume) }
    }
    @Test fun libraryPickerSwapsPendingOverlayAndPreservesTiming() {
        val session = ProjectSession(movie(), {}, plus)
        val a = VideoFxCatalog.overlays[0]; val b = VideoFxCatalog.overlays[1]
        session.apply(PickLibraryOverlay(null, a.id, 900)); val id = session.project.video.images.single().id
        session.apply(PickLibraryOverlay(id, b.id, 5000))
        val item = session.project.video.images.single(); assertEquals(900, item.startMs); assertEquals("overlay:${b.id}", item.uri)
    }
    @Test fun mutableCallerDataCannotChangeCommittedState() {
        val clips = movie().clips.toMutableList(); val session = ProjectSession(movie().copy(clips = clips), {}, plus)
        clips.clear(); assertEquals(2, session.project.clips.size)
        (session.project.clips as? MutableList<Clip>)?.clear(); assertEquals(2, session.project.clips.size)
    }
    @Test fun photoAdjustmentsUsePhotoHistoryAndRetainCutoutRecipe() {
        val session = ProjectSession(movie().copy(kind = ProjectKind.PHOTO), {}, plus)
        session.apply(SetAdjustments(Adjustments(exposure = 1f, cutout = PhotoCutout(ops = listOf(CutoutOp(listOf(BrushPoint(.5f, .5f)))), imageUri = "file:///background.png"))))
        assertEquals(1, session.project.photoHistory.past.size); assertEquals(0, session.project.videoHistory.past.size)
        session.apply(Undo); assertEquals(0f, session.project.adjustments.exposure)
        session.apply(Redo); assertEquals("file:///background.png", session.project.adjustments.cutout.imageUri)
    }
}
