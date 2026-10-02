package com.pinwheel.core.media.video

/* P1 Intensity, P2 Speed, P3 Size (or family-specific amount), P3 Colour, unless noted. */

internal const val FX_CLASSIC = """
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2); float I = P1; float pr = uProgress;
  vec3 c = src(uv).rgb; vec3 col = c;
  #if MODE == 1
    col = blurDisc(uv, 0.035 * I);
  #elif MODE == 2
    // Zoom lens: pulsing punch-in with radial blur.
    float z = 1.0 - 0.12 * I * (0.5 + 0.5 * sin(T * 2.0)); vec2 q = (uv - 0.5) * z + 0.5; col = vec3(0.0);
    for (int i = 0; i < 8; i++) col += src((q - 0.5) * (1.0 - float(i) * 0.01 * I) + 0.5).rgb;
    col /= 8.0;
  #elif MODE == 3
    col = c * ease(pr * 1.15);
  #elif MODE == 4
    col = c * (1.0 - ease(pr * 1.1 - 0.05));
  #elif MODE == 5
    col = mix(c, vec3(1.0), pow(sat(1.0 - fract(T * 0.6) * 3.0), 2.0) * I);
  #elif MODE == 6
    col = c * (1.0 - step(0.82, fract(T * 0.8)) * I);
  #elif MODE == 7
    col = vec3(0.0); float a = (P3 - 0.5) * PI;
    for (int i = 0; i < 10; i++) col += src(uv + vec2(cos(a), sin(a)) * (float(i) - 4.5) * 0.004 * I * 2.0).rgb;
    col /= 10.0;
  #elif MODE == 8
    float z = 1.0 - 0.18 * I * ease(fract(T * 0.35) * 1.3); col = src((uv - 0.5) * z + 0.5).rgb;
  #elif MODE == 9
    vec2 o = vec2(vnoise(vec2(T * 9.0, 1.0)) - 0.5, vnoise(vec2(1.0, T * 9.0)) - 0.5) * 0.04 * I;
    col = src((uv - 0.5) * 0.95 + 0.5 + o).rgb;
  #elif MODE == 10
    float g = hash(floor(uv * uSize * 0.7) + fract(T * 7.0) * 31.0) - 0.5;
    col = c + g * 0.22 * I * sqrt(luma(c) + 0.05);
  #elif MODE == 11
    float bar = mix(0.0, 0.14, I); col = c * vec3(1.02, 1.0, 0.95);
    col = mix(col, vec3(0.0), step(uv.y, bar) + step(1.0 - bar, uv.y));
  #elif MODE == 12
    float w = ease(pr * 1.3) * 0.5; col = mix(vec3(0.0), c, step(abs(uv.x - 0.5), w));
  #elif MODE == 13
    float w = ease(pr * 1.3) * 0.5; col = mix(vec3(0.0), c, step(abs(uv.y - 0.5), w));
  #elif MODE == 14
    col = blurDisc(uv, 0.04 * I * (0.5 + 0.5 * cos(T * 1.3)));
  #elif MODE == 15
    // Spin blur around the centre.
    col = vec3(0.0); vec2 p = asp(uv);
    for (int i = 0; i < 10; i++) col += src(unasp(rot((float(i) - 4.5) * 0.012 * I * (1.0 + sin(T))) * p)).rgb;
    col /= 10.0;
  #elif MODE == 16
    col = src(unasp(rot(sin(T * 1.5) * 0.08 * I) * asp(uv) * 0.94)).rgb;
  #elif MODE == 17
    col = c * mix(1.0, 0.45, I) * vec3(0.95, 0.98, 1.05);
  #elif MODE == 18
    vec3 b = blur9(uv, 0.02); col = mix(c, max(c, b) * 1.1 + 0.06, 0.6 * I);
  #elif MODE == 19
    col = mix(vec3(luma(c)), c, ease(pr * 1.4 - 0.2));
  #elif MODE == 20
    col = mix(c, vec3(luma(c)) * hsv(P3, 0.45, 1.25), I);
  #elif MODE == 21
    vec2 d = (uv - 0.5) * 2.0; col = c * (1.0 - smoothstep(0.3, 1.5, dot(d, d)) * 0.85 * I);
  #elif MODE == 22
    float band = smoothstep(0.1, 0.35, abs(uv.y - 0.5)); col = mix(c, blurDisc(uv, 0.03), band * I);
    col = mix(col, col * 1.15, 1.0 - band);
  #endif
  return vec4(col, 1.0);
}
"""

