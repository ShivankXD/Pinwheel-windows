package com.pinwheel.core.data

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption.*
import java.nio.file.AtomicMoveNotSupportedException

/** Android AtomicFile-compatible backup recovery with fsynced sibling staging. */
class AtomicFile(private val base: File) {
    private val backup = File(base.path + ".bak")
    private val staging = File(base.path + ".new")

    fun openRead(): FileInputStream {
        if (backup.exists()) move(backup, base)
        return FileInputStream(base)
    }

    fun startWrite(): FileOutputStream {
        Files.createDirectories(base.toPath().toAbsolutePath().parent)
        if (backup.exists()) move(backup, base)
        return FileOutputStream(staging)
    }

    fun finishWrite(stream: FileOutputStream) {
        stream.fd.sync()
        stream.close()
        move(staging, base)
        Files.deleteIfExists(backup.toPath())
    }

    fun failWrite(stream: FileOutputStream) {
        runCatching { stream.close() }
        Files.deleteIfExists(staging.toPath())
    }

    fun delete() {
        for (file in listOf(base, backup, staging)) Files.deleteIfExists(file.toPath())
    }

    private fun move(from: File, to: File) {
        try { Files.move(from.toPath(), to.toPath(), ATOMIC_MOVE, REPLACE_EXISTING) }
        catch (_: AtomicMoveNotSupportedException) { Files.move(from.toPath(), to.toPath(), REPLACE_EXISTING) }
    }
}
