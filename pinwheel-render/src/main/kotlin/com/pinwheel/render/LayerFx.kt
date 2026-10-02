package com.pinwheel.render

import com.pinwheel.core.media.video.VideoFxCatalog
import com.pinwheel.core.model.*
import org.lwjgl.opengles.GLES20.*

/** Receives a premultiplied, canvas-sized painted layer. Colour and coverage each run the same FX. */
class LayerFx(private val device: AngleDevice, private val width: Int, private val height: Int,
    private val layer: VideoImageOverlay, effects: List<VideoTimedEffect>) : AutoCloseable {
    private val effects = effects.filter { it.enabled && VideoFxCatalog.find(it.kind) != null }
    private val programs = HashMap<String, FxProgram>()
    private val resources = GpuResources()
    private val split = resources.own { FxProgram(SPLIT) }
    private val merge = resources.own { FxProgram(MERGE) }
    // Colour ping-pong, mask ping-pong, output.
    private val targets = Array(5) { resources.own { GpuTarget(device, width, height) } }
    init { resources.initialized() }
    fun render(painted: GpuTexture, presentationTimeUs: Long): GpuTarget {
        device.checkThread(); require(painted.device === device && painted.width == width && painted.height == height)
        val framebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING); val viewport = IntArray(4); glGetIntegerv(GL_VIEWPORT, viewport)
        val program = glGetInteger(GL_CURRENT_PROGRAM); val activeUnit = glGetInteger(GL_ACTIVE_TEXTURE)
        val blend = glIsEnabled(GL_BLEND)
        try {
            val timeMs = presentationTimeUs / 1000
            if (timeMs < layer.startMs || timeMs >= layer.endMs) {
                targets[4].bind(); glClearColor(0f, 0f, 0f, 0f); glClear(GL_COLOR_BUFFER_BIT); return targets[4]
            }
            pass(split, targets[0]) { it.texture("uTexture", painted.id, 0); it.float("uMode", 0f) }
            pass(split, targets[2]) { it.texture("uTexture", painted.id, 0); it.float("uMode", 1f) }
            var colour = 0; var mask = 2
            for (effect in effects.filter { timeMs >= it.startMs && timeMs < it.endMs }) {
                val spec = requireNotNull(VideoFxCatalog.find(effect.kind))
                val p = programs.getOrPut(spec.id) { resources.own { FxProgram(FxProgram.source(spec)) } }
                for (channel in 0..1) {
                    val from = if (channel == 0) colour else mask
                    val to = if (from % 2 == 0) from + 1 else from - 1
                    pass(p, targets[to]) {
                        it.texture("uTexture", targets[from].texture.id, 0)
                        it.texture("uPrev", targets[from].texture.id, 1); it.texture("uTrail", targets[from].texture.id, 2)
                        setEffectUniforms(it, spec, effect, presentationTimeUs, width, height)
                    }
                    if (channel == 0) colour = to else mask = to
                }
            }
            pass(merge, targets[4]) { it.texture("uTexture", targets[colour].texture.id, 0); it.texture("uMask", targets[mask].texture.id, 1) }
            checkGl("layer colour/coverage effects"); return targets[4]
        } finally {
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer); glViewport(viewport[0], viewport[1], viewport[2], viewport[3])
            glUseProgram(program); glActiveTexture(activeUnit)
            if (blend) glEnable(GL_BLEND) else glDisable(GL_BLEND)
        }
    }
    private inline fun pass(p: FxProgram, target: GpuTarget, setup: (FxProgram) -> Unit) {
        target.bind(); p.use(); p.float("uFlipY", 1f); setup(p); p.draw()
    }
    override fun close() {
        device.checkThread(); resources.close()
    }
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
        private const val SPLIT = HEADER + """
uniform float uMode;
void main() {
  vec4 c = texture2D(uTexture, vUv);
  gl_FragColor = uMode < 0.5 ? vec4(c.rgb, 1.0) : vec4(vec3(c.a), 1.0);
}"""
        private const val MERGE = HEADER + """
uniform sampler2D uMask;
void main() {
  vec3 c = texture2D(uTexture, vUv).rgb;
  float a = clamp(texture2D(uMask, vUv).g, 0.0, 1.0);
  gl_FragColor = vec4(a > 0.002 ? clamp(c / a, 0.0, 1.0) : vec3(0.0), a);
}"""
        fun size(width: Int, height: Int): Pair<Int, Int> {
            val scale = minOf(1f, 1080f / minOf(width, height).coerceAtLeast(1))
            return (width * scale).toInt().coerceAtLeast(2) to (height * scale).toInt().coerceAtLeast(2)
        }
    }
}