internal const val FX_INTRO = """
vec4 fx(vec2 uv) {
  float pr = sat(uProgress * mix(0.6, 1.6, P2)); float I = P1;
  vec3 c = src(uv).rgb; vec3 col = c; vec2 p = asp(uv);
  #if MODE == 1
    float r = easeOut(pr) * 1.2 * max(uAspect, 1.0); col = mix(vec3(0.0), c, smoothstep(r, r - 0.01, length(p)));
  #elif MODE == 2
    float r = (1.0 - ease(pr)) * 1.2 * max(uAspect, 1.0); col = mix(vec3(0.0), c, smoothstep(r, r - 0.01, length(p)));
  #elif MODE == 3
    float y = 1.0 - easeOut(pr); vec2 q = uv + vec2(0.0, y); col = mix(vec3(0.0), src(q).rgb, inside(q));
  #elif MODE == 4
    float z = mix(2.2, 1.0, easeOut(pr)); col = src((uv - 0.5) / z + 0.5).rgb * sat(pr * 3.0);
  #elif MODE == 5
    col = blurDisc(uv, 0.06 * (1.0 - easeOut(pr))) * sat(pr * 4.0);
  #elif MODE == 6
    col = blurDisc(uv, 0.06 * ease(pr)) * (1.0 - ease(pr * 1.2 - 0.2));
  #elif MODE == 7
    float o = easeOut(pr) * 0.5;
    vec2 ql = uv + vec2(o, 0.0); vec2 qr = uv - vec2(o, 0.0);
    vec3 curtain = vec3(0.45, 0.02, 0.05) * (0.6 + 0.4 * sin(uv.x * 80.0));
    if (uv.x < 0.5 - o) col = curtain; else if (uv.x > 0.5 + o) col = curtain;
  #elif MODE == 8
    float bar = ease(pr) * 0.5; col = mix(c, vec3(0.0), step(uv.y, bar) + step(1.0 - bar, uv.y));
  #elif MODE == 9
    col = mix(vec3(1.0), c, easeOut(pr * 1.5)) + vec3(1.0) * exp(-pr * 10.0) * I;
  #elif MODE == 10
    float g = (1.0 - pr) * I; float row = floor(uv.y * 24.0); float h = hash(vec2(row, floor(pr * 30.0)));
    vec2 q = uv + vec2((h - 0.5) * 0.3 * g * step(0.5, h), 0.0);
    col = vec3(src(q + vec2(0.02 * g, 0.0)).r, src(q).g, src(q - vec2(0.02 * g, 0.0)).b) * sat(pr * 3.0);
  #elif MODE == 11
    float burn = fbm(uv * 3.0 + pr * 2.0) + pr * 1.5 - 0.6;
    col = mix(c, vec3(1.0, 0.55, 0.2) * 1.4, smoothstep(0.55, 0.8, burn)); col = mix(col, vec3(1.0), smoothstep(0.85, 1.1, burn));
  #elif MODE == 12
    // Camera shutter blades close and open.
    float k = 1.0 - abs(pr * 2.0 - 1.0); float closed = ease(k * 1.15);
    float a = atan(p.y, p.x) + closed * 1.4; float seg = TAU / 6.0;
    float hex = length(p) * cos(PI / 6.0) / cos(mod(a, seg) - PI / 6.0);
    float radius = (1.0 - closed) * 0.95 * max(uAspect, 1.0);
    float blade = fract(a / seg);
    vec3 metal = vec3(0.1 + 0.22 * blade + 0.04 * sin(length(p) * 60.0)) * vec3(0.95, 0.97, 1.05);
    col = mix(metal, c, smoothstep(radius, radius - 0.008, hex));
  #elif MODE == 13
    // Heart-shaped reveal.
    float r = easeOut(pr) * 2.2; col = mix(vec3(0.0), c, heart(p, r * 0.5));
  #elif MODE == 14
    col = mix(vec3(0.0), c, step(uv.x, easeOut(pr) * 1.1 - 0.05 + (hash(vec2(floor(uv.y * 40.0), 1.0)) - 0.5) * 0.08));
  #elif MODE == 15
    // Pinwheel wipe: blades sweep the picture in.
    float a = fract(atan(p.y, p.x) / TAU * 6.0);
    col = mix(vec3(0.0), c, step(a, easeOut(pr) * 1.05));
  #endif
  return vec4(col, 1.0);
}
"""

