package com.pinwheel.media

import com.pinwheel.core.RgbaFrame
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** P0 process adapter. P3 will add persistent decoding and bounded frame queues. */
class FfmpegDecoder(private val nativeDirectory: Path) {
    fun version(): String = String(run("ffmpeg", listOf("-version"), 256 * 1024)).lineSequence().first()
    fun configuration(): String = String(run("ffmpeg", listOf("-version"), 256 * 1024))

    fun frame(source: Path, timeUs: Long = 0): RgbaFrame {
        require(timeUs >= 0)
        require(Files.isRegularFile(source)) { "Missing video: $source" }
        val dimensions = String(run("ffprobe", listOf("-v", "error", "-select_streams", "v:0",
            "-show_entries", "stream=width,height", "-of", "csv=p=0:s=x", source.toAbsolutePath().toString()), 4096))
            .trim().split("x")
        require(dimensions.size == 2) { "Invalid video dimensions: $dimensions" }
        val width = dimensions[0].toInt(); val height = dimensions[1].toInt()
        require(width in 1..8192 && height in 1..8192 && width.toLong() * height <= 25_000_000)
        val seconds = java.math.BigDecimal(timeUs).movePointLeft(6).toPlainString()
        // Seek after the input, so FFmpeg decodes through to the requested time rather than a keyframe.
        val pixels = run("ffmpeg", listOf("-v", "error", "-nostdin", "-i", source.toAbsolutePath().toString(),
            "-ss", seconds, "-frames:v", "1", "-an", "-pix_fmt", "rgba", "-f", "rawvideo", "pipe:1"), width * height * 4)
        return RgbaFrame(width, height, pixels)
    }

    private fun run(tool: String, arguments: List<String>, maxOutput: Int): ByteArray {
        val executable = nativeDirectory.resolve("$tool.exe").toAbsolutePath()
        require(Files.isRegularFile(executable)) { "Run scripts/bootstrap-native.ps1 first: $executable" }
        val errors = Files.createTempFile("pinwheel-ffmpeg-", ".log")
        try {
            val process = ProcessBuilder(listOf(executable.toString()) + arguments).redirectError(errors.toFile()).start()
            var result: ByteArray? = null
            var readFailure: Throwable? = null
            val reader = thread(name = "pinwheel-$tool-output", isDaemon = true) {
                try {
                    result = process.inputStream.use { it.readNBytes(maxOutput + 1) }
                    check(result!!.size <= maxOutput) { "$tool output exceeded $maxOutput bytes" }
                } catch (error: Throwable) { readFailure = error; process.destroyForcibly() }
            }
            try {
                check(process.waitFor(30, TimeUnit.SECONDS)) { "$tool timed out" }
                reader.join(5000)
                check(!reader.isAlive) { "$tool output reader timed out" }
                readFailure?.let { throw it }
                check(process.exitValue() == 0) { "$tool failed (${process.exitValue()}): ${Files.readString(errors)}" }
                return requireNotNull(result)
            } finally {
                if (process.isAlive) process.destroyForcibly()
                process.inputStream.close()
                reader.join(1000)
            }
        } finally { Files.deleteIfExists(errors) }
    }
}
