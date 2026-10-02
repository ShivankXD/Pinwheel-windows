package com.pinwheel.core.media.video

/*
 * Clip transitions, overlays and montage looks. Transitions run over a window centred on a cut:
 * uProgress 0..0.5 is the outgoing clip, 0.5..1 the incoming one. P1 intensity, P2 speed curve.
 */

internal const val FX_TRANSITIONS = """
vec3 mirrorSrc(vec2 uv) { return srcm(uv); }
float sdStar(vec2 p, float r) {
  float a = atan(p.y, p.x) + PI * 0.5; float seg = TAU / 5.0;
  a = mod(a, seg) - seg * 0.5; float e = mix(0.5, 1.0, abs(a) / (seg * 0.5));
  return length(p) - r * 0.55 / e;
}
vec4 fx(vec2 uv) {
  float pr = uProgress;
  bool outp = pr < 0.5;
  float h = outp ? pr * 2.0 : pr * 2.0 - 1.0;
  float sp = mix(1.0, 3.2, P2);
  float eOut = pow(h, sp);
  float eIn = 1.0 - pow(1.0 - h, sp);
  float m = (outp ? eOut : 1.0 - eIn) * P1;
  float sgn = outp ? 1.0 : -1.0;
  vec2 p = asp(uv);
  vec3 base = src(uv).rgb; vec3 c = base;
#if MODE == 1
  c = mix(base, blurDisc(uv, 0.035 * m) * 1.08 + 0.06 * m, m);
#elif MODE == 2
  c = base * (1.0 - m);
#elif MODE == 3
  c = mix(base, vec3(1.0), m);
#elif MODE == 4
  c = blurDisc(uv, 0.08 * m);
#elif MODE == 5
  float lid = abs(uv.y - 0.5) * 2.0;
  c = lid > 1.0 - m * 1.05 ? vec3(0.0) : base * (1.0 - 0.3 * m);
#elif MODE == 6
  float z = 1.0 + m * 0.9;
  vec3 acc = vec3(0.0);
  for (int i = 0; i < 8; i++) acc += mirrorSrc((uv - 0.5) / (z * (1.0 + float(i) * 0.025 * m)) + 0.5);
  c = acc / 8.0;
#elif MODE == 7
  float z = 1.0 / (1.0 + m * 0.7);
  vec3 acc = vec3(0.0);
  for (int i = 0; i < 8; i++) acc += mirrorSrc((uv - 0.5) / (z * (1.0 - float(i) * 0.02 * m)) + 0.5);
  c = acc / 8.0;
#elif MODE == 8
  vec3 acc = vec3(0.0);
  for (int i = 0; i < 8; i++) {
    float f = float(i) / 7.0;
    acc += mirrorSrc(unasp(rot(sgn * (m + f * 0.12 * m) * PI) * p / (1.0 + m * 0.5)));
  }
  c = acc / 8.0;
#elif MODE == 9 || MODE == 10
  float dir = MODE == 9 ? 1.0 : -1.0;
  vec3 acc = vec3(0.0);
  for (int i = 0; i < 8; i++) acc += mirrorSrc(unasp(rot(dir * sgn * (m + float(i) * 0.03 * m) * 1.4) * p * (1.0 + 0.2 * m)));
  c = acc / 8.0;
#elif MODE == 11
  vec2 o = (vec2(vnoise(vec2(uTime * 30.0, 1.0)), vnoise(vec2(3.0, uTime * 30.0))) - 0.5) * 0.12 * m;
  c = mirrorSrc(uv + o);
  c = mix(c, vec3(src(uv + o + vec2(0.01 * m, 0.0)).r, c.g, src(uv + o - vec2(0.01 * m, 0.0)).b), m);
#elif MODE >= 12 && MODE <= 15
  vec2 d = MODE == 12 ? vec2(-1.0, 0.0) : MODE == 13 ? vec2(1.0, 0.0) : MODE == 14 ? vec2(0.0, 1.0) : vec2(0.0, -1.0);
  vec2 off = outp ? -d * eOut : d * (1.0 - eIn);
  vec3 acc = vec3(0.0);
  for (int i = 0; i < 10; i++) acc += mirrorSrc(uv + off * P1 - d * float(i) * 0.02 * m);
  c = acc / 10.0;
#elif MODE >= 16 && MODE <= 19
  vec2 d = MODE == 16 ? vec2(-1.0, 0.0) : MODE == 17 ? vec2(1.0, 0.0) : MODE == 18 ? vec2(0.0, 1.0) : vec2(0.0, -1.0);
  vec2 off = (outp ? -d * eOut : d * (1.0 - eIn)) * P1;
  vec2 q = uv + off;
  c = inside(q) > 0.5 ? src(q).rgb : blur9(uv, 0.03) * 0.25;
#elif MODE == 20
  float sx = max(1.0 - m, 0.001);
  vec2 q = vec2((uv.x - 0.5) / sx + 0.5, uv.y);
  c = inside(q) > 0.5 ? src(q).rgb : vec3(0.0);
#elif MODE >= 21 && MODE <= 24
  float r = (1.0 - m) * 1.25 * max(uAspect, 1.0);
  float d;
  if (MODE == 21) d = length(p) - r;
  else if (MODE == 22) d = sdHeart(p / max(r, 0.001) * vec2(1.0, -1.0) * 0.9 * vec2(1.0, -1.0) + vec2(0.0, 0.55)) * r;
  else if (MODE == 23) d = sdStar(p, r * 1.4);
  else d = (abs(p.x) + abs(p.y)) - r * 1.2;
  c = d > 0.0 ? vec3(0.0) : base;
  c += vec3(1.0) * exp(-abs(d) * 160.0) * 0.6 * m;
#elif MODE == 25
  float a = fract(atan(p.x, p.y) / TAU + 0.5);
  c = (outp ? a < m : a > 1.0 - m) ? vec3(0.0) : base;
#elif MODE == 26
  c = fract(uv.x * 8.0) < m ? vec3(0.0) : base;
#elif MODE == 27
  c = abs(uv.x - 0.5) * 2.0 > 1.0 - m ? vec3(0.0) : base;
#elif MODE == 28
  float s = floor(uTime * 24.0);
  float band = floor(uv.y * 18.0);
  float shift = (hash(vec2(band, s)) - 0.5) * 0.25 * m * step(0.4, hash(vec2(band + 3.0, s)));
  vec2 q = uv + vec2(shift, 0.0);
  c = vec3(src(q + vec2(0.02 * m, 0.0)).r, src(q).g, src(q - vec2(0.02 * m, 0.0)).b);
#elif MODE == 29
  vec2 o = normalize(p + 1e-4) * 0.05 * m / vec2(uAspect, 1.0);
  c = vec3(src(uv + o).r, src(uv).g, src(uv - o).b) * (1.0 + 0.2 * m);
#elif MODE == 30
  float n = mix(160.0, 8.0, m);
  vec2 g = vec2(n * uAspect, n);
  c = src((floor(uv * g) + 0.5) / g).rgb;
#elif MODE == 31
  float r = length(p);
  c = mirrorSrc(unasp(rot(sgn * m * 7.0 * max(0.0, 0.9 - r)) * p));
#elif MODE == 32
  float r = length(p);
  c = src(uv + normalize(p + 1e-4) * sin(r * 40.0 - uTime * 20.0) * 0.02 * m).rgb;
#elif MODE == 33
  float n = fbm(uv * 3.0 + uTime * 0.6);
  vec3 leak = mix(vec3(1.0, 0.45, 0.12), vec3(1.0, 0.9, 0.6), n);
  c = 1.0 - (1.0 - base) * (1.0 - leak * m * 1.2);
  c = mix(c, vec3(1.0), smoothstep(0.7, 1.0, m));
#elif MODE == 34
  float n = fbm(p * 3.0 + 2.0) + length(p) * 0.7;
  float burn = smoothstep(n - 0.15, n + 0.05, m * 1.6);
  c = mix(base, vec3(1.0, 0.55, 0.15) * 1.3, burn * (1.0 - smoothstep(0.85, 1.0, m)));
  c = mix(c, vec3(1.0, 0.95, 0.85), smoothstep(0.85, 1.0, m));
#elif MODE == 35
  float r = length(p); float a = atan(p.y, p.x); float seg = TAU / 6.0;
  float ka = abs(mod(a + m * 2.0, seg) - seg * 0.5);
  c = mix(base, mirrorSrc(unasp(vec2(cos(ka), sin(ka)) * r)), smoothstep(0.0, 0.4, m));
#elif MODE == 36
  float z = 1.0 + 0.35 * m;
  c = mirrorSrc((uv - 0.5) / z + 0.5);
  c = mix(c, vec3(1.0), smoothstep(0.55, 1.0, m));
#elif MODE == 37
  float s = uv.x + uv.y * 0.6;
  c = fract(s * 7.0) < m ? vec3(1.0) : base;
#elif MODE == 38
  vec3 acc = vec3(0.0);
  for (int i = -5; i <= 5; i++) acc += mirrorSrc(uv + vec2(0.0, float(i) * 0.018 * m));
  c = acc / 11.0;
#elif MODE == 39
  float z = 1.0 + 0.45 * m;
  vec2 o = (vec2(hash(vec2(floor(uTime * 30.0), 1.0)), hash(vec2(2.0, floor(uTime * 30.0)))) - 0.5) * 0.05 * m;
  c = mirrorSrc((uv - 0.5) / z + 0.5 + o) * (1.0 + 0.3 * m);
#elif MODE == 40
  float f = step(0.5, hash(vec2(floor(uTime * 18.0), 3.0)));
  c = mix(base, base * vec3(1.5, 0.9, 0.5) + vec3(0.25, 0.08, 0.0), m * (0.4 + 0.6 * f));
#elif MODE == 41
  float s = (uv.x + uv.y) * 0.5;
  float g = exp(-pow((s - mix(-0.3, 1.3, outp ? eOut : 1.0 - eIn)) * 5.0, 2.0));
  c = mix(base, vec3(1.0), g * P1) + vec3(1.0) * m * 0.3;
#elif MODE == 42
  float y = outp ? 1.0 - eOut : 1.0 - eIn;
  c = (outp ? uv.y > y : uv.y < y) ? blur9(uv, 0.02) * 0.2 : base;
  c += vec3(0.7, 0.95, 1.0) * exp(-abs(uv.y - y) * 90.0) * 1.3 * P1;
#elif MODE == 43
  float z = outp ? 1.0 + eOut * 0.6 * P1 : 1.0 + (1.0 - eIn) * 0.6 * P1;
  c = mirrorSrc((uv - 0.5) / z + 0.5);
#elif MODE == 44
  float z = 1.0 + 0.4 * m;
  vec3 acc = vec3(0.0);
  for (int i = 0; i < 8; i++) acc += mirrorSrc((uv - 0.5) / z + 0.5 + vec2(sgn * (float(i) * 0.03 + 0.3) * m, 0.0));
  c = acc / 8.0;
#elif MODE == 45
  float edge = abs(uv.y - 0.5) * 2.0;
  c = edge > 1.0 - m ? vec3(0.02) : base;
  c += vec3(1.0) * exp(-abs(edge - (1.0 - m)) * 120.0) * m;
#elif MODE == 46
  vec2 q = uv + vec2(sin(uv.y * 30.0 + uTime * 25.0), cos(uv.x * 24.0 + uTime * 20.0)) * 0.015 * m;
  c = mix(src(q).rgb, vec3(1.0, 0.8, 0.55), smoothstep(0.5, 1.0, m));
#else
  c = base;
#endif
  return vec4(c, 1.0);
}
"""