internal const val FX_PARTY = """
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2); float I = P1; vec3 c = src(uv).rgb; vec3 col = c; vec2 p = asp(uv);
  #if MODE == 1
    // Disco beams sweeping from above.
    vec3 light = vec3(0.0);
    for (int i = 0; i < 5; i++) {
      float fi = float(i); vec2 o = vec2((fi - 2.0) * 0.25 * uAspect, 0.65);
      float a = sin(T * (0.8 + fi * 0.21) + fi) * 0.8 - PI * 0.5;
      vec2 d = p - o; float ang = atan(d.y, d.x) - a; ang = mod(ang + PI, TAU) - PI;
      light += hsv(fract(fi * 0.21 + T * 0.1 + P3), 0.8, 1.0) * exp(-ang * ang * 90.0) * 0.8;
    }
    col = c * (1.0 - 0.35 * I) + light * I;
  #elif MODE == 2
    float beat = pow(sat(1.0 - fract(T * 1.5) * 4.0), 2.0);
    col = mix(c, hsv(fract(floor(T * 1.5) * 0.37 + P3), 0.7, 1.0), beat * 0.55 * I) + beat * 0.15 * I;
  #elif MODE == 3
    // Laser show.
    vec3 l = vec3(0.0);
    for (int i = 0; i < 6; i++) {
      float fi = float(i); vec2 o = vec2(sin(fi * 2.1) * 0.5 * uAspect, -0.55);
      float a = PI * 0.5 + sin(T * (1.0 + fi * 0.3) + fi * 2.0) * 0.9;
      vec2 d = p - o; float dist = abs(d.x * sin(a) - d.y * cos(a));
      l += hsv(fract(0.3 + fi * 0.13 + P3), 0.9, 1.0) * (0.002 / (dist + 0.002)) * step(0.0, dot(d, vec2(cos(a), sin(a))));
    }
    col = c * (1.0 - 0.45 * I) + l * 0.9 * I;
  #elif MODE == 4
    // Club spotlights with haze.
    vec3 l = vec3(0.0);
    for (int i = 0; i < 3; i++) {
      float fi = float(i); vec2 cc = vec2(sin(T * (0.6 + fi * 0.3) + fi * 2.0) * 0.4 * uAspect, cos(T * (0.5 + fi * 0.2) + fi) * 0.35);
      l += hsv(fract(fi / 3.0 + P3 + T * 0.05), 0.8, 1.0) * exp(-dot(p - cc, p - cc) * 12.0);
    }
    col = c * (0.35 + 0.65 * (1.0 - I)) + c * l * 1.6 * I + l * 0.12 * I;
  #elif MODE == 5
    float beat = pow(sat(1.0 - fract(T * 1.2) * 3.0), 2.0); float z = 1.0 - beat * 0.08 * I;
    vec2 q = (uv - 0.5) * z + 0.5; float o = beat * 0.012 * I;
    col = vec3(src(q + vec2(o, 0.0)).r, src(q).g, src(q - vec2(o, 0.0)).b);
  #elif MODE == 6
    vec4 s = particles(uv, T, 16.0, vec2(0.15, 0.1), 0.22, 1.0, 4.0, P3, 1.0);
    col = c * (1.0 - 0.2 * I) + s.rgb * s.a * 1.6 * I;
  #elif MODE == 7
    vec4 s = particles(uv, T, 12.0, vec2(0.05, 0.6), 0.22, 3.0, 5.0, P3, 1.0);
    col = mix(c, s.rgb, s.a * I);
  #elif MODE == 8
    float r = length(p); float ring = 0.0;
    for (int i = 0; i < 3; i++) { float rr = fract(T * 0.5 + float(i) / 3.0) * 1.2; ring += smoothstep(0.02, 0.0, abs(r - rr)) * (1.0 - rr / 1.2); }
    col = c + hsv(fract(P3 + T * 0.1), 0.8, 1.0) * ring * I;
  #elif MODE == 9
    col = mix(c, hsv(fract(luma(c) + T * 0.2 + P3), 0.7, luma(c) * 1.2 + 0.1), I * 0.85);
  #elif MODE == 10
    float f = step(0.7, hash(vec2(floor(T * 8.0), 2.0)));
    col = c + hsv(hash(vec2(floor(T * 8.0), 5.0)), 0.6, 1.0) * f * 0.45 * I;
  #elif MODE == 11
    // Balloons floating up.
    vec2 q = p * 5.0 + vec2(0.0, -T * 0.6); vec2 id = floor(q); vec2 f = fract(q) - 0.5;
    float h = hash(id); f.x += sin(T + h * 6.0) * 0.1;
    float b = smoothstep(0.24, 0.2, length(f * vec2(1.0, 0.85))) * step(h, 0.5);
    float string = smoothstep(0.012, 0.0, abs(f.x + sin(f.y * 12.0) * 0.02)) * step(f.y, -0.22) * step(h, 0.5);
    vec3 bc = hsv(fract(h * 7.0 + P3), 0.7, 1.0) * (0.75 + 0.5 * smoothstep(0.2, 0.0, length(f - vec2(-0.07, 0.08))));
    col = mix(c, bc, b * I); col = mix(col, vec3(0.9), string * 0.6 * I);
  #endif
  return vec4(col, 1.0);
}
"""

internal const val FX_MOTION = """
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2); float I = P1; vec2 q = uv;
  #if MODE == 1
    q = (uv - 0.5) * 0.9 + 0.5 + vec2(vnoise(vec2(T * 14.0, 2.0)) - 0.5, vnoise(vec2(3.0, T * 14.0)) - 0.5) * 0.08 * I;
  #elif MODE == 2
    float jolt = step(0.75, hash(vec2(floor(T * 6.0), 1.0)));
    q = (uv - 0.5) * 0.9 + 0.5 + (hash2(vec2(floor(T * 20.0), 3.0)) - 0.5) * 0.09 * I * jolt;
  #elif MODE == 3
    q = uv + vec2(sin(uv.y * 10.0 + T * 3.0), cos(uv.x * 10.0 + T * 2.5)) * 0.012 * I;
  #elif MODE == 4
    vec2 p = asp(uv); float r = length(p); p = rot(sin(T) * 2.5 * I * sat(1.0 - r * 1.6)) * p; q = unasp(p);
  #elif MODE == 5
    q = unasp(rot(T * 0.8) * asp(uv) * mix(1.0, 0.6, I));
  #elif MODE == 6
    q = (uv - 0.5) * 0.92 + 0.5 + vec2(0.0, -abs(sin(T * 3.0)) * 0.06 * I + 0.03 * I);
  #elif MODE == 7
    vec2 p = asp(uv) - vec2(0.0, 0.6); p = rot(sin(T * 1.6) * 0.12 * I) * p; q = unasp(p + vec2(0.0, 0.6)) ;
    q = (q - 0.5) * 0.92 + 0.5;
  #elif MODE == 8
    q = (uv - 0.5) * 0.97 + 0.5 + vec2(sin(T * 60.0), cos(T * 53.0)) * 0.004 * I;
  #elif MODE == 9
    q = (uv - 0.5) * (1.0 - 0.1 * I * (0.5 + 0.5 * sin(T * 3.0))) + 0.5;
  #elif MODE == 10
    q = (uv - 0.5) * (1.0 - 0.15 * I * fract(T * 0.12)) + 0.5 + vec2(fract(T * 0.12) * 0.04 * I, 0.0);
  #elif MODE == 11
    float s = sin(T * 3.0) * 0.12 * I; q = (uv - 0.5) * vec2(1.0 + s, 1.0 - s) + 0.5;
  #elif MODE == 12
    float t = fract(T * 0.8); float beat = exp(-t * 14.0) + exp(-max(t - 0.2, 0.0) * 14.0) * step(0.2, t) * 0.7;
    q = (uv - 0.5) * (1.0 - beat * 0.09 * I) + 0.5;
  #endif
  return vec4(srcm(q), 1.0);
}
"""

