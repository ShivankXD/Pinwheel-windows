package com.pinwheel.render.qa

import com.pinwheel.core.RgbaFrame
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

object FrameImages {
    fun buffered(frame: RgbaFrame) = BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_ARGB).apply {
        val argb = IntArray(frame.width * frame.height) { pixel ->
            val at = pixel * 4
            ((frame.pixels[at + 3].toInt() and 255) shl 24) or ((frame.pixels[at].toInt() and 255) shl 16) or
                ((frame.pixels[at + 1].toInt() and 255) shl 8) or (frame.pixels[at + 2].toInt() and 255)
        }
        setRGB(0, 0, frame.width, frame.height, argb, 0, frame.width)
    }
    fun write(frame: RgbaFrame, path: Path) {
        Files.createDirectories(path.parent); check(ImageIO.write(buffered(frame), "png", path.toFile()))
    }
}