/** Full-frame overlay layers blended over the picture; P1 is opacity, P3 colour. */
internal const val FX_OVERLAYS = """
vec3 scr(vec3 a, vec3 b) { return 1.0 - (1.0 - a) * (1.0 - b); }
vec4 fx(vec2 uv) {
  float I = P1; float t = uTime * spd(P2);
  vec3 base = src(uv).rgb; vec3 c = base;
  vec2 p = asp(uv);
  vec3 tint = hsv(P3, 0.75, 1.0);
#if MODE == 1
  float n = fbm(vec2(uv.x * 1.6 + t * 0.15, uv.y * 1.1 - t * 0.08));
  float m = smoothstep(0.3, 0.8, n) * (0.6 + 0.9 * sat(1.3 - uv.x - uv.y * 0.3));
  c = scr(base, mix(tint, vec3(1.0, 0.95, 0.8), 0.2) * m * 2.2 * I);
#elif MODE == 2
  float x = fract(t * 0.25) * 1.8 - 0.4;
  vec2 f = vec2(x * uAspect - 0.5 * uAspect, 0.25);
  float d = length(p - f);
  vec3 fl = tint * exp(-d * 6.0) * 0.8 + vec3(1.0) * exp(-d * 40.0);
  for (int i = 1; i < 5; i++) { vec2 g = -f * (float(i) * 0.35); fl += hsv(P3 + float(i) * 0.1, 0.6, 1.0) * exp(-length(p - g) * 30.0) * 0.35; }
  c = scr(base, fl * I);
#elif MODE == 3
  vec4 a = particles(uv, t * 0.4, 5.0, vec2(0.05, -0.08), 0.22, 5.0, 3.0, P3, 0.12);
  vec4 b = particles(uv + 0.4, t * 0.3, 3.0, vec2(-0.04, -0.05), 0.3, 0.0, 7.0, P3, 0.1);
  c = scr(base, (a.rgb * a.a * 1.1 + b.rgb * b.a * 0.9) * I);
#elif MODE == 4
  float k = floor(t * 12.0);
  float scratch = (1.0 - smoothstep(0.0, 0.002, abs(uv.x - hash1(k)))) * step(0.5, hash1(k * 3.1));
  float dust = step(0.998, hash(floor(uv * uSize * 0.35) + k));
  float hair = (1.0 - smoothstep(0.0, 0.003, abs(uv.y - 0.3 - 0.2 * sin(uv.x * 9.0 + k)))) * step(0.85, hash1(k * 7.0)) * step(uv.x, 0.4);
  c = base * (1.0 - (scratch * 0.35 + dust * 0.8 + hair * 0.6) * I) + vec3(0.08) * (hash(uv * uSize + k) - 0.5) * I;
#elif MODE == 5
  c = base + (hash(uv * uSize + floor(t * 24.0)) - 0.5) * 0.28 * I;
#elif MODE == 6
  float k = floor(t * 20.0);
  float scan = 0.88 + 0.12 * sin(uv.y * uSize.y * 1.4);
  float track = exp(-abs(uv.y - fract(t * 0.3)) * 50.0);
  vec2 q = uv + vec2(track * 0.02 * I, 0.0);
  vec3 v = vec3(src(q + vec2(0.003, 0.0)).r, src(q).g, src(q - vec2(0.003, 0.0)).b) * scan;
  v += (hash(uv * uSize * 0.5 + k) - 0.5) * 0.1 + track * 0.25;
  c = mix(base, v, I);
#elif MODE == 7
  vec4 s = particles(uv, t, 12.0, vec2(0.0, -0.02), 0.12, 1.0, 5.0, P3, 0.15);
  c = base + s.rgb * s.a * 1.4 * I;
#elif MODE == 8
  vec4 a = particles(uv, t, 22.0, vec2(0.01, 0.2), 0.06, 1.0, 5.0, 0.12, 0.04);
  c = base + a.rgb * a.a * vec3(1.0, 0.85, 0.45) * 1.6 * I;
#elif MODE == 9
  vec4 a = particles(uv, t, 9.0, vec2(0.08, 0.35), 0.1, 0.0, 4.0, 0.6, 0.0);
  vec4 b = particles(uv + 0.3, t, 16.0, vec2(0.03, 0.22), 0.07, 0.0, 8.0, 0.6, 0.0);
  c = base + vec3(1.0) * (a.a + b.a * 0.7) * I;
#elif MODE == 10
  vec2 q = vec2(uv.x * 90.0 + uv.y * 10.0, uv.y * 5.0 + t * 8.0);
  float drop = step(0.965, hash(floor(q))) * smoothstep(0.0, 0.7, fract(q.y));
  c = mix(base, base * vec3(0.8, 0.85, 0.95), 0.4 * I) + vec3(0.85, 0.9, 1.0) * drop * 0.6 * I;
#elif MODE == 11
  vec4 a = particles(uv, t, 5.0, vec2(0.0, -0.3), 0.14, 2.0, 1.0, P3, 0.06);
  c = mix(base, a.rgb * 1.15, a.a * I);
#elif MODE == 12
  vec4 s = particles(uv, t * 0.5, 16.0, vec2(0.0), 0.12, 6.0, 9.0, 0.14, 0.1);
  c = base + vec3(1.0, 0.95, 0.8) * s.a * I;
#elif MODE == 13
  vec4 a = particles(uv, t, 9.0, vec2(0.05, 0.5), 0.12, 3.0, 2.0, 0.0, 1.0);
  c = mix(base, a.rgb, a.a * I);
#elif MODE == 14
  float n = fbm(vec2(p.x * 2.0 - t * 0.2, p.y * 3.0 + t * 0.05));
  float fog = smoothstep(0.35, 0.85, n) * (0.4 + 0.6 * (1.0 - uv.y));
  c = mix(base, mix(vec3(0.9), tint, 0.25), fog * 0.75 * I);
#elif MODE == 15
  vec4 e = particles(uv, t, 14.0, vec2(0.03, -0.5), 0.07, 0.0, 6.0, 0.06, 0.05);
  c = base + vec3(1.0, 0.5, 0.15) * e.a * 1.6 * I + vec3(1.0, 0.35, 0.1) * 0.12 * (1.0 - uv.y) * I;
#elif MODE == 16
  vec3 rb = hsv(fract(uv.x * 0.8 + uv.y * 0.3 + t * 0.05), 0.6, 1.0);
  c = mix(base, base * rb * 1.35, 0.45 * I);
#elif MODE == 17
  vec2 o = asp(vec2(0.05, 1.05));
  vec2 d = p - o; float a = atan(d.y, d.x);
  float rays = pow(0.5 + 0.5 * sin(a * 18.0 + t * 0.6) * sin(a * 7.0 - t * 0.4), 3.0) * exp(-length(d) * 1.1);
  c = scr(base, vec3(1.0, 0.9, 0.7) * rays * 1.2 * I);
#elif MODE == 18
  vec3 g = mix(tint, hsv(fract(P3 + 0.12), 0.7, 1.0), uv.y);
  c = mix(base, scr(base * 0.9, g * 0.55), I);
#elif MODE == 19
  vec2 e = abs(uv - 0.5);
  float band = step(0.42, e.x);
  float hole = step(0.445, e.x) * step(e.x, 0.475) * step(0.3, fract(uv.y * 14.0 + t * 0.3)) * step(fract(uv.y * 14.0 + t * 0.3), 0.7);
  vec3 f = mix(vec3(0.04, 0.03, 0.02), vec3(0.95, 0.9, 0.8), hole);
  c = mix(base, f, band * I);
#elif MODE == 20
  float d = sdBox(p, vec2(0.5 * uAspect - 0.035, 0.5 - 0.035)) ;
  c = mix(base, vec3(0.97, 0.96, 0.93), step(0.0, d) * I);
#elif MODE == 21
  float d = sdBox(p, vec2(0.5 * uAspect - 0.05, 0.5 - 0.05));
  c = base + tint * (exp(-abs(d) * 160.0) * 1.2 + exp(-abs(d) * 25.0) * 0.35) * (0.85 + 0.15 * sin(t * 6.0)) * I;
#elif MODE == 22
  vec2 e = abs(uv - 0.5); vec2 mm = vec2(0.43, 0.45);
  float lw = 0.004;
  float corner = step(mm.x - lw, e.x) * step(e.x, mm.x) * step(mm.y - 0.07, e.y) * step(e.y, mm.y)
               + step(mm.y - lw * uAspect, e.y) * step(e.y, mm.y) * step(mm.x - 0.07 / uAspect, e.x) * step(e.x, mm.x);
  float dotd = length((uv - vec2(0.12, 0.9)) * vec2(uAspect, 1.0));
  c = mix(base, vec3(1.0), sat(corner) * I);
  c = mix(c, vec3(1.0, 0.1, 0.1), (1.0 - smoothstep(0.01, 0.014, dotd)) * step(0.5, fract(t)) * I);
#elif MODE == 23
  c = base * (1.0 - I * 0.85 * sat(dot(p, p) * 1.9 - 0.1));
#elif MODE == 24
  float n = 80.0; vec2 g = vec2(n * uAspect, n);
  vec2 f = fract(uv * g) - 0.5;
  float l = luma(base);
  c = mix(base, base * step(length(f), 0.62 * (1.0 - l) + 0.1), 0.5 * I);
#elif MODE == 25
  float n = fbm(uv * vec2(40.0, 30.0)) * 0.6 + fbm(uv * 6.0) * 0.4;
  c = mix(base, base * vec3(1.05, 1.0, 0.9) * (0.85 + 0.3 * n), I);
#elif MODE == 26
  vec4 b = particles(uv, t, 6.0, vec2(0.02, -0.25), 0.16, 5.0, 4.0, 0.55, 0.3);
  c = base + b.rgb * b.a * 0.7 * I;
#elif MODE == 27
  vec2 g = abs(fract(uv * vec2(3.0, 3.0)) - 0.5);
  float line = step(0.497, max(g.x, g.y));
  c = mix(base, vec3(1.0), line * 0.6 * I);
#elif MODE == 28
  float n = fbm(p * 4.0);
  float edge = sat((length(p) - 0.42 + (n - 0.5) * 0.4) * 3.0);
  c = mix(base, base * vec3(0.9, 0.75, 0.55), 0.4 * I);
  c = mix(c, vec3(0.25, 0.12, 0.04), edge * I);
#elif MODE == 29
  float k = floor(t * 3.0); float fl = step(0.8, hash1(k)) * exp(-fract(t * 3.0) * 8.0);
  c = base + vec3(0.8, 0.85, 1.0) * fl * I;
#else
  c = base;
#endif
  return vec4(c, 1.0);
}
"""

