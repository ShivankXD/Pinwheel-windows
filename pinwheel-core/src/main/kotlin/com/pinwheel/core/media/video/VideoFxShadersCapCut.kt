package com.pinwheel.core.media.video

/*
 * CapCut-style effects, written from scratch for Pinwheel. Each family is one fx(uv) with MODE
 * branches; P1 is intensity, P2 speed (via spd), P3 size or colour. Every look is mixed toward
 * the untouched frame by intensity so the Adjust tray behaves the same way on all of them.
 */

/** Camera, flash, reveal and distortion looks: the Trending / Classic workhorses. */
internal const val FX_CC_CAMERA = """
vec3 ccGlow(vec2 uv, float r) { return blurDisc(uv, r); }
vec2 hexCell(vec2 p) {
  vec2 r = vec2(1.0, 1.7320508); vec2 h = r * 0.5;
  vec2 a = mod(p, r) - h; vec2 b = mod(p - h, r) - h;
  return dot(a, a) < dot(b, b) ? a : b;
}
vec4 fx(vec2 uv) {
  float I = P1; float t = uTime * spd(P2);
  vec3 base = src(uv).rgb;
  vec3 c = base;
#if MODE == 1
  // Diamond Zoom: an endless dive through diamond frames, each ring one step closer.
  vec2 p = asp(uv);
  float d = abs(p.x) + abs(p.y) + 1e-4;
  float z = fract(t * 0.55);
  float L = log(d / 0.95) / log(0.6) + z;
  float k = floor(L);
  vec2 q = p / pow(0.6, k - z);
  vec3 inner = src(unasp(q * 0.8)).rgb;
  float edge = 1.0 - smoothstep(0.0, 0.06, min(fract(L), 1.0 - fract(L)));
  c = k < 0.0 ? base * 0.55 : inner;
  c += vec3(1.0) * edge * 0.85;
  c = mix(base, c, I);
#elif MODE == 2
  // Chromatic: pulsing radial RGB split with a breathing zoom.
  float pulse = 0.5 + 0.5 * sin(t * 6.0);
  vec2 q = (uv - 0.5) / (1.0 + 0.05 * pulse * I) + 0.5;
  vec2 p = q - 0.5;
  float amt = 0.022 * I * (0.35 + 0.65 * pulse) * (0.3 + length(p) * 1.8);
  vec2 dir = normalize(p + 1e-4);
  c = vec3(src(q + dir * amt).r, src(q).g, src(q - dir * amt).b);
  c *= 1.0 + 0.1 * pulse * I;
#elif MODE == 3
  // Black Flash: beat-synced black frames, each followed by a zoom punch.
  float beat = fract(t * 1.3);
  float z = 1.0 + 0.14 * I * exp(-beat * 6.0);
  c = src((uv - 0.5) / z + 0.5).rgb;
  float black = 1.0 - smoothstep(0.1, 0.14, beat);
  c *= 1.0 + 0.35 * exp(-(beat - 0.14) * 12.0) * step(0.14, beat) * I;
  c = mix(c, vec3(0.0), black * I);
#elif MODE == 4
  // Ripple: water rings travel out from the centre and bend the picture.
  vec2 p = asp(uv); float r = length(p);
  float front = fract(t * 0.45) * 1.4;
  float env = exp(-abs(r - front) * 5.0) * (1.0 - fract(t * 0.45));
  float w = sin((r - front) * 42.0) * env;
  vec2 off = normalize(p + 1e-4) * w * 0.02 * I;
  c = src(uv + off / vec2(uAspect, 1.0)).rgb;
  c += vec3(w * 0.12 * I);
#elif MODE == 5
  // Retro Flicker: warm faded stock, gate jitter, exposure flicker and grain.
  float k = floor(t * 12.0);
  vec2 j = vec2((hash1(k + 7.0) - 0.5) * 0.004, (hash1(k + 3.0) - 0.5) * 0.012) * I;
  c = src(uv + j).rgb;
  float l = luma(c);
  vec3 warm = vec3(l * 1.12 + 0.05, l * 0.96 + 0.02, l * 0.74);
  c = mix(c, warm, 0.6 * I);
  c *= mix(1.0, 0.8 + 0.4 * hash1(k), I);
  c += (hash(uv * uSize + k) - 0.5) * 0.16 * I;
  vec2 v = asp(uv); c *= 1.0 - 0.55 * I * dot(v, v);
#elif MODE == 6
  // Slash Reveal: diagonal slices fly in from alternate sides and lock into place.
  float cyc = fract(t * 0.42);
  float e = easeOut(sat(cyc * 1.9));
  vec2 p = asp(uv);
  vec2 dir = normalize(vec2(1.0, 0.55)); vec2 nrm = vec2(-dir.y, dir.x);
  float across = dot(p, nrm) * 6.0;
  float band = floor(across);
  float sgn = mod(band, 2.0) < 1.0 ? 1.0 : -1.0;
  vec2 q = p - dir * (1.0 - e) * 1.8 * sgn;
  vec2 quv = unasp(q);
  c = inside(quv) > 0.5 ? src(quv).rgb : vec3(0.02);
  float seam = 1.0 - smoothstep(0.0, 0.05, min(fract(across), 1.0 - fract(across)));
  c += vec3(1.0) * seam * (1.0 - e) * 1.2;
  c = mix(base, c, I);
#elif MODE == 7
  // Shake: handheld camera shake with motion blur along the jolt.
  float s = 0.035 * I;
  vec2 o = (vec2(vnoise(vec2(t * 9.0, 1.3)), vnoise(vec2(4.1, t * 9.0))) - 0.5) * s * 2.0;
  vec2 o2 = (vec2(vnoise(vec2((t - 0.04) * 9.0, 1.3)), vnoise(vec2(4.1, (t - 0.04) * 9.0))) - 0.5) * s * 2.0;
  float a = (vnoise(vec2(t * 7.0, 8.2)) - 0.5) * 0.09 * I;
  vec2 q = unasp(rot(a) * asp(uv) / (1.0 + 0.07 * I)) + o;
  vec2 v = o - o2;
  c = (src(q).rgb + src(q - v).rgb + src(q - v * 2.0).rgb + src(q - v * 3.0).rgb) * 0.25;
#elif MODE == 8
  // Feverish Imprint: colour-shifted echoes breathe out of the frame.
  vec3 acc = vec3(0.0);
  for (int i = 1; i <= 4; i++) {
    float k = float(i);
    float z = 1.0 + 0.07 * k * I * (0.6 + 0.4 * sin(t * 3.0));
    vec2 q = (uv - 0.5) / z + 0.5 + vec2(sin(t * 2.0 + k), cos(t * 1.7 + k)) * 0.008 * k * I;
    acc += src(q).rgb * hsv(fract(t * 0.25 + k * 0.2), 0.75, 1.0);
  }
  c = mix(base, max(base, acc * 0.33) + acc * 0.06, I);
#elif MODE == 9
  // Film Wave: the picture rolls like warped celluloid, with colour fringing.
  float w = (sin(uv.y * 13.0 + t * 5.0) * 0.012 + sin(uv.y * 41.0 - t * 9.0) * 0.004) * I;
  c = vec3(src(uv + vec2(w * 1.5, 0.0)).r, src(uv + vec2(w, 0.0)).g, src(uv + vec2(w * 0.5, 0.0)).b);
  c *= 1.0 - 0.08 * I * (0.5 + 0.5 * sin(uv.y * uSize.y * 0.6));
#elif MODE == 10
  // Wiggle Flicker: stop-motion jolts with light flicker and a chroma kick.
  float k = floor(t * 10.0);
  vec2 o = (hash2(vec2(k, 3.0)) - 0.5) * 0.045 * I;
  float a = (hash1(k * 1.3) - 0.5) * 0.12 * I;
  float z = 1.06 + hash1(k * 2.1) * 0.06 * I;
  vec2 q = unasp(rot(a) * asp(uv) / z) + o;
  float ca = 0.006 * I * hash1(k * 5.3);
  c = vec3(src(q + vec2(ca, 0.0)).r, src(q).g, src(q - vec2(ca, 0.0)).b);
  c *= mix(1.0, 0.8 + 0.45 * hash1(k * 4.7), I);
#elif MODE == 11
  // Dance Flash: every beat a new neon tint, a zoom thump and a white pop.
  float beat = fract(t * 1.6); float k = floor(t * 1.6);
  vec3 tint = hsv(fract(k * 0.29), 0.85, 1.0);
  float z = 1.0 + 0.1 * I * exp(-beat * 5.0);
  c = src((uv - 0.5) / z + 0.5).rgb;
  c = mix(c, c * tint * 1.7, 0.55 * I);
  c += vec3(exp(-beat * 10.0) * 0.55 * I);
#elif MODE == 12
  // B&W Slider: a bright seam sweeps across, colour on one side, monochrome on the other.
  float x = fract(t * 0.4) * 1.3 - 0.15;
  vec3 g = vec3(luma(base)) * 1.05;
  c = uv.x < x ? base : g;
  float glow = exp(-abs(uv.x - x) * uSize.x * 0.08);
  c += vec3(0.9, 0.95, 1.0) * glow * 0.9;
  c = mix(base, c, I);
#elif MODE == 13
  // Hexagonal Split: a honeycomb of the picture, cells popping in turn.
  float n = mix(1.6, 4.0, P3);
  vec2 p = asp(uv) * n;
  vec2 gv = hexCell(p); vec2 id = p - gv;
  float pop = 0.5 + 0.5 * sin(t * 3.0 + hash(id) * 6.28);
  vec2 q = unasp(gv * mix(1.15, 0.85, pop * I));
  vec3 cell = src(q).rgb * (0.85 + 0.25 * pop);
  float hd = max(dot(abs(gv), normalize(vec2(1.0, 1.7320508))), abs(gv.x));
  float line = smoothstep(0.44, 0.49, hd);
  cell = mix(cell, vec3(1.0), line * 0.9);
  c = mix(base, cell, I);
#elif MODE == 14
  // 360 Shimmer: the frame spins into a spiral smear and settles back.
  vec2 p = asp(uv); float r = length(p);
  float amt = I * 1.4 * (0.5 - 0.5 * cos(t * 1.6));
  vec3 acc = vec3(0.0);
  for (int i = 0; i < 10; i++) {
    float f = float(i) / 9.0;
    acc += src(unasp(rot(f * amt * (0.25 + r)) * p * (1.0 - 0.03 * f * amt))).rgb;
  }
  c = acc / 10.0;
#elif MODE == 15
  // Dynamic Blur: a directional smear that swings around the frame.
  float a = t * 1.2; vec2 dir = vec2(cos(a), sin(a)) * 0.035 * I * (0.6 + 0.4 * sin(t * 3.0));
  vec3 acc = vec3(0.0);
  for (int i = -4; i <= 4; i++) acc += src(uv + dir * float(i) / 4.0).rgb;
  c = acc / 9.0;
#elif MODE == 16
  // Wobbly Flash: jelly wobble with bright pulses.
  float beat = fract(t * 1.2);
  vec2 w = vec2(sin(uv.y * 9.0 + t * 7.0), cos(uv.x * 8.0 + t * 6.0)) * 0.018 * I * (0.4 + exp(-beat * 4.0));
  c = src(uv + w).rgb;
  c += vec3(exp(-beat * 9.0) * 0.45 * I);
#elif MODE == 17
  // Rebound Swing: the picture swings on a hinge, springs back and settles.
  float cyc = fract(t * 0.5);
  float ang = sin(cyc * 18.0) * exp(-cyc * 4.0) * 0.45 * I;
  vec2 pivot = vec2(0.0, 0.5);
  vec2 p = asp(uv) - pivot;
  vec2 q = unasp(rot(-ang) * p + pivot);
  c = inside(q) > 0.5 ? src(q).rgb : blurredBg(uv, 0.45);
#elif MODE == 18
  // Shiny Stack: stacked prints fan out behind the frame with a sheen passing over.
  vec3 back = blurredBg(uv, 0.4);
  c = back;
  for (int i = 3; i >= 0; i--) {
    float k = float(i);
    float s = 0.78 - k * 0.03;
    vec2 off = vec2(k * 0.035, -k * 0.03) * I * (0.7 + 0.3 * sin(t * 2.0 + k));
    vec2 q = (uv - 0.5 - off) / s + 0.5;
    if (inside(q) > 0.5) c = src(q).rgb * (1.0 - k * 0.14);
  }
  float sh = exp(-pow((uv.x + uv.y - fract(t * 0.5) * 2.6 + 0.3) * 6.0, 2.0));
  c += vec3(sh * 0.5 * I);
  c = mix(base, c, I);
#elif MODE == 19
  // Cut-up Twist: horizontal slices twist out of line and snap back.
  float n = 9.0; float row = floor(uv.y * n);
  float ph = fract(t * 0.6 - row * 0.05);
  float off = sin(ph * 6.2831) * exp(-ph * 2.5) * 0.12 * I * (mod(row, 2.0) < 1.0 ? 1.0 : -1.0);
  vec2 q = vec2(uv.x + off, uv.y);
  c = inside(q) > 0.5 ? src(q).rgb : vec3(0.0);
#elif MODE == 20
  // Cobweb Zoom: the lens cracks into a web of shards that refract the frame.
  vec2 p = asp(uv); float r = length(p) + 1e-4; float ang = atan(p.y, p.x);
  float spokes = 11.0; float sid = floor((ang + PI) / TAU * spokes);
  float ring = floor(pow(r, 0.7) * 6.0);
  float shard = hash(vec2(sid, ring));
  float z = 1.0 + 0.1 * I * (0.5 + 0.5 * sin(t * 2.0));
  vec2 q = unasp(p / z + (hash2(vec2(sid, ring)) - 0.5) * 0.03 * I);
  c = src(q).rgb * (0.9 + 0.2 * shard);
  float da = abs(fract((ang + PI) / TAU * spokes) - 0.5);
  float dr = abs(fract(pow(r, 0.7) * 6.0) - 0.5);
  float crack = (1.0 - smoothstep(0.46, 0.5, da * 1.0 + 0.0) * 0.0) * smoothstep(0.47, 0.5, max(da, dr));
  c += vec3(crack) * I * 0.8;
  c = mix(base, c, I);
#elif MODE == 21
  // BG Copies: the picture floats small in the middle over a wall of moving copies.
  float s = mix(0.55, 0.8, 1.0 - I);
  vec2 q = (uv - 0.5) / s + 0.5;
  vec2 tile = fract(uv * 3.0 + vec2(t * 0.15, t * 0.08));
  vec3 wall = src(tile).rgb * 0.4;
  float shadow = 1.0 - smoothstep(0.0, 0.06, sdBox(asp(uv), vec2(0.5 * uAspect * s, 0.5 * s)) + 0.02);
  c = inside(q) > 0.5 ? src(q).rgb : wall * (1.0 - 0.6 * shadow);
#elif MODE == 22
  // Camera Punch: rhythmic zoom-in punches with a motion streak.
  float beat = fract(t * 1.1);
  float z = 1.0 + 0.22 * I * exp(-beat * 5.0);
  vec2 q = (uv - 0.5) / z + 0.5;
  vec3 acc = vec3(0.0);
  for (int i = 0; i < 6; i++) acc += src((q - 0.5) * (1.0 + float(i) * 0.012 * I * exp(-beat * 5.0)) + 0.5).rgb;
  c = acc / 6.0;
#else
  c = base;
#endif
  return vec4(c, 1.0);
}
"""

