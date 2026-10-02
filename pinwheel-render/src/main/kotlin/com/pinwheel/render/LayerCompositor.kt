package com.pinwheel.render

/** Composites straight-alpha layer FX results over the styled main frame, preserving list order. */
class LayerCompositor(private val device: AngleDevice, width: Int, height: Int) : AutoCloseable {
    private val targets = Array(2) { GpuTarget(device, width, height) }
    private val p = FxProgram("""
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
varying highp vec2 vUv;
#else
precision mediump float;
varying mediump vec2 vUv;
#endif
uniform sampler2D uTexture;
uniform sampler2D uLayer;
void main() {
  vec4 base = texture2D(uTexture, vUv);
  vec4 layer = texture2D(uLayer, vUv);
  float alpha = layer.a + base.a * (1.0 - layer.a);
  vec3 rgb = layer.rgb * layer.a + base.rgb * base.a * (1.0 - layer.a);
  gl_FragColor = vec4(alpha > 0.0 ? rgb / alpha : vec3(0.0), alpha);
}
""")
    fun render(base: GpuTarget, layers: List<GpuTarget>): GpuTarget {
        device.checkThread(); var source = base
        layers.forEachIndexed { i, layer ->
            val to = targets[i % 2]; to.bind(); p.use(); p.float("uFlipY", 1f)
            p.texture("uTexture", source.texture.id, 0); p.texture("uLayer", layer.texture.id, 1); p.draw()
            checkGl("layer composition"); source = to
        }
        return source
    }
    override fun close() { device.checkThread(); targets.forEach { it.close() }; p.close() }
}
