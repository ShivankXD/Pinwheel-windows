package com.pinwheel.core.media.video

/*
 * Pinwheel's second creative pack: Pixel Bead copies, 3D mockups (phone, card deck, polaroid,
 * TV, billboard, film strip), neon looks, stylish extras and the rebuilt Pinwheel originals.
 * GLSL ES 1.00: float literals only, constant loop bounds, no bit operators.
 */

/** Pixel-art helpers shared by the Pixel Bead family. */
private const val PIX_HELPERS = """
float bit(float mask, float x) { return mod(floor(mask / exp2(x)), 2.0); }
// 7x6 heart, rows top to bottom.
float heartRow(float r) { return r < 0.5 ? 54.0 : r < 1.5 ? 127.0 : r < 2.5 ? 127.0 : r < 3.5 ? 62.0 : r < 4.5 ? 28.0 : 8.0; }
float pixHeart(vec2 q) { // q in sprite pixels, origin top-left
  if (q.x < 0.0 || q.y < 0.0 || q.x >= 7.0 || q.y >= 6.0) return 0.0;
  return bit(heartRow(floor(q.y)), 6.0 - floor(q.x));
}
// 9x5 bow.
float bowRow(float r) { return r < 0.5 ? 387.0 : r < 1.5 ? 495.0 : r < 2.5 ? 511.0 : r < 3.5 ? 495.0 : 387.0; }
float pixBow(vec2 q) {
  if (q.x < 0.0 || q.y < 0.0 || q.x >= 9.0 || q.y >= 5.0) return 0.0;
  return bit(bowRow(floor(q.y)), 8.0 - floor(q.x));
}
// 5x5 plus star.
float starRow(float r) { return r < 0.5 ? 4.0 : r < 1.5 ? 14.0 : r < 2.5 ? 31.0 : r < 3.5 ? 14.0 : 4.0; }
float pixStar(vec2 q) {
  if (q.x < 0.0 || q.y < 0.0 || q.x >= 5.0 || q.y >= 5.0) return 0.0;
  return bit(starRow(floor(q.y)), 4.0 - floor(q.x));
}
float bayer(vec2 p) {
  vec2 q = mod(floor(p), 4.0);
  float a = mod(q.x, 2.0); float b = mod(q.y, 2.0); float c = mod(floor(q.x / 2.0), 2.0); float d = mod(floor(q.y / 2.0), 2.0);
  return (8.0 * mod(a + b, 2.0) + 4.0 * b + 2.0 * mod(c + d, 2.0) + d) / 16.0;
}
vec3 pixelate(vec2 uv, float n) { vec2 g = vec2(n * uAspect, n); return src((floor(uv * g) + 0.5) / g).rgb; }
"""

