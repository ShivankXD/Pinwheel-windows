package com.pinwheel.core.data

import com.pinwheel.core.model.*
import org.json.JSONArray
import org.json.JSONObject

/** Mobile JSON contract, separated from file access. */
object ProjectCodec {
    fun encode(p: StudioProject): String = JSONObject().apply {
        put("version", 10); put("id", p.id); put("name", p.name); put("kind", p.kind.name); put("modified", p.modifiedAt)
        put("adjustments", AdjustmentCodec.encode(p.adjustments))
        if (p.kind == ProjectKind.PHOTO) put("history", PhotoHistoryCodec.encode(p.photoHistory))
        if (p.kind == ProjectKind.VIDEO) put("video", VideoEditCodec.encodeProject(p.video))
        if (p.kind == ProjectKind.VIDEO) put("videoHistory",VideoHistoryCodec.encode(p.videoHistory))
        put("clips", JSONArray().apply { p.clips.forEach { c -> put(JSONObject().apply {
            put("id", c.id); put("uri", c.uri); put("name", c.name); put("duration", c.sourceDurationMs)
            put("start", c.startMs); put("end", c.endMs); put("muted", c.muted)
            if (p.kind == ProjectKind.VIDEO) put("video", VideoEditCodec.encodeClip(c.video))
        }) } })
    }.toString()
    fun decode(text: String, includeHistory: Boolean = true,includeVideoHistory:Boolean=includeHistory,strictVideoHistory:Boolean=false): StudioProject {
        val o = JSONObject(text)
        require(o.getInt("version") in 1..10) { "Unsupported project version" }
        if(strictVideoHistory && o.has("videoHistory") && !o.isNull("videoHistory"))require(o.opt("videoHistory") is JSONObject){"Unreadable video history"}
        val a = o.getJSONObject("adjustments")
        val clips = o.getJSONArray("clips")
        return StudioProject(o.getString("id"), o.getString("name"), ProjectKind.valueOf(o.getString("kind")),
            (0 until clips.length()).map { i -> clips.getJSONObject(i).let { c ->
                Clip(c.getString("id"), c.getString("uri"), c.getString("name"), c.getLong("duration"), c.getLong("start"), c.getLong("end"), c.optBoolean("muted"), VideoEditCodec.decodeClip(c.optJSONObject("video")))
            } }, AdjustmentCodec.decode(a), o.getLong("modified"), if (includeHistory) PhotoHistoryCodec.decode(o.optJSONObject("history")) else PhotoHistory(), VideoEditCodec.decodeProject(o.optJSONObject("video")),
            if(!includeVideoHistory)VideoHistory()else if(strictVideoHistory)VideoHistoryCodec.decodeChecked(o.optJSONObject("videoHistory"))else VideoHistoryCodec.decode(o.optJSONObject("videoHistory")))
    }
}