/** Whimsical, light and atmosphere overlays: particles, leaks, film and soft glows. */
internal const val FX_CC_WHIMSY = """
float butterfly(vec2 d, float s, float flap) {
  d /= s; d.x = abs(d.x) / max(flap, 0.15);
  vec2 u = rot(0.5) * (d - vec2(0.55, 0.3)); vec2 l = rot(-0.35) * (d - vec2(0.42, -0.35));
  float up = length(u / vec2(0.55, 0.42)); float lo = length(l / vec2(0.38, 0.3));
  float wing = 1.0 - smoothstep(0.92, 1.0, min(up, lo));
  float body = 1.0 - smoothstep(0.9, 1.0, length(vec2(d.x * flap, d.y) / vec2(0.07, 0.5)));
  return max(wing, body);
}
vec3 screen(vec3 a, vec3 b) { return 1.0 - (1.0 - a) * (1.0 - b); }
vec4 fx(vec2 uv) {
  float I = P1; float t = uTime * spd(P2);
  vec3 base = src(uv).rgb;
  vec3 c = base;
  vec2 p = asp(uv);
#if MODE == 1
  // Heart Ascent: pink bloom with hearts floating up.
  vec3 glow = blurDisc(uv, 0.02);
  c = screen(base, glow * vec3(1.0, 0.45, 0.7) * 0.45 * I);
  vec4 h1 = particles(uv, t, 5.0, vec2(0.0, -0.35), 0.14, 2.0, 1.0, 0.95, 0.08);
  vec4 h2 = particles(uv + 0.37, t * 0.8, 3.0, vec2(0.05, -0.25), 0.12, 2.0, 7.0, 0.92, 0.06);
  c = mix(c, h1.rgb * 1.2, h1.a * I);
  c = mix(c, h2.rgb * 1.1, h2.a * 0.85 * I);
  c += vec3(1.0, 0.5, 0.75) * 0.12 * I * (1.0 - uv.y);
#elif MODE == 2
  // Butterfly: a few butterflies flutter across the frame.
  for (int i = 0; i < 5; i++) {
    float k = float(i);
    vec2 pos = vec2(sin(t * 0.55 + k * 2.1) * 0.55 * uAspect, fract(k * 0.37 - t * 0.12) * 1.3 - 0.65 + sin(t * 1.3 + k) * 0.05);
    float flap = 0.25 + 0.75 * abs(sin(t * 11.0 + k * 1.7));
    vec2 d = rot(0.35 * sin(t + k)) * (p - pos);
    float b = butterfly(d, 0.05 + 0.03 * hash1(k), flap);
    vec3 col = mix(hsv(fract(0.55 + k * 0.13 + P3), 0.7, 1.0), vec3(1.0), 0.2 + 0.3 * sat(-d.y * 8.0));
    c = mix(c, col, b * I);
    c += col * exp(-dot(d, d) * 90.0) * 0.25 * I;
  }
#elif MODE == 3
  // Butterfly Dream: violet haze, drifting butterflies and sparkles.
  c = screen(base, blurDisc(uv, 0.025) * vec3(0.7, 0.45, 1.0) * 0.5 * I);
  c = mix(c, c * vec3(0.92, 0.85, 1.08), I);
  for (int i = 0; i < 4; i++) {
    float k = float(i);
    vec2 pos = vec2(cos(t * 0.4 + k * 1.9) * 0.6 * uAspect, sin(t * 0.33 + k * 2.4) * 0.45);
    float flap = 0.2 + 0.8 * abs(sin(t * 9.0 + k));
    vec2 d = p - pos;
    float b = butterfly(d, 0.045, flap);
    c = mix(c, vec3(0.85, 0.75, 1.0), b * 0.9 * I);
    c += vec3(0.8, 0.6, 1.0) * exp(-dot(d, d) * 60.0) * 0.35 * I;
  }
  vec4 sp = particles(uv, t, 9.0, vec2(0.02, -0.06), 0.1, 1.0, 3.0, 0.78, 0.1);
  c += sp.rgb * sp.a * 0.8 * I;
#elif MODE == 4
  // Leak 1: warm amber light leaks drift in from the edges.
  float n = fbm(vec2(uv.x * 2.0 + t * 0.25, uv.y * 1.2 - t * 0.1));
  float edge = sat(1.2 - uv.x * 1.6 + n * 0.6) + sat(uv.y * 1.3 - 0.9 + n * 0.5);
  vec3 leak = mix(vec3(1.0, 0.45, 0.1), vec3(1.0, 0.85, 0.4), n) * edge;
  c = screen(base, leak * I);
#elif MODE == 5
  // Leak 2: magenta and cyan light sweeps across like a lens leak.
  float x = fract(t * 0.3) * 2.2 - 0.6;
  float band = exp(-pow((uv.x + uv.y * 0.35 - x) * 3.2, 2.0));
  float n = fbm(uv * 3.0 + t * 0.2);
  vec3 leak = mix(vec3(1.0, 0.25, 0.65), vec3(0.2, 0.8, 1.0), n) * band * (0.7 + 0.5 * n);
  c = screen(base, leak * 1.1 * I);
#elif MODE == 6
  // Vignette Rose: rosy edges and a soft romantic bloom.
  float v = sat(dot(p, p) * 1.8);
  vec3 bloom = blurDisc(uv, 0.018);
  c = screen(base, bloom * 0.3 * I);
  c = mix(c, c * vec3(1.0, 0.55, 0.65) * 0.9, v * I);
  c += vec3(1.0, 0.35, 0.5) * v * v * 0.35 * I * (0.8 + 0.2 * sin(t * 2.0));
#elif MODE == 7
  // Falling Petals: pink petals tumble down through the frame.
  vec4 a = particles(uv, t, 5.0, vec2(0.12, 0.5), 0.1, 4.0, 2.0, 0.95, 0.05);
  vec4 b = particles(uv + 0.21, t * 1.2, 8.0, vec2(0.06, 0.7), 0.08, 4.0, 9.0, 0.97, 0.05);
  c = mix(c, a.rgb * vec3(1.0, 0.7, 0.8), a.a * I);
  c = mix(c, b.rgb * vec3(1.0, 0.75, 0.85), b.a * 0.8 * I);
#elif MODE == 8
  // Smoky Focus: smoke curls around the edges, the centre stays crisp.
  float n = fbm(p * 3.0 + vec2(t * 0.3, -t * 0.2));
  float m = sat(length(p) * 1.6 - 0.35 + (n - 0.5) * 0.9);
  vec3 soft = blurDisc(uv, 0.02 * m);
  c = mix(base, soft, m * I);
  c = mix(c, vec3(0.85, 0.87, 0.9), m * n * 0.7 * I);
#elif MODE == 9
  // Vintage Film: sepia stock, scratches, dust, gate weave and burned corners.
  float k = floor(t * 18.0);
  vec2 q = uv + vec2(0.0, (hash1(k) - 0.5) * 0.006 * I);
  vec3 f = src(q).rgb; float l = luma(f);
  vec3 sep = vec3(l * 1.1 + 0.06, l * 0.95 + 0.03, l * 0.72);
  c = mix(f, sep, 0.8 * I);
  float sx = hash1(k * 1.7);
  c -= vec3(0.25) * (1.0 - smoothstep(0.0, 0.0025, abs(uv.x - sx))) * step(0.6, hash1(k * 2.3)) * I;
  float dust = step(0.9985, hash(floor(uv * uSize * 0.5) + k));
  c = mix(c, vec3(0.05), dust * I);
  c += (hash(uv * uSize + k) - 0.5) * 0.12 * I;
  c *= mix(1.0, 0.88 + 0.2 * hash1(k * 3.1), I);
  c *= 1.0 - I * 0.7 * sat(dot(p, p) * 1.6 - 0.2);
#elif MODE == 10
  // Stellar: a night-blue grade with twinkling stars.
  c = mix(base, base * vec3(0.55, 0.65, 1.1), 0.6 * I);
  vec4 s1 = particles(uv, t, 14.0, vec2(0.0, 0.0), 0.12, 1.0, 5.0, 0.6, 0.15);
  vec4 s2 = particles(uv + 0.5, t * 1.3, 7.0, vec2(0.0, 0.0), 0.16, 1.0, 11.0, 0.12, 0.2);
  c += vec3(1.0) * s1.a * 0.9 * I + vec3(1.0, 0.9, 0.7) * s2.a * I;
#elif MODE == 11
  // Border Current: electric light races around the frame edge.
  vec2 e = min(uv, 1.0 - uv);
  float dEdge = min(e.x * uAspect, e.y);
  float per = uv.y < 0.02 || uv.y > 0.98 ? uv.x : uv.y;
  float ang = atan(p.y, p.x) / TAU + 0.5;
  float runner = fract(ang - t * 0.4);
  float head = exp(-min(runner, 1.0 - runner) * 18.0);
  float line = exp(-dEdge * mix(90.0, 35.0, P3));
  vec3 col = hsv(fract(ang + t * 0.2), 0.7, 1.0);
  c = base + col * line * (0.5 + 1.5 * head) * I;
#elif MODE == 12
  // Ink Spill: black ink blooms across the frame, then drains away.
  float cyc = fract(t * 0.35);
  float cover = sin(cyc * PI);
  float n = fbm(p * 3.5 + 1.7) + length(p) * 0.6;
  float ink = smoothstep(cover * 1.4 - 0.1, cover * 1.4 + 0.02, 1.4 - n);
  c = mix(base, vec3(0.02, 0.02, 0.03), ink * I);
#elif MODE == 13
  // Move Cloud: soft clouds drift across in front of the scene.
  float n = fbm(vec2(p.x * 1.6 - t * 0.3, p.y * 2.4));
  float cloud = smoothstep(0.45, 0.8, n) * (0.6 + 0.4 * uv.y);
  c = mix(base, vec3(0.97), cloud * 0.85 * I);
#elif MODE == 14
  // Exploding Love: hearts burst out from the centre with a pink flash.
  float cyc = fract(t * 0.5);
  c = base;
  for (int i = 0; i < 14; i++) {
    float k = float(i);
    float a = k / 14.0 * TAU + hash1(k) * 0.4;
    float dist = easeOut(cyc) * (0.35 + 0.35 * hash1(k * 3.0));
    vec2 pos = vec2(cos(a), sin(a)) * dist;
    float h = heart(p - pos, 0.05 + 0.04 * hash1(k * 5.0)) * (1.0 - cyc);
    c = mix(c, hsv(0.95 + hash1(k * 7.0) * 0.08, 0.7, 1.0), h * I);
  }
  c += vec3(1.0, 0.4, 0.6) * exp(-cyc * 10.0) * 0.5 * I;
#elif MODE == 15
  // Damaged Vignette: burned, dirty edges from worn film.
  float n = fbm(p * 4.0 + floor(t * 6.0) * 0.13);
  float edge = sat((length(p) - 0.45 + (n - 0.5) * 0.35) * 3.0);
  c = mix(base, base * vec3(0.6, 0.4, 0.25), 0.5 * I);
  c = mix(c, vec3(0.9, 0.45, 0.1) * n, edge * 0.8 * I);
  c = mix(c, vec3(0.03), smoothstep(0.7, 1.0, edge) * I);
#elif MODE == 16
  // Firefly Fairies: warm glowing specks wander through the scene.
  c = base * mix(1.0, 0.85, I);
  for (int i = 0; i < 18; i++) {
    float k = float(i);
    vec2 pos = vec2(sin(t * (0.3 + hash1(k) * 0.5) + k * 3.1) * 0.6 * uAspect, cos(t * (0.25 + hash1(k * 2.0) * 0.4) + k * 1.7) * 0.5);
    float blink = 0.5 + 0.5 * sin(t * 4.0 + k * 2.3);
    float d = length(p - pos);
    c += vec3(1.0, 0.85, 0.4) * (exp(-d * d * 3500.0) * 1.5 + exp(-d * d * 250.0) * 0.25) * blink * I;
  }
#elif MODE == 17
  // Misty Tint: a cool fog rolls through with a soft glow.
  float n = fbm(vec2(p.x * 1.5 + t * 0.2, p.y * 3.0 - t * 0.05));
  vec3 soft = blurDisc(uv, 0.015);
  c = mix(base, screen(base, soft * 0.35), I);
  c = mix(c, vec3(0.78, 0.86, 0.92), n * 0.45 * I);
#elif MODE == 18
  // Sepia Cool: a teal-and-sand duotone with fine grain.
  float l = luma(base);
  vec3 duo = mix(vec3(0.08, 0.22, 0.28), vec3(0.98, 0.9, 0.74), l);
  c = mix(base, duo, 0.85 * I);
  c += (hash(uv * uSize + floor(t * 20.0)) - 0.5) * 0.06 * I;
#elif MODE == 19
  // Snow Night: snow falls through a cold blue night.
  c = mix(base, base * vec3(0.72, 0.82, 1.08), 0.6 * I);
  vec4 a = particles(uv, t, 10.0, vec2(0.05, 0.35), 0.08, 0.0, 4.0, 0.6, 0.0);
  vec4 b = particles(uv + 0.3, t, 5.0, vec2(0.1, 0.55), 0.1, 0.0, 8.0, 0.6, 0.0);
  c += vec3(1.0) * (a.a * 0.8 + b.a) * I;
#elif MODE == 20
  // Rain: streaks slash down over a darker, wetter picture.
  c = mix(base, base * vec3(0.72, 0.8, 0.9), 0.6 * I);
  vec2 q = vec2(uv.x * 80.0 + uv.y * 8.0, uv.y * 6.0 + t * 7.0);
  float drop = step(0.965, hash(vec2(floor(q.x), floor(q.y)))) * smoothstep(0.0, 0.6, fract(q.y));
  c += vec3(0.8, 0.85, 1.0) * drop * 0.6 * I;
#elif MODE == 21
  // Rose Bloom: red petals spiral out around a rosy glow.
  c = screen(base, blurDisc(uv, 0.02) * vec3(1.0, 0.3, 0.35) * 0.35 * I);
  for (int i = 0; i < 16; i++) {
    float k = float(i);
    float ph = fract(t * 0.35 + hash1(k));
    float a = k * 2.39996 + ph * 3.0;
    vec2 pos = vec2(cos(a), sin(a)) * ph * 0.8;
    vec2 d = rot(a * 2.0) * (p - pos);
    float petal = 1.0 - smoothstep(0.0, 0.01, length(d * vec2(1.0, 2.0)) - 0.03 * (1.0 - ph * 0.5));
    c = mix(c, vec3(0.85, 0.08, 0.18), petal * I * (1.0 - ph * 0.3));
  }
#elif MODE == 22
  // Sparkle Shine: glints bloom on the brightest spots.
  vec2 cell = floor(uv * vec2(18.0 * uAspect, 18.0));
  vec2 cc = (cell + 0.5) / vec2(18.0 * uAspect, 18.0);
  float bright = luma(src(cc).rgb);
  float tw = 0.5 + 0.5 * sin(t * 5.0 + hash(cell) * 30.0);
  vec2 d = (fract(uv * vec2(18.0 * uAspect, 18.0)) - 0.5);
  float s = star4(d, 0.35) * step(0.62, bright) * step(0.55, hash(cell)) * tw;
  c = base + vec3(1.0, 0.97, 0.9) * s * 1.4 * I;
#else
  c = base;
#endif
  return vec4(c, 1.0);
}
"""

