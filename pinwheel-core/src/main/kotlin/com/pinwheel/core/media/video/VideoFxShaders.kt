package com.pinwheel.core.media.video

/** Shader data only. GLES execution belongs to P2. */
object FxShaderSources {
        /** Full fragment source for one catalog effect: shared header and library, its defines, its fx(). */
        fun source(spec: VideoFxSpec): String = FX_HEADER + FX_LIBRARY + spec.defines + "\n" + spec.shader + FX_MAIN

        val COPY = FX_HEADER + "void main(){ gl_FragColor = texture2D(uTexture, vUv); }"
        /** Trail = mix(current, previous trail, decay): a fading memory of where the subject has been. */
        val TRAIL = FX_HEADER + "uniform float uDecay; void main(){ gl_FragColor = mix(texture2D(uTexture, vUv), texture2D(uTrail, vUv), uDecay); }"
        /** Thumbnail source: the sample photo drifting slowly, so motion-reactive effects have motion to react to. */
        val SWAY = FX_HEADER + """
uniform vec2 uSway;
uniform float uBody;
uniform float uZoom;
void main(){
  vec2 c = (vUv - vec2(0.5, 0.45)) * vec2(1.4, 1.0);
  // Body previews: only the subject in the middle moves. Others: a slow Ken Burns drift.
  vec2 uv = (vUv - 0.5) * uZoom + 0.5 + uSway * mix(1.0, exp(-dot(c, c) * 5.0), uBody);
  gl_FragColor = texture2D(uTexture, clamp(uv, 0.0, 1.0));
}"""
}

const val FX_VERTEX = """
attribute vec4 aPosition;
uniform float uFlipY;
varying highp vec2 vUv;
void main() {
  gl_Position = vec4(aPosition.x, aPosition.y * (uFlipY == 0.0 ? 1.0 : uFlipY), 0.0, 1.0);
  vUv = aPosition.xy * 0.5 + 0.5;
}
"""

private const val FX_HEADER = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
varying highp vec2 vUv;
#else
precision mediump float;
varying mediump vec2 vUv;
#endif
uniform sampler2D uTexture;
uniform sampler2D uPrev;
uniform sampler2D uTrail;
uniform vec2 uSize;
uniform float uAspect;
uniform float uTime;
uniform float uProgress;
uniform float uDuration;
uniform vec4 uP0;
uniform vec4 uP1;
"""

/** Shared helpers. Every literal is a float literal: GLSL ES 1.00 has no implicit int conversion. */
private const val FX_LIBRARY = """
#define P1 uP0.x
#define P2 uP0.y
#define P3 uP0.z
#define P4 uP0.w
#define P5 uP1.x
#define P6 uP1.y
#define P7 uP1.z
#define P8 uP1.w
#define PI 3.14159265
#define TAU 6.2831853
float sat(float x) { return clamp(x, 0.0, 1.0); }
float hash(vec2 p) { p = fract(p * vec2(123.34, 456.21)); p += dot(p, p + 45.32); return fract(p.x * p.y); }
float hash1(float n) { return fract(sin(n * 12.9898) * 43758.5453); }
vec2 hash2(vec2 p) { return vec2(hash(p), hash(p + 17.13)); }
float vnoise(vec2 p) {
  vec2 i = floor(p); vec2 f = fract(p); f = f * f * (3.0 - 2.0 * f);
  return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}
