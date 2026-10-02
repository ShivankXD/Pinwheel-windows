package com.pinwheel.render

/** Shader literals copied unchanged from mobile legacy and Soft Glow runtime. */
internal object MobileLegacyShaders {
    val LEGACY = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
varying mediump vec2 vUv;
uniform sampler2D uTexture;
""" + """
uniform vec2 uSize;
uniform vec4 uAmounts;
uniform float uSeed;
uniform vec2 uTilt;
uniform float uAspect;
vec2 tiltedUv(vec2 uv) {
  float cy = cos(uTilt.x), sy = sin(uTilt.x);
  float cx = cos(uTilt.y), sx = sin(uTilt.y);
  vec3 right = vec3(cy, 0.0, -sy);
  vec3 up = vec3(sy * sx, cx, cy * sx);
  vec3 normal = vec3(sy * cx, -sx, cy * cx);
  vec3 ray = vec3((uv.x - 0.5) * 2.0 * uAspect, (uv.y - 0.5) * 2.0, -3.0);
  float distance = -normal.z * 3.0 / dot(normal, ray);
  vec3 hit = vec3(0.0, 0.0, 3.0) + ray * distance;
  return vec2(dot(hit, right) / uAspect, dot(hit, up)) * 0.5 + 0.5;
}
vec2 sampleUv(vec2 uv) {
  if (uAmounts.w > 0.0) {
    float cell = max(1.0, min(uSize.x, uSize.y) * (0.003 + uAmounts.w * 0.047));
    vec2 grid = uSize / cell;
    uv = (floor(uv * grid) + 0.5) / grid;
  }
  return clamp(uv, vec2(0.0), vec2(1.0));
}
float noise(vec2 coordinate) {
  vec3 value = fract(vec3(coordinate.xyx) * 0.1031 + uSeed * 0.013);
  value += dot(value, value.yzx + 33.33);
  return fract((value.x + value.y) * value.z);
}
void main() {
  vec2 cardUv = tiltedUv(vUv);
  if (cardUv.x < 0.0 || cardUv.x > 1.0 || cardUv.y < 0.0 || cardUv.y > 1.0) {
    gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
    return;
  }
  vec4 source = texture2D(uTexture, sampleUv(cardUv));
  vec3 color = source.rgb;
  if (uAmounts.z > 0.0) {
    float offset = min(uSize.x, uSize.y) / uSize.x * 0.035 * uAmounts.z;
    color.r = texture2D(uTexture, sampleUv(cardUv + vec2(offset, 0.0))).r;
    color.b = texture2D(uTexture, sampleUv(cardUv - vec2(offset, 0.0))).b;
  }
  if (uAmounts.x > 0.0) {
    vec2 centered = (vUv - 0.5) * 2.0;
    float edge = smoothstep(0.22, 1.45, dot(centered, centered));
    color *= 1.0 - edge * 0.85 * uAmounts.x;
  }
  if (uAmounts.y > 0.0) {
    vec2 referenceSize = uSize / min(uSize.x, uSize.y) * 480.0;
    float grain = noise(floor(vUv * referenceSize)) - 0.5;
    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = max(vec3(0.0), color + vec3(grain * 0.20 * uAmounts.y * sqrt(max(luminance, 0.0) + 0.03)));
  }
  gl_FragColor = vec4(color, source.a);
}
"""
    val GLOW_PREFIX = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
varying mediump vec2 vUv;
uniform sampler2D uTexture;
"""
    val GLOW_THRESHOLD = GLOW_PREFIX + """
void main() {
  vec4 c = texture2D(uTexture, vUv);
  vec3 selected = step(vec3(0.5), c.rgb) * clamp((c.rgb - vec3(0.5)) / 0.5, 0.0, 1.0);
  float gain = (selected.r + selected.g + selected.b) / max(c.r + c.g + c.b, 0.00001);
  gl_FragColor = vec4(c.rgb * gain, c.a);
}
"""
    val GLOW_BLUR = GLOW_PREFIX + """
uniform vec2 uStep;
uniform float uSigma;
uniform float uSamples;
void main() {
  vec4 sum = texture2D(uTexture, vUv);
  float weight = 1.0;
  for (int i = 1; i <= 7; ++i) {
    if (float(i) > uSamples) break;
    vec2 offset = float(i) * uStep;
    float w = exp(-0.5 * dot(offset, offset) / (uSigma * uSigma));
    vec2 a = vUv - offset;
    vec2 b = vUv + offset;
    if (a.x >= 0.0 && a.y >= 0.0) { sum += texture2D(uTexture, a) * w; weight += w; }
    if (b.x <= 1.0 && b.y <= 1.0) { sum += texture2D(uTexture, b) * w; weight += w; }
  }
  gl_FragColor = sum / weight;
}
"""
    val GLOW_BLEND = GLOW_PREFIX + """
uniform sampler2D uGlow;
uniform float uExposure;
void main() {
  vec4 source = texture2D(uTexture, vUv);
  vec3 glow = clamp(texture2D(uGlow, vUv).rgb * uExposure, 0.0, 1.0);
  gl_FragColor = vec4(clamp(source.rgb + glow - source.rgb * glow, 0.0, 1.0), source.a);
}
"""
}
