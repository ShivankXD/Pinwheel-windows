package com.pinwheel.core.data

import com.pinwheel.core.model.PhotoEdit
import com.pinwheel.core.model.PhotoHistory
import org.json.JSONArray
import org.json.JSONObject

/** Bound disk overhead even for long brush strokes. Keep contiguous nearest undo/redo steps. */
object PhotoHistoryCodec {
    private const val MAX_BYTES = 2 * 1024 * 1024
    fun encode(history: PhotoHistory): JSONObject {
        var remaining = MAX_BYTES
        fun states(values: List<PhotoEdit>): JSONArray {
            val result = mutableListOf<JSONObject>()
            for (entry in values.takeLast(PhotoHistory.LIMIT).asReversed()) {
                val json = JSONObject().put("name", entry.name).put("adjustments", AdjustmentCodec.encode(entry.adjustments))
                val bytes = json.toString().toByteArray(Charsets.UTF_8).size
                if (bytes > remaining) break
                remaining -= bytes
                result.add(json)
            }
            return JSONArray().apply { result.asReversed().forEach { put(it) } }
        }
        return JSONObject().put("past", states(history.past)).put("future", states(history.future))
    }

    fun decode(json: JSONObject?): PhotoHistory {
        if (json == null) return PhotoHistory()
        // Damaged optional history must never make the current photograph inaccessible.
        return runCatching {
            require(json.toString().toByteArray(Charsets.UTF_8).size <= MAX_BYTES + 256)
            fun states(key: String): List<PhotoEdit> {
                val values = json.optJSONArray(key) ?: return emptyList()
                require(values.length() <= PhotoHistory.LIMIT)
                return (0 until values.length()).map { index ->
                    val value = values.getJSONObject(index)
                    PhotoEdit(value.getString("name").take(80), AdjustmentCodec.decode(value.getJSONObject("adjustments")))
                }
            }
            PhotoHistory(states("past"), states("future"))
        }.getOrElse { PhotoHistory() }
    }
}