/** Beat-driven montage looks for fast edits. */
internal const val FX_MONTAGE = """
vec4 fx(vec2 uv) {
  float I = P1; float t = uTime * spd(P2);
  vec3 base = src(uv).rgb; vec3 c = base;
  vec2 p = asp(uv);
  float beat = fract(t * 1.2); float k = floor(t * 1.2);
  float hit = exp(-beat * 6.0);
#if MODE == 1
  float z = 1.0 + 0.25 * I * hit;
  vec3 acc = vec3(0.0);
  for (int i = 0; i < 8; i++) acc += srcm((uv - 0.5) / (z * (1.0 - float(i) * 0.018 * hit * I)) + 0.5);
  c = acc / 8.0;
#elif MODE == 2
  float a = (mod(k, 2.0) < 1.0 ? 1.0 : -1.0) * 0.25 * I * hit;
  c = srcm(unasp(rot(a) * p / (1.0 + 0.15 * hit * I)));
#elif MODE == 3
  float fl = exp(-beat * 14.0);
  float tilt = (hash1(k) - 0.5) * 0.12 * I;
  vec2 q = unasp(rot(tilt) * p / 0.86);
  float border = step(0.0, sdBox(p, vec2(0.43 * uAspect, 0.43)));
  vec3 photo = inside(q) > 0.5 ? src(q).rgb : vec3(1.0);
  photo = mix(photo, vec3(1.0), border);
  c = mix(base, blur9(uv, 0.03) * 0.5, I);
  float inCard = 1.0 - step(0.0, sdBox(rot(-tilt) * p, vec2(0.46 * uAspect, 0.46)));
  c = mix(c, photo, inCard * I);
  c = mix(c, vec3(1.0), fl * I);
#elif MODE == 4
  vec2 o = (vec2(hash1(k * 2.0 + floor(beat * 12.0)), hash1(k * 3.0 + floor(beat * 12.0))) - 0.5) * 0.06 * I * hit;
  c = srcm((uv - 0.5) / (1.0 + 0.12 * hit * I) + 0.5 + o);
#elif MODE == 5
  float side = uv.x < 0.5 ? 1.0 : -1.0;
  float off = side * (1.0 - easeOut(sat(beat * 3.0))) * 0.5 * I;
  vec2 q = vec2(uv.x + off, uv.y);
  c = inside(q) > 0.5 ? src(q).rgb : vec3(0.0);
  c *= step(0.004, abs(uv.x - 0.5));
#elif MODE == 6
  c = mix(base, blurDisc(uv, 0.05 * hit), I);
  c *= 1.0 + 0.25 * hit * I;
#elif MODE == 7
  vec2 q = mod(k, 2.0) < 1.0 ? uv : vec2(1.0 - uv.x, uv.y);
  c = mix(base, src(q).rgb, I);
#elif MODE == 8
  float n = mod(k, 3.0) + 1.0;
  float z = 1.0 + n * 0.12 * I;
  c = src((uv - 0.5) / z + 0.5).rgb;
#elif MODE == 9
  vec2 o = vec2(0.02 * I * hit, 0.0);
  c = vec3(src(uv + o).r, src(uv).g, src(uv - o).b);
  c = srcm((uv - 0.5) / (1.0 + 0.1 * hit * I) + 0.5) * 0.4 + c * 0.6;
#elif MODE == 10
  float a = (mod(k, 2.0) < 1.0 ? 1.0 : -1.0) * 0.14 * I;
  c = srcm(unasp(rot(a) * p / 1.12));
#elif MODE == 11
  vec3 acc = base;
  for (int i = 1; i < 5; i++) { float f = float(i); acc = max(acc, src((uv - 0.5) / (1.0 + f * 0.07 * I * (0.5 + hit)) + 0.5).rgb * (1.0 - f * 0.18)); }
  c = mix(base, acc, I);
#elif MODE == 12
  float z = 1.0 + 0.08 * I * fract(uTime * 0.12);
  c = src((uv - 0.5) / z + 0.5).rgb * vec3(1.03, 1.0, 0.95);
  float bar = 0.12 * I;
  c *= step(bar, uv.y) * step(uv.y, 1.0 - bar);
#else
  c = base;
#endif
  return vec4(c, 1.0);
}
"""

