package com.pinwheel.core.editing

import com.pinwheel.core.data.*
import com.pinwheel.core.model.*
import org.json.JSONArray
import org.json.JSONObject

/** Explicit allow-list; never deserializes JVM class names or executable objects. */
object EditCommandCodec {
    fun encode(c: EditCommand): String {
        val o = JSONObject().put("version", 1).put("type", c.javaClass.simpleName)
        fun put(key: String, value: Any?) { o.put(key, value ?: JSONObject.NULL) }
        when (c) {
            is RenameProject -> put("name", c.name)
            is SetAdjustments -> put("value", AdjustmentCodec.encode(c.value))
            is AddClips -> { put("clips", clips(c.clips)); put("at", c.at) }
            is ReplaceClip -> { put("clipId", c.clipId); put("replacement", clips(listOf(c.replacement))) }
            is SplitClip -> { put("clipId", c.clipId); put("atMs", c.atMs) }
            is TrimClip -> { put("clipId", c.clipId); put("startMs", c.startMs); put("endMs", c.endMs) }
            is DeleteClip -> put("clipId", c.clipId)
            is DuplicateClip -> put("clipId", c.clipId)
            is MoveClip -> { put("clipId", c.clipId); put("to", c.to) }
            is MuteClip -> put("clipId", c.clipId)
            is SetClipEdits -> { put("clipId", c.clipId); put("value", VideoEditCodec.encodeClip(c.value)) }
            is SetTransition -> { put("clipId", c.clipId); put("transitionId", c.transitionId); put("durationMs", c.durationMs); put("speed", c.speed) }
            is ApplyTransitionToAll -> put("value", VideoEditCodec.encodeClip(c.value))
            is ApplyGradeToAll -> put("value", VideoEditCodec.encodeClip(c.value))
            is SetCanvas -> put("value", VideoEditCodec.encodeProject(c.value))
            is AddEffect -> { put("kind", c.kind); put("atMs", c.atMs); put("lengthMs", c.lengthMs); put("params", c.params?.let { JSONObject(it) }); put("target", c.target) }
            is PickEffect -> { put("effectId", c.effectId); put("kind", c.kind); put("atMs", c.atMs); put("target", c.target) }
            is SetEffectParams -> { put("effectId", c.effectId); put("params", JSONObject(c.params)) }
            is SetEffect -> put("value", VideoEditCodec.encodeProject(VideoProjectEdits(effects = listOf(c.value))))
            is DuplicateEffect -> put("effectId", c.effectId)
            is AddText -> put("value", VideoEditCodec.encodeProject(VideoProjectEdits(texts = listOf(c.value))))
            is SetText -> put("value", VideoEditCodec.encodeProject(VideoProjectEdits(texts = listOf(c.value))))
            is AddImage -> put("value", VideoEditCodec.encodeProject(VideoProjectEdits(images = listOf(c.value))))
            is SetImage -> put("value", VideoEditCodec.encodeProject(VideoProjectEdits(images = listOf(c.value))))
            is PickLibraryOverlay -> { put("pendingId", c.pendingId); put("itemId", c.itemId); put("atMs", c.atMs) }
            is AddAudio -> put("value", VideoEditCodec.encodeProject(VideoProjectEdits(audio = listOf(c.value))))
            is SetAudio -> put("value", VideoEditCodec.encodeProject(VideoProjectEdits(audio = listOf(c.value))))
            is SplitAudio -> { put("trackId", c.trackId); put("atMs", c.atMs) }
            is ExtractClipAudio -> { put("clipId", c.clipId); put("hasAudio", c.hasAudio) }
            is SaveCaption -> put("value", VideoEditCodec.encodeProject(VideoProjectEdits(captions = listOf(c.value))))
            is SplitCaption -> { put("captionId", c.captionId); put("atMs", c.atMs); put("textOffset", c.textOffset) }
            is ShiftCaptions -> put("offsetMs", c.offsetMs)
            is SetCaptions -> put("value", VideoEditCodec.encodeProject(VideoProjectEdits(captions = c.cues, captionStyle = c.style)))
            is DeleteLayer -> { put("kind", c.kind.name); put("id", c.id) }
            is DuplicateLayer -> { put("kind", c.kind.name); put("id", c.id) }
            is DragLayer -> { put("kind", c.kind.name); put("id", c.id); put("edge", c.edge.name); put("deltaMs", c.deltaMs) }
            is SetLane -> { put("kind", c.kind.name); put("id", c.id); put("lane", c.lane) }
            MuteAllClips, Undo, Redo -> Unit
        }
        return o.toString()
    }
    fun decode(json: String): EditCommand {
        require(json.length <= 2 * 1024 * 1024) { "Command too large" }
        val o = JSONObject(json)
        require(o.getInt("version") == 1) { "Unsupported command version" }
        fun s(key: String) = o.getString(key)
        fun nullable(key: String) = if (o.isNull(key)) null else s(key)
        fun l(key: String) = o.getLong(key)
        fun v() = VideoEditCodec.decodeProject(o.getJSONObject("value"))
        fun params(): Map<String, Float> = o.getJSONObject("params").let { p -> p.keySet().associateWith { p.getDouble(it).toFloat() } }
        return when (s("type")) {
            "RenameProject" -> RenameProject(s("name"))
            "SetAdjustments" -> SetAdjustments(AdjustmentCodec.decode(o.getJSONObject("value")))
            "AddClips" -> AddClips(readClips(o.getJSONArray("clips")), if (o.isNull("at")) null else o.getInt("at"))
            "ReplaceClip" -> ReplaceClip(s("clipId"), readClips(o.getJSONArray("replacement")).single())
            "SplitClip" -> SplitClip(s("clipId"), l("atMs"))
            "TrimClip" -> TrimClip(s("clipId"), l("startMs"), l("endMs"))
            "DeleteClip" -> DeleteClip(s("clipId"))
            "DuplicateClip" -> DuplicateClip(s("clipId"))
            "MoveClip" -> MoveClip(s("clipId"), o.getInt("to"))
            "MuteClip" -> MuteClip(s("clipId"))
            "MuteAllClips" -> MuteAllClips
            "SetClipEdits" -> SetClipEdits(s("clipId"), VideoEditCodec.decodeClip(o.getJSONObject("value")))
            "SetTransition" -> SetTransition(s("clipId"), s("transitionId"), l("durationMs"), o.getDouble("speed").toFloat())
            "ApplyTransitionToAll" -> ApplyTransitionToAll(VideoEditCodec.decodeClip(o.getJSONObject("value")))
            "ApplyGradeToAll" -> ApplyGradeToAll(VideoEditCodec.decodeClip(o.getJSONObject("value")))
            "SetCanvas" -> SetCanvas(v())
            "AddEffect" -> AddEffect(s("kind"), l("atMs"), if (o.isNull("lengthMs")) null else l("lengthMs"), if (o.isNull("params")) null else params(), s("target"))
            "PickEffect" -> PickEffect(nullable("effectId"), nullable("kind"), l("atMs"), s("target"))
            "SetEffectParams" -> SetEffectParams(s("effectId"), params())
            "SetEffect" -> SetEffect(v().effects.single())
            "DuplicateEffect" -> DuplicateEffect(s("effectId"))
            "AddText" -> AddText(v().texts.single())
            "SetText" -> SetText(v().texts.single())
            "AddImage" -> AddImage(v().images.single())
            "SetImage" -> SetImage(v().images.single())
            "PickLibraryOverlay" -> PickLibraryOverlay(nullable("pendingId"), s("itemId"), l("atMs"))
            "AddAudio" -> AddAudio(v().audio.single())
            "SetAudio" -> SetAudio(v().audio.single())
            "SplitAudio" -> SplitAudio(s("trackId"), l("atMs"))
            "ExtractClipAudio" -> ExtractClipAudio(s("clipId"), o.getBoolean("hasAudio"))
            "SaveCaption" -> SaveCaption(v().captions.single())
            "SplitCaption" -> SplitCaption(s("captionId"), l("atMs"), o.getInt("textOffset"))
            "ShiftCaptions" -> ShiftCaptions(l("offsetMs"))
            "SetCaptions" -> v().let { SetCaptions(it.captions, it.captionStyle) }
            "DeleteLayer" -> DeleteLayer(LayerKind.valueOf(s("kind")), s("id"))
            "DuplicateLayer" -> DuplicateLayer(LayerKind.valueOf(s("kind")), s("id"))
            "DragLayer" -> DragLayer(LayerKind.valueOf(s("kind")), s("id"), TimelineEdge.valueOf(s("edge")), l("deltaMs"))
            "SetLane" -> SetLane(LayerKind.valueOf(s("kind")), s("id"), o.getInt("lane"))
            "Undo" -> Undo
            "Redo" -> Redo
            else -> error("Unknown edit command")
        }
    }
    private fun clips(value: List<Clip>) = JSONObject(ProjectCodec.encode(StudioProject(id = "command", name = "command", kind = ProjectKind.VIDEO, clips = value, modifiedAt = 0))).getJSONArray("clips")
    private fun readClips(value: JSONArray) = ProjectCodec.decode(JSONObject().put("version", 10).put("id", "command").put("name", "command").put("kind", "VIDEO").put("modified", 0).put("adjustments", JSONObject()).put("clips", value).toString()).clips
}
