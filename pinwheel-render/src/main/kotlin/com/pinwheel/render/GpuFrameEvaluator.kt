package com.pinwheel.render

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.model.*
import com.pinwheel.media.*
import java.nio.file.Path

/** One frame path for preview/export. Construction, use and close stay on the render worker. */
class GpuFrameEvaluator(private val nativeDirectory: Path, override val project: StudioProject,
    val width: Int, val height: Int, val mode: RenderMode = RenderMode.PREVIEW,
    factory: MediaDecoderFactory = LibavDecoderFactory(nativeDirectory),
    private val onTiming: ((String,Double)->Unit)?=null) : VideoFrameEvaluator {
    init { require(width in 2..8192 && height in 2..8192);require(project.clips.isNotEmpty()) }
    private val resources = GpuResources()
    private val sources = resources.own { VideoFrameSource(factory, DecodeConfig(maxVideoEdge = if(mode==RenderMode.PREVIEW) maxOf(width,height) else 0,
        videoQueueFrames=2,maxQueuedBytes=32L*1024*1024,decodeAudio=false),maxOpen=8) }
    init {
        try {
            val first=project.clips.first();sources.prefetch(first.uri,first.startMs*1000,first.id)
            project.video.images.filter { it.video!=null && it.startMs<=1000 }.sortedBy { it.startMs }.forEach {
                sources.prefetch(it.uri,it.video!!.startMs*1000,"pip:"+it.id)
            }
        } catch(failure:Throwable) { resources.close();throw failure }
    }
    private val device = resources.own { AngleDevice(nativeDirectory) }
    private val clips = resources.own { ClipGpuRenderer(device,width,height) }
    private val compositor = resources.own { LayerCompositor(device,width,height) }
    private val motion = VideoClipMotion(project.clips)
    private val movement = resources.own { FxProgram(MOTION) }
    private val moved = resources.own { GpuTarget(device,width,height) }
    private val black = resources.own { GpuTarget(device,width,height) }
    private val effects = resources.own { EffectRuntime(nativeDirectory,width,height,project,device) }
    private val pip = project.video.images.filter { it.video != null }.associateWith { layer -> resources.own {
        PipGpuRenderer(device,width,height,layer,project.video.effects.filter { it.target==layer.id }) } }
    private val ordinaryPhotos = project.video.images.filter { it.video==null && !it.uri.startsWith("overlay:") && !it.uri.startsWith("sticker:") }
        .associateWith { layer -> resources.own { PipGpuRenderer(device,width,height,layer,emptyList()) } }
    private val spans = buildList { var at=0L;project.clips.forEach { clip -> val end=at+clip.durationMs*1000;add(Triple(at,end,clip));at=end } }
    private var lastTime = Long.MIN_VALUE
    val renderer get() = device.renderer
    val decodedSources get() = sources.descriptions
    init {
        resources.initialized()
        try {
            pip.forEach { (layer,renderer) -> layer.video!!.let { source ->
                if(layer.startMs<=1000 && source.width>0 && source.height>0)renderer.prepareMask(source.width,source.height)
            } }
        } catch(failure:Throwable) { resources.close();throw failure }
    }
    private inline fun <T> timed(stage:String,block:()->T):T {
        val observer=onTiming ?: return block()
        val start=System.nanoTime();val result=block();observer(stage,(System.nanoTime()-start)/1e6);return result
    }
    override fun render(timeUs: Long): RgbaFrame = timed("total") { val target=renderTexture(timeUs);timed("readback") { target.read() } }
    /** Borrowed output texture can be consumed by a later encoder without duplicating the recipe. */
    fun renderTexture(timeUs: Long, paintedLayers: Map<String,RgbaFrame> = emptyMap()): GpuTarget {
        device.checkThread();require(timeUs>=0 && timeUs<project.durationMs*1000)
        spans.firstOrNull { (start,_,_) -> start>timeUs && start<=timeUs+1_000_000 }?.let { (_,_,clip) -> sources.prefetch(clip.uri,clip.startMs*1000,clip.id) }
        pip.keys.filter { it.startMs*1000>timeUs && it.startMs*1000<=timeUs+1_000_000 }.sortedBy { it.startMs }.forEach {
            sources.prefetch(it.uri,it.video!!.startMs*1000,"pip:"+it.id)
        }
        if (lastTime != Long.MIN_VALUE && (timeUs<lastTime || timeUs-lastTime>250_000)) resetHistory()
        lastTime=timeUs
        val span=spans.firstOrNull { timeUs>=it.first && timeUs<it.second }
        val base=if(span==null) { black.bind(); org.lwjgl.opengles.GLES20.glClearColor(0f,0f,0f,1f); org.lwjgl.opengles.GLES20.glClear(org.lwjgl.opengles.GLES20.GL_COLOR_BUFFER_BIT);black }
            else {
                val (start,_,clip)=span;val description=sources.description(clip.uri,clip.id)
                val sourceTime=if(description.still)0 else clip.startMs*1000+((timeUs-start)*clip.playbackSpeed.toDouble()).toLong()
                clips.render(sources.frame(clip.uri,sourceTime,identity=clip.id),clip.video,project.video,timeUs,description.rotationDegrees)
            }
        // Media3 composites its input sequences before composition-level motion/FX.
        val moving=pip.mapNotNull { (layer,renderer) ->
            if(timeUs<layer.startMs*1000 || timeUs>=layer.endMs*1000 || layer.opacity<=0f) null else {
                val source=layer.video!!; val local=source.startMs*1000+timeUs-layer.startMs*1000
                if(local>=source.durationMs*1000) null else {
                    val frame=timed("pip-source") { sources.frame(layer.uri,local,identity="pip:"+layer.id) }
                    timed("pip-render") { renderer.render(frame,timeUs) }
                }
            }
        }
        val composited=timed("composite") { compositor.render(base,moving) }
        val pose=motion.at(timeUs)
        val movingCanvas=if(pose.scale==1f && pose.x==0f && pose.y==0f)composited else {
            moved.bind();movement.use();movement.float("uFlipY",1f);movement.texture("uTexture",composited.texture.id,0)
            movement.vec4("uPose",floatArrayOf(pose.scale,pose.x,pose.y,0f));movement.draw();moved }
        val styled=effects.renderTarget(movingCanvas,timeUs,paintedLayers)
        // Preview hosts paint plain layers live; targeted painted layers above share P2's coverage pass.
        if(mode==RenderMode.PREVIEW) return styled
        val plain=ordinaryPhotos.filterKeys { timeUs>=it.startMs*1000 && timeUs<it.endMs*1000 && it.opacity>0f &&
            it.id !in effects.plan.layerEffects.keys.map { key->key.id } }.map { (layer,renderer)->renderer.render(sources.frame(layer.uri,0),timeUs) }
        return compositor.render(styled,plain)
    }
    override fun resetHistory() { device.checkThread();effects.resetHistory();pip.values.forEach { it.resetHistory() } }
    override fun close() { device.checkThread();resources.close() }
    companion object {
        private const val MOTION="""
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float; varying highp vec2 vUv;
#else
precision mediump float; varying mediump vec2 vUv;
#endif
uniform sampler2D uTexture; uniform vec4 uPose;
void main() { vec2 uv=(vUv-0.5-uPose.yz*0.5)/uPose.x+0.5;
  gl_FragColor=(uv.x<0.0||uv.x>1.0||uv.y<0.0||uv.y>1.0)?vec4(0.0,0.0,0.0,1.0):texture2D(uTexture,uv); }
"""
        fun previewSize(aspect: Float, shortSide: Int=720): Pair<Int,Int> {
            require(aspect.isFinite() && aspect>0f && shortSide in 2..2160)
            fun even(v: Float)=(kotlin.math.round(v/2)*2).toInt().coerceAtLeast(2)
            return if(aspect>=1f) even(shortSide*aspect) to shortSide else shortSide to even(shortSide/aspect)
        }
    }
}
