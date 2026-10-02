package com.pinwheel.core.media.video

/*
 * Overlay library, laid out like CapCut's: Atmosphere, Light, Background, Scenery, Transitions and
 * Elements. P1 is opacity, P2 speed, P3 colour where the look has one. Backgrounds and scenery
 * cover the frame at full opacity, like CapCut's stock clips; the rest blend over the picture.
 */
internal const val FX_OVERLAYS2 = """
vec3 scr(vec3 a, vec3 b) { return 1.0 - (1.0 - clamp(a, 0.0, 1.0)) * (1.0 - clamp(b, 0.0, 1.0)); }
float segD(vec2 p, vec2 a, vec2 b) { vec2 pa = p - a; vec2 ba = b - a; float h = sat(dot(pa, ba) / dot(ba, ba)); return length(pa - ba * h); }
float sdTriEq(vec2 p, float r) {
  const float k = 1.7320508;
  p.x = abs(p.x) - r; p.y = p.y + r / k;
  if (p.x + k * p.y > 0.0) p = vec2(p.x - k * p.y, -k * p.x - p.y) / 2.0;
  p.x -= clamp(p.x, -2.0 * r, 0.0);
  return -length(p) * sign(p.y);
}
float stars(vec2 uv, float t, float dens) {
  vec2 g = uv * uSize / dens; vec2 cell = floor(g); vec2 f = fract(g) - 0.5;
  float h = hash(cell);
  float tw = 0.55 + 0.45 * sin(t * (2.0 + h * 5.0) + h * 40.0);
  return step(0.9, h) * exp(-dot(f, f) * 40.0) * tw * (0.4 + 0.6 * hash(cell + 3.0));
}
float ridge(float x, float base, float amp, float seed) { return base + amp * (fbm(vec2(x * 2.2 + seed, seed)) - 0.5) + amp * 0.5 * sin(x * 1.3 + seed); }
vec4 fx(vec2 uv) {
  float I = P1; float t = uTime * spd(P2);
  vec3 base = src(uv).rgb; vec3 c = base;
  vec2 p = asp(uv);
  vec3 tint = hsv(P3, 0.75, 1.0);
  float ph = fract(t * 0.42);
#if MODE == 30
  // Light Streaks: fast diagonal streaks of light racing across.
  vec2 q = rot(-0.6) * p;
  float lane = floor(q.y * 16.0);
  float h = hash1(lane * 1.37);
  float x = fract(q.x * 0.3 - t * (0.35 + h * 0.7) + h * 7.0);
  float len = 0.18 + 0.3 * hash1(lane + 3.0);
  float seg = sat(x / len) * step(x, len);
  float line = exp(-abs(fract(q.y * 16.0) - 0.5) * 55.0);
  vec3 col = mix(tint, vec3(1.0), pow(seg, 4.0));
  c = scr(base * (1.0 - 0.3 * I), col * (line + exp(-abs(fract(q.y * 16.0) - 0.5) * 14.0) * 0.35) * seg * step(0.3, h) * 2.4 * I);
#elif MODE == 31
  // Stage Lights: concert beams sweeping down through haze.
  vec3 acc = vec3(0.0);
  for (int i = 0; i < 5; i++) {
    float fi = float(i);
    vec2 o = vec2((fi - 2.0) * 0.26 * uAspect, 0.6);
    float ang = -1.5708 + sin(t * (0.55 + fi * 0.13) + fi * 1.7) * 0.5;
    vec2 d = p - o; float a = atan(d.y, d.x);
    float beam = exp(-pow((a - ang) / 0.075, 2.0)) * exp(-length(d) * 0.8);
    acc += hsv(fract(P3 + fi * 0.07), 0.65, 1.0) * beam;
    acc += vec3(1.0) * exp(-length(d) * 30.0) * 0.8;
  }
  float haze = 0.7 + 0.3 * fbm(p * 3.0 + vec2(t * 0.1, 0.0));
  c = scr(base * (1.0 - 0.4 * I), acc * haze * 1.4 * I);
#elif MODE == 32
  // Prism Flare: an anamorphic streak and rainbow ghosts slide across.
  float x = fract(t * 0.18) * 2.6 - 1.3;
  vec2 f = vec2(x * uAspect * 0.5, 0.15);
  vec2 d = p - f;
  vec3 col = vec3(0.55, 0.75, 1.0) * exp(-abs(d.y) * 110.0) * exp(-abs(d.x) * 1.2) * 1.3 + vec3(1.0) * exp(-length(d) * 16.0);
  for (int i = 1; i < 5; i++) {
    float fi = float(i);
    float r = length(p + f * (0.25 + 0.3 * fi));
    col += hsv(fi * 0.2, 0.7, 1.0) * (exp(-abs(r - 0.03 * fi) * 70.0) * 0.35 + exp(-r * 25.0) * 0.2);
  }
  c = scr(base, col * I);
#elif MODE == 33
  // Spotlight: a soft spotlight roams the frame.
  vec2 o = vec2(sin(t * 0.7) * 0.28 * uAspect, cos(t * 0.5) * 0.2);
  float m = smoothstep(0.34, 0.2, length(p - o));
  c = base * mix(1.0 - 0.82 * I, 1.0 + 0.2 * I, m) + vec3(1.0, 0.95, 0.85) * m * 0.05 * I;
#elif MODE == 34
  // Glitter Rain: dense twinkling glitter falling on a slant.
  vec3 acc = vec3(0.0);
  for (int L = 0; L < 3; L++) {
    float fl = float(L);
    vec2 q = rot(0.35) * p * (11.0 + fl * 8.0) + vec2(0.0, t * (2.5 + fl * 1.2));
    vec2 cell = floor(q); vec2 f = fract(q) - 0.5;
    float h = hash(cell + fl * 11.0);
    vec2 o = (hash2(cell + fl) - 0.5) * 0.6;
    float tw = 0.5 + 0.5 * sin(t * 8.0 + h * 50.0);
    acc += hsv(fract(P3 + (h - 0.5) * 0.12), 0.55, 1.0) * (exp(-length(f - o) * 18.0) * 1.3 + star4(f - o, 0.35) * 0.8 * tw) * step(0.4, h) * (0.6 + 0.8 * tw);
  }
  c = scr(base * (1.0 - 0.25 * I), acc * 1.6 * I);
#elif MODE == 35
  // Gold Dust: gold specks and soft blue bokeh drifting.
  vec4 a = particles(uv, t * 0.3, 16.0, vec2(0.02, -0.03), 0.08, 0.0, 3.0, 0.1, 0.05);
  vec4 b = particles(uv + 0.2, t * 0.2, 5.0, vec2(-0.02, 0.02), 0.28, 0.0, 6.0, 0.6, 0.05);
  c = scr(base * (1.0 - 0.3 * I), (a.rgb * a.a * 1.3 + b.rgb * b.a * 0.55) * I);
#elif MODE == 36
  // Sparks: fire sparks shooting up from the lower corner.
  vec3 acc = vec3(0.0);
  vec2 dir = normalize(vec2(0.75, 1.0));
  for (int L = 0; L < 3; L++) {
    float fl = float(L);
    float dens = 5.0 + fl * 4.0;
    vec2 q = p * dens - dir * t * (4.0 + fl * 2.0);
    vec2 cell = floor(q); vec2 f = fract(q) - 0.5;
    float h = hash(cell + fl * 5.0);
    vec2 o = (hash2(cell + fl) - 0.5) * 0.5;
    float d = segD(f, o - dir * (0.2 + 0.2 * h), o);
    acc += mix(vec3(1.0, 0.35, 0.05), vec3(1.0, 0.9, 0.5), h) * exp(-d * (60.0 + fl * 20.0)) * step(0.55, h);
  }
  acc += vec3(1.0, 0.4, 0.1) * exp(-length(uv) * 2.5) * 0.35;
  c = scr(base, acc * 1.5 * I);
#elif MODE == 37
  // Smoke: slow smoke drifting across.
  vec2 q = p * 2.0 + vec2(t * 0.1, -t * 0.04);
  vec2 w = vec2(fbm(q + t * 0.08), fbm(q + 5.2 - t * 0.07));
  float n = fbm(q + w * 1.8);
  float sm = smoothstep(0.28, 0.62, n) * (0.55 + 0.45 * sat(1.2 - uv.y));
  c = mix(base, vec3(0.86, 0.88, 0.92) * (0.75 + 0.4 * n), sm * 0.8 * I);
#elif MODE == 38
  // Neon Ring: two neon rings pulse around the middle.
  float r = length(p); float pulse = 0.5 + 0.5 * sin(t * 3.0);
  float r1 = 0.3 + 0.02 * pulse;
  vec3 col = tint * (exp(-abs(r - r1) * 90.0) * 1.3 + exp(-abs(r - r1) * 10.0) * 0.35)
    + hsv(fract(P3 + 0.25), 0.8, 1.0) * exp(-abs(r - r1 * 0.8) * 120.0);
  c = scr(base * (1.0 - 0.35 * I), col * I);
#elif MODE == 39
  // Neon Hearts: nested neon hearts flying out of the middle.
  vec3 acc = vec3(0.0);
  for (int i = 0; i < 6; i++) {
    float fi = float(i);
    float k = fract(t * 0.25 + fi / 6.0);
    float s = mix(0.04, 1.3, k * k);
    float d = abs(sdHeart(p / s * 0.9 + vec2(0.0, 0.55))) * s / 0.9;
    acc += hsv(fract(P3 + fi * 0.025), 0.75, 1.0) * (exp(-d * 110.0) * 1.2 + exp(-d * 18.0) * 0.25) * sat(k * 4.0) * (1.0 - k * 0.5);
  }
  c = scr(base * (1.0 - 0.45 * I), acc * I);
#elif MODE == 40
  // Galaxy: a drifting nebula full of stars.
  vec2 q = rot(t * 0.03) * p * 1.6;
  float n = fbm(q * 2.0 + fbm(q * 3.0 + t * 0.05) * 1.5);
  vec3 neb = mix(vec3(0.15, 0.0, 0.35), vec3(0.95, 0.25, 0.6), n) * smoothstep(0.35, 0.9, n)
    + vec3(0.1, 0.5, 1.0) * smoothstep(0.55, 1.0, fbm(q * 4.0 - 3.0)) * 0.7;
  c = scr(base * (1.0 - 0.35 * I), (neb * 1.6 + vec3(stars(uv, t, 2.5)) * 1.6) * I);
#elif MODE == 41
  // Fireflies: warm glowing dots wandering.
  vec2 wob = vec2(sin(t * 0.5 + uv.y * 4.0), cos(t * 0.4 + uv.x * 4.0)) * 0.02;
  vec4 a = particles(uv + wob, t * 0.2, 7.0, vec2(0.02, 0.03), 0.12, 0.0, 13.0, 0.17, 0.04);
  float fl = 0.6 + 0.4 * sin(t * 3.0 + uv.x * 20.0);
  c = scr(base * (1.0 - 0.3 * I), a.rgb * a.a * 1.6 * fl * I);
#elif MODE == 42
  // Magic Dust: a ribbon of sparkles swirls across.
  float cur = 0.18 * sin(p.x * 4.0 - t * 2.0) + 0.1 * sin(p.x * 7.0 + t);
  float d = abs(p.y - cur);
  vec4 s = particles(uv, t, 26.0, vec2(-0.1, 0.0), 0.1, 1.0, 5.0, fract(P3), 0.2);
  c = scr(base, (s.rgb * s.a * exp(-d * 9.0) * 2.6 + hsv(P3, 0.45, 1.0) * (exp(-d * 45.0) * 0.8 + exp(-d * 10.0) * 0.25)) * I);
#elif MODE == 43
  // Swirl: a colourful vortex of light.
  float r = length(p); float a = atan(p.y, p.x);
  float sp = a + log(r + 0.05) * 2.5 - t * 1.5;
  float band = pow(0.5 + 0.5 * sin(sp * 6.0), 8.0) * smoothstep(0.02, 0.2, r) * exp(-r * 1.1);
  c = scr(base * (1.0 - 0.4 * I), hsv(fract(sp * 0.08 + t * 0.05), 0.8, 1.0) * band * 1.8 * I);
#elif MODE == 44
  // Background - Purple Smoke.
  vec2 q = p * 1.8; vec2 w = vec2(fbm(q + t * 0.08), fbm(q + 3.1 - t * 0.07));
  float n = fbm(q + w * 2.0);
  vec3 bg = mix(vec3(0.16, 0.02, 0.32), vec3(0.6, 0.32, 0.95), n);
  bg = mix(bg, vec3(0.95, 0.8, 1.0), smoothstep(0.7, 1.0, n) * 0.6);
  c = mix(base, bg, I);
#elif MODE == 45
  // Background - Pastel Ink.
  vec2 q = p * 1.5; vec2 w = vec2(fbm(q + t * 0.05), fbm(q + 7.0 - t * 0.04));
  float n = fbm(q + w * 1.6);
  vec3 bg = mix(vec3(1.0, 0.78, 0.86), vec3(1.0, 0.88, 0.6), smoothstep(0.3, 0.7, n));
  bg = mix(bg, vec3(0.74, 0.72, 1.0), smoothstep(0.4, 0.75, fbm(q * 1.4 - 2.0 + w)));
  c = mix(base, bg, I);
#elif MODE == 46
  // Background - Pink Gradient with drifting glints.
  vec3 bg = mix(vec3(0.98, 0.12, 0.72), vec3(0.42, 0.08, 0.78), uv.y);
  vec4 s = particles(uv, t * 0.3, 10.0, vec2(0.0, 0.03), 0.06, 0.0, 4.0, 0.9, 0.05);
  c = mix(base, bg + vec3(1.0) * s.a * 0.6, I);
#elif MODE == 47
  // Background - Blue Hearts.
  vec3 bg = mix(vec3(0.02, 0.1, 0.45), vec3(0.1, 0.45, 0.95), uv.y + 0.2 * sin(p.x * 3.0 + t * 0.3));
  vec4 b = particles(uv, t * 0.2, 4.0, vec2(0.02, 0.02), 0.3, 0.0, 2.0, 0.58, 0.04);
  vec4 h = particles(uv + 0.3, t * 0.4, 4.0, vec2(0.0, -0.05), 0.16, 2.0, 7.0, 0.55, 0.05);
  bg = scr(bg, b.rgb * b.a * 0.35);
  bg = mix(bg, vec3(0.55, 0.8, 1.0) * 1.1, h.a * 0.85);
  c = mix(base, bg, I);
#elif MODE == 48
  // Background - Pink Bokeh.
  vec3 bg = mix(vec3(0.35, 0.0, 0.25), vec3(0.75, 0.05, 0.45), uv.y);
  vec4 b = particles(uv, t * 0.15, 4.0, vec2(0.02, 0.01), 0.35, 0.0, 5.0, 0.9, 0.06);
  vec4 s = particles(uv, t, 12.0, vec2(0.0), 0.12, 1.0, 9.0, 0.9, 0.05);
  c = mix(base, scr(bg, b.rgb * b.a * 0.55 + vec3(1.0) * s.a * 0.8), I);
#elif MODE == 49
  // Background - Golden Band of particles on black.
  vec4 a = particles(uv, t * 0.3, 22.0, vec2(0.05, 0.0), 0.1, 0.0, 2.0, 0.12, 0.03);
  float band = exp(-pow((uv.y - 0.5 - 0.03 * sin(uv.x * 6.0 + t)) / 0.12, 2.0));
  vec3 bg = vec3(0.01) + a.rgb * a.a * band * 3.0 + vec3(1.0, 0.72, 0.25) * band * 0.3;
  c = mix(base, bg, I);
#elif MODE == 50
  // Background - Blue Smoke on black.
  vec2 q = p * 1.7 + vec2(0.0, -t * 0.06); vec2 w = vec2(fbm(q + t * 0.1), fbm(q + 4.0 - t * 0.09));
  float n = fbm(q + w * 2.2);
  float m = smoothstep(0.32, 0.7, n);
  vec3 bg = mix(vec3(0.0, 0.75, 1.0), vec3(0.85, 0.3, 1.0), fbm(q * 0.8 + 9.0)) * m;
  c = mix(base, bg, I);
#elif MODE == 51
  // Background - Soft Gradient.
  float g = sat(uv.x * 0.5 + (1.0 - uv.y) * 0.6 + 0.1 * sin(t * 0.4));
  c = mix(base, mix(vec3(0.3, 0.3, 0.42), vec3(0.95, 0.3, 0.52), g), I);
#elif MODE == 52
  // Background - Light Wall: bright stage lamps and LED dots.
  vec3 bg = vec3(0.12, 0.14, 0.2);
  for (int i = 0; i < 4; i++) {
    float fi = float(i);
    vec2 o = vec2((mod(fi, 2.0) - 0.5) * 0.95 * uAspect, (floor(fi / 2.0) - 0.5) * 0.7);
    bg += vec3(0.85, 0.9, 1.0) * exp(-length(p - o) * 5.5) * (0.9 + 0.2 * sin(t * 3.0 + fi));
  }
  vec2 g = fract(p * 26.0) - 0.5; vec2 gc = floor(p * 26.0);
  bg += vec3(0.9, 0.95, 1.0) * exp(-dot(g, g) * 50.0) * step(0.55, fbm(gc * 0.3)) * (0.6 + 0.4 * sin(t * 4.0 + hash(gc) * 20.0));
  c = mix(base, bg, I);
#elif MODE == 53
  // Background - 3D Shapes floating on soft grey.
  vec3 bg = mix(vec3(0.86, 0.87, 0.9), vec3(0.95, 0.95, 0.97), uv.y);
  vec3 L = normalize(vec3(-0.4, 0.5, 0.8));
  for (int i = 0; i < 12; i++) {
    float fi = float(i);
    vec2 o = (vec2(hash1(fi * 3.1), hash1(fi * 5.7)) - 0.5) * vec2(uAspect, 1.0) * 1.1 + vec2(sin(t * 0.3 + fi), cos(t * 0.25 + fi * 2.0)) * 0.03;
    float kind = mod(fi, 3.0);
    vec2 d = p - o; vec2 ds = d - vec2(0.012, -0.02);
    if (kind < 0.5) {
      float r = 0.045 + 0.035 * hash1(fi + 2.0);
      bg *= 1.0 - 0.2 * smoothstep(r * 1.4, r * 0.6, length(ds));
      if (length(d) < r) {
        vec3 n = normalize(vec3(d / r, sqrt(max(1.0 - dot(d / r, d / r), 0.0))));
        bg = vec3(0.72, 0.74, 0.8) * (0.45 + 0.55 * max(dot(n, L), 0.0)) + vec3(1.0) * pow(max(dot(reflect(-L, n), vec3(0.0, 0.0, 1.0)), 0.0), 20.0) * 0.6;
      }
    } else if (kind < 1.5) {
      vec2 rq = rot(hash1(fi) * 6.0 + t * 0.2) * d;
      float tri = sdTriEq(rq, 0.06);
      bg *= 1.0 - 0.18 * smoothstep(0.015, -0.01, sdTriEq(rot(hash1(fi) * 6.0 + t * 0.2) * ds, 0.06));
      if (tri < 0.0) bg = vec3(0.55, 0.57, 0.63) * (0.8 + 0.4 * (rq.y / 0.12 + 0.5));
    } else {
      vec2 rq = rot(hash1(fi) * 6.0) * d;
      float rod = sdBox(rq, vec2(0.09, 0.012));
      if (rod < 0.0) bg = vec3(0.5, 0.52, 0.58) * (0.8 + 0.5 * sat(rq.y / 0.024 + 0.5));
    }
  }
  c = mix(base, bg, I);
#elif MODE == 54
  // Background - Red Hearts.
  vec3 bg = vec3(0.78, 0.1, 0.18) * (0.85 + 0.15 * uv.y);
  vec4 h = particles(uv, t * 0.25, 4.5, vec2(0.01, -0.03), 0.3, 2.0, 3.0, 0.98, 0.02);
  bg = mix(bg, vec3(0.62, 0.02, 0.1) + vec3(0.4, 0.2, 0.2) * sat(uv.y - 0.3), h.a);
  c = mix(base, bg, I);
#elif MODE == 55
  // Scenery - Aurora over a still lake.
  float wy = 0.3;
  vec2 sp = uv;
  bool water = uv.y < wy;
  if (water) sp = vec2(uv.x + 0.004 * sin(uv.y * 160.0 + t * 3.0), 2.0 * wy - uv.y);
  vec2 sq = asp(sp);
  vec3 sky = mix(vec3(0.0, 0.02, 0.07), vec3(0.02, 0.1, 0.18), sp.y);
  sky += vec3(stars(sp, t, 2.0));
  float by = 0.55 + 0.1 * sin(sq.x * 2.2 + t * 0.25) + 0.06 * sin(sq.x * 5.0 - t * 0.4);
  float h = sp.y - by;
  float rays = pow(fbm(vec2(sq.x * 9.0 + t * 0.3, t * 0.1)), 2.0) * 2.4;
  float cur = smoothstep(-0.03, 0.0, h) * exp(-max(h, 0.0) * 5.0) * rays;
  sky += mix(vec3(0.1, 1.0, 0.5), vec3(0.6, 0.2, 0.9), sat(h * 3.0)) * cur * 1.5;
  float mtn = ridge(p.x, wy + 0.06, 0.1, 3.0);
  vec3 bg = sky;
  if (water) bg *= 0.55;
  if (!water && uv.y < mtn) bg = vec3(0.01, 0.02, 0.03);
  if (water && 2.0 * wy - uv.y < mtn) bg = vec3(0.01, 0.015, 0.025);
  c = mix(base, bg, I);
#elif MODE == 56
  // Scenery - Milky Way over the horizon.
  vec3 sky = mix(vec3(0.02, 0.02, 0.06), vec3(0.08, 0.05, 0.15), uv.y);
  sky += vec3(0.8, 0.35, 0.6) * exp(-uv.y * 4.0) * exp(-abs(uv.x - 0.2) * 2.0) * 0.8;
  vec2 q = rot(-0.7) * (p - vec2(0.0, 0.1));
  float band = exp(-q.y * q.y * 18.0);
  float dust = fbm(q * 5.0 + vec2(t * 0.02, 0.0));
  sky += vec3(0.75, 0.7, 0.85) * band * smoothstep(0.35, 0.8, dust) * 0.6;
  sky -= vec3(0.2) * band * smoothstep(0.55, 0.8, fbm(q * 9.0 + 3.0));
  sky += vec3(stars(uv, t, 1.6)) * (1.0 + band * 2.0);
  float gnd = ridge(p.x, 0.14, 0.06, 7.0);
  c = mix(base, uv.y < gnd ? vec3(0.01) : max(sky, 0.0), I);
#elif MODE == 57
  // Scenery - Sunset over the sea.
  float hz = 0.42;
  vec3 sky = mix(vec3(1.0, 0.55, 0.2), vec3(0.35, 0.2, 0.55), sat((uv.y - hz) * 1.8));
  vec2 sun = vec2(0.0, hz + 0.06 - 0.03 * sin(t * 0.1) - asp(vec2(0.5)).y);
  float sd = length(p - vec2(0.0, hz + 0.06 - 0.5));
  sky += vec3(1.0, 0.8, 0.45) * (smoothstep(0.09, 0.08, sd) + exp(-sd * 5.0) * 0.6);
  float cl = smoothstep(0.55, 0.8, fbm(vec2(p.x * 2.0 + t * 0.05, uv.y * 6.0)));
  sky = mix(sky, vec3(0.55, 0.3, 0.45), cl * 0.6 * step(hz, uv.y));
  vec3 bg = sky;
  if (uv.y < hz) {
    float dz = (hz - uv.y);
    float wv = sin(uv.y * 400.0 / (dz * 4.0 + 0.3) + t * 2.0 + fbm(vec2(p.x * 8.0, uv.y * 40.0)) * 4.0);
    vec3 sea = mix(vec3(0.25, 0.1, 0.25), vec3(0.6, 0.3, 0.3), sat(1.0 - dz * 3.0));
    float glit = exp(-abs(p.x) * (6.0 + dz * 20.0)) * smoothstep(0.3, 1.0, wv);
    bg = sea + vec3(1.0, 0.75, 0.4) * glit * 0.9;
  }
  c = mix(base, bg, I);
#elif MODE == 58
  // Scenery - Moonlit dunes.
  vec3 sky = mix(vec3(0.05, 0.1, 0.35), vec3(0.01, 0.03, 0.14), uv.y);
  sky += vec3(stars(uv, t, 2.0));
  vec2 mc = vec2(0.22 * uAspect, 0.2);
  float md = length(p - mc);
  vec3 moon = vec3(0.92, 0.94, 1.0) * (0.85 + 0.15 * fbm((p - mc) * 30.0)) - vec3(0.15) * smoothstep(0.55, 0.75, fbm((p - mc) * 12.0 + 4.0));
  sky = md < 0.09 ? moon : sky + vec3(0.5, 0.6, 1.0) * exp(-(md - 0.09) * 12.0) * 0.5;
  vec3 bg = sky;
  for (int i = 0; i < 3; i++) {
    float fi = float(i);
    float y = ridge(p.x + fi * 0.7 + t * 0.01 * (fi + 1.0), 0.42 - fi * 0.11, 0.22, fi * 5.0);
    if (uv.y < y) bg = mix(vec3(0.07, 0.12, 0.35), vec3(0.02, 0.04, 0.14), fi / 2.0) + vec3(0.1, 0.12, 0.2) * sat((y - uv.y) * -8.0 + 1.0) * 0.5;
  }
  c = mix(base, bg, I);
#elif MODE == 59
  // Scenery - Cloud time-lapse.
  vec3 sky = mix(vec3(0.35, 0.6, 0.95), vec3(0.1, 0.35, 0.8), uv.y);
  vec2 q = vec2(p.x * 1.5 - t * 0.25, uv.y * 2.5);
  float n = fbm(q) * 0.65 + fbm(q * 2.3 + 4.0) * 0.35;
  float n2 = fbm(q + vec2(0.05, 0.08)) * 0.65 + fbm((q + vec2(0.05, 0.08)) * 2.3 + 4.0) * 0.35;
  float d = smoothstep(0.45, 0.7, n);
  vec3 cloud = mix(vec3(0.65, 0.7, 0.8), vec3(1.0), sat(0.6 + (n - n2) * 5.0));
  c = mix(base, mix(sky, cloud, d), I);
#elif MODE == 60
  // Scenery - Snowy night.
  vec3 sky = mix(vec3(0.12, 0.18, 0.35), vec3(0.03, 0.05, 0.14), uv.y);
  vec3 bg = sky;
  for (int i = 0; i < 2; i++) {
    float fi = float(i);
    float y = ridge(p.x + fi * 1.3, 0.3 - fi * 0.12, 0.1, 2.0 + fi * 4.0);
    if (uv.y < y) bg = mix(vec3(0.7, 0.78, 0.92), vec3(0.45, 0.52, 0.7), fi) * (0.8 + 0.2 * sat((y - uv.y) * 5.0));
  }
  vec4 a = particles(uv, t, 9.0, vec2(0.05, 0.3), 0.1, 0.0, 4.0, 0.6, 0.0);
  vec4 b = particles(uv + 0.3, t, 18.0, vec2(0.02, 0.2), 0.07, 0.0, 8.0, 0.6, 0.0);
  c = mix(base, bg + vec3(1.0) * (a.a + b.a * 0.7), I);
#elif MODE == 61
  // Scenery - Ocean horizon.
  float hz = 0.55;
  vec3 bg = mix(vec3(0.75, 0.88, 1.0), vec3(0.3, 0.6, 0.95), sat((uv.y - hz) * 2.0));
  if (uv.y < hz) {
    float dz = hz - uv.y;
    float k = 1.0 / (dz + 0.04);
    float w = fbm(vec2(p.x * k * 0.6 + t * 0.2, k * 1.5 - t * 0.8));
    bg = mix(vec3(0.0, 0.35, 0.55), vec3(0.1, 0.55, 0.7), w) + vec3(1.0) * smoothstep(0.72, 0.8, w) * 0.5;
    bg = mix(bg, vec3(0.55, 0.75, 0.9), exp(-dz * 30.0) * 0.6);
  }
  c = mix(base, bg, I);
#elif MODE == 62
  // Scenery - Shooting stars.
  vec3 bg = mix(vec3(0.04, 0.05, 0.16), vec3(0.0, 0.0, 0.04), uv.y) + vec3(stars(uv, t, 1.8));
  for (int i = 0; i < 3; i++) {
    float fi = float(i);
    float cyc = fract(t * 0.35 + fi * 0.37); float k = floor(t * 0.35 + fi * 0.37);
    vec2 s0 = vec2((hash1(k + fi * 9.0) - 0.2) * uAspect, 0.2 + 0.3 * hash1(k + fi * 5.0));
    vec2 dir = normalize(vec2(-1.0, -0.45));
    vec2 head = s0 + dir * cyc * 1.2;
    float d = segD(p, head - dir * 0.25, head);
    float along = sat(dot(p - (head - dir * 0.25), dir) / 0.25);
    bg += vec3(0.9, 0.95, 1.0) * exp(-d * 400.0) * along * step(cyc, 0.5) * 1.5;
  }
  c = mix(base, bg, I);
#elif MODE == 63
  // Transition - Colour Wipe: a solid colour sweeps in and off.
  float e1 = ease(sat(ph / 0.42)); float e2 = ease(sat((ph - 0.5) / 0.42));
  float x = uv.x + (uv.y - 0.5) * 0.25;
  float cov = step(x, mix(-0.3, 1.3, e1)) * step(mix(-0.3, 1.3, e2), x);
  c = mix(base, tint, cov * I);
#elif MODE == 64
  // Transition - Diamond Burst: a pink field opens with a spinning star, then bursts away.
  float r = length(p);
  float open = easeOut(sat(ph / 0.25)) * 1.4; float close = ease(sat((ph - 0.65) / 0.3)) * 1.4;
  float field = step(r, open) * step(close, r);
  vec2 q = rot(ph * 4.0) * p;
  float sz = mix(0.04, 0.36, easeOut(sat(ph / 0.55))) * (1.0 + close * 2.0);
  float st = pow(abs(q.x) / sz, 0.55) + pow(abs(q.y) / sz, 0.55);
  vec3 col = vec3(0.92, 0.25, 0.5);
  col = mix(col, vec3(0.45, 0.2, 0.65), step(st, 1.0));
  col = mix(col, vec3(0.97, 0.93, 1.0), step(st, 0.6));
  c = mix(base, col, field * I);
#elif MODE == 65
  // Transition - Shape Pop: coloured tiles pop in to fill the frame, then pop away.
  float n = 7.0;
  vec2 g = vec2(uv.x * n, uv.y * n / uAspect);
  vec2 cell = floor(g); vec2 f = fract(g) - 0.5;
  float h = hash(cell + 3.0);
  float tin = 0.05 + 0.3 * (1.0 - cell.y / (n / uAspect)) + 0.1 * h;
  float tout = 0.6 + 0.3 * h;
  float sIn = sat((ph - tin) / 0.12); float sOut = sat((ph - tout) / 0.1);
  float s = (sIn * sIn * (3.0 - 2.0 * sIn)) * (1.0 - sOut);
  vec2 rq = rot((1.0 - s) * 1.5) * f;
  float tile = step(max(abs(rq.x), abs(rq.y)), 0.5 * s + 0.02 * s);
  c = mix(base, hsv(fract(h * 0.6 + 0.8), 0.75, 0.95), tile * I);
#elif MODE == 66
  // Transition - Glow Blob: a glowing green blob swells over the frame and shrinks away.
  float R = sin(ph * PI) * 1.25;
  float n = fbm(p * 3.0 + t) * 0.3;
  float d = length(p) - R + n * R;
  float blob = smoothstep(0.02, -0.02, d);
  c = mix(base, vec3(0.78, 1.0, 0.35), blob * I) + vec3(0.3, 1.0, 0.15) * exp(-max(d, 0.0) * 10.0) * 0.7 * I * (1.0 - blob);
#elif MODE == 67
  // Transition - Fire Wipe: a wall of flame crosses the frame.
  float front = mix(-0.5, 1.5, ph);
  float n = fbm(vec2(uv.y * 6.0, t * 2.0)) * 0.25;
  float d = uv.x - front + n;
  float flame = exp(-abs(d) * 3.5) * (0.5 + 0.9 * fbm(vec2(uv.x * 7.0 - t * 3.0, uv.y * 5.0 + t * 6.0)));
  vec3 fire = mix(vec3(0.9, 0.2, 0.0), vec3(1.0, 0.9, 0.4), sat(flame * 1.5 - 0.3));
  c = mix(base, fire, sat(flame * 1.6) * I);
  c *= 1.0 - 0.4 * smoothstep(0.0, -0.25, d) * smoothstep(-0.6, -0.25, d) * I;
#elif MODE == 68
  // Transition - Ink Wipe: black ink floods in and ebbs away.
  float cov = sin(ph * PI);
  float n = fbm(p * 3.5 + vec2(t * 0.2, 0.0));
  float ink = smoothstep(0.02, -0.02, length(p * vec2(0.8, 1.0)) - cov * 1.0 - (n - 0.5) * 0.5 * cov);
  c = mix(base, vec3(0.02), ink * I);
#elif MODE == 69
  // Transition - Stripes: bands of colour slide across one after another.
  vec3 acc = base; float cov = 0.0;
  for (int i = 0; i < 5; i++) {
    float fi = float(i);
    float e = ease(sat((ph - fi * 0.06) / 0.35)); float e2 = ease(sat((ph - 0.5 - fi * 0.06) / 0.35));
    float x = uv.x + (uv.y - 0.5) * 0.4;
    float band = step(fi / 5.0, uv.y) * step(uv.y, (fi + 1.0) / 5.0) * step(x, mix(-0.4, 1.4, e)) * step(mix(-0.4, 1.4, e2), x);
    acc = mix(acc, hsv(fract(P3 + fi * 0.06), 0.7, 1.0 - fi * 0.08), band);
  }
  c = mix(base, acc, I);
#elif MODE == 70
  // Transition - Circle Iris: a ring closes to black and opens again.
  float R = mix(1.2, 0.0, ease(sat(ph / 0.45))) + mix(0.0, 1.2, ease(sat((ph - 0.55) / 0.45)));
  float r = length(p);
  c = mix(base, vec3(0.0), step(R, r) * I);
  c += tint * exp(-abs(r - R) * 90.0) * I;
#elif MODE == 71
  // Transition - Light Burst: white sparks explode outward with a flash.
  vec3 acc = vec3(0.0);
  for (int i = 0; i < 40; i++) {
    float fi = float(i);
    float a = fi * 2.39996; float sp = 0.3 + hash1(fi) * 0.9;
    vec2 pos = vec2(cos(a), sin(a)) * easeOut(ph) * sp;
    acc += vec3(1.0) * exp(-length(p - pos) * (60.0 + 60.0 * hash1(fi + 1.0))) * (1.0 - ph);
  }
  acc += vec3(1.0, 0.95, 0.85) * exp(-length(p) * 3.0) * exp(-ph * 6.0) * 1.3;
  c = scr(base, acc * I);
#elif MODE == 72
  // Transition - Scribble: red marker loops scrawl on and off.
  float draw = sat(ph / 0.5); float erase = sat((ph - 0.6) / 0.35);
  float ink = 0.0;
  for (int i = 0; i < 4; i++) {
    float fi = float(i);
    vec2 o = (vec2(hash1(fi), hash1(fi + 4.0)) - 0.5) * 0.3;
    float R = 0.15 + 0.12 * hash1(fi + 8.0);
    vec2 d = p - o; float a = fract(atan(d.y, d.x) / TAU + hash1(fi + 2.0));
    float on = step(a, draw * 1.1 - fi * 0.08) * step(erase, a);
    ink = max(ink, exp(-abs(length(d) - R - 0.02 * sin(a * 20.0)) * 180.0) * on);
  }
  c = mix(base, vec3(0.9, 0.12, 0.1), sat(ink * 1.5) * I);
#elif MODE == 73
  // Element - Speed Lines: anime rush lines flickering in from the edges.
  float a = atan(p.y, p.x); float r = length(p * vec2(1.0 / uAspect, 1.0) * 0.8);
  float k = floor(t * 14.0);
  float bin = floor((a + PI) / TAU * 110.0); float fa = fract((a + PI) / TAU * 110.0);
  float h = hash(vec2(bin, k));
  float start = 0.25 + 0.3 * hash(vec2(bin + 7.0, k));
  float w = sat((r - start) * 1.4) * 0.7;
  float line = step(h, 0.5) * step(abs(fa - 0.5) * 2.0, w);
  c = mix(base, vec3(1.0), sat(line) * I);
#elif MODE == 74
  // Element - Lightning Strike: a bolt cracks down with a flash.
  float k = floor(t * 0.9); float b = fract(t * 0.9);
  float x0 = (hash1(k) - 0.5) * uAspect * 0.8;
  float xp = x0 + (fbm(vec2(p.y * 7.0, k)) - 0.5) * 0.3 + (fbm(vec2(p.y * 25.0, k + 3.0)) - 0.5) * 0.06;
  float d = abs(p.x - xp);
  float reach = mix(0.5, -0.5, sat(b * 8.0));
  float life = 1.0 - smoothstep(0.25, 0.45, b);
  vec3 bolt = vec3(0.8, 0.9, 1.0) * (exp(-d * 500.0) * 1.6 + exp(-d * 40.0) * 0.4) * step(reach, p.y) * life;
  c = scr(base, bolt * I) + vec3(0.7, 0.8, 1.0) * exp(-b * 12.0) * 0.35 * I;
#elif MODE == 75
  // Element - Full Moon floating in the top corner.
  vec2 mc = vec2(0.2 * uAspect, 0.24 + 0.01 * sin(t * 0.8));
  float md = length(p - mc); float R = 0.13;
  vec2 mq = (p - mc) / R;
  vec3 moon = vec3(0.9, 0.9, 0.88) * (0.8 + 0.2 * fbm(mq * 3.0)) - vec3(0.25) * smoothstep(0.55, 0.75, fbm(mq * 2.2 + 4.0));
  moon *= 0.6 + 0.4 * sat(dot(normalize(vec3(mq, sqrt(max(1.0 - dot(mq, mq), 0.0)))), normalize(vec3(-0.5, 0.4, 0.8))));
  c = md < R ? mix(base, moon, I) : scr(base, vec3(0.9, 0.9, 1.0) * exp(-(md - R) * 14.0) * 0.4 * I);
#elif MODE == 76
  // Element - Smoke Puff: a cloud of smoke billows up and fades.
  float cyc = fract(t * 0.35);
  vec2 q = (p - vec2(0.0, -0.35 + cyc * 0.3)) / (0.2 + cyc * 0.5);
  float n = fbm(q * 2.0 + vec2(0.0, -t * 0.3)) + fbm(q * 4.0 + 3.0) * 0.4;
  float m = smoothstep(1.1, 0.4, length(q) - (n - 0.6) * 0.8) * (1.0 - cyc);
  c = mix(base, vec3(0.88, 0.89, 0.92) * (0.75 + 0.25 * n), sat(m) * I);
#elif MODE == 77
  // Element - Scan Line: a laser scan sweeps up and down leaving a grid glow.
  float y = 0.5 + 0.45 * sin(t * 1.4);
  float d = uv.y - y;
  vec3 col = tint * (exp(-abs(d) * 300.0) * 1.5 + exp(-abs(d) * 25.0) * 0.4);
  vec2 g = abs(fract(p * 12.0) - 0.5);
  col += tint * step(0.46, max(g.x, g.y)) * exp(-abs(d) * 12.0) * 0.4;
  c = scr(base, col * I);
#elif MODE == 78
  // Element - Rating: five stars fill with gold one by one.
  float lit = floor(fract(t * 0.3) * 7.0);
  vec2 q = p - vec2(0.0, -0.28);
  float cell = floor(q.x / 0.14 + 2.5);
  vec2 sc = vec2((cell - 2.0) * 0.14, 0.0);
  float inRow = step(0.0, cell) * step(cell, 4.0);
  float fill = star5(q - sc, 0.06) * inRow;
  float edge = (star5(q - sc, 0.068) - fill) * inRow;
  vec3 col = cell < lit ? vec3(1.0, 0.8, 0.2) : vec3(0.9, 0.9, 0.92);
  c = mix(base, col, fill * I);
  c = mix(c, vec3(0.3, 0.25, 0.1), edge * I);
#else
  c = base;
#endif
  return vec4(c, 1.0);
}
"""