internal const val FX_LIGHT = """
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2); float I = P1; vec3 c = src(uv).rgb; vec3 col = c; vec2 p = asp(uv);
  #if MODE == 1
    // Warm light leaks drifting across.
    float n = fbm(p * 1.5 + vec2(T * 0.25, T * 0.1));
    vec3 leak = mix(vec3(1.0, 0.35, 0.1), vec3(1.0, 0.8, 0.4), n) * smoothstep(0.45, 0.85, n + (uv.x - 0.5) * 0.6 * sin(T * 0.3));
    col = c + leak * 0.9 * I;
  #elif MODE == 2
    // Lens flare following a moving light.
    vec2 L = vec2(sin(T * 0.4) * 0.5 * uAspect, 0.3 + cos(T * 0.3) * 0.1);
    vec3 f = vec3(1.0, 0.9, 0.7) * 0.02 / (length(p - L) + 0.02);
    for (int i = 1; i < 5; i++) { vec2 g = L * (1.0 - float(i) * 0.55); f += hsv(float(i) * 0.18, 0.6, 1.0) * smoothstep(0.06, 0.0, abs(length(p - g) - 0.03 * float(i))) * 0.3; }
    f += vec3(1.0, 0.8, 0.6) * exp(-abs(p.y - L.y) * 60.0) * 0.25;
    col = c + f * I;
  #elif MODE == 3
    // God rays from a bright point.
    vec2 L = vec2(0.5 + sin(T * 0.3) * 0.2, 0.9); vec3 acc = vec3(0.0); vec2 d = (uv - L) / 18.0;
    vec2 s = uv;
    for (int i = 0; i < 18; i++) { s -= d; vec3 v = src(s).rgb; acc += v * smoothstep(0.65, 0.9, luma(v)) * (1.0 - float(i) / 18.0); }
    col = c + acc / 18.0 * 1.4 * I * vec3(1.0, 0.92, 0.8);
  #elif MODE == 4
    vec4 b = particles(uv, T * 0.3, 7.0, vec2(0.08, 0.04), 0.3, 0.0, 6.0, 0.08 + P3, 0.25);
    col = c + b.rgb * b.a * 0.55 * I;
  #elif MODE == 5
    float beam = smoothstep(0.25, 0.0, abs(p.x * 0.8 + p.y * 0.6 - sin(T * 0.5) * 0.4));
    col = c + vec3(1.0, 0.85, 0.55) * beam * 0.5 * I;
  #elif MODE == 6
    float band = sin((p.x + p.y) * 3.0 - T * 1.2) * 0.5 + 0.5;
    col = c + hsv(fract(p.x * 0.5 + T * 0.1), 0.8, 1.0) * smoothstep(0.7, 1.0, band) * 0.45 * I;
  #elif MODE == 7
    vec3 b = blur9(uv, 0.012) + blur9(uv, 0.03); col = c + max(b * 0.5 - 0.35, 0.0) * 2.0 * I;
  #elif MODE == 8
    col = c * mix(1.0, 0.45, I) + hsv(fract(P3 + uv.y * 0.3 + T * 0.1), 0.8, 1.0) * smoothstep(0.2, 0.7, sobel(uv)) * 1.2 * I;
  #elif MODE == 9
    vec2 cc = vec2(sin(T * 0.7) * 0.4 * uAspect, sin(T * 0.5) * 0.25);
    col = c * mix(1.0, 0.25 + 1.2 * exp(-dot(p - cc, p - cc) * 6.0), I);
  #elif MODE == 10
    float f = 0.85 + 0.15 * sin(T * 23.0) * sin(T * 7.0) - step(0.93, hash(vec2(floor(T * 12.0), 1.0))) * 0.4;
    col = c * mix(1.0, f, I);
  #elif MODE == 11
    // Glints: stars sparkle on the brightest spots.
    vec2 g = uv * vec2(uAspect, 1.0) * 26.0; vec2 id = floor(g); vec2 f = fract(g) - 0.5;
    vec3 v = src((id + 0.5) / vec2(uAspect, 1.0) / 26.0).rgb;
    float tw = sin(T * 4.0 + hash(id) * 30.0) * 0.5 + 0.5;
    col = c + vec3(1.0) * star4(f, 0.28) * step(0.72, luma(v)) * step(0.6, hash(id)) * tw * 1.5 * I;
  #elif MODE == 12
    float a = atan(p.y, p.x); float r = length(p);
    float rays = pow(abs(sin(a * 8.0 + T)), 20.0) * exp(-r * 2.0);
    col = c + vec3(1.0, 0.9, 0.7) * (rays + exp(-r * 10.0)) * 0.7 * I;
  #endif
  return vec4(col, 1.0);
}
"""

