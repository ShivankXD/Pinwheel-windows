package com.pinwheel.render

import com.pinwheel.core.model.*
import kotlin.math.*

/** Legacy recipes preserve the mobile combined pass and separate Soft Glow stage. */
class LegacyEffects(private val device: AngleDevice, private val width: Int, private val height: Int,
    effects: List<VideoTimedEffect>) : AutoCloseable {
    private val effects = effects.map { it.sanitized() }
    private val resources = GpuResources()
    private val legacy = resources.own { FxProgram(highp(MobileLegacyShaders.LEGACY)) }
    private val threshold = resources.own { FxProgram(highp(MobileLegacyShaders.GLOW_THRESHOLD)) }
    private val blur = resources.own { FxProgram(highp(MobileLegacyShaders.GLOW_BLUR)) }
    private val blend = resources.own { FxProgram(highp(MobileLegacyShaders.GLOW_BLEND)) }
    private val legacyOutput = resources.own { GpuTarget(device, width, height) }
    private val output = resources.own { GpuTarget(device, width, height) }
    private val scale = min(.7f, 1024f / max(width, height))
    private val smallWidth = (width * scale).roundToInt().coerceAtLeast(1)
    private val smallHeight = (height * scale).roundToInt().coerceAtLeast(1)
    private val glow = Array(3) { resources.own { GpuTarget(device, smallWidth, smallHeight) } }
    init { resources.initialized() }

    fun beforeCatalog(input: GpuTexture, timeUs: Long): GpuTarget {
        device.checkThread(); legacyOutput.bind(); legacy.use(); legacy.float("uFlipY", 1f)
        legacy.texture("uTexture", input.id, 0); legacy.vec2("uSize", width.toFloat(), height.toFloat())
        legacy.float("uAspect", width.toFloat() / height); legacy.vec4("uAmounts", activeVideoEffectIntensities(effects, timeUs))
        val tilt = activeVideo3DTilt(effects, timeUs); legacy.vec2("uTilt", tilt[0], tilt[1])
        legacy.float("uSeed", ((timeUs.coerceAtLeast(0) * 30 / 1_000_000) % 4096).toFloat())
        legacy.draw(); checkGl("legacy effects"); return legacyOutput
    }
    fun afterCatalog(input: GpuTexture, timeUs: Long): GpuTarget {
        device.checkThread(); val amount = activeVideoSoftGlow(effects, timeUs)
        if (amount > 0f) {
            pass(threshold, input.id, glow[0])
            val sourceBase = max(min(width, height).toFloat(), max(width, height) / 2f)
            val count = 28.86f * .25118864f
            val radiusX = sourceBase / width * .05f * amount; val radiusY = sourceBase / height * .05f * amount
            blur.use(); blur.vec2("uStep", radiusX / count, 0f); blur.float("uSigma", max(radiusX / 2.5f, .00001f)); blur.float("uSamples", count)
            pass(blur, glow[0].texture.id, glow[1])
            blur.use(); blur.vec2("uStep", 0f, radiusY / count); blur.float("uSigma", max(radiusY / 2.5f, .00001f))
            pass(blur, glow[1].texture.id, glow[2])
        }
        output.bind(); blend.use(); blend.float("uFlipY", 1f); blend.texture("uTexture", input.id, 0)
        blend.texture("uGlow", if (amount > 0f) glow[2].texture.id else input.id, 1)
        blend.float("uExposure", if (amount > 0f) 2.4f * amount else 0f); blend.draw()
        checkGl("Soft Glow"); return output
    }
    private fun pass(p: FxProgram, input: Int, target: GpuTarget) {
        target.bind(); p.use(); p.float("uFlipY", 1f); p.texture("uTexture", input, 0); p.draw()
    }
    override fun close() {
        device.checkThread(); resources.close()
    }
    private fun highp(shader: String) = shader.replace("varying mediump", "varying highp")
}
