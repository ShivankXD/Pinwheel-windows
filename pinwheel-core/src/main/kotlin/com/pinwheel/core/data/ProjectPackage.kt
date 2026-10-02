package com.pinwheel.core.data

import com.pinwheel.core.model.StudioProject
import org.json.JSONArray
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.net.URI
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** v10 project.json with media/ relative URIs, including references in persisted histories. */
object ProjectPackage {
    data class Limits(val jsonBytes: Long = 16L * 1024 * 1024, val mediaBytes: Long = 16L * 1024 * 1024 * 1024,
        val entries: Int = 4096)

    fun export(project: StudioProject, destination: Path, resolve: (String) -> Path = { Path.of(URI(it)) }) {
        require(!Files.exists(destination)) { "Package destination already exists" }
        val json = JSONObject(ProjectCodec.encode(project))
        val sources = linkedMapOf<String, Pair<String, Path>>()
        visitUris(json) { uri ->
            if (uri.startsWith("sticker:") || uri.startsWith("overlay:") || uri.startsWith("asset:")) uri else {
                val pair = sources.getOrPut(uri) {
                    val source = resolve(uri)
                    require(Files.isRegularFile(source)) { "Missing package media: $uri" }
                    "media/${sources.size}-${source.fileName.toString().replace(Regex("[^A-Za-z0-9._-]"), "_")}" to source
                }
                pair.first
            }
        }
        destination.parent?.let { Files.createDirectories(it) }
        val staging = destination.resolveSibling(".${destination.fileName}.${UUID.randomUUID()}.new")
        try {
            ZipOutputStream(Files.newOutputStream(staging)).use { zip ->
                zip.putNextEntry(ZipEntry("project.json")); zip.write(json.toString().toByteArray(Charsets.UTF_8)); zip.closeEntry()
                for ((name, source) in sources.values) {
                    zip.putNextEntry(ZipEntry(name)); Files.newInputStream(source).use { it.copyTo(zip) }; zip.closeEntry()
                }
            }
            // No REPLACE_EXISTING: a race must never overwrite an existing package.
            Files.move(staging, destination)
        } finally { Files.deleteIfExists(staging) }
    }

    fun import(source: Path, filesDir: Path, store: ProjectStore, limits: Limits = Limits()): StudioProject {
        val copied = mutableListOf<Path>()
        val originals = filesDir.resolve("originals").toAbsolutePath().normalize()
        Files.createDirectories(originals)
        try {
            return ZipFile(source.toFile()).use { zip ->
                val entries = zip.entries().toList()
                require(entries.size <= limits.entries) { "Too many package entries" }
                require(entries.map { it.name.lowercase(java.util.Locale.ROOT) }.distinct().size == entries.size) { "Duplicate package entry" }
                entries.forEach { require(safeName(it.name)) { "Unsafe package path: ${it.name}" } }
                val record = entries.singleOrNull { it.name == "project.json" && !it.isDirectory } ?: error("Package needs project.json")
                val json = JSONObject(zip.getInputStream(record).use { readBounded(it, limits.jsonBytes) }.toString(Charsets.UTF_8))
                val original = ProjectCodec.decode(json.toString(), strictVideoHistory = true)
                require(!store.contains(original.id)) { "Project identifier already exists" }
                val media = linkedMapOf<String, String>()
                var bytes = 0L
                visitUris(json) { uri ->
                    if (uri.startsWith("sticker:") || uri.startsWith("overlay:") || uri.startsWith("asset:")) uri else media.getOrPut(uri) {
                        require(uri.startsWith("media/") && safeName(uri)) { "Package media must use relative media/ paths" }
                        val entry = entries.singleOrNull { it.name == uri && !it.isDirectory } ?: error("Missing package media: $uri")
                        val target = originals.resolve("${UUID.randomUUID()}-${Path.of(uri).fileName}")
                        require(target.parent == originals && target.parent.toRealPath() == originals.toRealPath())
                        copied.add(target)
                        Files.newOutputStream(target, java.nio.file.StandardOpenOption.CREATE_NEW).use { output ->
                            zip.getInputStream(entry).use { input ->
                                val buffer = ByteArray(64 * 1024)
                                while (true) {
                                    val n = input.read(buffer); if (n < 0) break
                                    bytes = Math.addExact(bytes, n.toLong()); require(bytes <= limits.mediaBytes) { "Package media exceeds limit" }
                                    output.write(buffer, 0, n)
                                }
                            }
                        }
                        target.toUri().toString()
                    }
                }
                require(entries.filterNot { it.isDirectory || it.name == "project.json" }.all { it.name in media }) { "Unreferenced package file" }
                val result = ProjectCodec.decode(json.toString(), strictVideoHistory = true)
                store.save(result)
                result
            }
        } catch (failure: Exception) {
            for (file in copied) Files.deleteIfExists(file)
            throw failure
        }
    }

    private fun safeName(name: String): Boolean = name.isNotEmpty() && !name.startsWith('/') && '\\' !in name && ':' !in name &&
        name.split('/').all { it != ".." && it != "." && (it.isNotEmpty() || name.endsWith('/')) } && '\u0000' !in name
    private fun readBounded(input: java.io.InputStream, limit: Long): ByteArray {
        val result = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192); var total = 0L
        while (true) { val n = input.read(buffer); if (n < 0) break; total += n; require(total <= limit) { "Package JSON exceeds limit" }; result.write(buffer, 0, n) }
        return result.toByteArray()
    }
    private fun visitUris(value: Any, transform: (String) -> String) {
        when (value) {
            is JSONObject -> for (key in value.keySet().toList()) {
                val item = value.get(key)
                if ((key == "uri" || (key == "image" && value.has("ops") && value.has("background"))) && item is String && item.isNotEmpty()) value.put(key, transform(item))
                else if (item is JSONObject || item is JSONArray) visitUris(item, transform)
            }
            is JSONArray -> for (i in 0 until value.length()) { val item = value.get(i); if (item is JSONObject || item is JSONArray) visitUris(item, transform) }
        }
    }
}
