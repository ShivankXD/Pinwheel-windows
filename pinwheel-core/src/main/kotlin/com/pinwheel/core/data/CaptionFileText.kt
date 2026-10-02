package com.pinwheel.core.data

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** Subtitle input is bounded before decoding; invalid bytes never silently become replacement characters. */
object CaptionFileText {
    const val MAX_BYTES = 2 * 1024 * 1024

    fun read(input: InputStream): String {
        val output = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        while (output.size() <= MAX_BYTES) {
            val count = input.read(chunk, 0, minOf(chunk.size, MAX_BYTES + 1 - output.size()))
            if (count < 0) break
            if (count == 0) {
                val single = input.read()
                if (single < 0) break
                output.write(single)
                continue
            }
            output.write(chunk, 0, count)
        }
        val bytes = output.toByteArray()
        require(bytes.size <= MAX_BYTES) { "Subtitle files must be smaller than 2 MB." }
        return decode(bytes)
    }

    fun decode(bytes: ByteArray): String {
        require(bytes.size <= MAX_BYTES) { "Subtitle files must be smaller than 2 MB." }
        val (charset, skip) = when {
            bytes.size >= 2 && bytes[0] == 0xff.toByte() && bytes[1] == 0xfe.toByte() -> Charsets.UTF_16LE to 2
            bytes.size >= 2 && bytes[0] == 0xfe.toByte() && bytes[1] == 0xff.toByte() -> Charsets.UTF_16BE to 2
            bytes.size >= 3 && bytes[0] == 0xef.toByte() && bytes[1] == 0xbb.toByte() && bytes[2] == 0xbf.toByte() -> Charsets.UTF_8 to 3
            else -> Charsets.UTF_8 to 0
        }
        return try {
            charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes, skip, bytes.size - skip)).toString()
        } catch (_: java.nio.charset.CharacterCodingException) {
            throw IllegalArgumentException("Save the subtitle file as UTF-8 or UTF-16 with a byte-order mark, then try again.")
        }
    }
}
