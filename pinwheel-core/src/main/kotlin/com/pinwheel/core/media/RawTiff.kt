package com.pinwheel.core.media
import com.pinwheel.core.model.Adjustments
import com.pinwheel.core.model.PhotoExportOptions
import com.pinwheel.core.model.RawDevelopment
import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

object RawTiff {
    fun eligibilityIssue(a: Adjustments): String? = if (a.rotation % 90 != 0) "Rotation must be a multiple of 90 degrees." else null

}

object Tiff16Writer {
    private data class Field(val tag: Int, val type: Int, val count: Int, val data: ByteArray)
    private fun shorts(vararg values: Int) = ByteBuffer.allocate(values.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        .apply { values.forEach { putShort(it.toShort()) } }.array()
    private fun longs(vararg values: Int) = ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        .apply { values.forEach { putInt(it) } }.array()

    fun write(master: RawLinearImage, development: RawDevelopment, profile: ByteArray, output: OutputStream,
              onProgress: (Int) -> Unit = {}, checkCancellation: () -> Unit = {}) {
        val transform = RawLinearTransform(development)
        val tables = Array(3) { channel -> IntArray(65536) { n ->
            if (n % 4096 == 0) checkCancellation()
            (RawLinearTransform.toSrgb(transform.channel(n / 65535.0, channel)).coerceIn(0.0, 1.0) * 65535).roundToInt()
        } }
        writeRows(master.width,master.height,profile,output,{y,row ->
            var index=0
            for(x in 0 until master.width)for(channel in 0..2) {
                val encoded=tables[channel][master.sample(x,y,channel)]
                row[index++]=encoded.toByte();row[index++]=(encoded ushr 8).toByte()
            }
        },onProgress,checkCancellation)
    }

    fun writeRows(width:Int,height:Int,profile:ByteArray,output:OutputStream,
        renderRow:(Int,ByteArray)->Unit,onProgress:(Int)->Unit={},checkCancellation:()->Unit={}) {
        require(width>0 && height>0)
        checkCancellation()
        require(profile.size in 128..1_048_576 && String(profile, 36, 4, Charsets.US_ASCII) == "acsp") {
            "A valid sRGB ICC profile is required."
        }
        val byteCount = width.toLong() * height * 6
        require(byteCount < Int.MAX_VALUE - profile.size - 4096) { "This image is too large for TIFF export." }
        val software = "Pinwheel\u0000".toByteArray(Charsets.US_ASCII)
        val fields = listOf(
            Field(256, 4, 1, longs(width)), Field(257, 4, 1, longs(height)),
            Field(258, 3, 3, shorts(16, 16, 16)), Field(259, 3, 1, shorts(1)),
            Field(262, 3, 1, shorts(2)), Field(273, 4, 1, longs(0)),
            Field(274, 3, 1, shorts(1)), Field(277, 3, 1, shorts(3)),
            Field(278, 4, 1, longs(height)), Field(279, 4, 1, longs(byteCount.toInt())),
            Field(282, 5, 1, longs(72, 1)), Field(283, 5, 1, longs(72, 1)),
            Field(284, 3, 1, shorts(1)), Field(296, 3, 1, shorts(2)),
            Field(305, 2, software.size, software), Field(339, 3, 3, shorts(1, 1, 1)),
            Field(34675, 7, profile.size, profile)
        )
        val directoryEnd = 8 + 2 + fields.size * 12 + 4
        val pixelOffset = directoryEnd + fields.filter { it.data.size > 4 }.sumOf { (it.data.size + 1) and -2 }
        val header = ByteBuffer.allocate(pixelOffset).order(ByteOrder.LITTLE_ENDIAN)
        header.put('I'.code.toByte()).put('I'.code.toByte()).putShort(42).putInt(8)
        header.putShort(fields.size.toShort())
        var offset = directoryEnd
        for (field in fields) {
            header.putShort(field.tag.toShort()).putShort(field.type.toShort()).putInt(field.count)
            if (field.tag == 273) header.putInt(pixelOffset)
            else if (field.data.size <= 4) {
                header.put(field.data); repeat(4 - field.data.size) { header.put(0) }
            } else {
                header.putInt(offset)
                System.arraycopy(field.data, 0, header.array(), offset, field.data.size)
                offset += (field.data.size + 1) and -2
            }
        }
        header.putInt(0)
        checkCancellation(); output.write(header.array())
        val row=ByteArray(width*6)
        for(y in 0 until height) {
            checkCancellation();renderRow(y,row);output.write(row)
            onProgress(((y+1L)*100/height).toInt())
        }
        checkCancellation()
    }
}
