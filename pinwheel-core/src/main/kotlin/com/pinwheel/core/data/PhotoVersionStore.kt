package com.pinwheel.core.data

import com.pinwheel.core.model.Adjustments
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class PhotoVersion(val id: String, val name: String, val createdAt: Long, val adjustments: Adjustments)

/** Separate snapshots avoid rewriting every named version on each slider gesture. */
class PhotoVersionStore(filesDir: File) {
    private val root=File(filesDir,"photo-versions")
    private fun directory(projectId: String): File {
        require(projectId.matches(Regex("[A-Za-z0-9-]+"))) {"Invalid project identifier"}
        return File(root,projectId)
    }
    fun list(projectId: String): List<PhotoVersion> = directory(projectId).listFiles().orEmpty()
        .filter {it.extension=="json"}.mapNotNull {file -> runCatching {
            val objectValue=JSONObject(AtomicFile(file).openRead().bufferedReader().use {it.readText()})
            PhotoVersion(objectValue.getString("id"),objectValue.getString("name"),objectValue.getLong("created"),AdjustmentCodec.decode(objectValue.getJSONObject("adjustments")))
        }.getOrNull()}.sortedByDescending {it.createdAt}

    fun save(projectId: String, name: String, adjustments: Adjustments): PhotoVersion {
        require(name.isNotBlank()) {"Give this version a name"}
        require(list(projectId).size<20) {"This photo has 20 versions. Delete a version before saving another."}
        val version=PhotoVersion(UUID.randomUUID().toString(),name.trim().take(60),System.currentTimeMillis(),adjustments)
        val folder=directory(projectId).apply {mkdirs()}
        val atomic=AtomicFile(File(folder,"${version.id}.json"))
        val data=JSONObject().put("id",version.id).put("name",version.name).put("created",version.createdAt).put("adjustments",AdjustmentCodec.encode(adjustments)).toString()
        val output=atomic.startWrite()
        try {output.write(data.toByteArray(Charsets.UTF_8));atomic.finishWrite(output)} catch(e:Exception){atomic.failWrite(output);throw e}
        return version
    }
    fun delete(projectId: String, versionId: String) {
        require(versionId.matches(Regex("[A-Za-z0-9-]+"))) {"Invalid version identifier"}
        AtomicFile(File(directory(projectId),"$versionId.json")).delete()
    }
}