internal const val FX_SPLIT = """
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2); float I = P1; vec2 q = uv; vec3 tint = vec3(1.0);
  #if MODE == 1
    q = vec2(fract(uv.x * 2.0) * 0.5 + 0.25, uv.y);
  #elif MODE == 2
    q = vec2(uv.x, fract(uv.y * 2.0) * 0.5 + 0.25);
  #elif MODE == 3
    q = vec2(fract(uv.x * 3.0) / 3.0 + 1.0 / 3.0, uv.y);
  #elif MODE == 4
    q = fract(uv * 2.0);
  #elif MODE == 5
    q = fract(uv * 3.0);
  #elif MODE == 6
    q = vec2(0.5 - abs(uv.x - 0.5), uv.y);
  #elif MODE == 7
    q = vec2(uv.x, 0.5 - abs(uv.y - 0.5));
  #elif MODE == 8
    q = 0.5 - abs(uv - 0.5);
  #elif MODE == 9
    float id = floor(uv.x * 3.0); q = vec2(fract(uv.x * 3.0) / 3.0 + 1.0 / 3.0, uv.y);
    tint = id < 0.5 ? vec3(1.3, 0.5, 0.5) : (id < 1.5 ? vec3(0.5, 1.3, 0.6) : vec3(0.5, 0.6, 1.4));
  #elif MODE == 10
    float id = floor(uv.y * 6.0); q = uv + vec2(sin(T * 2.0 + id * 1.3) * 0.06 * I, 0.0);
  #elif MODE == 11
    float side = step(uv.x + uv.y, 1.0); q = uv + vec2(side * 2.0 - 1.0) * 0.04 * I * sin(T * 1.5);
  #elif MODE == 12
    vec2 id = floor(uv * 4.0); q = fract(uv * 4.0);
    float on = step(0.5, hash(id + floor(T * 2.0)));
    tint = mix(vec3(0.35), vec3(1.15), on);
  #endif
  vec3 col = srcm(q) * tint;
  #if MODE == 4 || MODE == 5 || MODE == 12
    vec2 g = fract(uv * (MODE == 5 ? 3.0 : (MODE == 12 ? 4.0 : 2.0)));
    col *= 1.0 - smoothstep(0.012, 0.0, min(min(g.x, 1.0 - g.x), min(g.y, 1.0 - g.y))) * 0.8 * I;
  #endif
  return vec4(col, 1.0);
}
"""

internal const val FX_GRAFFITI = """
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2); float I = P1; vec3 c = src(uv).rgb; vec3 col = c; vec2 p = asp(uv);
  // Hand-drawn lines "boil": the drawing wobbles a little, several times a second.
  vec2 wob = (hash2(vec2(floor(T * 8.0), 7.0)) - 0.5) * 0.004;
  #if MODE == 1
    float e = sobel(uv + wob); float paper = 0.92 + 0.08 * vnoise(uv * uSize * 0.3);
    col = mix(c, vec3(paper) * (1.0 - smoothstep(0.08, 0.4, e) * 0.85), I);
  #elif MODE == 2
    float e = smoothstep(0.15, 0.45, sobel(uv + wob));
    col = mix(c, c * 0.9 + hsv(fract(P3 + floor(uv.y * 6.0) * 0.17), 0.9, 1.0) * e * 1.4, I);
  #elif MODE == 3
    // Doodled stars and hearts drawn around the frame.
    vec2 g = p * 3.2 + wob * 50.0; vec2 id = floor(g); vec2 f = fract(g) - 0.5; float h = hash(id);
    float ring = step(0.35, length(p * vec2(1.0 / uAspect, 1.0)) * 1.2);
    float shape = h < 0.72 ? star5(f, 0.34) - star5(f, 0.27) : heart(f, 0.32) - heart(f, 0.25);
    float ink = sat(shape) * step(0.45, h) * ring;
    col = mix(c, hsv(fract(h * 3.0 + P3), 0.8, 1.0), ink * I);
  #elif MODE == 4
    float e = smoothstep(0.1, 0.4, sobel(uv)); float spray = step(0.5, hash(floor(uv * uSize * 0.5) + floor(T * 10.0)));
    col = mix(c, c * 0.3 + hsv(fract(P3 + uv.x * 0.5), 1.0, 1.0) * e * (0.6 + 0.4 * spray) * 2.0, I);
  #elif MODE == 5
    // Comic halftone print.
    float s = mix(90.0, 45.0, P3); vec2 g = rot(0.785) * uv * vec2(uAspect, 1.0) * s; vec2 f = fract(g) - 0.5;
    float l = luma(c); float dotv = smoothstep(0.05, 0.0, length(f) - (1.0 - l) * 0.55);
    col = mix(c, mix(floor(c * 3.0 + 0.5) / 3.0, vec3(0.05), dotv * 0.8), I);
  #elif MODE == 6
    vec2 sp = rot(0.6) * uv * vec2(uAspect, 1.0);
    float wax = vnoise(sp * vec2(420.0, 28.0)) * 0.6 + vnoise(sp * vec2(90.0, 9.0)) * 0.4;
    vec3 flatc = floor(c * 5.0 + 0.5) / 5.0 * 1.1;
    float cover = smoothstep(0.25, 0.6, wax + (1.0 - luma(c)) * 0.35);
    col = mix(c, mix(vec3(0.98, 0.96, 0.9), flatc, cover) * (1.0 - smoothstep(0.25, 0.6, sobel(uv)) * 0.6), I);
  #elif MODE == 7
    float e = smoothstep(0.1, 0.4, sobel(uv + wob)); vec3 board = vec3(0.1, 0.22, 0.17) + vnoise(uv * 40.0) * 0.04;
    col = mix(c, board + vec3(0.95) * e * (0.7 + 0.3 * vnoise(uv * uSize * 0.2)), I);
  #elif MODE == 8
    // Scribbled marker frame.
    float d = min(min(uv.x, 1.0 - uv.x) * uAspect, min(uv.y, 1.0 - uv.y));
    float line = sin((uv.x * uAspect + uv.y) * 90.0 + T * 3.0) * 0.5 + 0.5;
    float band = smoothstep(0.06, 0.05, d + (vnoise(uv * 30.0 + T) - 0.5) * 0.02);
    col = mix(c, hsv(fract(P3 + (uv.x + uv.y) * 0.5 + T * 0.1), 0.85, 1.0), band * (0.6 + 0.4 * line) * I);
  #endif
  return vec4(col, 1.0);
}
"""