internal const val FX_PIXEL_BEAD = PIX_HELPERS + """
vec4 fx(vec2 uv) {
  float I = P1; float t = uTime * spd(P2);
  vec3 base = src(uv).rgb; vec3 c = base;
  vec2 p = asp(uv);
#if MODE == 1
  // Pixel Scan: a diagonal wave turns the picture into glittering beads as it passes.
  float n = 70.0; vec2 g = vec2(n * uAspect, n);
  vec2 cell = floor(uv * g); vec2 f = fract(uv * g) - 0.5;
  vec3 pc = src((cell + 0.5) / g).rgb;
  float s = uv.x * 0.55 + (1.0 - uv.y) * 0.45;
  float front = fract(t * 0.42) * 1.9 - 0.45;
  float band = smoothstep(front - 0.45, front - 0.2, s) * (1.0 - smoothstep(front - 0.02, front + 0.02, s));
  float bead = 1.0 - smoothstep(0.36, 0.48, length(f));
  float glint = step(0.86, hash(cell + floor(t * 14.0)));
  vec3 beads = pc * (0.55 + 0.6 * bead) + hsv(hash(cell + 3.0), 0.55, 1.0) * glint * bead * 0.9;
  float edge = exp(-abs(s - front) * 40.0);
  c = mix(base, beads, band * I);
  c += vec3(1.0, 0.85, 0.95) * edge * bead * 0.5 * I;
#elif MODE == 2
  // Flip Phone: a pixel-art flip phone rises in, held by a pixel hand, playing the clip.
  float S = min(1.0, uAspect / 0.56);
  float cyc = fract(t * 0.32);
  float rise = 1.0 - easeOut(sat(cyc * 4.0));
  vec2 q = floor(p * 110.0) / 110.0 / (S * 0.8) - vec2(0.0, 0.03 - 1.1 * rise);
  vec3 bgc = pixelate(uv, 26.0) * 0.8;
  c = bgc;
  // Hand: palm under the phone, thumb over the left edge, fingertips on the right.
  float palm = length((q - vec2(0.1, -0.5)) / vec2(0.27, 0.2));
  vec3 skin = vec3(1.0, 0.78, 0.7);
  if (palm < 1.0) c = skin * (0.92 + 0.08 * step(0.85, palm)) * (palm > 0.93 ? 0.6 : 1.0);
  // Phone body: lower keypad half and upper screen half with a hinge.
  float lower = sdBox(q - vec2(0.0, -0.24), vec2(0.17, 0.2));
  float upper = sdBox(q - vec2(0.0, 0.2), vec2(0.18, 0.22));
  float body = min(lower, upper);
  if (body < 0.0) {
    vec3 metal = mix(vec3(0.72, 0.74, 0.8), vec3(0.93, 0.94, 0.97), sat(q.x * 3.0 + 0.5));
    c = metal;
    if (body > -0.012) c = vec3(0.18, 0.18, 0.22);
    // Screen.
    vec2 sq = q - vec2(0.0, 0.22);
    if (sdBox(sq, vec2(0.13, 0.155)) < 0.0) {
      vec2 suv = sq / vec2(0.26, 0.31) + 0.5;
      float cover = 0.26 / 0.31 / uAspect;
      suv.x = (suv.x - 0.5) * min(cover, 1.0) + 0.5; suv.y = (suv.y - 0.5) * min(1.0 / cover, 1.0) + 0.5;
      vec2 sg = vec2(52.0, 62.0);
      c = src((floor(suv * sg) + 0.5) / sg).rgb;
      if (sdBox(sq, vec2(0.13, 0.155)) > -0.01) c = vec3(0.15);
    }
    // Keypad.
    vec2 kq = q - vec2(0.0, -0.3);
    vec2 kc = floor((kq + vec2(0.12, 0.12)) / vec2(0.08, 0.06));
    if (kc.x >= 0.0 && kc.x < 3.0 && kc.y >= 0.0 && kc.y < 4.0) {
      vec2 kp = kq + vec2(0.12, 0.12) - (kc + 0.5) * vec2(0.08, 0.06);
      if (sdBox(kp, vec2(0.028, 0.018)) < 0.0) c = vec3(0.86, 0.87, 0.9) * (sdBox(kp, vec2(0.028, 0.018)) > -0.006 ? 0.6 : 1.0);
    }
    float smile = length(q - vec2(0.0, -0.1));
    if (smile < 0.035) c = smile > 0.028 ? vec3(0.45, 0.25, 0.05) : vec3(1.0, 0.62, 0.15);
    // Hinge.
    if (abs(q.y + 0.03) < 0.012 && abs(q.x) < 0.17) c = vec3(0.55, 0.56, 0.62);
  }
  float thumb = length((q - vec2(-0.2, -0.25)) / vec2(0.05, 0.09));
  if (thumb < 1.0) c = thumb > 0.85 ? skin * 0.6 : (q.y > -0.2 ? vec3(0.9, 0.35, 0.75) : skin);
  for (int i = 0; i < 3; i++) {
    float fi = float(i);
    float fing = length((q - vec2(0.2, -0.12 - fi * 0.075)) / vec2(0.05, 0.034));
    if (fing < 1.0) c = fing > 0.8 ? skin * 0.6 : (q.x < 0.18 ? vec3(0.9, 0.35, 0.75) : skin);
  }
  c = mix(base, c, I);
#elif MODE == 3
  // Pixel Creation: the picture assembles from 2x2 blocks up to full resolution.
  float cyc = fract(t * 0.38);
  float k = floor(cyc * 8.0);
  float n = exp2(k + 1.0);
  vec3 pc = k >= 7.0 ? base : pixelate(uv, n);
  c = mix(base, pc, I);
#elif MODE == 4
  // Pixel Breakdown: blocks crumble and fall away, then the picture rebuilds.
  float n = 34.0; vec2 g = vec2(n * uAspect, n);
  vec2 cell = floor(uv * g);
  float cyc = fract(t * 0.35) * 2.2;
  vec3 back = blur9(uv, 0.03) * 0.25;
  vec3 hit = back; bool found = false;
  for (int i = 0; i < 10; i++) {
    float fi = float(i);
    vec2 sc = cell + vec2(0.0, fi);
    float delay = hash(sc) * 0.5 + (sc.y / g.y) * 0.7;
    float tt = max(cyc - delay, 0.0);
    float fall = tt * tt * 60.0;
    if (!found && fall >= fi && fall < fi + 1.0) {
      vec2 local = fract(uv * g) + vec2(0.0, fall - fi);
      vec2 suv = (sc + vec2(fract(uv * g).x, fract(local.y))) / g;
      hit = src(suv).rgb * (1.0 - 0.3 * sat(tt * 2.0)); found = true;
    }
  }
  c = mix(base, hit, I);
#elif MODE == 5
  // Losing HP: a pixel heart bar drains; every lost heart flashes red and jolts the frame.
  float cyc = fract(t * 0.3);
  float lost = floor(cyc * 6.0);
  float hitT = fract(cyc * 6.0);
  float jolt = exp(-hitT * 10.0) * step(0.5, lost);
  vec2 q = uv + vec2(sin(t * 80.0), cos(t * 70.0)) * 0.006 * jolt * I;
  c = src(q).rgb;
  c = mix(c, c * vec3(1.4, 0.35, 0.35), jolt * 0.7 * I);
  float ps = 0.011 * min(1.0, 0.56 / uAspect + 0.44);
  for (int i = 0; i < 5; i++) {
    float fi = float(i);
    vec2 o = vec2(0.04 + fi * 8.5 * ps / uAspect, 0.93);
    vec2 sp = vec2((uv.x - o.x) * uAspect, o.y - uv.y) / ps;
    float h = pixHeart(sp);
    float shadow = pixHeart(sp - vec2(1.0, 1.0));
    if (shadow > 0.5 && h < 0.5) c = mix(c, vec3(0.05), 0.8 * I);
    if (h > 0.5) c = mix(c, fi < 5.0 - lost ? (sp.y < 1.5 && sp.x > 1.0 && sp.x < 3.0 ? vec3(1.0, 0.7, 0.7) : vec3(0.95, 0.12, 0.2)) : vec3(0.25), I);
  }
#elif MODE == 6
  // Sweet Party: pastel tint and bouncing pixel hearts and stars.
  c = mix(base, base * vec3(1.08, 0.9, 1.05) + vec3(0.06, 0.0, 0.05), 0.7 * I);
  float ps = 0.012;
  for (int i = 0; i < 10; i++) {
    float fi = float(i);
    vec2 o = vec2(fract(hash1(fi) + t * 0.06 * (hash1(fi + 3.0) - 0.5)), 0.12 + 0.76 * abs(sin(t * (1.2 + hash1(fi * 2.0)) + fi)));
    vec2 sp = vec2((uv.x - o.x) * uAspect, o.y - uv.y) / ps + 3.0;
    float h = mod(fi, 2.0) < 1.0 ? pixHeart(sp) : pixStar(sp);
    vec3 col = mod(fi, 2.0) < 1.0 ? vec3(1.0, 0.45, 0.7) : vec3(1.0, 0.9, 0.35);
    c = mix(c, col, h * I);
  }
#elif MODE == 7
  // Popping Bow: a big pixel bow pops on with little hearts around it.
  float cyc = fract(t * 0.5);
  float pop = sat(1.0 + sin(cyc * 18.0) * exp(-cyc * 6.0) * 0.4) * easeOut(sat(cyc * 6.0));
  c = mix(base, base * vec3(1.05, 0.93, 1.0), 0.5 * I);
  float ps = 0.018 * pop + 1e-4;
  vec2 sp = vec2((uv.x - 0.5) * uAspect, 0.9 - uv.y) / ps + vec2(4.5, 2.5);
  float b = pixBow(sp);
  vec3 bowCol = floor(sp.x) == 4.0 ? vec3(0.8, 0.1, 0.35) : vec3(1.0, 0.35, 0.6);
  if (b > 0.5) c = mix(c, bowCol * (fract(sp.y) < 0.2 ? 1.15 : 1.0), I);
  for (int i = 0; i < 6; i++) {
    float fi = float(i);
    float a = fi * 1.047 + t;
    vec2 o = vec2(0.5, 0.88) + vec2(cos(a) * 0.3 / uAspect * 0.56, sin(a) * 0.07) * pop;
    vec2 hp = vec2((uv.x - o.x) * uAspect, o.y - uv.y) / 0.009 + 3.0;
    c = mix(c, vec3(1.0, 0.4, 0.65), pixHeart(hp) * I * (0.6 + 0.4 * sin(t * 6.0 + fi)));
  }
#elif MODE == 8
  // Misty Tint: soft monochrome with twinkling bead highlights.
  float l = luma(base);
  vec3 mono = vec3(l) * 1.05 + 0.03;
  vec3 glow = vec3(luma(blurDisc(uv, 0.02))) * 0.25;
  float n = 55.0; vec2 g = vec2(n * uAspect, n);
  vec2 cell = floor(uv * g); vec2 f = fract(uv * g) - 0.5;
  float bright = luma(src((cell + 0.5) / g).rgb);
  float tw = 0.5 + 0.5 * sin(t * 5.0 + hash(cell) * 30.0);
  float bead = (1.0 - smoothstep(0.25, 0.42, length(f))) * smoothstep(0.55, 0.85, bright) * tw;
  c = mix(base, mono + glow + vec3(bead) * 0.6, I);
#elif MODE == 9
  // Game Boy: four greens, dithered, on an LCD grid.
  float n = 80.0; vec2 g = vec2(n * uAspect, n);
  float l = luma(pixelate(uv, n)) + (bayer(uv * g) - 0.5) * 0.25;
  float lv = floor(sat(l) * 3.99);
  vec3 pal = lv < 1.0 ? vec3(0.06, 0.22, 0.06) : lv < 2.0 ? vec3(0.19, 0.38, 0.19) : lv < 3.0 ? vec3(0.55, 0.67, 0.06) : vec3(0.61, 0.74, 0.06);
  vec2 f = fract(uv * g);
  pal *= 0.85 + 0.15 * step(0.12, f.x) * step(0.12, f.y);
  c = mix(base, pal, I);
#elif MODE == 10
  // Pixel Dissolve: an ordered-dither dissolve to black and back.
  float n = 60.0; vec2 g = vec2(n * uAspect, n);
  float cover = 0.5 - 0.5 * cos(t * 1.4);
  float d = step(bayer(uv * g), cover);
  c = mix(base, mix(pixelate(uv, n), vec3(0.02), d), I);
#elif MODE == 11
  // Bead Art: the whole picture as fused perler beads with centre holes.
  float n = mix(30.0, 70.0, P3); vec2 g = vec2(n * uAspect, n);
  vec2 cell = floor(uv * g); vec2 f = fract(uv * g) - 0.5;
  vec3 pc = src((cell + 0.5) / g).rgb;
  pc = floor(pc * 6.0 + 0.5) / 6.0;
  float r = length(f);
  float bead = 1.0 - smoothstep(0.42, 0.5, r);
  float hole = smoothstep(0.1, 0.16, r);
  float spec = exp(-dot(f - vec2(-0.15, 0.15), f - vec2(-0.15, 0.15)) * 40.0) * (0.6 + 0.4 * sin(t * 3.0 + hash(cell) * 6.0));
  vec3 beads = mix(vec3(0.95), pc * (0.8 + 0.3 * (0.5 - r)) * hole + spec * 0.4, bead);
  c = mix(base, beads, I);
#elif MODE == 12
  // Pixel Rain: coloured pixels stream down in front of the scene.
  float n = 40.0; vec2 g = vec2(n * uAspect, n);
  vec2 cell = floor(uv * g);
  float speed = 6.0 + hash(vec2(cell.x, 1.0)) * 10.0;
  float head = fract(-t * speed / g.y + hash(vec2(cell.x, 7.0)));
  float dist = fract(uv.y - head);
  float trail = exp(-dist * 9.0) * step(0.55, hash(vec2(cell.x, 3.0)));
  vec3 pc = src((cell + 0.5) / g).rgb;
  c = mix(base, base * 0.75 + pc * trail * 1.3 + vec3(0.9, 1.0, 1.0) * exp(-dist * 60.0) * trail, I);
#else
  c = base;
#endif
  return vec4(c, 1.0);
}
"""

