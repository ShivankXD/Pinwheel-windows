package com.pinwheel.core.editing

import com.pinwheel.core.data.ProjectCodec
import com.pinwheel.core.model.*
import com.pinwheel.core.media.video.VideoFxCatalog
import com.pinwheel.core.media.video.VideoAnimatedStickers
import com.pinwheel.core.plans.*
import java.util.UUID

fun interface ProjectAutosave { fun save(project: StudioProject) }

/** One writer, synchronous durable commits, no save or history while a gesture is drafted. */
class ProjectSession(initial: StudioProject, private val save: ProjectAutosave,
    private val entitlement: Entitlement = FreeEntitlement, private val clock: () -> Long = System::currentTimeMillis) {
    private var committed = snapshot(initial)
    private var draftCommand: EditCommand? = null
    private var draft: StudioProject? = null
    val project: StudioProject @Synchronized get() = snapshot(draft ?: committed)
    val canUndo: Boolean @Synchronized get() = if (committed.kind == ProjectKind.PHOTO) committed.photoHistory.past.isNotEmpty() else committed.videoHistory.past.isNotEmpty()
    val canRedo: Boolean @Synchronized get() = if (committed.kind == ProjectKind.PHOTO) committed.photoHistory.future.isNotEmpty() else committed.videoHistory.future.isNotEmpty()

    @Synchronized fun apply(command: EditCommand): StudioProject {
        check(draft == null) { "Finish the current gesture first" }
        return commit(command)
    }
    @Synchronized fun updateDraft(command: EditCommand): StudioProject {
        require(command is SetAdjustments || command is SetClipEdits || command is SetCanvas || command is DragLayer || command is SetEffectParams || command is SetEffect || command is SetText || command is SetImage || command is SetAudio) { "This command cannot stream a gesture" }
        val candidate = reduce(committed, command)
        validate(committed, candidate)
        draft = snapshot(candidate); draftCommand = EditCommandCodec.decode(EditCommandCodec.encode(command))
        return project
    }
    @Synchronized fun cancelDraft() { draft = null; draftCommand = null }
    @Synchronized fun commitDraft(): StudioProject {
        val command = draftCommand ?: return project
        // Save failure leaves the draft intact so the user can retry or cancel.
        val result = commit(command)
        cancelDraft()
        return result
    }
    private fun commit(command: EditCommand): StudioProject {
        var next = reduce(committed, command)
        validate(committed, next, enforcePlan = command != Undo && command != Redo)
        if (next == committed) return snapshot(committed)
        next = next.copy(modifiedAt = clock(), video = if (next.kind == ProjectKind.VIDEO) next.video.clampedToDuration(next.durationMs) else next.video)
        if (command != Undo && command != Redo) next = if (next.kind == ProjectKind.PHOTO)
            next.copy(photoHistory = committed.photoHistory.record(committed)) else next.copy(videoHistory = committed.videoHistory.record(committed))
        next = snapshot(next)
        save.save(snapshot(next))
        committed = next
        return snapshot(committed)
    }
    private fun snapshot(p: StudioProject) = ProjectCodec.decode(ProjectCodec.encode(p))
    private fun validate(old: StudioProject, next: StudioProject, enforcePlan: Boolean = true) {
        require(next.clips.isNotEmpty()) { "Keep at least one clip" }
        require(next.clips.map { it.id }.distinct().size == next.clips.size) { "Duplicate clip identifier" }
        require(next.clips.all { it.uri.isNotBlank() && it.sourceDurationMs > 0 && it.startMs >= 0 && it.endMs in (it.startMs + 1)..it.sourceDurationMs }) { "Invalid clip source or trim" }
        val v = next.video
        require(v.images.all { it.uri.isNotBlank() } && v.audio.all { it.uri.isNotBlank() && it.sourceDurationMs > 0 && it.durationMs > 0 }) { "Invalid layer media" }
        require(v.effects.size <= MAX_VIDEO_EFFECTS && v.images.size <= MAX_VIDEO_OVERLAYS && v.texts.size <= MAX_VIDEO_OVERLAYS && v.audio.size <= MAX_VIDEO_AUDIO_TRACKS && v.captions.size <= MAX_VIDEO_CAPTIONS) { "Layer limit reached" }
        for (ids in listOf(v.effects.map { it.id }, v.images.map { it.id }, v.texts.map { it.id }, v.audio.map { it.id }, v.captions.map { it.id })) require(ids.distinct().size == ids.size) { "Duplicate layer identifier" }
        require(v.captions.all { it.text.isNotBlank() && it.startMs >= 0 && it.endMs > it.startMs && it.endMs <= next.durationMs && !captionOverlaps(v.captions, it) }) { "Invalid or overlapping captions" }
        require(v.effects.all { it.target.isEmpty() || v.images.any { image -> image.id == it.target } || old.video.effects.any { previous -> previous.id == it.id && previous.target == it.target } }) { "Effect target does not exist" }
        if (enforcePlan && entitlement.tier != PlanTier.PLUS) {
            val existing = old.video.effects.associateBy { it.id }
            check(v.effects.all { existing[it.id] == it }) { "Effects require Plus" }
        }
    }
    private fun reduce(p: StudioProject, c: EditCommand): StudioProject {
        if (c == Undo) return (if (p.kind == ProjectKind.PHOTO) p.photoHistory.undo(p) else p.videoHistory.undo(p)) ?: p
        if (c == Redo) return (if (p.kind == ProjectKind.PHOTO) p.photoHistory.redo(p) else p.videoHistory.redo(p)) ?: p
        if (c is RenameProject) return p.copy(name = c.name.trim().take(80).also { require(it.isNotBlank()) { "Enter a project name" } })
        if (c is SetAdjustments) return p.copy(adjustments = c.value)
        require(p.kind == ProjectKind.VIDEO) { "This command requires a video project" }
        fun clip(id: String) = p.clips.firstOrNull { it.id == id } ?: error("Clip not found")
        fun changeClip(id: String, f: (Clip) -> Clip): StudioProject { clip(id); return p.copy(clips = p.clips.map { if (it.id == id) f(it) else it }) }
        fun effect(id: String) = p.video.effects.firstOrNull { it.id == id } ?: error("Effect not found")
        fun layerStart(at: Long) = at.coerceIn(0, (p.durationMs - 100).coerceAtLeast(0))
        fun caption(cue: VideoCaptionCue): VideoCaptionCue {
            require(cue.startMs >= 0 && cue.endMs <= p.durationMs && cue.endMs > cue.startMs)
            return cue.sanitized().let { it.copy(text = it.text.trim()) }
        }
        val v = p.video
        return when (c) {
            is AddClips -> { require(c.clips.isNotEmpty()); val index = c.at ?: p.clips.size; require(index in 0..p.clips.size); p.copy(clips = p.clips.toMutableList().apply { addAll(index, c.clips) }) }
            is ReplaceClip -> changeClip(c.clipId) { c.replacement.copy(id = it.id) }
            is SplitClip -> { val pair = clip(c.clipId).split(c.atMs) ?: error("Split at least 0.1 seconds from either edge"); p.copy(clips = p.clips.flatMap { if (it.id == c.clipId) listOf(pair.first, pair.second) else listOf(it) }) }
            is TrimClip -> changeClip(c.clipId) { it.trim(c.startMs, c.endMs) }
            is DeleteClip -> { clip(c.clipId); require(p.clips.size > 1) { "Keep at least one clip" }; p.copy(clips = p.clips.filterNot { it.id == c.clipId }) }
            is DuplicateClip -> { val original = clip(c.clipId); p.copy(clips = p.clips.toMutableList().apply { add(indexOf(original) + 1, original.copy(id = UUID.randomUUID().toString())) }) }
            is MoveClip -> { val original = clip(c.clipId); require(c.to in p.clips.indices); p.copy(clips = p.clips.toMutableList().apply { remove(original); add(c.to, original) }) }
            is MuteClip -> changeClip(c.clipId) { it.copy(muted = !it.muted) }
            MuteAllClips -> p.copy(clips = p.clips.map { it.copy(muted = p.clips.any { item -> !item.muted }) })
            is SetClipEdits -> changeClip(c.clipId) { it.copy(video = c.value.sanitized()) }
            is SetTransition -> changeClip(c.clipId) { require(c.transitionId in VIDEO_TRANSITIONS || VideoFxCatalog.transitions.any { spec -> spec.id == c.transitionId }); it.copy(video = it.video.copy(transition = c.transitionId, transitionDurationMs = c.durationMs, transitionSpeed = c.speed).sanitized()) }
            is ApplyTransitionToAll -> { val g = c.value.sanitized(); p.copy(clips = p.clips.mapIndexed { i, item -> if (i == p.clips.lastIndex) item else item.copy(video = item.video.copy(transition = g.transition, transitionDurationMs = g.transitionDurationMs, transitionSpeed = g.transitionSpeed)) }) }
            is ApplyGradeToAll -> { val g = c.value.sanitized(); p.copy(clips = p.clips.map { it.copy(video = it.video.copy(lookId = g.lookId, lookIntensity = g.lookIntensity, exposure = g.exposure, contrast = g.contrast, saturation = g.saturation, warmth = g.warmth, brightness = g.brightness, highlights = g.highlights, shadows = g.shadows, sharpen = g.sharpen, fade = g.fade, vignette = g.vignette, grain = g.grain, tint = g.tint, vibrance = g.vibrance)) }) }
            is SetCanvas -> { validate(p, p.copy(video = c.value)); p.copy(video = c.value.sanitized()) }
            is AddEffect -> {
                val spec = VideoFxCatalog.find(c.kind)
                require(spec != null || c.kind in VIDEO_EFFECT_KINDS) { "Unknown effect" }
                require(c.kind !in VideoFxCatalog.retired)
                val start = layerStart(c.atMs)
                val length = c.lengthMs ?: if (spec?.transitionLike == true) 1500 else 3000
                require(length > 0 && length <= MAX_VIDEO_CAPTION_TIME_MS)
                val item = VideoTimedEffect(kind = c.kind, startMs = start, endMs = minOf(p.durationMs, start + length), intensity = 1f, params = c.params ?: spec?.defaults().orEmpty(), target = c.target)
                p.copy(video = v.copy(effects = v.effects + item.sanitized()))
            }
            is PickEffect -> {
                val selected = c.effectId?.let { effect(it) }
                if (c.kind == null) p.copy(video = v.copy(effects = v.effects.filterNot { it.id == selected?.id }))
                else if (selected == null) reduce(p, AddEffect(c.kind, c.atMs, target = c.target))
                else { val spec = VideoFxCatalog.find(c.kind); require(spec != null || c.kind in VIDEO_EFFECT_KINDS); p.copy(video = v.copy(effects = v.effects.map { if (it.id == selected.id) it.copy(kind = c.kind, intensity = 1f, params = spec?.defaults().orEmpty()) else it })) }
            }
            is SetEffectParams -> { effect(c.effectId); p.copy(video = v.copy(effects = v.effects.map { if (it.id == c.effectId) it.copy(params = c.params).sanitized() else it })) }
            is SetEffect -> { effect(c.value.id); p.copy(video = v.copy(effects = v.effects.map { if (it.id == c.value.id) c.value.sanitized() else it })) }
            is DuplicateEffect -> p.copy(video = v.copy(effects = v.effects + effect(c.effectId).copy(id = UUID.randomUUID().toString())))
            is AddText -> p.copy(video = v.copy(texts = v.texts + c.value.sanitized()))
            is SetText -> { require(v.texts.any { it.id == c.value.id }); p.copy(video = v.copy(texts = v.texts.map { if (it.id == c.value.id) c.value.sanitized() else it })) }
            is AddImage -> p.copy(video = v.copy(images = v.images + c.value.sanitized()))
            is SetImage -> { require(v.images.any { it.id == c.value.id }); p.copy(video = v.copy(images = v.images.map { if (it.id == c.value.id) c.value.sanitized() else it })) }
            is PickLibraryOverlay -> {
                val card = VideoAnimatedStickers.find(c.itemId)
                val spec = if (card == null) VideoFxCatalog.overlays.firstOrNull { it.id == c.itemId } else null
                require(card != null || spec != null) { "Unknown library overlay" }
                val uri = if (card != null) "sticker:${c.itemId}" else "overlay:${c.itemId}"
                val name = card?.name ?: spec!!.name; val width = if (card != null) .9f else 1f
                val existing = v.images.firstOrNull { it.id == c.pendingId }
                val start = layerStart(c.atMs)
                val next = existing?.copy(uri = uri, name = name, width = width, x = .5f, y = .5f, rotation = 0) ?: VideoImageOverlay(uri = uri, name = name, startMs = start, endMs = minOf(p.durationMs, start + 3000), width = width)
                p.copy(video = v.copy(images = if (existing == null) v.images + next else v.images.map { if (it.id == existing.id) next else it }))
            }
            is AddAudio -> p.copy(video = v.copy(audio = v.audio + c.value.sanitized()))
            is SetAudio -> { require(v.audio.any { it.id == c.value.id }); p.copy(video = v.copy(audio = v.audio.map { if (it.id == c.value.id) c.value.sanitized() else it })) }
            is SplitAudio -> {
                val item = v.audio.firstOrNull { it.id == c.trackId } ?: error("Audio track not found")
                val offset = c.atMs - item.startMs
                require(offset >= 100 && item.durationMs - offset >= 100 && c.atMs < p.durationMs - 100)
                val cut = (item.sourceStartMs + (offset * item.speed).toLong()).coerceIn(item.sourceStartMs + 1, item.sourceEndMs - 1)
                val left = item.copy(sourceEndMs = cut, fadeOutMs = 0)
                val right = item.copy(id = UUID.randomUUID().toString(), sourceStartMs = cut, startMs = c.atMs, fadeInMs = 0)
                p.copy(video = v.copy(audio = v.audio.flatMap { if (it.id == item.id) listOf(left, right) else listOf(it) }))
            }
            is ExtractClipAudio -> {
                val item = clip(c.clipId); require(c.hasAudio && !item.uri.endsWith("_still.jpg")) { "This clip has no audio" }
                val start = p.clips.takeWhile { it.id != item.id }.sumOf { it.durationMs }
                val track = VideoAudioTrack(uri = item.uri, name = "Extracted · ${item.name.substringBeforeLast('.')}", sourceDurationMs = item.sourceDurationMs, sourceStartMs = item.startMs, sourceEndMs = item.endMs, startMs = start, volume = item.video.volume.coerceIn(0f, 2f), speed = item.playbackSpeed)
                p.copy(clips = p.clips.map { if (it.id == item.id) it.copy(muted = true) else it }, video = v.copy(audio = v.audio + track))
            }
            is SaveCaption -> { val cue = caption(c.value); p.copy(video = v.copy(captions = (v.captions.filterNot { it.id == cue.id } + cue).sortedBy { it.startMs })) }
            is SplitCaption -> { val cue = v.captions.firstOrNull { it.id == c.captionId } ?: error("Caption not found"); val pair = splitCaption(cue, c.atMs, c.textOffset) ?: error("Caption cannot split here"); p.copy(video = v.copy(captions = (v.captions.filterNot { it.id == cue.id } + pair.first + pair.second).sortedBy { it.startMs })) }
            is ShiftCaptions -> { require(c.offsetMs in -MAX_VIDEO_CAPTION_TIME_MS..MAX_VIDEO_CAPTION_TIME_MS); p.copy(video = v.copy(captions = v.captions.map { caption(it.copy(startMs = it.startMs + c.offsetMs, endMs = it.endMs + c.offsetMs)) })) }
            is SetCaptions -> p.copy(video = v.copy(captions = c.cues.map { caption(it) }, captionStyle = c.style.sanitized()))
            is DeleteLayer -> p.copy(video = when (c.kind) {
                LayerKind.EFFECT -> v.copy(effects = v.effects.filterNot { it.id == c.id })
                LayerKind.TEXT -> v.copy(texts = v.texts.filterNot { it.id == c.id })
                LayerKind.IMAGE -> v.copy(images = v.images.filterNot { it.id == c.id })
                LayerKind.AUDIO -> v.copy(audio = v.audio.filterNot { it.id == c.id })
                LayerKind.CAPTION -> v.copy(captions = v.captions.filterNot { it.id == c.id })
            })
            is DuplicateLayer -> {
                val id = UUID.randomUUID().toString()
                p.copy(video = when (c.kind) {
                    LayerKind.EFFECT -> v.copy(effects = v.effects + effect(c.id).copy(id = id))
                    LayerKind.TEXT -> v.copy(texts = v.texts + v.texts.first { it.id == c.id }.copy(id = id))
                    LayerKind.IMAGE -> v.copy(images = v.images + v.images.first { it.id == c.id }.copy(id = id))
                    LayerKind.AUDIO -> v.copy(audio = v.audio + v.audio.first { it.id == c.id }.copy(id = id))
                    LayerKind.CAPTION -> error("Caption duplicates need a free time range")
                })
            }
            is DragLayer -> p.copy(video = v.dragLayer(c.kind.mobileName, c.id, c.edge, c.deltaMs, p.durationMs))
            is SetLane -> p.copy(video = v.withLane(c.kind.mobileName, c.id, c.lane))
            else -> error("Unhandled command")
        }
    }
}