internal const val FX_CELEBRATE = """
vec3 firework(vec2 p, float T, float seed) {
  vec3 c = vec3(0.0);
  float cycle = 2.2; float k = floor(T / cycle + seed); float age = fract(T / cycle + seed);
  vec2 o = vec2((hash1(k * 3.1 + seed) - 0.5) * 1.4 * uAspect, 0.1 + hash1(k * 7.7 + seed) * 0.35);
  vec3 hue = hsv(hash1(k + seed * 9.0), 0.75, 1.0);
  for (int i = 0; i < 24; i++) {
    float a = float(i) / 24.0 * TAU + hash1(k + float(i)) * 0.2;
    float sp = 0.45 + hash1(float(i) * 1.3 + k) * 0.2;
    vec2 q = o + vec2(cos(a), sin(a)) * sp * easeOut(age) * 0.9 - vec2(0.0, age * age * 0.25);
    float d = length(p - q);
    c += hue * 0.0009 / (d * d + 0.0009) * (1.0 - age) * 0.35;
  }
  return c;
}
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2); float I = P1; vec3 c = src(uv).rgb; vec3 col = c; vec2 p = asp(uv);
  #if MODE == 1
    col = c * (1.0 - 0.4 * I) + (firework(p, T, 0.0) + firework(p, T, 0.37) + firework(p, T, 0.71)) * 2.2 * I;
  #elif MODE == 2
    vec4 s = particles(uv, T, 10.0, vec2(0.1, 0.9), 0.24, 3.0, 11.0, P3, 1.0);
    vec4 s2 = particles(uv, T * 1.3, 16.0, vec2(-0.1, 1.2), 0.2, 3.0, 12.0, P3 + 0.3, 1.0);
    col = mix(mix(c, s.rgb, s.a * I), s2.rgb, s2.a * I);
  #elif MODE == 3
    vec4 s = particles(uv, T, 16.0, vec2(0.05, 0.5), 0.22, 1.0, 13.0, 0.12, 0.05);
    col = c + vec3(1.0, 0.82, 0.4) * s.a * 2.0 * I;
  #elif MODE == 4
    vec4 s = particles(uv, T, 8.0, vec2(0.05, 0.45), 0.25, 2.0, 14.0, 0.96 + P3 * 0.1, 0.1);
    col = mix(c, s.rgb, s.a * 0.9 * I);
  #elif MODE == 5
    vec4 s = particles(uv, T, 7.0, vec2(0.0, -0.35), 0.28, 5.0, 15.0, 0.55 + P3, 0.4);
    col = c + s.rgb * s.a * 0.6 * I;
  #elif MODE == 6
    vec4 s = particles(uv, T, 9.0, vec2(0.0, 0.15), 0.26, 6.0, 16.0, 0.14 + P3, 0.1);
    float pop = 0.6 + 0.4 * sin(T * 5.0);
    col = mix(c, s.rgb, s.a * pop * I);
  #elif MODE == 7
    vec4 s = particles(uv, T, 9.0, vec2(0.25, 0.4), 0.22, 4.0, 17.0, 0.93 + P3 * 0.1, 0.05);
    col = mix(c, s.rgb * vec3(1.0, 0.75, 0.82), s.a * I);
  #elif MODE == 8
    vec4 s = particles(uv, T, 12.0, vec2(0.08, 0.35), 0.17, 0.0, 18.0, 0.6, 0.0);
    col = mix(c, vec3(1.0), s.a * 0.9 * I);
  #elif MODE == 9
    // Party poppers from the bottom corners.
    vec3 burst = vec3(0.0);
    for (int side = 0; side < 2; side++) {
      float sgn = side == 0 ? 1.0 : -1.0; vec2 o = vec2(-0.5 * uAspect * sgn, -0.55);
      vec2 d = p - o; d.x *= sgn;
      float a = atan(d.y, d.x); float r = length(d);
      float stripe = step(0.5, hash(vec2(floor(a * 18.0), floor(r * 12.0 - T * 6.0))));
      float cone = smoothstep(0.35, 0.25, abs(a - 0.9)) * smoothstep(1.3, 0.2, r) * fract(-T * 0.7);
      burst += hsv(hash(vec2(floor(a * 18.0), 1.0)) + P3, 0.8, 1.0) * stripe * cone;
    }
    col = mix(c, burst, sat(luma(burst) * 2.0) * I);
  #elif MODE == 10
    // Sparkler trail tracing a moving path.
    vec3 s = vec3(0.0);
    for (int i = 0; i < 16; i++) {
      float t = T * 1.2 - float(i) * 0.04;
      vec2 q = vec2(sin(t * 1.3) * 0.45 * uAspect, cos(t * 0.9) * 0.3);
      s += vec3(1.0, 0.8, 0.4) * 0.0004 / (dot(p - q, p - q) + 0.0004) * (1.0 - float(i) / 16.0);
    }
    col = c + s * 0.8 * I;
  #endif
  return vec4(col, 1.0);
}
"""