/** 3D mockups: the clip plays on a phone, a card deck, a polaroid, a TV, a billboard or film. */
internal const val FX_STYLE_3D = """
vec3 hitPlane(vec2 uv, mat3 m, vec3 c) {
  vec3 rd = vec3(asp(uv) * 2.0, 2.2); vec3 n = m * vec3(0.0, 0.0, -1.0);
  float den = dot(rd, n); if (abs(den) < 1e-4) return vec3(0.0, 0.0, -1.0);
  float d = dot(c, n) / den; if (d <= 0.0) return vec3(0.0, 0.0, -1.0);
  vec3 l = rd * d - c; return vec3(dot(l, m * vec3(1.0, 0.0, 0.0)), dot(l, m * vec3(0.0, 1.0, 0.0)), 1.0);
}
float rbox(vec2 q, vec2 b, float r) { return sdBox(q, b - r) - r; }
/** Video uv covering a box of half size b (world units), cropped to fill. */
vec2 coverUv(vec2 q, vec2 b) {
  vec2 u = q / (2.0 * b) + 0.5; float boxA = b.x / b.y; float k = boxA / uAspect;
  if (k < 1.0) u.x = (u.x - 0.5) * k + 0.5; else u.y = (u.y - 0.5) / k + 0.5;
  return u;
}
/** Deep, dim, heavily blurred backdrop so the mockup reads as a separate object on real footage. */
vec3 room(vec2 uv) {
  vec2 q = (uv - 0.5) * 0.85 + 0.5;
  vec3 b = (blurDisc(q, 0.08) + blurDisc(q + vec2(0.03, -0.02), 0.12)) * 0.5;
  vec2 v = asp(uv);
  return mix(b * 0.32, vec3(0.03, 0.03, 0.05), 0.25) * (1.0 - 0.7 * dot(v, v));
}
vec4 fx(vec2 uv) {
  float I = P1; float t = uTime * spd(P2);
  vec3 base = src(uv).rgb; vec3 c = base;
  vec2 p = asp(uv);
  float S = min(1.0, uAspect / 0.56);
#if MODE == 1
  // 3D Phone: a modern phone turns slowly in space with the clip on its screen.
  c = room(uv);
  float sh = length((p - vec2(0.0, -0.46)) / vec2(0.3 * S, 0.035));
  c *= 1.0 - 0.6 * exp(-sh * sh * 1.5);
  mat3 m = rotY(0.42 * sin(t * 0.9)) * rotX(0.14 * sin(t * 0.7 + 1.0)) * rotZ(0.05 * sin(t * 0.5));
  vec3 ctr = vec3(0.0, 0.0, 2.45);
  vec2 bh = vec2(0.47, 0.98) * S;
  // Side band: a slightly deeper copy of the body gives the frame thickness.
  vec3 side = hitPlane(uv, m, ctr + m * vec3(0.0, 0.0, 0.07));
  if (side.z > 0.0 && rbox(side.xy, bh, 0.12 * S) < 0.0) c = vec3(0.3, 0.31, 0.35) * (0.7 + 0.3 * sin(side.x * 20.0));
  vec3 h = hitPlane(uv, m, ctr);
  if (h.z > 0.0) {
    float body = rbox(h.xy, bh, 0.12 * S);
    if (body < 0.0) {
      c = vec3(0.07, 0.07, 0.08);
      if (body > -0.012 * S) c = vec3(0.62, 0.64, 0.7);
      vec2 sb = bh - 0.035 * S;
      float scr = rbox(h.xy, sb, 0.09 * S);
      if (scr < 0.0) {
        c = src(coverUv(h.xy, sb)).rgb;
        float isl = rbox(h.xy - vec2(0.0, sb.y - 0.07 * S), vec2(0.1, 0.028) * S, 0.028 * S);
        if (isl < 0.0) c = vec3(0.01);
        float gl = exp(-pow((h.x * 0.8 + h.y * 0.5 - sin(t * 0.9) * 0.9) * 3.0, 2.0));
        c += vec3(0.2) * gl;
      }
    }
  }
  c = mix(base, c, I);
#elif MODE == 2
  // Card Deck: the top card flies off the deck and the clip keeps playing on the next one.
  c = room(uv) * 0.8;
  float cyc = fract(t * 0.42);
  float go = smoothstep(0.5, 1.0, cyc);
  vec2 hs = vec2(0.34 * uAspect, 0.34) * min(1.0, 0.9 / max(uAspect, 0.9) + 0.1);
  hs = uAspect > 1.0 ? vec2(0.36, 0.36 / uAspect) * vec2(uAspect, uAspect) * 0.6 : vec2(0.34 * uAspect, 0.34);
  for (int i = 5; i >= 0; i--) {
    float k = float(i) - (i == 0 ? 0.0 : go);
    vec2 off = vec2(0.012, -0.016) * k;
    float ang = (mod(float(i), 2.0) < 1.0 ? 1.0 : -1.0) * 0.035 * k;
    if (i == 0) { off += vec2(go * 1.4 * max(uAspect, 0.6), go * 0.35); ang = -go * 0.8; }
    vec2 q = rot(-ang) * (p - off);
    float d = rbox(q, hs + 0.02, 0.035);
    float shadowD = rbox(q - vec2(0.012, -0.02), hs + 0.02, 0.035);
    c = mix(c, c * 0.45, (1.0 - smoothstep(0.0, 0.05, shadowD)) * 0.7);
    if (d < 0.0) {
      c = vec3(0.97);
      if (rbox(q, hs, 0.025) < 0.0) {
        vec2 vu = q / (2.0 * hs) + 0.5;
        c = src(vu).rgb * (i == 0 ? 1.0 : 0.72 - 0.06 * k);
      }
    }
  }
  c = mix(base, c, I);
#elif MODE == 3
  // Polaroid Drop: a polaroid drops in, bounces and settles with the clip inside.
  c = room(uv);
  float cyc = fract(t * 0.3);
  float e = sat(cyc * 2.6);
  float bounce = abs(sin(e * PI * 2.5)) * (1.0 - e) * 0.25;
  float y = (1.0 - easeOut(e)) * 1.1 + bounce;
  float ang = -0.1 + (1.0 - easeOut(e)) * 0.5;
  float ph = 0.33 * min(1.0, uAspect / 0.75 + 0.2);
  vec2 q = rot(-ang) * (p - vec2(0.0, y + 0.03));
  vec2 frame = vec2(ph + 0.035, ph + 0.035);
  vec2 fq = q - vec2(0.0, -0.05);
  float fd = sdBox(fq, frame + vec2(0.0, 0.06)) ;
  float sd = sdBox(fq - vec2(0.015, -0.025), frame + vec2(0.0, 0.06));
  c *= 1.0 - 0.55 * (1.0 - smoothstep(0.0, 0.06, sd));
  if (fd < 0.0) {
    c = vec3(0.96, 0.95, 0.92) * (0.95 + 0.05 * vnoise(q * 90.0));
    vec2 pq = q - vec2(0.0, 0.0);
    if (sdBox(pq, vec2(ph)) < 0.0) c = src(coverUv(pq, vec2(ph))).rgb * 0.97;
  }
  c = mix(base, c, I);
#elif MODE == 4
  // Retro TV: the clip plays on a curved CRT in a wooden cabinet.
  float s = min(1.0, uAspect / 0.95);
  vec3 wall = mix(vec3(0.12, 0.1, 0.09), vec3(0.2, 0.16, 0.13), uv.y);
  c = uv.y < 0.28 ? vec3(0.08, 0.06, 0.05) : wall;
  vec2 q = (p - vec2(0.0, 0.03)) / s;
  float cab = rbox(q, vec2(0.44, 0.34), 0.06);
  if (cab < 0.0) {
    c = mix(vec3(0.35, 0.2, 0.1), vec3(0.5, 0.3, 0.15), vnoise(vec2(q.x * 6.0, q.y * 80.0)));
    if (cab > -0.012) c *= 0.6;
    vec2 sq = q - vec2(-0.07, 0.01);
    float scr = rbox(sq, vec2(0.3, 0.25), 0.07);
    if (scr < 0.012) c = vec3(0.1);
    if (scr < 0.0) {
      vec2 n = sq / vec2(0.3, 0.25);
      n *= 1.0 + 0.08 * dot(n, n);
      vec2 vu = n * 0.5 + 0.5;
      vec3 v = src(vu).rgb * inside(vu);
      v *= 0.85 + 0.15 * sin(vu.y * uSize.y * 1.6);
      v *= 1.0 - 0.35 * dot(n, n);
      v *= 0.93 + 0.07 * hash1(floor(t * 20.0));
      c = v + vec3(0.04);
    }
    for (int i = 0; i < 2; i++) {
      float d = length(q - vec2(0.33, 0.12 - float(i) * 0.14));
      if (d < 0.04) c = d > 0.032 ? vec3(0.1) : vec3(0.75, 0.72, 0.65);
    }
    if (q.x > 0.28 && q.x < 0.39 && q.y < -0.08 && q.y > -0.26 && mod(q.y * 100.0, 3.0) < 1.2) c *= 0.5;
  }
  c = mix(base, c, I);
#elif MODE == 5
  // Billboard: the clip on a glowing billboard over a night skyline.
  c = mix(vec3(0.02, 0.02, 0.08), vec3(0.25, 0.08, 0.3), uv.y * uv.y);
  float bx = floor(uv.x * 22.0);
  float bhgt = 0.12 + hash(vec2(bx, 2.0)) * 0.22;
  if (uv.y < bhgt) {
    c = vec3(0.02, 0.02, 0.05);
    vec2 w = fract(uv * vec2(90.0, 70.0));
    if (w.x < 0.4 && w.y < 0.4 && hash(floor(uv * vec2(90.0, 70.0))) > 0.6) c = vec3(1.0, 0.8, 0.45) * (0.6 + 0.4 * sin(t + bx));
  }
  mat3 m = rotY(-0.35) * rotX(-0.08);
  vec3 h = hitPlane(uv, m, vec3(0.0, 0.18, 3.0));
  vec2 bb = vec2(0.62, 0.62 / uAspect * 0.56) * S; bb = uAspect < 1.0 ? vec2(0.55 * uAspect / 0.56 * 0.56, 0.55) * vec2(1.0, 1.0) : vec2(0.62 * S, 0.35 * S);
  bb = vec2(0.55 * max(uAspect, 0.7), 0.55);
  if (h.z > 0.0) {
    float d = sdBox(h.xy, bb);
    vec3 neon = hsv(fract(t * 0.1), 0.8, 1.0);
    c += neon * exp(-max(d, 0.0) * 25.0) * 0.6;
    if (d < 0.0) c = src(h.xy / (2.0 * bb) + 0.5).rgb * 1.05;
    if (abs(d) < 0.012) c = neon * 1.4;
    if (abs(h.x) < 0.02 && h.y < -bb.y && h.y > -bb.y - 0.8) c = vec3(0.15);
  }
  c = mix(base, c, I);
#elif MODE == 6
  // Film Strip: frames of the clip run by on a tilted strip of film.
  c = room(uv) * 0.7;
  vec2 q = rot(0.18) * p;
  float wdt = 0.3;
  if (abs(q.y) < wdt) {
    c = vec3(0.05, 0.04, 0.03);
    float fw = wdt * 1.3 * 0.9;
    float xs = q.x + t * 0.35;
    float idx = floor(xs / fw); float fx = fract(xs / fw);
    vec2 fr = vec2((fx - 0.5) * fw, q.y);
    if (abs(fr.x) < fw * 0.44 && abs(fr.y) < wdt * 0.72) {
      vec2 vu = vec2(fr.x / (fw * 0.88), fr.y / (wdt * 1.44)) + 0.5;
      c = src(vu).rgb * (0.9 + 0.1 * hash1(idx)) * vec3(1.02, 0.97, 0.9);
    }
    float hole = fract(xs / 0.07);
    if (abs(q.y) > wdt * 0.82 && abs(q.y) < wdt * 0.95 && hole > 0.3 && hole < 0.7) c = vec3(0.85, 0.82, 0.75);
  }
  c = mix(base, c, I);
#elif MODE == 7
  // Fanned Cards: three cards fan out, each playing the clip.
  c = room(uv) * 0.85;
  vec2 hs = vec2(0.24 * min(uAspect, 1.0) / 0.56 * 0.56, 0.24);
  hs = vec2(0.24 * max(min(uAspect, 1.4), 0.56) / 0.56 * 0.34, 0.3) ;
  for (int i = 0; i < 3; i++) {
    float k = float(i) - 1.0;
    float spread = 0.5 + 0.5 * sin(t * 1.2);
    float ang = k * 0.32 * spread;
    vec2 pivot = vec2(0.0, -0.45);
    vec2 q = rot(-ang) * (p - pivot) - vec2(0.0, 0.42);
    float d = rbox(q, hs + 0.015, 0.03);
    c *= 1.0 - 0.4 * (1.0 - smoothstep(0.0, 0.04, rbox(q - vec2(0.01, -0.015), hs + 0.015, 0.03)));
    if (d < 0.0) { c = vec3(0.97); if (rbox(q, hs, 0.02) < 0.0) c = src(coverUv(q, hs)).rgb * (i == 1 ? 1.0 : 0.85); }
  }
  c = mix(base, c, I);
#elif MODE == 8
  // Glass Card: a tilting frosted-glass card with the clip inside and a travelling glint.
  c = blur9(uv, 0.035) * 0.55;
  mat3 m = rotY(0.3 * sin(t * 0.8)) * rotX(0.2 * cos(t * 0.6));
  vec3 h = hitPlane(uv, m, vec3(0.0, 0.0, 2.7));
  vec2 gb = vec2(0.82 * uAspect, 0.82);
  if (h.z > 0.0) {
    float d = rbox(h.xy, gb, 0.08);
    if (d < 0.0) {
      c = blur9(uv, 0.02) * 0.8 + vec3(0.12);
      float inner = rbox(h.xy, gb - 0.06, 0.05);
      if (inner < 0.0) c = src(h.xy / (2.0 * (gb - 0.06)) + 0.5).rgb;
      if (d > -0.01) c += vec3(0.5);
      c += vec3(0.35) * exp(-pow((h.x + h.y * 0.6 - sin(t * 0.8) * 1.2) * 4.0, 2.0));
    }
  }
  c = mix(base, c, I);
#elif MODE == 9
  // Page Flip: the picture turns like a book page around its left edge.
  float cyc = fract(t * 0.4);
  float a = easeOut(sat(cyc * 1.6)) * PI;
  vec3 back = base * 0.95;
  c = back;
  mat3 m = rotY(-a);
  vec3 h = hitPlane(uv, m, vec3(-0.5 * uAspect * 2.0 * 0.0, 0.0, 2.2) + m * vec3(uAspect, 0.0, 0.0) - vec3(uAspect, 0.0, 0.0));
  if (h.z > 0.0) {
    vec2 q = h.xy;
    vec2 lu = vec2(q.x / (2.0 * uAspect) + 0.5, q.y / 2.0 + 0.5);
    if (inside(lu) > 0.5) {
      float shade = 0.55 + 0.45 * abs(cos(a));
      c = a < PI * 0.5 ? src(lu).rgb * shade : vec3(0.92, 0.9, 0.86) * shade;
    }
  }
  c = mix(base, c, I);
#elif MODE == 10
  // Mirror Floor: the clip stands on a glossy floor, turning slowly under a spotlight.
  c = mix(vec3(0.01), vec3(0.08, 0.07, 0.1), uv.y);
  c += vec3(0.25, 0.22, 0.3) * exp(-length((p - vec2(0.0, 0.45)) * vec2(1.0, 0.6)) * 3.0);
  mat3 m = rotY(0.5 * sin(t * 0.7));
  vec2 hb = vec2(0.5 * uAspect, 0.5);
  vec3 ctr = vec3(0.0, 0.12, 2.9);
  vec3 h = hitPlane(uv, m, ctr);
  float floorY = 0.5 + (ctr.y - hb.y) * 1.1 / ctr.z;
  if (uv.y < floorY) {
    vec2 ruv = vec2(uv.x, 2.0 * floorY - uv.y);
    vec3 rh = hitPlane(ruv, m, ctr);
    if (rh.z > 0.0 && sdBox(rh.xy, hb) < 0.0) c = mix(c, src(rh.xy / (2.0 * hb) + 0.5).rgb * 0.35, sat(1.0 - (floorY - uv.y) * 4.0));
  }
  if (h.z > 0.0 && sdBox(h.xy, hb) < 0.0) c = src(h.xy / (2.0 * hb) + 0.5).rgb;
  c = mix(base, c, I);
#else
  c = base;
#endif
  return vec4(c, 1.0);
}
"""