/** Pixel Bead style looks. */
internal const val FX_CC_PIXEL = """
float bayer4(vec2 p) {
  vec2 q = mod(floor(p), 4.0);
  float v = 0.0;
  v += mod(q.x, 2.0) * 2.0 + mod(q.y + q.x, 2.0) * 1.0;
  v = v * 4.0 + mod(floor(q.x / 2.0), 2.0) * 2.0 + mod(floor(q.y / 2.0) + floor(q.x / 2.0), 2.0);
  return v / 16.0;
}
vec4 fx(vec2 uv) {
  float I = P1; float t = uTime * spd(P2);
  vec3 base = src(uv).rgb;
  vec3 c = base;
#if MODE == 1
  // Color Pixel: chunky vivid pixels that pulse in size.
  float n = mix(18.0, 60.0, 1.0 - P3) * (0.8 + 0.2 * sin(t * 3.0));
  vec2 g = vec2(n * uAspect, n);
  vec2 cell = (floor(uv * g) + 0.5) / g;
  vec3 pc = src(cell).rgb;
  pc = floor(pc * 5.0 + 0.5) / 5.0;
  pc = mix(vec3(luma(pc)), pc, 1.5);
  c = mix(base, pc, I);
#elif MODE == 2
  // Pixel Mosaic: random blocks crumble into pixels and back.
  vec2 g = vec2(10.0 * uAspect, 10.0);
  vec2 blk = floor(uv * g);
  float on = step(0.5, sin(t * 2.0 + hash(blk) * 6.2831) * 0.5 + 0.5);
  float n = 12.0 + 40.0 * hash(blk + 3.0);
  vec2 cell = (floor(uv * n * vec2(uAspect, 1.0)) + 0.5) / (n * vec2(uAspect, 1.0));
  c = mix(base, src(cell).rgb, on * I);
#elif MODE == 3
  // Pixel Mutant: blocks shift and change colour in glitchy steps.
  float k = floor(t * 8.0);
  vec2 g = vec2(24.0 * uAspect, 24.0);
  vec2 blk = floor(uv * g);
  float h = hash(blk + k);
  vec2 shift = h > 0.85 ? (hash2(blk + k * 1.3) - 0.5) * 0.08 : vec2(0.0);
  vec2 cell = (floor((uv + shift) * g) + 0.5) / g;
  vec3 pc = src(cell).rgb;
  if (h > 0.93) pc = pc.gbr;
  c = mix(base, pc, I);
#elif MODE == 4
  // Pixel Art: an 8-bit palette with ordered dithering.
  float n = mix(90.0, 40.0, P3);
  vec2 g = vec2(n * uAspect, n);
  vec2 cell = (floor(uv * g) + 0.5) / g;
  vec3 pc = src(cell).rgb;
  float th = bayer4(uv * g) - 0.5;
  pc = floor(pc * 3.0 + th * 0.9 + 0.5) / 3.0;
  c = mix(base, pc, I);
#elif MODE == 5
  // Pixel Universe: the picture dissolves into twinkling pixel stars.
  vec2 g = vec2(40.0 * uAspect, 40.0);
  vec2 cell = floor(uv * g);
  vec3 pc = src((cell + 0.5) / g).rgb;
  float tw = 0.5 + 0.5 * sin(t * 4.0 + hash(cell) * 30.0);
  vec2 f = fract(uv * g) - 0.5;
  float dotm = 1.0 - smoothstep(0.25, 0.45, length(f));
  vec3 sky = vec3(0.02, 0.01, 0.06) + pc * dotm * (0.6 + 0.6 * tw);
  float mixv = 0.5 + 0.5 * sin(t * 1.2);
  c = mix(base, sky, I * mixv);
#else
  c = base;
#endif
  return vec4(c, 1.0);
}
"""
