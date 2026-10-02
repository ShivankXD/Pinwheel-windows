package com.pinwheel.core.data

import com.pinwheel.core.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ProjectStore(filesDir: File) {
    private val library = LibraryStorage(filesDir)
    private val directory = File(filesDir, "projects").apply { mkdirs() }
    private fun projectFile(id: String): File {
        require(id.matches(Regex("[A-Za-z0-9-]+"))) { "Invalid project identifier" }
        return File(directory, "$id.json")
    }
    private fun records(): List<File> = directory.listFiles().orEmpty()
        .filter { it.name.endsWith(".json") || it.name.endsWith(".json.bak") }
        .map { if (it.name.endsWith(".bak")) File(directory,it.name.removeSuffix(".bak")) else it }.distinctBy { it.path }
    private fun read(file: File, includeHistory: Boolean = true, includeVideoHistory:Boolean=includeHistory,strictVideoHistory:Boolean=false): StudioProject = ProjectCodec.decode(AtomicFile(file).openRead().bufferedReader().use { it.readText() }, includeHistory,includeVideoHistory,strictVideoHistory).also {
        require(projectFile(it.id).name == file.name) { "Project identifier does not match its file" }
    }
    @Synchronized fun list(includeHistory: Boolean = true): List<StudioProject> = records()
        .mapNotNull { runCatching { read(it, includeHistory) }.getOrNull() }
        .sortedByDescending { it.modifiedAt }
    @Synchronized fun load(id: String): StudioProject = read(projectFile(id))
    @Synchronized fun save(project: StudioProject) {
        val atomic = AtomicFile(projectFile(project.id))
        val stream = atomic.startWrite()
        try { stream.write(ProjectCodec.encode(project).toByteArray()); atomic.finishWrite(stream) }
        catch (e: Exception) { atomic.failWrite(stream); throw e }
    }
    @Synchronized fun rename(id: String, name: String): StudioProject {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty() && cleanName.length <= 80) { "Use a name between 1 and 80 characters" }
        val original = read(projectFile(id))
        val renamed = original.copy(name = cleanName, modifiedAt = System.currentTimeMillis(),
            photoHistory = if (original.kind == ProjectKind.PHOTO && original.name != cleanName)
                original.photoHistory.record(original) else original.photoHistory,
            videoHistory = if(original.kind==ProjectKind.VIDEO && original.name!=cleanName) original.videoHistory.record(original) else original.videoHistory)
        save(renamed)
        return renamed
    }
    private fun snapshot(additionalProjects:List<StudioProject> = emptyList()): LibrarySnapshot {
        val results = records().map { runCatching { read(it, includeHistory = false,includeVideoHistory=true,strictVideoHistory=true) } }
        return LibrarySnapshot(results.mapNotNull { it.getOrNull() }+additionalProjects, results.count { it.isFailure })
    }
    @Synchronized fun storageUsage(additionalProjects:List<StudioProject> = emptyList()): LibraryStorageUsage = library.usage(snapshot(additionalProjects))
    @Synchronized fun contains(id: String): Boolean = runCatching { read(projectFile(id)) }.isSuccess
    @Synchronized fun cleanUnusedImports(additionalProjects:List<StudioProject> = emptyList()): LibraryCleanupResult = library.cleanUnused(snapshot(additionalProjects))
    @Synchronized fun delete(id: String,additionalProjects:List<StudioProject> = emptyList()): LibraryCleanupResult {
        val file = projectFile(id)
        val project = read(file)
        val recordFiles = listOf(file, File(file.path + ".bak"), File(file.path + ".new")).filter { it.exists() }
        val recordBytes = recordFiles.sumOf { it.length() }
        AtomicFile(file).delete()
        check(recordFiles.none { it.exists() }) { "Couldn't delete the project" }
        val cleanup = library.afterProjectDeleted(project, snapshot(additionalProjects.filterNot{it.id==id}))
        return cleanup.copy(freedBytes = cleanup.freedBytes + recordBytes, removedFiles = cleanup.removedFiles + recordFiles.size)
    }
}
