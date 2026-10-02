package com.pinwheel.core.data

import com.pinwheel.core.model.*
import org.json.JSONArray
import org.json.JSONObject

/** Nearest contiguous states survive the shared 2 MiB budget; current project data is never truncated. */
object VideoHistoryCodec {
    const val MAX_BYTES=2*1024*1024
    fun encode(history:VideoHistory):JSONObject {
        var remaining=MAX_BYTES-128;var steps=VideoHistory.LIMIT
        val inputs=listOf(history.past.takeLast(VideoHistory.LIMIT).asReversed(),history.future.takeLast(VideoHistory.LIMIT).asReversed())
        val outputs=listOf(mutableListOf<JSONObject>(),mutableListOf<JSONObject>())
        val indices=intArrayOf(0,0);val active=booleanArrayOf(true,true)
        // Alternate nearest Undo and Redo states so a long undo branch cannot consume the entire budget.
        while(steps>0 && active.any{it}) {
            for(side in 0..1) {
                if(!active[side] || steps==0)continue
                val entry=inputs[side].getOrNull(indices[side])
                if(entry==null){active[side]=false;continue}
                val json=JSONObject().put("name",entry.name).put("clips",encodeClips(entry.clips))
                    .put("video",VideoEditCodec.encodeProject(entry.video)).put("adjustments",AdjustmentCodec.encode(entry.adjustments))
                val bytes=json.toString().toByteArray(Charsets.UTF_8).size+1
                if(bytes>remaining){active[side]=false;continue}
                remaining-=bytes;steps--;indices[side]++;outputs[side]+=json
            }
        }
        fun states(side:Int)=JSONArray().apply{outputs[side].asReversed().forEach{put(it)}}
        return JSONObject().put("past",states(0)).put("future",states(1))
    }
    fun decode(json:JSONObject?):VideoHistory=runCatching{decodeChecked(json)}.getOrElse{VideoHistory()}
    /** Cleanup must fail closed if optional history cannot be read; editing can still open the current state. */
    fun decodeChecked(json:JSONObject?):VideoHistory {
        if (json == null) { return VideoHistory() }
        require(json.toString().toByteArray(Charsets.UTF_8).size<=MAX_BYTES)
        listOf("past","future").forEach{key->if(json.has(key))require(json.opt(key) is JSONArray)}
        val past=json.optJSONArray("past")?:JSONArray();val future=json.optJSONArray("future")?:JSONArray()
        require(past.length()+future.length()<=VideoHistory.LIMIT)
        fun states(values:JSONArray)=List(values.length()){index->
            val value=values.getJSONObject(index)
            VideoEdit(value.getString("name").take(80),decodeClips(value.getJSONArray("clips")),
                VideoEditCodec.decodeProject(value.getJSONObject("video")),AdjustmentCodec.decode(value.getJSONObject("adjustments")))
        }
        return VideoHistory(states(past),states(future))
    }
    private fun encodeClips(clips:List<Clip>)=JSONArray().apply{clips.forEach{clip->put(JSONObject()
        .put("id",clip.id).put("uri",clip.uri).put("name",clip.name).put("duration",clip.sourceDurationMs)
        .put("start",clip.startMs).put("end",clip.endMs).put("muted",clip.muted).put("video",VideoEditCodec.encodeClip(clip.video)))}}
    private fun decodeClips(values:JSONArray):List<Clip> {
        require(values.length()<=1000)
        return List(values.length()){index->
            val value=values.getJSONObject(index)
            val duration=value.getLong("duration");val start=value.getLong("start");val end=value.getLong("end")
            require(duration>=0 && start in 0..duration && end in start..duration)
            Clip(value.getString("id"),value.getString("uri"),value.getString("name"),duration,start,end,value.optBoolean("muted"),VideoEditCodec.decodeClip(value.optJSONObject("video")))
        }
    }
}