/** Crisp Y2K flip phone and the upgraded 3D phone. */
internal const val FX_PHONES = """
float rbx(vec2 q, vec2 b, float r) { return sdBox(q, b - r) - r; }
vec2 fitUv(vec2 q, vec2 b) {
  vec2 u = q / (2.0 * b) + 0.5; float k = (b.x / b.y) / uAspect;
  if (k < 1.0) u.x = (u.x - 0.5) * k + 0.5; else u.y = (u.y - 0.5) / k + 0.5;
  return u;
}
vec3 plane(vec2 uv, mat3 m, vec3 c) {
  vec3 rd = vec3(asp(uv) * 2.0, 2.2); vec3 n = m * vec3(0.0, 0.0, -1.0);
  float den = dot(rd, n); if (abs(den) < 1e-4) return vec3(0.0, 0.0, -1.0);
  float d = dot(c, n) / den; if (d <= 0.0) return vec3(0.0, 0.0, -1.0);
  vec3 l = rd * d - c; return vec3(dot(l, m * vec3(1.0, 0.0, 0.0)), dot(l, m * vec3(0.0, 1.0, 0.0)), 1.0);
}
vec3 studio(vec2 uv, vec3 tint) {
  vec3 b = blurDisc((uv - 0.5) * 0.8 + 0.5, 0.09);
  vec2 v = asp(uv);
  return mix(b * 0.75, tint, 0.35) * (1.0 - 0.45 * dot(v, v));
}
vec4 fx(vec2 uv) {
  float I = P1; float t = uTime * spd(P2);
  vec3 base = src(uv).rgb; vec3 c = base;
  vec2 p = asp(uv);
  float S = min(1.0, uAspect / 0.56);
#if MODE == 1
  // 3D Phone: glides in, turns in space; titanium frame, buttons, island, glass reflection.
  c = studio(uv, vec3(0.12, 0.14, 0.22));
  float intro = easeOut(sat(uTime * 1.6));
  float ry = 0.5 * sin(t * 0.8) * intro + (1.0 - intro) * 0.9;
  mat3 m = rotY(ry) * rotX(0.12 * sin(t * 0.6 + 1.0)) * rotZ(0.04 * sin(t * 0.5));
  vec3 ctr = vec3(0.0, 0.0, mix(4.2, 2.35, intro));
  vec2 bh = vec2(0.46, 0.96) * S;
  float sh = length((p - vec2(0.12 * ry, -0.5 * S)) / vec2(0.34 * S, 0.05));
  c *= 1.0 - 0.55 * exp(-sh * sh * 1.2);
  for (int k = 3; k >= 1; k--) {
    vec3 side = plane(uv, m, ctr + m * vec3(0.0, 0.0, float(k) * 0.022));
    if (side.z > 0.0 && rbx(side.xy, bh, 0.13 * S) < 0.0) c = mix(vec3(0.42, 0.44, 0.5), vec3(0.8, 0.82, 0.88), 0.5 + 0.5 * sin(side.y * 6.0 + ry * 3.0)) * 0.8;
  }
  vec3 h = plane(uv, m, ctr);
  if (h.z > 0.0) {
    float body = rbx(h.xy, bh, 0.13 * S);
    if (body < 0.0) {
      c = mix(vec3(0.55, 0.57, 0.63), vec3(0.93, 0.94, 0.97), 0.5 + 0.5 * sin(h.x * 9.0 + ry * 4.0));
      vec2 sb = bh - 0.028 * S;
      float scr = rbx(h.xy, sb, 0.105 * S);
      if (scr < 0.0) {
        c = scr > -0.012 * S ? vec3(0.01) : src(fitUv(h.xy, sb)).rgb;
        float isl = rbx(h.xy - vec2(0.0, sb.y - 0.075 * S), vec2(0.105, 0.03) * S, 0.03 * S);
        if (isl < 0.0) c = vec3(0.0);
        c += vec3(0.28) * exp(-pow((h.x * 0.7 + h.y * 0.45 - ry * 1.6 + 0.2) * 3.2, 2.0));
      }
    }
    if (h.x < -bh.x && h.x > -bh.x - 0.018 && abs(h.y - 0.35 * S) < 0.14 * S) c = vec3(0.6, 0.62, 0.68);
    if (h.x > bh.x && h.x < bh.x + 0.018 && abs(h.y - 0.3 * S) < 0.2 * S) c = vec3(0.6, 0.62, 0.68);
  }
  c = mix(base, c, I);
#elif MODE == 2
  // Flip Phone: a glossy Y2K flip phone floats and sways with the clip on its screen.
  vec3 pink = vec3(1.0, 0.62, 0.8);
  c = studio(uv, vec3(0.55, 0.3, 0.5));
  vec4 sp = particles(uv, t, 10.0, vec2(0.0, -0.05), 0.12, 1.0, 5.0, 0.9, 0.1);
  c += sp.rgb * sp.a * 0.8;
  float a = 0.08 * sin(t * 1.1);
  vec2 q = rot(-a) * (p - vec2(0.0, 0.02 * sin(t * 1.7))) / (0.72 * S);
  float lower = rbx(q - vec2(0.0, -0.28), vec2(0.23, 0.25), 0.08);
  float upper = rbx(q - vec2(0.0, 0.27), vec2(0.24, 0.3), 0.08);
  float shd = min(rbx(q - vec2(0.03, -0.31), vec2(0.23, 0.25), 0.08), rbx(q - vec2(0.03, 0.24), vec2(0.24, 0.3), 0.08));
  c *= 1.0 - 0.45 * (1.0 - smoothstep(0.0, 0.06, shd));
  float body = min(lower, upper);
  if (body < 0.0) {
    float gloss = 0.5 + 0.5 * sin(q.x * 7.0 + 0.8);
    c = pink * (0.78 + 0.28 * gloss);
    if (body > -0.012) c *= 0.75;
    vec2 sq = q - vec2(0.0, 0.29);
    float bez = rbx(sq, vec2(0.19, 0.22), 0.04);
    if (bez < 0.0) c = vec3(0.08, 0.07, 0.1);
    float scr = rbx(sq, vec2(0.165, 0.195), 0.02);
    if (scr < 0.0) {
      c = src(fitUv(sq, vec2(0.165, 0.195))).rgb;
      c += vec3(0.18) * smoothstep(0.1, 0.0, abs(sq.x + sq.y * 0.6 + 0.05)) ;
    }
    if (abs(q.y + 0.02) < 0.018 && abs(q.x) < 0.2) c = pink * 0.55;
    vec2 kq = q - vec2(0.0, -0.33);
    vec2 kc = floor((kq + vec2(0.15, 0.12)) / vec2(0.1, 0.06));
    if (kc.x >= 0.0 && kc.x < 3.0 && kc.y >= 0.0 && kc.y < 4.0) {
      vec2 kp = kq + vec2(0.15, 0.12) - (kc + 0.5) * vec2(0.1, 0.06);
      float key = rbx(kp, vec2(0.038, 0.021), 0.018);
      if (key < 0.0) c = mix(vec3(0.98, 0.9, 0.95), vec3(1.0), sat(-key * 60.0)) * (key > -0.004 ? 0.8 : 1.0);
    }
    float nav = length(q - vec2(0.0, -0.1));
    if (nav < 0.055) c = nav > 0.045 ? pink * 0.6 : (nav < 0.02 ? pink * 0.9 : vec3(0.98, 0.92, 0.96));
  }
  c = mix(base, c, I);
#else
  c = base;
#endif
  return vec4(c, 1.0);
}
"""
