package com.pinwheel.render

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.media.video.VideoPreviewRecipe
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.roundToInt
import org.jetbrains.skia.*

/** Bitmap.createBitmap's crop-local matrix and mapped bounds, including Float rounding. */
internal object PreviewSamples {
    data class Mapping(val crop: VideoPreviewRecipe.Crop, val scaleX: Float, val scaleY: Float,
        val mappedWidth: Float, val mappedHeight: Float) {
        val width get() = mappedWidth.roundToInt()
        val height get() = mappedHeight.roundToInt()
    }

    fun mapping(width: Int, height: Int): Mapping {
        val crop = VideoPreviewRecipe.crop(width, height)
        val scaleX = VideoPreviewRecipe.WIDTH * 2f / crop.width
        val scaleY = -VideoPreviewRecipe.HEIGHT * 2f / crop.height
        return Mapping(crop, scaleX, scaleY, crop.width * scaleX, -crop.height * scaleY)
    }

    /** Returned row zero is the flipped bitmap's first row, uploaded unchanged like GLUtils.texImage2D. */
    fun read(assets: Path, name: String): RgbaFrame {
        Image.makeFromEncoded(Files.readAllBytes(assets.resolve(name))).use { decoded ->
            val m = mapping(decoded.width, decoded.height); val crop = m.crop
            Surface.makeRasterN32Premul(m.width, m.height).use { surface ->
                surface.canvas.translate(0f, m.mappedHeight)
                surface.canvas.scale(m.scaleX, m.scaleY)
                surface.canvas.drawImageRect(decoded,
                    Rect.makeXYWH(crop.x.toFloat(), crop.y.toFloat(), crop.width.toFloat(), crop.height.toFloat()),
                    Rect.makeWH(crop.width.toFloat(), crop.height.toFloat()),
                    // Android SkiaCanvas::drawBitmap uses kFast_SrcRectConstraint.
                    FilterMipmap(FilterMode.LINEAR, MipmapMode.NONE), null, false)
                surface.makeImageSnapshot().use { tile ->
                    Bitmap().use { pixels ->
                        check(pixels.allocPixels(ImageInfo(m.width, m.height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL)))
                        check(tile.readPixels(pixels))
                        return RgbaFrame(m.width, m.height, requireNotNull(pixels.readPixels(pixels.imageInfo, m.width * 4, 0, 0)))
                    }
                }
            }
        }
    }
}