/** Neon: glowing frames, rings, tubes and cyberpunk grades. */
internal const val FX_NEON = """
vec3 tube(float d, vec3 col, float w) { return col * (exp(-abs(d) / w) * 0.9 + exp(-abs(d) / (w * 6.0)) * 0.45) + vec3(1.0) * exp(-abs(d) / (w * 0.35)) * 0.6; }
float flick(float t) { return 0.82 + 0.18 * step(0.12, hash1(floor(t * 14.0))); }
vec4 fx(vec2 uv) {
  float I = P1; float t = uTime * spd(P2);
  vec3 base = src(uv).rgb; vec3 c = base;
  vec2 p = asp(uv);
  vec3 col = hsv(fract(P3 + t * 0.05), 0.85, 1.0);
#if MODE == 1
  // Neon Frame: a glowing tube traces the frame edge.
  vec2 hb = vec2(0.5 * uAspect, 0.5) - 0.06;
  float d = sdBox(p, hb - 0.04) - 0.04;
  vec3 cc = mix(col, hsv(fract(P3 + 0.5 + t * 0.05), 0.85, 1.0), 0.5 + 0.5 * sin(atan(p.y, p.x) * 2.0 + t * 2.0));
  c = base * mix(1.0, 0.85, I) + tube(d, cc, 0.004) * flick(t) * I;
#elif MODE == 2
  // Neon Ring: a glowing ring hangs behind the subject with a running highlight.
  float r = length(p - vec2(0.0, 0.05));
  float d = r - 0.3 * min(uAspect / 0.56, 1.0);
  float a = atan(p.y, p.x);
  float run = 0.5 + 0.5 * cos(a * 2.0 - t * 3.0);
  c = base * mix(1.0, 0.75, I) + tube(d, col, 0.005) * (0.6 + 0.6 * run) * flick(t) * I;
#elif MODE == 3
  // Neon Heart: a heart outline beats around the centre.
  float beat = 1.0 + 0.08 * pow(abs(sin(t * 3.0)), 8.0);
  vec2 q = p / (0.36 * beat) * vec2(1.0, -1.0) * vec2(1.0, -1.0);
  float d = sdHeart(q * 0.9 + vec2(0.0, 0.55)) * 0.36;
  c = base * mix(1.0, 0.8, I) + tube(d, vec3(1.0, 0.2, 0.55), 0.005) * flick(t + 3.0) * I;
#elif MODE == 4
  // Neon Glow Edges: the scene darkens and every edge glows cyan to pink.
  float e = sat(sobel(uv) * 3.0);
  vec3 ec = mix(vec3(0.1, 0.9, 1.0), vec3(1.0, 0.2, 0.8), uv.x + 0.2 * sin(t));
  c = mix(base, base * 0.25 + ec * e * 1.4 + ec * sat(sobel(uv + 0.004) * 3.0) * 0.3, I);
#elif MODE == 5
  // Cyberpunk: teal shadows, magenta highlights, bloom and scanlines.
  float l = luma(base);
  vec3 g = mix(vec3(0.0, 0.35, 0.45) * (l * 1.4), vec3(1.0, 0.35, 0.85) * l * 1.3, smoothstep(0.35, 0.8, l));
  vec3 bloom = blurDisc(uv, 0.02);
  g += bloom * vec3(0.6, 0.2, 0.8) * 0.4;
  g *= 0.9 + 0.1 * sin(uv.y * uSize.y * 1.2);
  vec2 ca = vec2(0.003, 0.0);
  g.r = mix(g.r, src(uv + ca).r * 1.1, 0.3);
  c = mix(base, g, I);
#elif MODE == 6
  // Neon Tunnel: neon squares fly out of the centre around the clip.
  vec3 bg = vec3(0.02, 0.0, 0.05);
  float d = max(abs(p.x) / max(uAspect, 0.5), abs(p.y)) * 2.0;
  float z = fract(log2(d + 0.001) - t * 0.6);
  float ring = exp(-abs(z - 0.5) * 30.0) * sat(d * 3.0);
  bg += hsv(fract(floor(log2(d + 0.001) - t * 0.6) * 0.17 + P3), 0.8, 1.0) * ring;
  vec2 q = (uv - 0.5) / 0.55 + 0.5;
  c = inside(q) > 0.5 ? src(q).rgb : bg;
  c = mix(base, c, I);
#elif MODE == 7
  // Neon Lightning: electric arcs crawl along the frame edges.
  vec2 e = min(uv, 1.0 - uv) * vec2(uAspect, 1.0);
  float edge = min(e.x, e.y);
  float along = uv.x + uv.y;
  float n = fbm(vec2(along * 18.0, floor(t * 12.0) * 3.1));
  float bolt = exp(-abs(edge - 0.03 - (n - 0.5) * 0.05) * 160.0);
  float n2 = fbm(vec2(along * 30.0 + 5.0, floor(t * 15.0)));
  bolt += exp(-abs(edge - 0.02 - (n2 - 0.5) * 0.04) * 220.0) * 0.6;
  c = base * mix(1.0, 0.8, I) + vec3(0.55, 0.75, 1.0) * bolt * 1.6 * I + vec3(0.3, 0.4, 1.0) * exp(-edge * 25.0) * 0.25 * I;
#elif MODE == 8
  // Neon Scan: a glowing line sweeps down, tinting what it passes.
  float y = 1.0 - fract(t * 0.45);
  float d = uv.y - y;
  vec3 tint = d > 0.0 ? mix(base, base * col * 1.6, exp(-d * 6.0) * 0.8) : base;
  c = mix(base, tint + col * exp(-abs(d) * 220.0) * 1.2 + col * exp(-abs(d) * 30.0) * 0.3, I);
#elif MODE == 9
  // Neon Scribble: a glowing line draws a loop around the subject.
  float prog = fract(t * 0.35) * 1.3;
  float best = 1e3;
  vec2 prevPt = vec2(0.0);
  for (int i = 0; i < 64; i++) {
    float f = float(i) / 63.0;
    if (f > prog) break;
    float a = f * TAU * 1.2 + 0.4;
    vec2 pt = vec2(cos(a) * 0.36 * min(uAspect / 0.56, 1.3), sin(a) * 0.42) * (1.0 + 0.06 * sin(a * 5.0));
    if (i > 0) {
      vec2 ab = pt - prevPt; float h = sat(dot(p - prevPt, ab) / max(dot(ab, ab), 1e-6));
      best = min(best, length(p - prevPt - ab * h));
    }
    prevPt = pt;
  }
  c = base + tube(best, col, 0.004) * I;
#elif MODE == 10
  // Synthwave: purple-orange grade, a neon grid floor and scanlines.
  float l = luma(base);
  vec3 g = mix(vec3(0.25, 0.05, 0.4), vec3(1.0, 0.55, 0.25), smoothstep(0.2, 0.85, l)) * (0.4 + l);
  if (uv.y < 0.3) {
    float z = 0.3 / (0.31 - uv.y);
    vec2 gp = vec2(p.x * z * 2.0, z + t * 1.5);
    vec2 gl = abs(fract(gp) - 0.5);
    float line = exp(-min(gl.x, gl.y) * 30.0) * sat((0.3 - uv.y) * 8.0);
    g = mix(g, g * 0.4, sat((0.3 - uv.y) * 4.0)) + vec3(1.0, 0.2, 0.9) * line;
  }
  g *= 0.92 + 0.08 * sin(uv.y * uSize.y * 1.4);
  c = mix(base, g, I);
#elif MODE == 11
  // Neon Stars: outlined neon stars drift and twinkle.
  c = base * mix(1.0, 0.8, I);
  for (int i = 0; i < 7; i++) {
    float fi = float(i);
    vec2 pos = vec2((hash1(fi) - 0.5) * uAspect * 0.9, fract(hash1(fi + 4.0) + t * 0.06) * 1.2 - 0.6);
    vec2 d = rot(t * 0.5 + fi) * (p - pos);
    float a = atan(d.y, d.x) + PI * 0.5; float seg = TAU / 5.0;
    a = mod(a, seg) - seg * 0.5;
    float r = length(d) / (0.05 + 0.03 * hash1(fi * 3.0));
    float edge = mix(0.45, 1.0, abs(a) / (seg * 0.5));
    float sd = (r - 0.5 / edge) * 0.05;
    c += tube(sd, hsv(fract(fi * 0.21 + P3), 0.8, 1.0), 0.003) * I * (0.6 + 0.4 * sin(t * 4.0 + fi));
  }
#else
  c = base;
#endif
  return vec4(c, 1.0);
}
"""

