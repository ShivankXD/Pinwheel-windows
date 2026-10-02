package com.pinwheel.core.media

import com.pinwheel.core.model.Adjustments
import com.pinwheel.core.model.MaskShape
import com.pinwheel.core.model.PhotoMask
import kotlin.math.*

data class PhotoPoint(val x: Float, val y: Float)

/** Matches affine and projective geometry in PhotoEngine, before and after cropping. */
class PhotoCoordinates(private val sourceWidth: Int, private val sourceHeight: Int, a: Adjustments) {
    private val perspective=PhotoPerspective(a.perspectiveHorizontal,a.perspectiveVertical)
    private val lens=PhotoLens(sourceWidth,sourceHeight,a.lensDistortion,a.lensProfileId,a.lensProfileEnabled,a.lensProfileCrop)
    private val rotation = ((a.rotation % 360) + 360) % 360
    private val width = if (rotation % 180 == 0) sourceWidth else sourceHeight
    private val height = if (rotation % 180 == 0) sourceHeight else sourceWidth
    private val crop = PhotoEngine.cropBounds(width, height, a)
    private val radians = Math.toRadians(a.straighten.toDouble())
    private val c = cos(radians).toFloat()
    private val s = sin(radians).toFloat()
    private val cover = max(abs(c) + abs(s) * height / width, abs(c) + abs(s) * width / height)
    private val sx = if (a.flipHorizontal) -cover else cover
    private val sy = if (a.flipVertical) -cover else cover

    fun toFrame(nx: Float, ny: Float): PhotoPoint {
        val corrected=lens.forward(nx,ny)
        val x = corrected.x * sourceWidth
        val y = corrected.y * sourceHeight
        val rotated = when (rotation) {
            90 -> PhotoPoint(sourceHeight - y, x)
            180 -> PhotoPoint(sourceWidth - x, sourceHeight - y)
            270 -> PhotoPoint(y, sourceWidth - x)
            else -> PhotoPoint(x, y)
        }
        val dx = (rotated.x - width / 2f) * sx
        val dy = (rotated.y - height / 2f) * sy
        val point=perspective.forward((c*dx-s*dy+width/2f)/width,(s*dx+c*dy+height/2f)/height)
        return PhotoPoint((point.x*width-crop.left)/crop.width(),(point.y*height-crop.top)/crop.height())
    }

    fun toSource(nx:Float,ny:Float):PhotoPoint {
        val output=FloatArray(2);mapSource(nx,ny,output,0);return PhotoPoint(output[0],output[1])
    }
    fun mapRow(count:Int,y:Float,output:FloatArray) {
        for(x in 0 until count)mapSource((x+.5f)/count,y,output,x*2)
    }
    private fun mapSource(nx:Float,ny:Float,output:FloatArray,index:Int) {
        val px=(nx*crop.width()+crop.left)/width-.5f
        val py=(ny*crop.height()+crop.top)/height-.5f
        val denominator=perspective.cover-perspective.horizontal*px-perspective.vertical*py
        val dx=px/denominator*width
        val dy=py/denominator*height
        val x=(c*dx+s*dy)/sx+width/2f
        val y=(-s*dx+c*dy)/sy+height/2f
        when(rotation) {
            90->{output[index]=y/sourceWidth;output[index+1]=1f-x/sourceHeight}
            180->{output[index]=1f-x/sourceWidth;output[index+1]=1f-y/sourceHeight}
            270->{output[index]=1f-y/sourceWidth;output[index+1]=x/sourceHeight}
            else->{output[index]=x/sourceWidth;output[index+1]=y/sourceHeight}
        }
        if(lens.active)lens.mapInverse(output[index],output[index+1],output,index)
    }

}

/** Smooth feathering; no segmentation model or image upload. */
class MaskWeight(private val mask: PhotoMask) {
    private val c = cos(Math.toRadians(mask.angle.toDouble())).toFloat()
    private val s = sin(Math.toRadians(mask.angle.toDouble())).toFloat()
    private val inverseX=1f/mask.radiusX.coerceAtLeast(.015f)
    private val inverseY=1f/mask.radiusY.coerceAtLeast(.015f)
    private val brush = if (mask.shape == MaskShape.BRUSH) BrushMaskIndex(mask.strokes, mask.feather) else null

    fun at(x: Float, y: Float): Float {
        if (!mask.enabled) return 0f
        brush?.let { val value = it.at(x,y); return if(mask.inverted)1f-value else value }
        val dx = x - mask.centerX
        val dy = y - mask.centerY
        val value = if (mask.shape == MaskShape.RADIAL) {
            val xx=dx*inverseX;val yy=dy*inverseY
            val square=xx*xx+yy*yy
            if(square>=1f)0f else ((1f-sqrt(square))/mask.feather.coerceAtLeast(.001f)).coerceIn(0f,1f)
        } else {
            val projection = dx * c + dy * s
            (.5f - projection / (2 * mask.radiusX.coerceAtLeast(.015f) *
                (.05f + .95f * mask.feather))).coerceIn(0f, 1f)
        }
        val smooth = value * value * (3f - 2f * value)
        return if (mask.inverted) 1f - smooth else smooth
    }
}

class LocalMaskRenderer(masks: List<PhotoMask>, private val cachedWeights: List<FloatArray>? = null) {
    private data class Layer(val weight: MaskWeight, val color: PhotoColorTransform?, val clarity:Float)
    private val layers = masks.filter {it.enabled && it.hasAdjustments()}.map {
        Layer(MaskWeight(it),if(it.exposure!=0f || it.contrast!=0f || it.warmth!=0f || it.saturation!=0f || it.highlights!=0f || it.shadows!=0f || it.tint!=0f)
            PhotoColorTransform(Adjustments(exposure=it.exposure,contrast=1f+it.contrast,warmth=it.warmth,saturation=1f+it.saturation,highlights=it.highlights,shadows=it.shadows,tint=it.tint))else null,it.clarity)
    }
    val active get() = layers.isNotEmpty()
    val hasClarity = layers.any{it.clarity!=0f}
    fun clarityAt(x:Float,y:Float):Float = layers.sumOf {if(it.clarity==0f)0.0 else (it.weight.at(x,y)*it.clarity).toDouble()}.toFloat().coerceIn(-1f,1f)

    fun pixel(original: Int, sourceX: Float, sourceY: Float, pixelIndex: Int = -1): Int {
        var result = original
        for ((index, layer) in layers.withIndex()) {
            val color=layer.color?:continue
            val weight = if (cachedWeights != null && pixelIndex >= 0) cachedWeights[index][pixelIndex]
                else layer.weight.at(sourceX, sourceY)
            if (weight < .001f) continue
            val changed = color.pixel(result, sourceX, sourceY)
            var output = result and -0x1000000
            for (channel in 2 downTo 0) {
                val shift = channel * 8
                val before = (result ushr shift) and 255
                val after = (changed ushr shift) and 255
                output = output or ((before + (after - before) * weight).roundToInt().coerceIn(0, 255) shl shift)
            }
            result = output
        }
        return result
    }
}

fun PhotoMask.hasAdjustments() = exposure!=0f || contrast!=0f || warmth!=0f || saturation!=0f || highlights!=0f || shadows!=0f || tint!=0f || clarity!=0f
