package com.pinwheel.core.model

import org.junit.Assert.*
import org.junit.Test

class VideoHistoryTest {
    private fun project()=StudioProject(name="Original",kind=ProjectKind.VIDEO,clips=listOf(Clip(uri="file:///a.mp4",name="A",sourceDurationMs=10000)))
    @Test fun undoRedoRestoresClipsCaptionsAudioAndBranchDiscardsRedo() {
        val original=project()
        val edited=original.copy(name="Edited",clips=original.clips.map{it.copy(video=VideoClipEdits(speed=2f))},
            video=VideoProjectEdits(captions=listOf(VideoCaptionCue(text="Speech")),audio=listOf(VideoAudioTrack(uri="file:///voice.m4a",name="Voice",sourceDurationMs=3000))),
            videoHistory=original.videoHistory.record(original))
        val undone=edited.videoHistory.undo(edited)!!
        assertEquals(original.clips,undone.clips);assertTrue(undone.video.audio.isEmpty());assertEquals("Original",undone.name)
        val redone=undone.videoHistory.redo(undone)!!
        assertEquals(edited.clips,redone.clips);assertEquals(edited.video,redone.video)
        val branch=undone.copy(name="Branch",videoHistory=undone.videoHistory.record(undone))
        assertTrue(branch.videoHistory.future.isEmpty());assertNull(branch.videoHistory.redo(branch))
    }
    @Test fun fortyStepLimitRemainsBoundedThroughBothDirections() {
        var value=project()
        repeat(65){index->value=value.copy(name="Edit$index",videoHistory=value.videoHistory.record(value))}
        assertEquals(40,value.videoHistory.past.size)
        repeat(40){value=value.videoHistory.undo(value)!!;assertEquals(40,value.videoHistory.past.size+value.videoHistory.future.size)}
        assertNull(value.videoHistory.undo(value));assertEquals("Edit24",value.name)
        repeat(40){value=value.videoHistory.redo(value)!!}
        assertEquals("Edit64",value.name)
    }
}
