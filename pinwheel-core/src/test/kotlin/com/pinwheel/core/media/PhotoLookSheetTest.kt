package com.pinwheel.core.media

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Renders every built-in look through the real colour pipeline on five reference photos and checks
 * each keeps a usable picture. Renders are written to build/look-renders (PPM) for visual review.
 */
class PhotoLookSheetTest {
    private class Rgb(val w: Int, val h: Int, val px: IntArray)
    private val names = listOf("portrait", "street", "food", "landscape", "car")
    private val photos = names.map { read(javaClass.getResourceAsStream("/looks/$it.ppm")!!.readBytes()) }

    /** Minimal binary PPM (P6, 8-bit) reader. */
    private fun read(bytes: ByteArray): Rgb {
        var i = 0
        fun token(): String {
            while (bytes[i].toInt().toChar().isWhitespace()) i++
            if (bytes[i].toInt().toChar() == '#') { while (bytes[i].toInt() != '\n'.code) i++; return token() }
            val start = i; while (!bytes[i].toInt().toChar().isWhitespace()) i++
            return String(bytes, start, i - start)
        }
        check(token() == "P6"); val w = token().toInt(); val h = token().toInt(); token(); i++
        return Rgb(w, h, IntArray(w * h) { k -> val o = i + k * 3
            -0x1000000 or ((bytes[o].toInt() and 255) shl 16) or ((bytes[o + 1].toInt() and 255) shl 8) or (bytes[o + 2].toInt() and 255) })
    }

    private fun render(src: Rgb, look: PhotoLook): Rgb {
        val t = PhotoColorTransform(look.adjustments)
        return Rgb(src.w, src.h, IntArray(src.px.size) { k -> t.pixel(src.px[k], (k % src.w + .5f) / src.w, (k / src.w + .5f) / src.h) })
    }

    private fun write(file: File, img: Rgb) = file.outputStream().buffered().use { out ->
        out.write("P6\n${img.w} ${img.h}\n255\n".toByteArray())
        for (p in img.px) { out.write((p shr 16) and 255); out.write((p shr 8) and 255); out.write(p and 255) }
    }

    @Test fun everyLookKeepsAUsablePicture() {
        val bad = mutableListOf<String>()
        for (look in PhotoLooks.builtIn) for ((i, photo) in photos.withIndex()) {
            val img = render(photo, look)
            var sum = 0.0; var clipped = 0
            for (p in img.px) {
                val l = .2126 * ((p shr 16) and 255) + .7152 * ((p shr 8) and 255) + .0722 * (p and 255)
                sum += l; if (l < 3 || l > 252) clipped++
            }
            val mean = sum / img.px.size
            if (mean < 25 || mean > 235 || clipped > img.px.size * .35) bad += "${look.name} on ${names[i]} (mean ${mean.toInt()}, clipped ${clipped * 100 / img.px.size}%)"
        }
        assertTrue("Looks that wreck the picture:\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test fun magicEnhanceReadsScenesAndImproves() {
        val dir = File("build/enhance-renders").apply { deleteRecursively(); mkdirs() }
        val report = StringBuilder()
        photos.forEachIndexed { i, photo ->
            val r = PhotoMagicEnhance.enhance(photo.px, photo.w, photo.h, com.pinwheel.core.model.Adjustments())
            report.appendLine("${names[i]}	${r.analysis.scene}	${r.analysis}	${r.adjustments.exposure} ${r.adjustments.warmth} ${r.adjustments.vibrance}")
            write(File(dir, "$i-before.ppm"), photo)
            write(File(dir, "$i-after.ppm"), render(photo, PhotoLook("Enhance", "", r.adjustments)))
            // A dark photo must come out brighter; a bright one must not be pushed brighter.
            val before = photo.px.map { (it shr 8) and 255 }.average(); val after = render(photo, PhotoLook("", "", r.adjustments)).px.map { (it shr 8) and 255 }.average()
            if (before < 90) assertTrue("${names[i]} should brighten", after > before)
            if (before > 170) assertTrue("${names[i]} should not blow out", after < 230)
        }
        File(dir, "report.txt").writeText(report.toString())
        assertTrue(PhotoMagicEnhance.enhance(photos[0].px, photos[0].w, photos[0].h, com.pinwheel.core.model.Adjustments()).analysis.scene == PhotoMagicEnhance.Scene.PORTRAIT)
    }

    @Test fun lookNamesAreUnique() {
        val all = PhotoLooks.builtIn.map { it.name }
        assertTrue(all.groupBy { it }.filter { it.value.size > 1 }.keys.toString(), all.size == all.toSet().size)
    }

    @Test fun writeRenders() {
        val dir = File("build/look-renders").apply { deleteRecursively(); mkdirs() }
        File(dir, "index.txt").writeText(PhotoLooks.builtIn.mapIndexed { i, l -> "%03d\t%s\t%s".format(i, l.name, l.category) }.joinToString("\n"))
        PhotoLooks.builtIn.forEachIndexed { i, look -> photos.forEachIndexed { c, photo -> write(File(dir, "%03d-%d.ppm".format(i, c)), render(photo, look)) } }
    }
}