float fbm(vec2 p) { float v = 0.0; float a = 0.5; for (int i = 0; i < 4; i++) { v += a * vnoise(p); p = p * 2.03 + 3.1; a *= 0.5; } return v; }
mat2 rot(float a) { float c = cos(a); float s = sin(a); return mat2(c, -s, s, c); }
float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }
vec3 hsv(float h, float s, float v) { vec3 k = clamp(abs(mod(h * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0); return v * mix(vec3(1.0), k, s); }
vec4 src(vec2 uv) { return texture2D(uTexture, clamp(uv, 0.0, 1.0)); }
vec3 srcm(vec2 uv) { uv = abs(mod(uv + 1.0, 2.0) - 1.0); return texture2D(uTexture, uv).rgb; }
vec3 prev(vec2 uv) { return texture2D(uPrev, clamp(uv, 0.0, 1.0)).rgb; }
vec3 trail(vec2 uv) { return texture2D(uTrail, clamp(uv, 0.0, 1.0)).rgb; }
float inside(vec2 uv) { return step(0.0, uv.x) * step(uv.x, 1.0) * step(0.0, uv.y) * step(uv.y, 1.0); }
vec2 asp(vec2 uv) { return (uv - 0.5) * vec2(uAspect, 1.0); }
vec2 unasp(vec2 p) { return p / vec2(uAspect, 1.0) + 0.5; }
float ease(float x) { x = sat(x); return x * x * (3.0 - 2.0 * x); }
float easeOut(float x) { x = sat(x); return 1.0 - (1.0 - x) * (1.0 - x) * (1.0 - x); }
float spd(float s) { return mix(0.25, 2.6, s); }
float px() { return 1.0 / min(uSize.x, uSize.y); }
vec3 blur9(vec2 uv, float r) {
  vec3 c = vec3(0.0); vec2 d = vec2(r / uAspect, r);
  for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) c += src(uv + vec2(float(x), float(y)) * d).rgb;
  return c / 9.0;
}
vec3 blurDisc(vec2 uv, float r) {
  vec3 c = src(uv).rgb; float w = 1.0;
  for (int i = 0; i < 12; i++) {
    float a = float(i) * 2.39996; float d = sqrt(float(i) + 0.5) / 3.6;
    c += src(uv + vec2(cos(a) / uAspect, sin(a)) * r * d).rgb; w += 1.0;
  }
  return c / w;
}
float sobel(vec2 uv) {
  vec2 e = vec2(1.0 / uSize.x, 1.0 / uSize.y) * 1.5;
  float tl = luma(src(uv + vec2(-e.x, e.y)).rgb); float t = luma(src(uv + vec2(0.0, e.y)).rgb); float tr = luma(src(uv + e).rgb);
  float l = luma(src(uv - vec2(e.x, 0.0)).rgb); float r = luma(src(uv + vec2(e.x, 0.0)).rgb);
  float bl = luma(src(uv - e).rgb); float b = luma(src(uv - vec2(0.0, e.y)).rgb); float br = luma(src(uv + vec2(e.x, -e.y)).rgb);
  float gx = tr + 2.0 * r + br - tl - 2.0 * l - bl; float gy = tl + 2.0 * t + tr - bl - 2.0 * b - br;
  return sqrt(gx * gx + gy * gy);
}
/** Pixels that changed since the previous frame, softened: the moving subject. */
float motion(vec2 uv) {
  float m = 0.0; vec2 e = vec2(2.5 / uSize.x, 2.5 / uSize.y);
  for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) {
    vec2 q = uv + vec2(float(x), float(y)) * e;
    m += length(src(q).rgb - prev(q));
  }
  return sat(m / 9.0 * 5.0 - 0.05);
}
float sdBox(vec2 p, vec2 b) { vec2 d = abs(p) - b; return length(max(d, 0.0)) + min(max(d.x, d.y), 0.0); }
float dot2(vec2 v) { return dot(v, v); }
float sdHeart(vec2 p) {
  p.x = abs(p.x);
  if (p.y + p.x > 1.0) return sqrt(dot2(p - vec2(0.25, 0.75))) - 0.35355;
  return sqrt(min(dot2(p - vec2(0.0, 1.0)), dot2(p - 0.5 * max(p.x + p.y, 0.0)))) * sign(p.x - p.y);
}
float heart(vec2 d, float r) { return 1.0 - smoothstep(-0.02, 0.02, sdHeart(d / r * 0.9 + vec2(0.0, 0.55))); }
float star4(vec2 d, float r) { d /= r; float c = 0.08 / (abs(d.x * d.y) * 6.0 + 0.08); return c * sat(1.0 - length(d) * 0.7) + exp(-dot(d, d) * 18.0); }
float star5(vec2 d, float r) {
  float a = atan(d.y, d.x) + PI * 0.5; float seg = TAU / 5.0;
  a = mod(a, seg) - seg * 0.5; float rr = length(d) / r;
  float edge = mix(0.45, 1.0, abs(a) / (seg * 0.5));
  return 1.0 - smoothstep(0.85 / edge * 0.55, 0.9 / edge * 0.55, rr);
}
/** One grid cell of drifting particles. kind: 0 glow dot, 1 sparkle, 2 heart, 3 confetti, 4 petal, 5 bubble, 6 star. */
vec4 particles(vec2 uv, float t, float density, vec2 drift, float size, float kind, float seed, float hueBase, float hueSpread) {
  vec2 p = asp(uv) * density + drift * t;
  vec2 cell = floor(p); vec2 f = fract(p) - 0.5;
  float h = hash(cell + seed);
  if (h > 0.7) return vec4(0.0);
  vec2 o = (hash2(cell + seed * 1.7) - 0.5) * 0.5;
  vec2 d = f - o;
  float spin = (h - 0.35) * 6.0 * t;
  d = rot(spin) * d;
  float s = size * (0.6 + 0.8 * hash(cell + 3.3));
  float a = 0.0;
  if (kind < 0.5) a = exp(-dot(d, d) / (s * s) * 3.0);
  else if (kind < 1.5) a = star4(d, s) * (0.6 + 0.4 * sin(t * 6.0 + h * 40.0));
  else if (kind < 2.5) a = heart(d, s * 1.2);
  else if (kind < 3.5) a = 1.0 - smoothstep(0.0, 0.01, sdBox(d, vec2(s * 0.9, s * 0.45 * abs(sin(t * 4.0 + h * 20.0)) + 0.01)));
  else if (kind < 4.5) a = 1.0 - smoothstep(0.0, 0.02, length(d * vec2(1.0, 2.2)) - s);
  else if (kind < 5.5) { float r = length(d); a = smoothstep(s, s * 0.8, r) * (0.25 + 0.75 * smoothstep(s * 0.55, s, r)); }
  else a = star5(d, s * 1.2);
  vec3 col = hsv(fract(hueBase + (hash(cell + 9.1) - 0.5) * hueSpread), kind == 4.0 ? 0.45 : 0.75, 1.0);
  return vec4(col, sat(a));
}
/** Ray from the screen point onto a card rotated by r (radians) at camera distance D. xy = uv on card, z = valid. */
vec3 card(vec2 uv, vec3 r, float D, float s) {
  vec2 p = asp(uv) * 2.0;
  vec3 rd = vec3(p, D);
  float cx = cos(r.x); float sx = sin(r.x); float cy = cos(r.y); float sy = sin(r.y); float cz = cos(r.z); float sz = sin(r.z);
  mat3 mx = mat3(1.0, 0.0, 0.0, 0.0, cx, sx, 0.0, -sx, cx);
  mat3 my = mat3(cy, 0.0, -sy, 0.0, 1.0, 0.0, sy, 0.0, cy);
  mat3 mz = mat3(cz, sz, 0.0, -sz, cz, 0.0, 0.0, 0.0, 1.0);
  mat3 m = mz * my * mx;
  vec3 c = vec3(0.0, 0.0, D);
  vec3 n = m * vec3(0.0, 0.0, -1.0); vec3 ax = m * vec3(1.0, 0.0, 0.0); vec3 ay = m * vec3(0.0, 1.0, 0.0);
  float den = dot(rd, n);
  if (abs(den) < 1e-4) return vec3(0.0);
  float t = dot(c, n) / den;
  if (t <= 0.0) return vec3(0.0);
  vec3 local = rd * t - c;
  vec2 q = vec2(dot(local, ax) / (uAspect * s), dot(local, ay) / s) * 0.5 + 0.5;
  return vec3(q, inside(q));
}
mat3 rotX(float a) { float c = cos(a); float s = sin(a); return mat3(1.0, 0.0, 0.0, 0.0, c, s, 0.0, -s, c); }
mat3 rotY(float a) { float c = cos(a); float s = sin(a); return mat3(c, 0.0, -s, 0.0, 1.0, 0.0, s, 0.0, c); }
mat3 rotZ(float a) { float c = cos(a); float s = sin(a); return mat3(c, s, 0.0, -s, c, 0.0, 0.0, 0.0, 1.0); }
/** Card of half-size (aspect*s, s) centred at c with orientation m. Returns uv, valid, ray distance. */
vec4 card2(vec2 uv, mat3 m, vec3 c, float s) {
  vec3 rd = vec3(asp(uv) * 2.0, 2.2);
  vec3 n = m * vec3(0.0, 0.0, -1.0);
  float den = dot(rd, n);
  if (abs(den) < 1e-4) return vec4(0.0);
  float t = dot(c, n) / den;
  if (t <= 0.0) return vec4(0.0);
  vec3 local = rd * t - c;
  vec2 q = vec2(dot(local, m * vec3(1.0, 0.0, 0.0)) / (uAspect * s), dot(local, m * vec3(0.0, 1.0, 0.0)) / s) * 0.5 + 0.5;
  return vec4(q, inside(q), t);
}
vec3 blurredBg(vec2 uv, float dim) { return blur9((uv - 0.5) * 0.8 + 0.5, 0.03) * dim; }
/** Rounded-card version with soft edge alpha. */
float cardMask(vec2 q, float radius) { vec2 d = abs(q - 0.5) - (0.5 - radius); return 1.0 - smoothstep(0.0, 0.004, length(max(d, 0.0)) - radius); }
vec3 bg(vec2 uv) { return mix(vec3(0.02, 0.02, 0.04), vec3(0.08, 0.07, 0.12), uv.y); }
"""

private const val FX_MAIN = """
void main() {
  vec4 c = fx(vUv);
  gl_FragColor = vec4(clamp(c.rgb, 0.0, 1.0), 1.0);
}
"""
