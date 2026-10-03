package com.pinwheel.render

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.model.StudioProject
import java.nio.file.Path

/** P2 texture stages shared by later P3/P5 callers; no decode, clock or encoding is hidden here. */
class EffectRuntime(nativeDirectory: Path, val width: Int, val height: Int, project: StudioProject,
    sharedDevice: AngleDevice? = null) : AutoCloseable {
    private val resources = GpuResources()
    private val device = sharedDevice ?: resources.own { AngleDevice(nativeDirectory) }
    val renderer get() = device.renderer
    val plan = EffectPlan.from(project)
    private val upload = resources.own { GpuTexture(device, width, height) }
    private val legacy = resources.own { LegacyEffects(device, width, height, plan.legacy) }
    private val main = resources.own { FxChain(device, width, height, plan.main) }
    private val transitions = resources.own { FxChain(device, width, height, plan.transitions) }
    private val overlays = resources.own { FxChain(device, width, height, plan.overlays) }
    private val compositor = resources.own { LayerCompositor(device, width, height) }
    private val layerUploads = plan.layerEffects.keys.associateWith { val (w, h) = LayerFx.size(width, height); resources.own { GpuTexture(device, w, h) } }
    private val layers = plan.layerEffects.mapValues { (layer, effects) -> val (w, h) = LayerFx.size(width, height); resources.own { LayerFx(device, w, h, layer, effects) } }
    init { resources.initialized() }

    /** Painted layers are straight-alpha, already transformed onto a canvas at LayerFx.size(). */
    fun render(frame: RgbaFrame, presentationTimeUs: Long, paintedLayers: Map<String, RgbaFrame> = emptyMap()): RgbaFrame {
        device.checkThread(); upload.upload(frame)
        return renderTexture(upload, presentationTimeUs, paintedLayers).read()
    }
    /** Borrowed GPU output remains valid until the next render; used by P3 without intermediate readback. */
    fun renderTexture(input: GpuTexture, presentationTimeUs: Long, paintedLayers: Map<String, RgbaFrame> = emptyMap()): GpuTarget {
        device.checkThread(); require(input.device === device && input.width == width && input.height == height)
        val old = legacy.beforeCatalog(input, presentationTimeUs)
        val fx = main.render(old.texture, presentationTimeUs)
        val glow = legacy.afterCatalog(fx.texture, presentationTimeUs)
        val cut = transitions.render(glow.texture, presentationTimeUs)
        val styled = overlays.render(cut.texture, presentationTimeUs)
        val outputs = layers.mapNotNull { (layer, fx) ->
            paintedLayers[layer.id]?.let { painted ->
                val texture = layerUploads.getValue(layer); texture.upload(painted, premultiply = true)
                fx.render(texture, presentationTimeUs)
            }
        }
        return compositor.render(styled, outputs)
    }
    fun resetHistory() { main.resetHistory(); transitions.resetHistory(); overlays.resetHistory() }
    /** P3 can borrow an existing RGBA8 target across inactive stages, retaining history updates. */
    fun renderTarget(input:GpuTarget,presentationTimeUs:Long,paintedLayers:Map<String,RgbaFrame> = emptyMap()):GpuTarget {
        device.checkThread();require(input.texture.device===device && input.texture.width==width && input.texture.height==height)
        fun active(effect:com.pinwheel.core.model.VideoTimedEffect)=effect.enabled &&
            presentationTimeUs>=effect.startMs*1000 && presentationTimeUs<effect.endMs*1000
        fun wanted(effects:List<com.pinwheel.core.model.VideoTimedEffect>)=effects.any { effect ->
            val spec=com.pinwheel.core.media.video.VideoFxCatalog.find(effect.kind)
            effect.enabled && spec!=null && (spec.history || active(effect))
        }
        var target=input
        if(plan.legacy.take(com.pinwheel.core.model.MAX_VIDEO_EFFECTS).any { active(it) && it.intensity>0f && it.kind in com.pinwheel.core.model.VIDEO_EFFECT_KINDS && it.kind!="Soft Glow" })
            target=legacy.beforeCatalog(target.texture,presentationTimeUs)
        if(wanted(plan.main))target=main.render(target.texture,presentationTimeUs)
        if(plan.legacy.take(com.pinwheel.core.model.MAX_VIDEO_EFFECTS).any { active(it) && it.intensity>0f && it.kind=="Soft Glow" })target=legacy.afterCatalog(target.texture,presentationTimeUs)
        if(wanted(plan.transitions))target=transitions.render(target.texture,presentationTimeUs)
        if(wanted(plan.overlays))target=overlays.render(target.texture,presentationTimeUs)
        val outputs=layers.mapNotNull { (layer,fx)->paintedLayers[layer.id]?.let { painted->
            val texture=layerUploads.getValue(layer);texture.upload(painted,premultiply=true);fx.render(texture,presentationTimeUs) } }
        return compositor.render(target,outputs)
    }
    override fun close() {
        device.checkThread(); resources.close()
    }
}