internal const val FX_PIXEL = """
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2); float I = P1; vec3 c = src(uv).rgb; vec3 col = c;
  float cells = mix(90.0, 18.0, P3); vec2 grid = vec2(cells * uAspect, cells);
  vec2 id = floor(uv * grid); vec2 f = fract(uv * grid); vec3 block = src((id + 0.5) / grid).rgb;
  #if MODE == 1
    col = mix(c, block, I);
  #elif MODE == 2
    float x = fract(T * 0.3); col = mix(c, block, smoothstep(0.12, 0.0, abs(uv.x - x)) * I);
    col += vec3(0.2, 0.8, 1.0) * smoothstep(0.004, 0.0, abs(uv.x - x)) * I;
  #elif MODE == 3
    float n = floor(mix(8.0, 120.0, 0.5 + 0.5 * cos(T * 1.5))); vec2 g = vec2(n * uAspect, n);
    col = mix(c, src((floor(uv * g) + 0.5) / g).rgb, I);
  #elif MODE == 4
    // 8-bit: limited palette with ordered dithering.
    float bayer = fract(sin(dot(mod(id, 4.0), vec2(12.9898, 78.233))) * 43758.5453) - 0.5;
    vec3 q = floor(block * 4.0 + bayer * 0.9 + 0.5) / 4.0;
    col = mix(c, q, I);
  #elif MODE == 5
    // Pixel sort: bright pixels smear downward in streaks.
    vec3 best = c; float bl = luma(c);
    for (int i = 1; i < 14; i++) { vec3 s = src(uv + vec2(0.0, float(i) * 0.012 * I)).rgb; float l = luma(s); if (l > bl && l > 0.55) { best = s; bl = l; } }
    col = best;
  #elif MODE == 6
    // Blocks break away and fall.
    float h = hash(id); float fall = max(0.0, fract(T * 0.4) * 2.0 - h) * 0.6 * I;
    vec2 q = uv + vec2(0.0, fall); vec2 qid = floor(q * grid);
    col = hash(qid) < fract(T * 0.4) * 2.0 - 0.1 ? src((qid + 0.5) / grid - vec2(0.0, fall)).rgb * 0.9 : src(q).rgb;
  #elif MODE == 7
    // Beads: round glossy pixels.
    float r = length(f - 0.5);
    vec3 bead = block * (1.1 - r * 1.2) + vec3(1.0) * smoothstep(0.12, 0.0, length(f - vec2(0.35, 0.65))) * 0.5;
    col = mix(c, mix(vec3(0.05), bead, smoothstep(0.5, 0.44, r)), I);
  #elif MODE == 8
    // LED wall.
    vec2 sub = fract(uv * grid * vec2(3.0, 1.0)); float ch = mod(floor(uv.x * grid.x * 3.0), 3.0);
    vec3 led = block * vec3(ch < 0.5 ? 1.0 : 0.15, ch > 0.5 && ch < 1.5 ? 1.0 : 0.15, ch > 1.5 ? 1.0 : 0.15) * 2.2;
    col = mix(c, led * smoothstep(0.5, 0.35, abs(f.y - 0.5)), I);
  #elif MODE == 9
    // Cross-stitch.
    float x = min(abs(f.x - f.y), abs(f.x + f.y - 1.0));
    col = mix(c, mix(vec3(0.95, 0.92, 0.85), block * 1.1, smoothstep(0.18, 0.08, x)), I);
  #elif MODE == 10
    // Toy bricks with studs.
    float stud = smoothstep(0.3, 0.26, length(f - 0.5)) * (0.9 + 0.3 * (f.y - f.x));
    vec3 brick = block * (0.9 + 0.2 * stud) * (0.85 + 0.15 * smoothstep(0.0, 0.08, min(min(f.x, f.y), min(1.0 - f.x, 1.0 - f.y))));
    col = mix(c, brick, I);
  #elif MODE == 11
    float g = step(0.85, hash(vec2(floor(uv.y * 20.0), floor(T * 9.0))));
    vec2 q = uv + vec2(g * 0.08 * I, 0.0); vec2 gid = floor(q * grid);
    col = mix(c, src((gid + 0.5) / grid).rgb, max(g, 0.3) * I);
  #endif
  return vec4(col, 1.0);
}
"""

internal const val FX_GLITCH = """
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2); float I = P1; vec3 c = src(uv).rgb; vec3 col = c;
  #if MODE == 1
    float o = 0.012 * I * (1.0 + sin(T * 3.0)); col = vec3(src(uv + vec2(o, 0.0)).r, c.g, src(uv - vec2(o, 0.0)).b);
  #elif MODE == 2
    vec2 b = floor(uv * vec2(12.0, 20.0)); float h = hash(b + floor(T * 8.0)); float on = step(0.88, h);
    vec2 q = uv + (hash2(b + floor(T * 11.0)) - 0.5) * 0.12 * on * I;
    col = vec3(src(q + vec2(0.01, 0.0) * on).r, src(q).g, src(q - vec2(0.01, 0.0) * on).b);
    col = mix(col, 1.0 - col, on * step(0.97, h) * I);
  #elif MODE == 3
    float line = floor(uv.y * uSize.y * 0.5); float j = (hash(vec2(line, floor(T * 30.0))) - 0.5) * 0.004 * I;
    float band = smoothstep(0.02, 0.0, abs(uv.y - fract(T * 0.2))) * 0.03 * I;
    vec2 q = uv + vec2(j + band, 0.0);
    col = vec3(src(q + vec2(0.004, 0.0)).r, src(q).g, src(q - vec2(0.004, 0.0)).b);
    col = mix(col, col * vec3(1.05, 0.95, 1.1), I) + (hash(uv * uSize + T) - 0.5) * 0.08 * I;
    col *= 0.95 + 0.05 * sin(uv.y * uSize.y * 1.5);
  #elif MODE == 4
    float row = floor(uv.y * 60.0); vec2 q = uv + vec2((hash(vec2(row, floor(T * 15.0))) - 0.5) * 0.03 * I * step(0.7, hash(vec2(row, floor(T * 4.0)))), 0.0);
    col = src(q).rgb * (0.9 + 0.1 * sin(uv.y * uSize.y * 2.0));
  #elif MODE == 5
    // Datamosh: moving blocks drag the previous frame along.
    vec2 b = floor(uv * vec2(24.0, 40.0));
    float moved = length(src((b + 0.5) / vec2(24.0, 40.0)).rgb - prev((b + 0.5) / vec2(24.0, 40.0)));
    vec2 q = uv + (hash2(b + floor(T * 5.0)) - 0.5) * 0.03 * I;
    col = mix(c, prev(q), step(0.08, moved) * 0.85 * I);
  #elif MODE == 6
    float band = step(0.9, hash(vec2(floor(uv.y * 8.0), floor(T * 6.0))));
    col = mix(c, vec3(hash(uv * uSize + T)), band * I);
    col = mix(col, vec3(0.0, 0.0, 0.3), step(0.97, hash(vec2(floor(T * 3.0), 9.0))) * I);
  #elif MODE == 7
    float o = sin(uv.y * 20.0 + T * 5.0) * 0.012 * I;
    col = vec3(src(uv + vec2(o, 0.0)).r, src(uv).g, src(uv - vec2(o, 0.0)).b);
  #elif MODE == 8
    float roll = fract(T * 0.15); vec2 q = vec2(uv.x + sin(uv.y * 40.0 + T * 10.0) * 0.003 * I, fract(uv.y + roll * I * 0.3));
    col = src(q).rgb * (0.85 + 0.15 * sin(q.y * uSize.y)) + (hash(uv * uSize + T) - 0.5) * 0.12 * I;
  #elif MODE == 9
    float f = step(0.8, hash(vec2(floor(T * 10.0), 3.0)));
    vec2 q = uv + vec2((hash(vec2(floor(uv.y * 30.0), floor(T * 20.0))) - 0.5) * 0.1 * f * I, 0.0);
    col = mix(c, vec3(src(q + vec2(0.02, 0.0)).r, src(q).g, src(q - vec2(0.02, 0.0)).b) + f * 0.15, f);
  #endif
  return vec4(col, 1.0);
}
"""