/** Stylish extras for Light, Glitch, Retro, Party, Celebrate, Intro and Split. */
internal const val FX_CREATIVE = """
vec3 screenB(vec3 a, vec3 b) { return 1.0 - (1.0 - a) * (1.0 - b); }
vec4 fx(vec2 uv) {
  float I = P1; float t = uTime * spd(P2);
  vec3 base = src(uv).rgb; vec3 c = base;
  vec2 p = asp(uv);
#if MODE == 1
  // Sparkle Burst: on each beat, sparkles burst out from the centre.
  float cyc = fract(t * 0.8);
  for (int i = 0; i < 18; i++) {
    float fi = float(i);
    float a = fi / 18.0 * TAU + hash1(fi + floor(t * 0.8)) * 0.5;
    vec2 pos = vec2(cos(a), sin(a)) * easeOut(cyc) * (0.3 + 0.3 * hash1(fi * 5.0));
    c += vec3(1.0, 0.95, 0.8) * star4(p - pos, 0.03 + 0.03 * hash1(fi)) * (1.0 - cyc) * I;
  }
  c += vec3(1.0, 0.9, 0.7) * exp(-cyc * 12.0) * 0.3 * I;
#elif MODE == 2
  // Glitter Rain: golden glitter drifts down and flashes.
  vec4 a = particles(uv, t, 18.0, vec2(0.02, 0.4), 0.07, 1.0, 5.0, 0.12, 0.05);
  vec4 b = particles(uv + 0.3, t * 1.3, 11.0, vec2(-0.03, 0.55), 0.09, 1.0, 9.0, 0.1, 0.05);
  c += (a.rgb * a.a + b.rgb * b.a) * vec3(1.0, 0.85, 0.5) * 1.3 * I;
#elif MODE == 3
  // Rainbow Prism: a prismatic flare sweeps diagonally across.
  float x = fract(t * 0.35) * 2.4 - 0.7;
  float s = uv.x * 0.8 + uv.y * 0.4 - x;
  vec3 rb = hsv(fract(s * 2.5), 0.75, 1.0) * exp(-s * s * 18.0);
  c = screenB(base, rb * 0.7 * I);
#elif MODE == 4
  // Anamorphic Flare: blue horizontal streaks bloom from the brightest spots.
  vec3 acc = vec3(0.0);
  for (int i = -6; i <= 6; i++) {
    float fi = float(i);
    vec3 s = src(uv + vec2(fi * 0.035, 0.0)).rgb;
    acc += max(s - 0.64, 0.0) * exp(-abs(fi) * 0.32);
  }
  c = base + acc * vec3(0.35, 0.6, 1.4) * (1.3 + 0.4 * sin(t * 2.0)) * I;
#elif MODE == 5
  // Matrix Rain: green glyph streams pour over a darkened scene.
  float n = 42.0; vec2 g = vec2(n * uAspect, n);
  vec2 cell = floor(uv * g); vec2 f = fract(uv * g);
  float speed = 4.0 + hash(vec2(cell.x, 2.0)) * 8.0;
  float head = fract(-t * speed / g.y + hash(vec2(cell.x, 9.0)));
  float dist = fract(uv.y - head);
  float glyph = step(0.5, hash(cell + floor(t * 10.0 + hash(cell) * 5.0))) * step(0.15, f.x) * step(f.x, 0.85) * step(0.1, f.y) * step(f.y, 0.9);
  glyph *= step(0.5, hash(floor(f * 3.0) + cell));
  float trail = exp(-dist * 7.0);
  vec3 m = vec3(0.2, 1.0, 0.4) * glyph * trail + vec3(0.8, 1.0, 0.85) * glyph * exp(-dist * 80.0);
  c = mix(base, base * 0.3 * vec3(0.6, 1.0, 0.7) + m, I);
#elif MODE == 6
  // Signal Lost: colour bars and static break into the picture.
  float k = floor(t * 3.0);
  float on = step(0.55, hash1(k));
  vec3 bars = hsv(floor(uv.x * 7.0) / 7.0, 0.8, 0.95);
  float stat = hash(uv * uSize * 0.5 + fract(t * 30.0));
  float band = step(0.7, hash(vec2(floor(uv.y * 20.0), floor(t * 12.0))));
  vec3 g = mix(base, vec3(stat), band * 0.7);
  g = mix(g, bars, on * step(0.35, uv.y) * 0.85);
  c = mix(base, g, I);
#elif MODE == 7
  // Slice Drift: vertical slices slip up and down with colour fringes.
  float n = 12.0; float col = floor(uv.x * n);
  float off = (hash(vec2(col, floor(t * 4.0))) - 0.5) * 0.12 * step(0.4, hash(vec2(col, floor(t * 4.0) + 2.0)));
  vec2 q = vec2(uv.x, uv.y + off * I);
  c = vec3(src(q + vec2(0.006 * I, 0.0)).r, src(q).g, src(q - vec2(0.006 * I, 0.0)).b);
#elif MODE == 8
  // Camcorder: viewfinder corners, a blinking REC dot and a battery.
  c = base * vec3(1.02, 1.0, 0.97);
  c *= 0.94 + 0.06 * sin(uv.y * uSize.y * 1.5);
  vec2 e = abs(uv - 0.5); vec2 m = vec2(0.42, 0.44);
  float lw = 0.004;
  float corner = step(m.x - lw, e.x) * step(e.x, m.x) * step(m.y - 0.08, e.y) * step(e.y, m.y)
               + step(m.y - lw * uAspect, e.y) * step(e.y, m.y) * step(m.x - 0.08 / uAspect, e.x) * step(e.x, m.x);
  c = mix(c, vec3(1.0), sat(corner) * I);
  float dotd = length((uv - vec2(0.13, 0.88)) * vec2(uAspect, 1.0));
  c = mix(c, vec3(1.0, 0.1, 0.1), (1.0 - smoothstep(0.012, 0.016, dotd)) * step(0.5, fract(t * 1.0)) * I);
  vec2 bq = (uv - vec2(0.85, 0.88)) * vec2(uAspect, 1.0);
  float bat = sdBox(bq, vec2(0.035, 0.014));
  c = mix(c, vec3(1.0), (step(abs(bat), 0.003) + step(bat, -0.006) * step(bq.x, 0.035 * (2.0 * fract(t * 0.1) - 1.0))) * I);
#elif MODE == 9
  // Projector: warm circular throw, flicker, dust and a slight gate weave.
  float k = floor(t * 16.0);
  vec2 q = uv + vec2(0.0, (hash1(k) - 0.5) * 0.004);
  vec3 f = src(q).rgb * vec3(1.08, 0.98, 0.82);
  float lamp = 1.0 - smoothstep(0.35, 0.75, length(p * vec2(0.9, 1.0)));
  f *= mix(0.25, 1.1, lamp) * (0.9 + 0.1 * hash1(k * 1.3));
  f += vec3(0.9, 0.8, 0.6) * step(0.9975, hash(floor(uv * uSize * 0.4) + k)) * 0.7;
  c = mix(base, f, I);
#elif MODE == 10
  // Comic Pop: halftone, ink outlines and a starburst flash behind on the beat.
  float beat = fract(t * 0.9);
  float n = 70.0; vec2 g = vec2(n * uAspect, n);
  vec2 f = fract(uv * g) - 0.5;
  vec3 pc = src((floor(uv * g) + 0.5) / g).rgb;
  float l = luma(pc);
  float dotm = step(length(f), 0.55 * (1.0 - l));
  vec3 poster = floor(base * 4.0 + 0.5) / 4.0;
  vec3 g2 = mix(poster, poster * 0.55, dotm);
  g2 *= 1.0 - smoothstep(0.35, 0.6, sobel(uv) * 2.0);
  float a = atan(p.y, p.x);
  float burst = step(0.5, fract(a / TAU * 16.0)) * exp(-beat * 5.0) * smoothstep(0.2, 0.6, length(p));
  g2 = mix(g2, vec3(1.0, 0.85, 0.1), burst * 0.6);
  c = mix(base, g2, I);
#elif MODE == 11
  // Dreamy: soft glow, lifted pastel tones and drifting light.
  vec3 glow = blurDisc(uv, 0.025);
  vec3 g = screenB(base, glow * 0.55);
  g = mix(g, g * vec3(1.05, 0.95, 1.08) + 0.05, 0.8);
  g += vec3(1.0, 0.8, 0.9) * exp(-length(p - vec2(sin(t * 0.5) * 0.3, 0.25)) * 4.0) * 0.25;
  c = mix(base, g, I);
#elif MODE == 12
  // Kaleido Bloom: a six-fold kaleidoscope that slowly turns.
  float r = length(p); float a = atan(p.y, p.x) + t * 0.3;
  float seg = TAU / 6.0; a = abs(mod(a, seg) - seg * 0.5);
  vec2 q = unasp(vec2(cos(a), sin(a)) * r * (0.9 + 0.1 * sin(t)));
  c = mix(base, srcm(q), I);
#elif MODE == 13
  // Zoom Burst In: the clip lands out of a radial zoom blur.
  float cyc = fract(t * 0.45);
  float amt = (1.0 - easeOut(sat(cyc * 1.8))) * 0.35 * I;
  vec3 acc = vec3(0.0);
  for (int i = 0; i < 12; i++) acc += src((uv - 0.5) * (1.0 - amt * float(i) / 11.0) + 0.5).rgb;
  c = acc / 12.0 + vec3(1.0) * exp(-cyc * 12.0) * 0.4 * I;
#elif MODE == 14
  // Ring Reveal: a glowing ring opens and the picture fills in behind it.
  float cyc = fract(t * 0.4);
  float r = easeOut(sat(cyc * 1.5)) * 1.1 * max(uAspect, 1.0);
  float d = length(p) - r;
  c = d < 0.0 ? base : base * 0.05;
  c += hsv(fract(P3 + 0.55), 0.7, 1.0) * exp(-abs(d) * 90.0) * 1.2 * I;
  c = mix(base, c, I);
#elif MODE == 15
  // Flash Cut: hard white flashes with a jolt on the beat.
  float beat = fract(t * 1.4);
  float fl = exp(-beat * 9.0);
  vec2 q = uv + vec2(hash1(floor(t * 1.4)) - 0.5, hash1(floor(t * 1.4) + 1.0) - 0.5) * 0.04 * fl * I;
  c = mix(src((q - 0.5) / (1.0 + 0.06 * fl * I) + 0.5).rgb, vec3(1.0), fl * 0.85 * I);
#elif MODE == 16
  // RGB Strobe: the frame strobes through red, green and blue channels.
  float k = mod(floor(t * 8.0), 4.0);
  vec3 m = k < 1.0 ? vec3(1.3, 0.2, 0.3) : k < 2.0 ? vec3(0.2, 1.3, 0.4) : k < 3.0 ? vec3(0.3, 0.4, 1.4) : vec3(1.0);
  c = mix(base, base * m, I);
#elif MODE == 17
  // Aurora: ribbons of green and violet light ripple across the top.
  float y = uv.y - 0.55 - 0.08 * sin(uv.x * 5.0 + t) - 0.04 * sin(uv.x * 13.0 - t * 1.7);
  float band = exp(-y * y * 60.0) * (0.6 + 0.4 * fbm(vec2(uv.x * 8.0 + t * 0.4, t * 0.2)));
  vec3 aur = mix(vec3(0.1, 1.0, 0.55), vec3(0.6, 0.3, 1.0), sat(uv.x + 0.3 * sin(t * 0.3)));
  float rays = 0.6 + 0.4 * sin(uv.x * 120.0 + fbm(uv * 6.0) * 6.0);
  c = screenB(base * mix(1.0, 0.85, I), aur * band * rays * I);
#elif MODE == 18
  // Heart Pulse: a double heartbeat zoom with a red rim.
  float ph = fract(t * 0.9);
  float beat = exp(-ph * 14.0) + 0.7 * exp(-max(ph - 0.18, 0.0) * 14.0) * step(0.18, ph);
  vec2 q = (uv - 0.5) / (1.0 + 0.07 * beat * I) + 0.5;
  c = src(q).rgb;
  c = mix(c, c * vec3(1.3, 0.4, 0.45), sat(dot(p, p) * 2.2) * beat * I);
#elif MODE == 19
  // Twinkle Sky: soft stars twinkle over a gently darkened scene.
  c = base * mix(1.0, 0.78, I);
  vec4 s1 = particles(uv, t * 0.3, 22.0, vec2(0.0), 0.1, 1.0, 13.0, 0.12, 0.3);
  vec4 s2 = particles(uv + 0.5, t * 0.3, 12.0, vec2(0.0), 0.14, 6.0, 17.0, 0.14, 0.1);
  c += vec3(1.0, 0.95, 0.85) * (s1.a * 0.9 + s2.a * 0.7) * I;
#elif MODE == 20
  // Tri Split Glitch: three panels that jump out of sync.
  float col = floor(uv.x * 3.0);
  float k = floor(t * 3.0 + col * 0.37);
  vec2 off = (vec2(hash1(k + col * 7.0), hash1(k * 1.3 + col)) - 0.5) * 0.06 * step(0.5, hash1(k * 2.0 + col));
  vec2 q = vec2(fract(uv.x * 3.0) * 0.6 + 0.2, uv.y) + off;
  c = vec3(src(q + vec2(0.005, 0.0)).r, src(q).g, src(q - vec2(0.005, 0.0)).b);
  if (fract(uv.x * 3.0) < 0.01 || fract(uv.x * 3.0) > 0.99) c = vec3(0.0);
  c = mix(base, c, I);
#else
  c = base;
#endif
  return vec4(c, 1.0);
}
"""

