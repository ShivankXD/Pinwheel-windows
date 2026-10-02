package com.pinwheel.render

import com.pinwheel.core.RgbaFrame
import com.pinwheel.core.model.StudioProject
import java.nio.file.Path

/** P2 texture stages shared by later P3/P5 callers; no decode, clock or encoding is hidden here. */
class EffectRuntime(nativeDirectory: Path, val width: Int, val height: Int, project: StudioProject) : AutoCloseable {
    private val resources = GpuResources()
    private val device = resources.own { AngleDevice(nativeDirectory) }
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
        val old = legacy.beforeCatalog(upload, presentationTimeUs)
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
        return compositor.render(styled, outputs).read()
    }
    fun resetHistory() { main.resetHistory(); transitions.resetHistory(); overlays.resetHistory() }
    override fun close() {
        device.checkThread(); resources.close()
    }
}
