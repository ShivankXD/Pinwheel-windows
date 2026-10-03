package com.pinwheel.render

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.model.VideoClipEdits
import com.pinwheel.core.model.VideoProjectEdits
import kotlin.math.pow
import kotlin.math.roundToInt

/** Rotation/mirror, crop, grade and canvas stages use the same snapshot for preview/export. */
internal class ClipGpuRenderer(private val device: AngleDevice, private val width: Int, private val height: Int) : AutoCloseable {
    private val resources = GpuResources()
    private val geometry = resources.own { FxProgram(GEOMETRY) }
    private val grade = resources.own { FxProgram(GRADE_FRAGMENT) }
    private val matrix = resources.own { FxProgram(MATRIX) }
    private val canvas = resources.own { FxProgram(CANVAS_FRAGMENT.replace("varying mediump", "varying highp")) }
    private val blurredCanvas = resources.own { FxProgram(BLUR_CANVAS_FRAGMENT.replace("varying mediump", "varying highp")) }
    private val blur = resources.own { FxProgram(BLUR_FRAGMENT.replace("varying mediump", "varying highp")) }
    private val output = resources.own { GpuTarget(device, width, height) }
    private var input: GpuTexture? = null
    private var crop: GpuTarget? = null
    private var graded: GpuTarget? = null
    private var horizontal: GpuTarget? = null
    private var vertical: GpuTarget? = null
    init { resources.initialized() }
    fun render(frame: RgbaFrame, raw: VideoClipEdits, edit: VideoProjectEdits, timeUs: Long, sourceRotation: Int = 0): GpuTarget {
        device.checkThread(); val e = raw.sanitized(); val rotation = (e.rotation + sourceRotation) % 360
        if (input?.let { it.width != frame.width || it.height != frame.height } != false) { input?.close(); input = GpuTexture(device, frame.width, frame.height) }
        val texture = input!!; texture.upload(frame)
        val rotatedW = if (rotation % 180 == 0) frame.width else frame.height
        val rotatedH = if (rotation % 180 == 0) frame.height else frame.width
        val w = (rotatedW / e.cropZoom).roundToInt().coerceAtLeast(2); val h = (rotatedH / e.cropZoom).roundToInt().coerceAtLeast(2)
        crop = target(crop, w, h); graded = target(graded, w, h)
        val geometryNeeded=rotation!=0 || e.mirrored || e.cropZoom!=1f
        if(geometryNeeded)pass(geometry, crop!!) { p -> p.texture("uTexture", texture.id, 0); p.float("uRotation", rotation / 90f)
            p.float("uMirror", if (e.mirrored) 1f else 0f); p.vec4("uCrop", floatArrayOf(e.cropX, e.cropY, 1f / e.cropZoom, 0f)) }
        val rawTexture=if(geometryNeeded)crop!!.texture else texture
        val source = if (e.needsGrade()) {
            pass(grade, graded!!) { p ->
                val recipe = VideoFilterCatalog.find(e.lookId)
                p.texture("uTexture", rawTexture.id, 0); p.vec2("uSize", w.toFloat(), h.toFloat()); p.float("uAspect", w.toFloat() / h)
                p.vec4("uF0", floatArrayOf(recipe?.exposure ?: 0f, recipe?.contrast ?: 0f, recipe?.saturation ?: 0f, recipe?.temperature ?: 0f))
                p.vec4("uF1", floatArrayOf(recipe?.tint ?: 0f, recipe?.fade ?: 0f, recipe?.vibrance ?: 0f, recipe?.curve ?: 0f))
                p.vec4("uF2", floatArrayOf(recipe?.mono ?: 0f, recipe?.hue ?: 0f, recipe?.split ?: 0f, recipe?.bleach ?: 0f))
                p.vec4("uShadow", rgb(recipe?.shadows?.toLong() ?: 0xff808080)); p.vec4("uHigh", rgb(recipe?.highlights?.toLong() ?: 0xff808080))
                p.float("uAmount", if (recipe == null) 0f else e.lookIntensity)
                p.vec4("uA0", floatArrayOf(e.exposure, e.brightness, e.contrast, e.saturation)); p.vec4("uA1", floatArrayOf(e.warmth, e.tint, e.highlights, e.shadows))
                p.vec4("uA2", floatArrayOf(e.fade, e.vibrance, e.sharpen, e.vignette)); p.vec4("uA3", floatArrayOf(e.grain, (timeUs / 33_333 % 997).toFloat(), 0f, 0f))
            }; graded!!.texture
        } else if(e.exposure!=0f || e.contrast!=0f || e.saturation!=0f || e.warmth!=0f || e.lookId!="original") {
            val m = colorMatrix(e)
            pass(matrix, graded!!) { p -> p.texture("uTexture", rawTexture.id, 0)
                p.vec4("uRow0", floatArrayOf(m[0], m[4], m[8], m[12])); p.vec4("uRow1", floatArrayOf(m[1], m[5], m[9], m[13])); p.vec4("uRow2", floatArrayOf(m[2], m[6], m[10], m[14])) }
            graded!!.texture
        } else rawTexture
        val inputAspect = w.toFloat() / h; val outputAspect = width.toFloat() / height
        val fit = if (outputAspect > inputAspect) floatArrayOf(outputAspect / inputAspect, 1f) else floatArrayOf(1f, inputAspect / outputAspect)
        val fill = if (outputAspect > inputAspect) floatArrayOf(1f, inputAspect / outputAspect) else floatArrayOf(outputAspect / inputAspect, 1f)
        if (edit.scaleMode == "Fit" && edit.backgroundMode == "Blur") {
            val strength = edit.backgroundBlur.coerceIn(0f, 1f)
            val workingSide = 256f - 192f * strength
            val factor = minOf(1f, workingSide / maxOf(w, h))
            val bw = (w * factor).roundToInt().coerceAtLeast(1); val bh = (h * factor).roundToInt().coerceAtLeast(1)
            horizontal = target(horizontal, bw, bh); vertical = target(vertical, bw, bh)
            pass(blur, horizontal!!) { p -> p.texture("uTexture", source.id, 0); p.vec2("uStep", 1f / bw, 0f) }
            pass(blur, vertical!!) { p -> p.texture("uTexture", horizontal!!.texture.id, 0); p.vec2("uStep", 0f, 1f / bh) }
            pass(blurredCanvas, output) { p -> p.texture("uTexture", source.id, 0); p.texture("uBlurred", if (strength == 0f) source.id else vertical!!.texture.id, 1)
                p.vec2("uFitScale", fit[0], fit[1]); p.vec2("uFillScale", fill[0], fill[1]) }
        } else pass(canvas, output) { p -> p.texture("uTexture", source.id, 0); val scale = if (edit.scaleMode == "Fill") fill else fit
            p.vec2("uUvScale", scale[0], scale[1]); val bg = rgb(edit.backgroundColor); org.lwjgl.opengles.GLES20.glUniform3f(org.lwjgl.opengles.GLES20.glGetUniformLocation(p.id,"uBackground"),bg[0],bg[1],bg[2]) }
        checkGl("clip geometry/grade/canvas"); return output
    }
    private fun target(old: GpuTarget?, w: Int, h: Int): GpuTarget {
        if (old != null && old.texture.width == w && old.texture.height == h) return old
        old?.close(); return GpuTarget(device, w, h)
    }
    private fun pass(p: FxProgram, to: GpuTarget, setup: (FxProgram) -> Unit) { to.bind(); p.use(); p.float("uFlipY", 1f); setup(p); p.draw() }
    override fun close() { device.checkThread(); vertical?.close(); horizontal?.close(); graded?.close(); crop?.close(); input?.close(); resources.close() }
    private fun rgb(color: Long) = floatArrayOf(((color shr 16) and 255) / 255f, ((color shr 8) and 255) / 255f, (color and 255) / 255f, 0f)
    companion object {
        private const val HEADER = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
varying highp vec2 vUv;
#else
precision mediump float;
varying mediump vec2 vUv;
#endif
uniform sampler2D uTexture;
"""
        private const val GEOMETRY = HEADER + """
uniform float uRotation; uniform float uMirror; uniform vec4 uCrop;
void main() {
  vec2 uv = vec2((1.0-uCrop.z)*uCrop.x, (1.0-uCrop.z)*(1.0-uCrop.y)) + vUv*uCrop.z;
  if (uRotation < 0.5) {} else if (uRotation < 1.5) uv = vec2(1.0-uv.y,uv.x);
  else if (uRotation < 2.5) uv = vec2(1.0-uv.x,1.0-uv.y); else uv = vec2(uv.y,1.0-uv.x);
  if (uMirror > 0.5) uv.x = 1.0-uv.x;
  gl_FragColor = texture2D(uTexture,uv);
}
"""
        private const val MATRIX = HEADER + """
uniform vec4 uRow0; uniform vec4 uRow1; uniform vec4 uRow2;
void main() { vec4 c = vec4(texture2D(uTexture,vUv).rgb,1.0); gl_FragColor=vec4(clamp(vec3(dot(uRow0,c),dot(uRow1,c),dot(uRow2,c)),0.0,1.0),1.0); }
"""
        // Copied from mobile VideoRenderGraph.colorMatrix, retaining legacy look behaviour.
        internal fun colorMatrix(edit: VideoClipEdits): FloatArray {
            val amount = edit.lookIntensity.coerceIn(0f, 1f)
            var saturation = 1f + edit.saturation.coerceIn(-1f, 1f); var warmth = edit.warmth.coerceIn(-1f, 1f)
            var contrast = edit.contrast.coerceIn(-1f, 1f); var lift = 0f
            when (edit.lookId) {
                "vivid" -> { saturation += .35f * amount; contrast += .15f * amount }
                "warm" -> { warmth += .7f * amount; saturation += .12f * amount }
                "cool" -> { warmth -= .7f * amount; contrast += .08f * amount }
                "cinema" -> { saturation -= .22f * amount; contrast += .23f * amount; warmth -= .12f * amount }
                "fade" -> { saturation -= .18f * amount; contrast -= .22f * amount; lift = .035f * amount }
                "mono" -> saturation *= 1f - amount
                "noir" -> { saturation *= 1f - amount; contrast += .38f * amount }
            }
            saturation = saturation.coerceIn(0f, 2.5f)
            val gain = 2f.pow(edit.exposure.coerceIn(-1f, 1f) * 2f); val factor = 1f + contrast.coerceIn(-1f, 1f) * .85f
            val channel = floatArrayOf(1f + warmth * .15f, 1f + warmth * .015f, 1f - warmth * .15f); val luma = floatArrayOf(.2126f,.7152f,.0722f)
            return FloatArray(16).apply {
                for (row in 0..2) { for (col in 0..2) this[col*4+row] = ((1f-saturation)*luma[col]+if(row==col) saturation else 0f)*gain*factor*channel[row]; this[12+row]=.18f*(1f-factor)+lift }
                this[15]=1f
            }
        }
    }
}
