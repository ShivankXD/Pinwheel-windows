package com.pinwheel.render

internal const val PRECISION = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
varying mediump vec2 vUv;
uniform sampler2D uTexture;
"""

internal val CANVAS_FRAGMENT = PRECISION + """
uniform vec2 uUvScale;
uniform vec3 uBackground;
void main() {
  vec2 source = (vUv - 0.5) * uUvScale + 0.5;
  if (source.x < 0.0 || source.x > 1.0 || source.y < 0.0 || source.y > 1.0) {
    gl_FragColor = vec4(uBackground, 1.0);
  } else {
    gl_FragColor = texture2D(uTexture, source);
  }
}
"""

internal val BLUR_FRAGMENT = PRECISION + """
uniform vec2 uStep;
void main() {
  vec4 color = texture2D(uTexture, vUv) * 0.2270270270;
  color += texture2D(uTexture, clamp(vUv + uStep, 0.0, 1.0)) * 0.1945945946;
  color += texture2D(uTexture, clamp(vUv - uStep, 0.0, 1.0)) * 0.1945945946;
  color += texture2D(uTexture, clamp(vUv + uStep * 2.0, 0.0, 1.0)) * 0.1216216216;
  color += texture2D(uTexture, clamp(vUv - uStep * 2.0, 0.0, 1.0)) * 0.1216216216;
  color += texture2D(uTexture, clamp(vUv + uStep * 3.0, 0.0, 1.0)) * 0.0540540541;
  color += texture2D(uTexture, clamp(vUv - uStep * 3.0, 0.0, 1.0)) * 0.0540540541;
  color += texture2D(uTexture, clamp(vUv + uStep * 4.0, 0.0, 1.0)) * 0.0162162162;
  color += texture2D(uTexture, clamp(vUv - uStep * 4.0, 0.0, 1.0)) * 0.0162162162;
  gl_FragColor = color;
}
"""

internal val BLUR_CANVAS_FRAGMENT = PRECISION + """
uniform sampler2D uBlurred;
uniform vec2 uFitScale;
uniform vec2 uFillScale;
void main() {
  vec2 source = (vUv - 0.5) * uFitScale + 0.5;
  if (source.x >= 0.0 && source.x <= 1.0 && source.y >= 0.0 && source.y <= 1.0) {
    gl_FragColor = texture2D(uTexture, source);
  } else {
    vec2 background = (vUv - 0.5) * uFillScale + 0.5;
    gl_FragColor = texture2D(uBlurred, background);
  }
}
"""