internal const val FX_RETRO = """
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2); float I = P1; vec3 c = src(uv).rgb; vec3 col = c; vec2 p = asp(uv);
  float grain = hash(floor(uv * uSize * 0.6) + fract(T * 13.0) * 50.0) - 0.5;
  #if MODE == 1
    vec2 q = uv + vec2((hash(vec2(floor(uv.y * 90.0), floor(T * 24.0))) - 0.5) * 0.006 * I, 0.0);
    col = vec3(src(q + vec2(0.005, 0.0)).r, src(q).g, src(q - vec2(0.006, 0.0)).b);
    col = mix(col, col * vec3(1.05, 0.95, 1.1), I) + grain * 0.1 * I;
    col += vec3(1.0) * smoothstep(0.015, 0.0, abs(uv.y - fract(-T * 0.5))) * 0.3 * I;
  #elif MODE == 2
    vec2 q = uv + vec2(0.0, (vnoise(vec2(T * 6.0, 0.0)) - 0.5) * 0.006 * I);
    vec3 s = src(q).rgb; float l = luma(s);
    col = mix(s, vec3(l) * vec3(1.1, 0.95, 0.75), 0.7 * I) + grain * 0.18 * I;
    col *= 1.0 - smoothstep(0.4, 1.4, dot(p, p) * 2.0) * 0.8 * I;
    col *= 0.93 + 0.07 * step(0.5, hash(vec2(floor(T * 12.0), 1.0)));
    col -= step(0.995, hash(vec2(floor(uv.x * 300.0), floor(T * 12.0)))) * 0.4 * I;
  #elif MODE == 3
    vec2 d = uv - 0.5; vec2 q = 0.5 + d * (1.0 + dot(d, d) * 0.25 * I);
    col = inside(q) > 0.5 ? src(q).rgb : vec3(0.0);
    col *= 0.8 + 0.2 * sin(q.y * uSize.y * 1.2); col += grain * 0.12 * I;
    col *= 1.0 - smoothstep(0.35, 0.75, length(d)) * I;
  #elif MODE == 4
    float burn = fbm(uv * 2.5 + T * 0.2) * sat(1.2 - length(p) * 0.5);
    col = c + vec3(1.0, 0.45, 0.15) * smoothstep(0.55, 0.9, burn + (1.0 - uv.x) * 0.2) * 1.2 * I;
  #elif MODE == 5
    col = mix(c, (c - 0.5) * 0.8 + 0.55 + vec3(0.03, 0.01, -0.03), I);
  #elif MODE == 6
    col = mix(c, c * vec3(1.12, 0.98, 0.78) + vec3(0.06, 0.03, 0.0), I) + grain * 0.08 * I;
  #elif MODE == 7
    float scratch = step(0.997, hash(vec2(floor(uv.x * 400.0), floor(T * 10.0))));
    float dust = step(0.9994, hash(floor(uv * uSize * 0.5) + floor(T * 12.0)));
    col = c + (scratch + dust) * 0.6 * I - dust * 0.8 * I * step(0.5, hash(vec2(T, 1.0)));
  #elif MODE == 8
    float s = 70.0; vec2 g = uv * vec2(uAspect, 1.0) * s; vec2 f = fract(g) - 0.5;
    vec3 inkc = vec3(0.0); vec3 lv = vec3(1.0) - c;
    for (int k = 0; k < 3; k++) { vec2 gg = rot(float(k) * 0.5 + 0.2) * uv * vec2(uAspect, 1.0) * s; vec2 ff = fract(gg) - 0.5;
      float ch = k == 0 ? lv.r : (k == 1 ? lv.g : lv.b); inkc[k] = smoothstep(0.05, 0.0, length(ff) - ch * 0.5); }
    col = mix(c, vec3(0.97, 0.95, 0.9) - inkc * 0.85, I);
  #elif MODE == 9
    col = mix(c, vec3(luma(c)) * vec3(1.15, 1.0, 0.8) * (0.9 + 0.1 * sin(T * 30.0)), I);
  #endif
  return vec4(col, 1.0);
}
"""

