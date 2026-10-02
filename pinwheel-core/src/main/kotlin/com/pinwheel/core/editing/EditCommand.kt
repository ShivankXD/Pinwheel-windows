package com.pinwheel.core.editing

import com.pinwheel.core.model.*

/** Inputs are resolved media descriptors. Import/probe work belongs to the media client. */
sealed interface EditCommand { val label: String }
data class RenameProject(val name: String) : EditCommand { override val label = "Rename project" }
data class SetAdjustments(val value: Adjustments) : EditCommand { override val label = "Adjust photo" }
data class AddClips(val clips: List<Clip>, val at: Int? = null) : EditCommand { override val label = "Add clips" }
data class ReplaceClip(val clipId: String, val replacement: Clip) : EditCommand { override val label = "Replace clip" }
data class SplitClip(val clipId: String, val atMs: Long) : EditCommand { override val label = "Split clip" }
data class TrimClip(val clipId: String, val startMs: Long, val endMs: Long) : EditCommand { override val label = "Trim clip" }
data class DeleteClip(val clipId: String) : EditCommand { override val label = "Delete clip" }
data class DuplicateClip(val clipId: String) : EditCommand { override val label = "Duplicate clip" }
data class MoveClip(val clipId: String, val to: Int) : EditCommand { override val label = "Move clip" }
data class MuteClip(val clipId: String) : EditCommand { override val label = "Mute clip" }
data object MuteAllClips : EditCommand { override val label = "Mute all clips" }
data class SetClipEdits(val clipId: String, val value: VideoClipEdits) : EditCommand { override val label = "Adjust clip" }
data class SetTransition(val clipId: String, val transitionId: String, val durationMs: Long, val speed: Float = .5f) : EditCommand { override val label = "Set transition" }
data class ApplyTransitionToAll(val value: VideoClipEdits) : EditCommand { override val label = "Apply transitions" }
data class ApplyGradeToAll(val value: VideoClipEdits) : EditCommand { override val label = "Apply grade" }
data class SetCanvas(val value: VideoProjectEdits) : EditCommand { override val label = "Edit canvas" }
data class AddEffect(val kind: String, val atMs: Long, val lengthMs: Long? = null,
    val params: Map<String, Float>? = null, val target: String = "") : EditCommand { override val label = "Add effect" }
data class PickEffect(val effectId: String?, val kind: String?, val atMs: Long, val target: String = "") : EditCommand { override val label = "Pick effect" }
data class SetEffectParams(val effectId: String, val params: Map<String, Float>) : EditCommand { override val label = "Adjust effect" }
data class SetEffect(val value: VideoTimedEffect) : EditCommand { override val label = "Edit effect" }
data class DuplicateEffect(val effectId: String) : EditCommand { override val label = "Duplicate effect" }
data class AddText(val value: VideoTextOverlay) : EditCommand { override val label = "Add title" }
data class SetText(val value: VideoTextOverlay) : EditCommand { override val label = "Edit title" }
data class AddImage(val value: VideoImageOverlay) : EditCommand { override val label = "Add overlay" }
data class SetImage(val value: VideoImageOverlay) : EditCommand { override val label = "Edit overlay" }
data class PickLibraryOverlay(val pendingId: String?, val itemId: String, val atMs: Long) : EditCommand { override val label = "Pick overlay" }
data class AddAudio(val value: VideoAudioTrack) : EditCommand { override val label = "Add audio" }
data class SetAudio(val value: VideoAudioTrack) : EditCommand { override val label = "Edit audio" }
data class SplitAudio(val trackId: String, val atMs: Long) : EditCommand { override val label = "Split audio" }
data class ExtractClipAudio(val clipId: String, val hasAudio: Boolean) : EditCommand { override val label = "Extract audio" }
data class SaveCaption(val value: VideoCaptionCue) : EditCommand { override val label = "Edit caption" }
data class SplitCaption(val captionId: String, val atMs: Long, val textOffset: Int) : EditCommand { override val label = "Split caption" }
data class ShiftCaptions(val offsetMs: Long) : EditCommand { override val label = "Shift captions" }
data class SetCaptions(val cues: List<VideoCaptionCue>, val style: VideoCaptionStyle) : EditCommand { override val label = "Import captions" }
enum class LayerKind(val mobileName: String) { EFFECT("Effect"), TEXT("Text"), IMAGE("Image"), AUDIO("Audio"), CAPTION("Caption") }
data class DeleteLayer(val kind: LayerKind, val id: String) : EditCommand { override val label = "Delete layer" }
data class DuplicateLayer(val kind: LayerKind, val id: String) : EditCommand { override val label = "Duplicate layer" }
data class DragLayer(val kind: LayerKind, val id: String, val edge: TimelineEdge, val deltaMs: Long) : EditCommand { override val label = "Move layer" }
data class SetLane(val kind: LayerKind, val id: String, val lane: Int) : EditCommand { override val label = "Move lane" }
data object Undo : EditCommand { override val label = "Undo" }
data object Redo : EditCommand { override val label = "Redo" }
