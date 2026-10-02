package com.pinwheel.core.data

import java.net.URI
import com.pinwheel.core.model.StudioProject
import java.io.File

data class LibraryStorageUsage(
    val originalsBytes: Long,
    val projectsBytes: Long,
    val versionsBytes: Long,
    val samplesBytes: Long,
    val reclaimableBytes: Long,
    val unreadableProjects: Int,
) {
    val totalBytes: Long get() = originalsBytes + projectsBytes + versionsBytes + samplesBytes
}

data class LibraryCleanupResult(val freedBytes: Long, val removedFiles: Int, val failedFiles: Int, val mediaCleanupBlocked: Boolean)
data class LibrarySnapshot(val projects: List<StudioProject>, val unreadableProjects: Int)

/** Only app-private files are managed here. Never follows a link outside the four library roots. */
class LibraryStorage(private val filesDir: File, private val now: () -> Long = System::currentTimeMillis) {
    private val originals = File(filesDir, "originals")
    private val versions = File(filesDir, "photo-versions")
    private val samples = File(filesDir, "demo")
    private val projects = File(filesDir, "projects")
    private val orphanGraceMs = 24L * 60 * 60 * 1000

    private fun contained(file: File, root: File): Boolean = runCatching {
        val base = root.canonicalFile
        // Root itself must also be a real app-owned directory, not an external symbolic link.
        base.parentFile == filesDir.canonicalFile && file.canonicalPath.startsWith(base.path + File.separator)
    }.getOrDefault(false)

    private fun files(root: File): List<File> {
        if (!root.exists()) return emptyList()
        val result = mutableListOf<File>()
        val pending = ArrayDeque<File>(); pending.add(root)
        val seen = mutableSetOf<String>()
        while (pending.isNotEmpty()) {
            val folder = pending.removeFirst()
            if (!seen.add(folder.canonicalPath)) continue
            folder.listFiles().orEmpty().forEach { file ->
                if (contained(file, root)) {
                    if (file.isFile) result += file
                    else if (file.isDirectory) pending.add(file)
                }
            }
        }
        return result.distinctBy { it.canonicalPath }
    }

    private fun sourceUris(project: StudioProject): List<String> = project.clips.map { it.uri } + project.video.images.map { it.uri } + project.video.audio.map { it.uri } +
        (project.videoHistory.past+project.videoHistory.future).flatMap{edit->edit.clips.map{it.uri}+edit.video.images.map{it.uri}+edit.video.audio.map{it.uri}}

    private fun references(snapshot: LibrarySnapshot): Set<String> = snapshot.projects.flatMap(::sourceUris).mapNotNull { source ->
        runCatching { source.toUri().takeIf { it.scheme == "file" }?.path?.let { File(it).canonicalPath } }.getOrNull()
    }.toSet()

    private fun oldEnough(file: File): Boolean = file.lastModified().let { it > 0L && it <= now() - orphanGraceMs }

    private fun unused(snapshot: LibrarySnapshot): List<File> {
        if (snapshot.unreadableProjects != 0) return emptyList()
        val used = references(snapshot)
        val projectIds = snapshot.projects.map { it.id }.toSet()
        val unusedMedia = (files(originals) + files(samples)).filter { it.canonicalPath !in used && oldEnough(it) }
        val unusedVersions = files(versions).filter {
            val relative = it.relativeTo(versions).invariantSeparatorsPath.split('/')
            relative.size == 2 && relative[0].matches(Regex("[A-Za-z0-9-]+")) && relative[0] !in projectIds && oldEnough(it)
        }
        return unusedMedia + unusedVersions
    }

    fun usage(snapshot: LibrarySnapshot) = LibraryStorageUsage(
        files(originals).sumOf { it.length() }, files(projects).sumOf { it.length() },
        files(versions).sumOf { it.length() }, files(samples).sumOf { it.length() },
        unused(snapshot).sumOf { it.length() }, snapshot.unreadableProjects,
    )

    fun cleanUnused(snapshot: LibrarySnapshot): LibraryCleanupResult = remove(unused(snapshot), snapshot.unreadableProjects != 0)

    fun afterProjectDeleted(project: StudioProject, remaining: LibrarySnapshot): LibraryCleanupResult {
        val used = references(remaining)
        val ownedSources = if (remaining.unreadableProjects != 0) emptyList() else sourceUris(project).mapNotNull { source ->
            runCatching {
                val uri = source.toUri()
                if (uri.scheme != "file") null else uri.path?.let(::File)?.takeIf {
                    it.isFile && (contained(it, originals) || contained(it, samples)) && it.canonicalPath !in used
                }
            }.getOrNull()
        }
        val ownVersions = files(versions).filter { it.parentFile?.name == project.id && it.parentFile?.parentFile?.canonicalFile == versions.canonicalFile }
        return remove(ownedSources + ownVersions, remaining.unreadableProjects != 0).also {
            // Remove only an empty, verified directory. Failed file cleanup is retained for retry.
            val folder = File(versions, project.id)
            if (contained(folder, versions) && folder.isDirectory && folder.listFiles()?.isEmpty() == true) folder.delete()
        }
    }

    private fun remove(candidates: List<File>, blocked: Boolean): LibraryCleanupResult {
        var freed = 0L; var removed = 0; var failed = 0
        candidates.distinctBy { it.canonicalPath }.forEach { file ->
            if (!file.exists()) return@forEach
            val size = file.length()
            if (file.delete()) { freed += size; removed++ } else failed++
        }
        return LibraryCleanupResult(freed, removed, failed, blocked)
    }
}

private data class SourceUri(val scheme: String?, val path: String?)
private fun String.toUri(): SourceUri {
    val uri = URI(replace(" ", "%20"))
    val path = uri.path?.let { if (it.matches(Regex("/[A-Za-z]:/.*"))) it.drop(1) else it }
    return SourceUri(uri.scheme, path)
}
