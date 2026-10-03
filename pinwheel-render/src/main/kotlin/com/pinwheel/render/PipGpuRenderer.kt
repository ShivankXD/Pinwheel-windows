package com.pinwheel.render

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.model.VideoImageOverlay
import com.pinwheel.core.model.VideoTimedEffect
import org.jetbrains.skia.*
import kotlin.math.*

/** PIP masks are painted once like mobile; geometry and straight-alpha blending stay on GPU. */
internal class PipGpuRenderer(private val device: AngleDevice, private val width: Int, private val height: Int,
    private val layer: VideoImageOverlay, private val effects: List<VideoTimedEffect>) : AutoCloseable {
    private val resources = GpuResources()
    private val program = resources.own { FxProgram(FRAGMENT) }
    private val output = resources.own { GpuTarget(device, width, height) }
    private var input: GpuTexture? = null
    private var mask: GpuTexture? = null
    private var chain: FxChain? = null
    init { resources.initialized() }
    fun prepareMask(frameWidth:Int,frameHeight:Int) {
        device.checkThread();require(frameWidth>0 && frameHeight>0)
        val factor=minOf(1f,1024f/maxOf(frameWidth,frameHeight))
        val mw=(frameWidth*factor).toInt().coerceAtLeast(1);val mh=(frameHeight*factor).toInt().coerceAtLeast(1)
        if(mask?.let { it.width==mw && it.height==mh }==true)return
        mask?.close();mask=GpuTexture(device,mw,mh).also { it.upload(maskPixels(layer.mask,mw,mh)) }
    }
    fun render(frame: RgbaFrame, timeUs: Long): GpuTarget {
        device.checkThread()
        if (input?.let { it.width != frame.width || it.height != frame.height } != false) {
            input?.close(); chain?.close()
            input = GpuTexture(device, frame.width, frame.height)
            prepareMask(frame.width,frame.height)
            chain = if (effects.isEmpty()) null else FxChain(device, frame.width, frame.height, effects)
        }
        input!!.upload(frame)
        val texture = chain?.render(input!!, timeUs)?.texture ?: input!!
        val pose = overlayPose(layer, timeUs)
        val radians = (layer.rotation + pose[3]) * PI.toFloat() / 180
        val size = width * layer.width * pose[2]
        output.bind(); program.use(); program.float("uFlipY", 1f)
        program.texture("uTexture", texture.id, 0); program.texture("uMask", mask!!.id, 1)
        program.vec2("uCanvas", width.toFloat(), height.toFloat()); program.vec2("uCenter", (layer.x + pose[1]).coerceIn(0f,1f),layer.y)
        program.vec2("uSize", size.coerceAtLeast(.001f), (size * frame.height / frame.width).coerceAtLeast(.001f))
        program.vec2("uRotation", cos(radians),sin(radians)); program.float("uMirror", if(layer.mirrored) 1f else 0f)
        program.float("uOpacity", layer.opacity * pose[0]); program.draw(); checkGl("PIP mask/transform")
        return output
    }
    fun resetHistory() { chain?.resetHistory() }
    override fun close() { device.checkThread(); chain?.close(); mask?.close(); input?.close(); resources.close() }
    companion object {
        private const val FRAGMENT = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
varying highp vec2 vUv;
#else
precision mediump float;
varying mediump vec2 vUv;
#endif
uniform sampler2D uTexture; uniform sampler2D uMask;
uniform vec2 uCanvas; uniform vec2 uCenter; uniform vec2 uSize; uniform vec2 uRotation;
uniform float uOpacity; uniform float uMirror;
void main() {
  vec2 delta = (vec2(vUv.x,1.0-vUv.y)-uCenter)*uCanvas;
  vec2 point = vec2(delta.x*uRotation.x+delta.y*uRotation.y,-delta.x*uRotation.y+delta.y*uRotation.x);
  vec2 uv = point/uSize+0.5;
  if (uMirror>0.5) uv.x=1.0-uv.x;
  if (uv.x<0.0 || uv.x>1.0 || uv.y<0.0 || uv.y>1.0) { gl_FragColor=vec4(0.0); return; }
  vec2 sampleUv = vec2(uv.x,1.0-uv.y);
  vec4 color=texture2D(uTexture,sampleUv);
  color.a*=texture2D(uMask,sampleUv).a*uOpacity;
  gl_FragColor=color;
}
"""
        internal fun maskPixels(shape: String, w: Int, h: Int): RgbaFrame {
            Surface.makeRasterN32Premul(w,h).use { surface ->
                surface.canvas.clear(0)
                Paint().use { paint ->
                    paint.color = -1; paint.isAntiAlias = true
                    val cx=w*.5f; val cy=h*.5f; val s=minOf(w,h)*.5f
                    Path().use { path ->
                        when(shape) {
                            "Circle" -> path.addCircle(cx,cy,s)
                            "Rounded" -> path.addRRect(RRect.makeXYWH(0f,0f,w.toFloat(),h.toFloat(),s*.25f))
                            "Diamond" -> { path.moveTo(cx,cy-s); path.lineTo(cx+s,cy);path.lineTo(cx,cy+s);path.lineTo(cx-s,cy);path.closePath() }
                            "Star" -> { for(i in 0 until 10) { val a=-PI/2+i*PI/5;val r=if(i%2==0)s else s*.45f;val x=cx+(cos(a)*r).toFloat();val y=cy+(sin(a)*r).toFloat();if(i==0)path.moveTo(x,y) else path.lineTo(x,y) };path.closePath() }
                            "Heart" -> { path.moveTo(cx,cy+s*.9f);path.cubicTo(cx-s*1.5f,cy-s*.1f,cx-s*.9f,cy-s*1.2f,cx,cy-s*.45f);path.cubicTo(cx+s*.9f,cy-s*1.2f,cx+s*1.5f,cy-s*.1f,cx,cy+s*.9f);path.closePath() }
                            else -> path.addRect(Rect.makeWH(w.toFloat(),h.toFloat()))
                        }
                        surface.canvas.drawPath(path,paint)
                    }
                }
                surface.makeImageSnapshot().use { image -> Bitmap().use { bitmap ->
                    check(bitmap.allocPixels(ImageInfo(w,h,ColorType.RGBA_8888,ColorAlphaType.UNPREMUL)));check(image.readPixels(bitmap))
                    return RgbaFrame(w,h,requireNotNull(bitmap.readPixels(bitmap.imageInfo,w*4,0,0)))
                } }
            }
        }
    }
}
