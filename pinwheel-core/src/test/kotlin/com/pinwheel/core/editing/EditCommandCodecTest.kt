package com.pinwheel.core.editing

import com.pinwheel.core.model.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class EditCommandCodecTest {
    @Test fun everyTypedCommandHasAnExplicitRoundTrip() {
        val clip = Clip(id = "clip", uri = "file:///a.mp4", name = "a", sourceDurationMs = 5000)
        val text = VideoTextOverlay(id = "text"); val image = VideoImageOverlay(id = "image", uri = "file:///a.png", name = "a")
        val audio = VideoAudioTrack(id = "audio", uri = "file:///a.wav", name = "a", sourceDurationMs = 5000)
        val cue = VideoCaptionCue(id = "cue", text = "Hello")
        val commands = listOf(RenameProject("Name"), SetAdjustments(Adjustments()), AddClips(listOf(clip)), AddClips(listOf(clip), 1),
            ReplaceClip("clip", clip), SplitClip("clip", 1000), TrimClip("clip", 1000, 3000), DeleteClip("clip"), DuplicateClip("clip"), MoveClip("clip", 1),
            MuteClip("clip"), MuteAllClips, SetClipEdits("clip", VideoClipEdits()), SetTransition("clip", "Cut", 500), ApplyTransitionToAll(VideoClipEdits()), ApplyGradeToAll(VideoClipEdits()),
            SetCanvas(VideoProjectEdits()), AddEffect("fx-shake", 0), AddEffect("fx-shake", 500, 1000, mapOf("speed" to .25f), "image"), PickEffect(null, "fx-shake", 0), PickEffect("fx", null, 0),
            SetEffectParams("fx", mapOf("speed" to .5f)), DuplicateEffect("fx"), AddText(text), SetText(text), AddImage(image), SetImage(image), PickLibraryOverlay(null, "fx-ov-dust", 0),
            AddAudio(audio), SetAudio(audio), SplitAudio("audio", 1000), ExtractClipAudio("clip", true), SaveCaption(cue), SplitCaption("cue", 1000, 2), ShiftCaptions(200),
            SetCaptions(listOf(cue), VideoCaptionStyle()), DeleteLayer(LayerKind.IMAGE, "image"), DragLayer(LayerKind.AUDIO, "audio", TimelineEdge.START, 100), SetLane(LayerKind.EFFECT, "fx", 3), Undo, Redo)
        for (command in commands) assertEquals(command, EditCommandCodec.decode(EditCommandCodec.encode(command)), command.label)
    }
    @Test fun unknownTypesAndVersionsAreRejected() {
        assertFails { EditCommandCodec.decode("{\"version\":1,\"type\":\"java.lang.Runtime\"}") }
        assertFails { EditCommandCodec.decode("{\"version\":2,\"type\":\"Undo\"}") }
    }
}
