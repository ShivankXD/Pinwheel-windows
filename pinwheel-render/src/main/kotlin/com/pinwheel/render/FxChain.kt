package com.pinwheel.render

import com.pinwheel.core.media.video.*
import com.pinwheel.core.model.*
import kotlin.math.abs

/** Mobile VideoFxEffect's ordered passes and untouched-input history. One chain per graph stage. */
class FxChain(private val device: AngleDevice, private val width: Int, private val height: Int,
    effects: List<VideoTimedEffect>) : AutoCloseable {
    private val effects = effects.map { it.sanitized() }.filter { it.enabled && VideoFxCatalog.find(it.kind) != null }.take(MAX_VIDEO_EFFECTS)
    private val programs = HashMap<String, FxProgram>()
    private val copy = FxProgram(FxProgram.COPY)
    private val trail = FxProgram(FxProgram.TRAIL)
    private val needsHistory = this.effects.any { VideoFxCatalog.find(it.kind)?.history == true }
    private val work = Array(if (needsHistory) 5 else 2) { GpuTarget(device, width, height) }
    private val output = GpuTarget(device, width, height)
    private var trailIndex = 3
    private var lastTimeUs = Long.MIN_VALUE
    var historyResets = 0
        private set

    fun resetHistory() { device.checkThread(); lastTimeUs = Long.MIN_VALUE }
    fun render(input: GpuTexture, presentationTimeUs: Long): GpuTarget {
        device.checkThread(); require(input.device === device && input.width == width && input.height == height)
        val timeMs = presentationTimeUs / 1000
        val active = effects.filter { timeMs >= it.startMs && timeMs < it.endMs }
        val jumped = lastTimeUs == Long.MIN_VALUE || presentationTimeUs < lastTimeUs || abs(presentationTimeUs - lastTimeUs) > 250_000
        if (needsHistory && jumped) {
            for (i in 2..4) blit(input.id, work[i]); trailIndex = 3; historyResets++
        }
        lastTimeUs = presentationTimeUs
        var source = input.id
        if (active.isEmpty()) blit(source, output)
        for ((index, effect) in active.withIndex()) {
            val spec = requireNotNull(VideoFxCatalog.find(effect.kind))
            val target = if (index == active.lastIndex) output else work[index % 2]
            target.bind()
            val p = programs.getOrPut(spec.id) { FxProgram(FxProgram.source(spec)) }; p.use()
            p.texture("uTexture", source, 0)
            p.texture("uPrev", if (needsHistory) work[2].texture.id else source, 1)
            p.texture("uTrail", if (needsHistory) work[trailIndex].texture.id else source, 2)
            p.float("uFlipY", 1f); setEffectUniforms(p, spec, effect, presentationTimeUs, width, height)
            p.draw(); checkGl("${spec.id} chain pass")
            source = target.texture.id
        }
        if (needsHistory) {
            val next = if (trailIndex == 3) 4 else 3
            work[next].bind(); trail.use(); trail.float("uFlipY", 1f)
            trail.texture("uTexture", input.id, 0); trail.texture("uTrail", work[trailIndex].texture.id, 1)
            trail.float("uDecay", .86f); trail.draw(); trailIndex = next
            blit(input.id, work[2]); checkGl("history update")
        }
        output.bind(); return output
    }
    private fun blit(texture: Int, target: GpuTarget) {
        target.bind(); copy.use(); copy.float("uFlipY", 1f); copy.texture("uTexture", texture, 0); copy.draw()
    }
    override fun close() {
        device.checkThread(); programs.values.forEach { it.close() }; work.forEach { it.close() }
        output.close(); copy.close(); trail.close()
    }
}

internal fun setEffectUniforms(p: FxProgram, spec: VideoFxSpec, effect: VideoTimedEffect,
    timeUs: Long, width: Int, height: Int) {
    val local = (timeUs - effect.startMs * 1000).coerceAtLeast(0) / 1_000_000f
    val duration = (effect.endMs - effect.startMs).coerceAtLeast(1) / 1000f
    p.vec2("uSize", width.toFloat(), height.toFloat()); p.float("uAspect", width.toFloat() / height)
    p.float("uTime", local); p.float("uDuration", duration); p.float("uProgress", (local / duration).coerceIn(0f, 1f))
    val uniforms = spec.uniforms(effect.params); p.vec4("uP0", uniforms); p.vec4("uP1", uniforms, 4)
}
