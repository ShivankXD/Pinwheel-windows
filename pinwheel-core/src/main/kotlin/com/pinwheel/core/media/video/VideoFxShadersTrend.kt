package com.pinwheel.core.media.video

/*
 * CapCut Trending set, rebuilt frame by frame against CapCut's own tray previews. Each MODE reads
 * its Adjust values in the order the catalog lists them (P1, P2, ...), mirroring CapCut's sliders.
 */
internal const val FX_TREND = """
float bitAt(float row, float col) { return mod(floor(row / exp2(4.0 - col)), 2.0); }
float glyphRow(float g, float r) {
  // 5x7 pixel font for G A M E O V R (rows top to bottom).
  if (g < 0.5) { return r < 0.5 ? 14.0 : r < 1.5 ? 17.0 : r < 2.5 ? 16.0 : r < 3.5 ? 23.0 : r < 5.5 ? 17.0 : 14.0; }
  if (g < 1.5) { return r < 0.5 ? 14.0 : r < 2.5 ? 17.0 : r < 3.5 ? 31.0 : 17.0; }
  if (g < 2.5) { return r < 0.5 ? 17.0 : r < 1.5 ? 27.0 : r < 3.5 ? 21.0 : 17.0; }
  if (g < 3.5) { return r < 0.5 ? 31.0 : r < 2.5 ? 16.0 : r < 3.5 ? 30.0 : r < 5.5 ? 16.0 : 31.0; }
  if (g < 4.5) { return r < 0.5 ? 14.0 : r < 5.5 ? 17.0 : 14.0; }
  if (g < 5.5) { return r < 4.5 ? 17.0 : r < 5.5 ? 10.0 : 4.0; }
  return r < 0.5 ? 30.0 : r < 2.5 ? 17.0 : r < 3.5 ? 30.0 : r < 4.5 ? 20.0 : r < 5.5 ? 18.0 : 17.0;
}
float gameOverText(vec2 p) {
  // "GAME OVER" with a trailing space (10 cells of 6 pixels); p in text pixels.
  float idx = floor(p.x / 6.0); float cx = mod(p.x, 6.0); float ry = floor(p.y);
  if (idx < 0.0 || idx > 8.0 || cx >= 5.0 || ry < 0.0 || ry > 6.0 || idx == 4.0) return 0.0;
  float g = idx == 0.0 ? 0.0 : idx == 1.0 ? 1.0 : idx == 2.0 ? 2.0 : idx == 3.0 ? 3.0 : idx == 5.0 ? 4.0 : idx == 6.0 ? 5.0 : idx == 7.0 ? 3.0 : 6.0;
  return bitAt(glyphRow(g, ry), floor(cx));
}
/** Smoothed coverage of the repeating GAME OVER marquee, 4 samples per pixel. */
float gameOverSoft(vec2 tp, vec2 aa) {
  float s = 0.0;
  s += gameOverText(vec2(mod(tp.x + aa.x, 60.0), tp.y + aa.y));
  s += gameOverText(vec2(mod(tp.x - aa.x, 60.0), tp.y + aa.y));
  s += gameOverText(vec2(mod(tp.x + aa.x, 60.0), tp.y - aa.y));
  s += gameOverText(vec2(mod(tp.x - aa.x, 60.0), tp.y - aa.y));
  return s * 0.25;
}
float sdSeg(vec2 p, vec2 a, vec2 b) { vec2 pa = p - a; vec2 ba = b - a; float h = sat(dot(pa, ba) / dot(ba, ba)); return length(pa - ba * h); }
float sdRB(vec2 p, vec2 b, float r) { vec2 q = abs(p) - b + r; return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r; }
/** Bold condensed lowercase glyphs for "game over": 0 g, 1 a, 2 m, 3 e, 4 o, 5 v, 6 r. Baseline 0, x-height 1. */
float glyphD(vec2 q, float id) {
  float th = 0.14;
  float bowl = abs(sdRB(q - vec2(0.3, 0.5), vec2(0.3 - th, 0.5 - th), 0.2)) - th;
  if (id < 0.5) {
    float stem = sdBox(q - vec2(0.6 - th, 0.3), vec2(th, 0.7));
    float hook = sdBox(q - vec2(0.3, -0.4 + th), vec2(0.3, th));
    float tip = sdBox(q - vec2(th, -0.3), vec2(th, 0.1));
    return min(min(bowl, stem), min(hook, tip));
  }
  if (id < 1.5) return min(bowl, sdBox(q - vec2(0.6 - th, 0.5), vec2(th, 0.5)));
  if (id < 2.5) {
    float s1 = sdBox(q - vec2(th, 0.5), vec2(th, 0.5));
    float s2 = sdBox(q - vec2(0.45, 0.4), vec2(th * 0.9, 0.4));
    float s3 = sdBox(q - vec2(0.9 - th, 0.4), vec2(th, 0.4));
    float arch = max(abs(sdRB(q - vec2(0.45, 0.5), vec2(0.45 - th, 0.5 - th), 0.22)) - th, 0.55 - q.y);
    return min(min(s1, s2), min(s3, arch));
  }
  if (id < 3.5) {
    float ring = max(bowl, -sdBox(q - vec2(0.55, 0.31), vec2(0.13, 0.1)));
    return min(ring, sdBox(q - vec2(0.3, 0.53), vec2(0.27, th * 0.8)));
  }
  if (id < 4.5) return bowl;
  if (id < 5.5) return min(sdSeg(q, vec2(0.09, 0.95), vec2(0.3, 0.07)), sdSeg(q, vec2(0.51, 0.95), vec2(0.3, 0.07))) - th;
  float arm = max(abs(sdRB(q - vec2(0.3, 0.55), vec2(0.3 - th, 0.45 - th), 0.2)) - th, max(0.6 - q.y, q.x - 0.47));
  return min(sdBox(q - vec2(th, 0.5), vec2(th, 0.5)), arm);
}
/** Signed distance to "game over" laid out from x = 0, 5.95 units wide. */
float gameOverDist(vec2 q) {
  if (q.y < -0.7 || q.y > 1.3 || q.x < -0.3 || q.x > 6.3) return 1.0;
  float d = glyphD(q, 0.0);
  d = min(d, glyphD(q - vec2(0.7, 0.0), 1.0));
  d = min(d, glyphD(q - vec2(1.4, 0.0), 2.0));
  d = min(d, glyphD(q - vec2(2.4, 0.0), 3.0));
  d = min(d, glyphD(q - vec2(3.4, 0.0), 4.0));
  d = min(d, glyphD(q - vec2(4.1, 0.0), 5.0));
  d = min(d, glyphD(q - vec2(4.8, 0.0), 3.0));
  d = min(d, glyphD(q - vec2(5.5, 0.0), 6.0));
  return d;
}
vec3 scr(vec3 a, vec3 b) { return 1.0 - (1.0 - clamp(a, 0.0, 1.0)) * (1.0 - clamp(b, 0.0, 1.0)); }
vec3 softLight(vec3 a, vec3 b) { return mix(2.0 * a * b + a * a * (1.0 - 2.0 * b), sqrt(max(a, 0.0)) * (2.0 * b - 1.0) + 2.0 * a * (1.0 - b), step(0.5, b)); }
vec3 sharpen(vec2 uv, float amt) {
  vec2 e = vec2(1.5 / uSize.x, 1.5 / uSize.y);
  vec3 c = src(uv).rgb;
  vec3 n = (src(uv + vec2(e.x, 0.0)).rgb + src(uv - vec2(e.x, 0.0)).rgb + src(uv + vec2(0.0, e.y)).rgb + src(uv - vec2(0.0, e.y)).rgb) * 0.25;
  return c + (c - n) * amt;
}
vec3 bigBlur(vec2 uv, float r) { return (blurDisc(uv, r) + blurDisc(uv, r * 0.5)) * 0.5; }
float hexDist(vec2 p) { p = abs(p); return max(dot(p, normalize(vec2(1.0, 1.7320508))), p.x); }
float rbx(vec2 q, vec2 b, float r) { return sdBox(q, b - r) - r; }
/** Lens bokeh: a disc blur where highlights weigh more, so lights bloom into round spots. */
vec3 bokeh(vec2 uv, float r) {
  vec3 acc = vec3(0.0); float w = 0.0;
  for (int i = 0; i < 20; i++) {
    float fi = float(i); float a = fi * 2.39996; float d = sqrt((fi + 0.5) / 20.0);
    vec3 s = src(uv + vec2(cos(a) / uAspect, sin(a)) * r * d).rgb;
    float ww = 1.0 + pow(luma(s), 4.0) * 10.0;
    acc += s * ww; w += ww;
  }
  return acc / w;
}
/** Same, with a six-bladed aperture: highlights turn into hexagons. */
vec3 hexBokeh(vec2 uv, float r) {
  vec3 acc = src(uv).rgb; float w = 1.0;
  for (int i = 0; i < 18; i++) {
    float fi = float(i);
    float ring = floor(fi / 6.0);
    float a = mod(fi, 6.0) * 1.0471976 + (ring > 1.5 ? 0.5235988 : 0.0);
    float d = ring < 0.5 ? 0.45 : ring < 1.5 ? 1.0 : 0.866;
    vec3 s = src(uv + vec2(cos(a) / uAspect, sin(a)) * r * d).rgb;
    float ww = 1.0 + pow(luma(s), 4.0) * 12.0;
    acc += s * ww; w += ww;
  }
  return acc / w;
}
vec3 zoomBlur(vec2 uv, vec2 ctr, float amt) {
  vec3 acc = vec3(0.0);
  for (int i = 0; i < 12; i++) { float f = float(i) / 11.0; acc += src(ctr + (uv - ctr) * (1.0 - amt * f)).rgb; }
  return acc / 12.0;
}
vec3 fireRamp(float x) {
  x = sat(x);
  vec3 c = mix(vec3(0.05, 0.0, 0.0), vec3(0.75, 0.12, 0.01), sat(x * 2.5));
  c = mix(c, vec3(1.0, 0.45, 0.05), sat(x * 2.5 - 0.6));
  c = mix(c, vec3(1.0, 0.8, 0.3), sat(x * 2.5 - 1.3));
  return mix(c, vec3(1.0, 0.97, 0.85), sat(x * 2.5 - 1.9));
}
vec3 thermal(float l) {
  l = sat(l);
  vec3 c = mix(vec3(0.02, 0.0, 0.04), vec3(0.55, 0.0, 0.08), sat(l * 4.0));
  c = mix(c, vec3(0.95, 0.1, 0.0), sat(l * 4.0 - 1.0));
  c = mix(c, vec3(1.0, 0.72, 0.0), sat(l * 4.0 - 2.0));
  return mix(c, vec3(1.0, 1.0, 0.75), sat(l * 4.0 - 3.0));
}
float sdTri(vec2 p, vec2 p0, vec2 p1, vec2 p2) {
  vec2 e0 = p1 - p0; vec2 e1 = p2 - p1; vec2 e2 = p0 - p2;
  vec2 v0 = p - p0; vec2 v1 = p - p1; vec2 v2 = p - p2;
  vec2 q0 = v0 - e0 * sat(dot(v0, e0) / dot(e0, e0));
  vec2 q1 = v1 - e1 * sat(dot(v1, e1) / dot(e1, e1));
  vec2 q2 = v2 - e2 * sat(dot(v2, e2) / dot(e2, e2));
  float s = sign(e0.x * e2.y - e0.y * e2.x);
  vec2 d = min(min(vec2(dot(q0, q0), s * (v0.x * e0.y - v0.y * e0.x)), vec2(dot(q1, q1), s * (v1.x * e1.y - v1.y * e1.x))), vec2(dot(q2, q2), s * (v2.x * e2.y - v2.y * e2.x)));
  return -sqrt(d.x) * sign(d.y);
}
/** A glowing picture window of half-height h centred at o on black, used by Shine Zoom and Grim Neo. */
vec3 shineWindow(vec2 uv, vec2 o, float h, float white) {
  vec2 p = asp(uv) - o;
  vec2 b = vec2(h * uAspect * 0.8, h);
  float d = sdBox(p, b);
  vec2 q = p / (b * 2.0) + 0.5;
  vec3 img = mix(src(q).rgb, vec3(1.0), white);
  vec3 c = d < 0.0 ? img : vec3(0.0);
  c += vec3(1.0, 0.98, 0.95) * (exp(-max(d, 0.0) * 9.0) * 0.55 + exp(-max(d, 0.0) * 40.0) * 0.5) * step(0.0, d) * (0.5 + white);
  return c;
}
vec4 fx(vec2 uv) {
  float t = uTime;
  vec3 base = src(uv).rgb; vec3 c = base;
  vec2 p = asp(uv);
#if MODE == 1
  // Shake: speed, intensity. Hard camera jolts with a little motion blur along each jolt.
  float s = spd(P1) * 9.0; float a = P2 * 0.06;
  float k = floor(t * s); float f = ease(fract(t * s));
  vec2 o0 = hash2(vec2(k, 1.0)) - 0.5; vec2 o1 = hash2(vec2(k + 1.0, 1.0)) - 0.5;
  vec2 o = mix(o0, o1, f) * a * 2.0; vec2 vel = (o1 - o0) * a * 2.0;
  float r = (mix(hash1(k), hash1(k + 1.0), f) - 0.5) * a * 1.5;
  vec3 acc = vec3(0.0);
  for (int i = 0; i < 6; i++) acc += src(unasp(rot(r) * p / (1.0 + a * 1.8)) + o + vel * (float(i) / 5.0 - 0.5) * 0.5).rgb;
  c = acc / 6.0;
#elif MODE == 2
  // Blur.
  c = bigBlur(uv, 0.07 * P1 + 0.001);
#elif MODE == 3
  // Edge Glow: glow. The outlines of every shape light up as a soft glowing rim in their own colour.
  float I = P1;
  float band = 0.0;
  for (int i = 0; i < 8; i++) {
    float a = float(i) * 0.785398;
    vec2 o = vec2(cos(a) / uAspect, sin(a)) * 0.012;
    vec2 dx = vec2(0.01 / uAspect, 0.0); vec2 dy = vec2(0.0, 0.01);
    float gx = luma(blur9(uv + o + dx, 0.006)) - luma(blur9(uv + o - dx, 0.006));
    float gy = luma(blur9(uv + o + dy, 0.006)) - luma(blur9(uv + o - dy, 0.006));
    band = max(band, length(vec2(gx, gy)));
  }
  band = smoothstep(0.05, 0.3, band);
  vec3 col = bigBlur(uv, 0.03);
  col = mix(vec3(luma(col)), col, 2.0) / max(max(col.r, max(col.g, col.b)), 0.2);
  col = mix(vec3(0.7, 0.85, 1.0), clamp(col, 0.0, 1.0), 0.6);
  c = scr(base * (1.0 - 0.25 * I * band), col * band * (0.5 + 1.2 * I) * (0.88 + 0.12 * sin(t * 3.0)));
#elif MODE == 4
  // Slash Reveal: speed, glow. Thin white slashes rake across the shot, then it splits into three
  // diagonal bands that slide in from alternating sides (the middle one still grey) and lock together.
  float s = spd(P1); float ph = fract(t * s * 0.42);
  vec2 dir = normalize(vec2(1.0, 1.6)); vec2 nrm = vec2(-dir.y, dir.x);
  float across = dot(p, nrm); float along = dot(p, dir);
  if (ph < 0.3) {
    float x = ph / 0.3;
    c = mix(base, vec3(luma(base)), 0.35) * 0.9;
    float lines = 0.0;
    for (int i = 0; i < 5; i++) {
      float fi = float(i);
      float off = (hash1(fi + 1.0) - 0.5) * 1.1;
      float reach = sat(x * 3.0 - fi * 0.35) * 2.4 - 1.2;
      lines += exp(-abs(across - off) * mix(900.0, 300.0, P2)) * step(along, reach);
    }
    c += vec3(1.0) * lines * (0.8 + 0.8 * P2);
  } else {
    float band = across < -0.14 ? 0.0 : across < 0.14 ? 1.0 : 2.0;
    float e = easeOut(sat((ph - 0.3 - band * 0.06) / 0.25));
    float side = mod(band, 2.0) * 2.0 - 1.0;
    vec2 q = unasp(p - dir * side * (1.0 - e) * 1.6);
    vec3 img = src(q).rgb;
    img = mix(img, vec3(luma(img)), (band == 1.0 ? 1.0 : 0.0) * (1.0 - smoothstep(0.7, 0.8, ph)));
    c = inside(q) > 0.5 ? img : vec3(0.0);
    float seam = min(abs(across + 0.14), abs(across - 0.14));
    c += vec3(1.0) * exp(-seam * 500.0) * (1.0 - smoothstep(0.55, 0.75, ph)) * (0.5 + P2);
  }
#elif MODE == 5
  // Black Flash 2: speed, intensity, size, twist. A black frame hits, then the shot punches back in.
  float s = spd(P1); float I = P2; float b = fract(t * s * 1.1); float k = floor(t * s * 1.1);
  float hit = exp(-max(b - 0.1, 0.0) * 6.0);
  float z = 1.0 + (0.05 + 0.3 * P3) * I * hit;
  float r = (P4 - 0.5) * 0.6 * I * hit * (mod(k, 2.0) * 2.0 - 1.0);
  c = src(unasp(rot(r) * p / z)).rgb;
  c *= 1.0 - step(b, 0.1) * I;
  c *= 1.0 - 0.3 * I * hit * step(0.1, b);
  c *= 1.0 - 0.35 * dot(p, p) * I;
#elif MODE == 6
  // Diamond Zoom: size, intensity, speed, horizontal, rotate. The lens breathes in and the exposure
  // blows out: whites clip to glowing light, highlights bloom and glint, then it all snaps back.
  float s = spd(P3); float I = P2;
  float ph = pow(0.5 - 0.5 * cos(t * s * 2.6), 1.5);
  float z = 1.0 + (0.04 + 0.2 * P1) * ph;
  vec2 q = unasp(rot((P5 - 0.5) * 0.5 * ph) * p / z) + vec2((P4 - 0.5) * 0.2 * ph, 0.0);
  vec3 sharp = src(q).rgb;
  vec3 bl = bigBlur(q, 0.02 + 0.04 * P1);
  vec3 ex = sharp * (1.0 + 1.3 * ph * I) + max(bl - 0.35, 0.0) * 1.8 * ph * I;
  c = mix(ex, bokeh(q, 0.035 * ph * (0.4 + P1)) * (1.0 + ph * I), 0.3 * ph * I);
  vec2 g = p * 9.0; vec2 cell = floor(g);
  float h = hash(cell);
  float lit = luma(src(unasp((cell + 0.5) / 9.0)).rgb);
  float glint = star4(fract(g) - 0.5 - (hash2(cell) - 0.5) * 0.4, 0.22) * step(0.55, h) * smoothstep(0.55, 0.8, lit);
  c += vec3(1.0, 0.97, 1.0) * glint * ph * I * (0.6 + 0.4 * sin(t * 9.0 + h * 30.0));
#elif MODE == 7
  // Error Quake: intensity, speed, horizontal, rotate. Signal errors: ghosted doubles, torn bands,
  // red channel spikes and cyan scan lines.
  float I = P1; float s = spd(P2); float k = floor(t * s * 6.0);
  float h1 = hash1(k); float h2 = hash1(k + 1.7); float h3 = hash1(k + 3.1);
  float on = step(0.3, h1);
  vec2 o = vec2((h2 - 0.5) * 0.12 * P3, (h3 - 0.5) * 0.06) * I * on;
  float r = (hash1(k + 5.0) - 0.5) * 0.2 * P4 * I * on;
  vec2 q = unasp(rot(r) * p) + o;
  float band = floor(uv.y * 14.0 + h1 * 5.0);
  float bs = step(0.72, hash(vec2(band, k))) * on;
  q.x += (hash(vec2(band + 3.0, k)) - 0.5) * 0.16 * I * bs;
  float ca = 0.016 * I * on;
  c = vec3(src(q + vec2(ca, 0.0)).r, src(q).g, src(q - vec2(ca, 0.0)).b);
  c = mix(c, src(q + vec2(0.0, 0.08 + 0.1 * (h3 - 0.5))).rgb, 0.45 * step(0.55, h2) * on * I);
  c *= mix(vec3(1.0), vec3(1.35, 0.5, 0.62), step(0.78, h3) * on * I);
  c = mix(c, vec3(0.65, 1.0, 1.0), bs * 0.35 * I);
  c += vec3(0.3, 0.9, 1.0) * exp(-abs(uv.y - hash1(k + 9.0)) * 280.0) * on * I;
#elif MODE == 8
  // Explosion: speed. A fireball erupts from the centre, rolling flames swallow the frame, then burn
  // back to the edges and clear through smoke.
  float s = spd(P1); float cyc = fract(t * s * 0.38);
  float r = length(p * vec2(1.0, 0.85));
  float R = easeOut(sat(cyc * 2.4)) * 1.35;
  vec2 w = vec2(fbm(p * 3.0 + vec2(0.0, -t * 1.3)), fbm(p * 3.0 + vec2(5.2, -t * 1.5)));
  float n = fbm(p * 4.2 + w * 1.9 - vec2(0.0, t * 2.4));
  float front = sat((R - r) * 3.0 + (n - 0.5) * 1.6);
  float fade = 1.0 - smoothstep(0.5, 0.95, cyc);
  float clear = smoothstep(0.3, 0.75, cyc);
  float n3 = fbm(p * 9.0 + w * 2.5 - vec2(0.0, t * 3.5));
  float dens = front * pow(sat(0.2 + 1.1 * n * (0.6 + 0.8 * n3)), 1.6) * fade;
  float hole = smoothstep(0.08, 0.5, r + (n - 0.5) * 0.5);
  dens *= mix(1.0, hole, sat(cyc * 3.0 - 0.3));
  dens *= mix(1.0, smoothstep(0.15, 0.75, r + (n - 0.5) * 0.6 - (1.0 - clear) * 0.4), clear);
  vec3 fire = fireRamp(dens * 1.5 + (n3 - 0.5) * 0.35);
  vec3 img = src(uv + (w - 0.5) * 0.025 * dens).rgb;
  float smoke = sat(dens * 1.6) * (1.0 - sat(dens * 2.2 - 1.0));
  c = img * (1.0 - 0.55 * smoke) * vec3(1.0, 0.9, 0.8);
  c = mix(c, fire, smoothstep(0.1, 0.45, dens) * 0.95);
  float core = exp(-r * r / (0.004 + cyc * cyc * 1.2)) * (1.0 - smoothstep(0.0, 0.25, cyc));
  c = scr(c, vec3(1.0, 0.92, 0.7) * core * 1.6);
  c = scr(c, vec3(1.0, 0.55, 0.15) * exp(-cyc * 9.0) * 0.35);
  float ringF = smoothstep(0.45, 0.6, cyc) * (1.0 - smoothstep(0.85, 1.0, cyc));
  float edgeD = sat(length(p * vec2(1.0 / uAspect, 1.0) * 1.35) - 0.45 + (n - 0.5) * 0.5);
  c = mix(c, fireRamp(n3 * 0.6 + 0.1) * 0.8, sat(edgeD * 2.2) * ringF);
#elif MODE == 9
  // Scene Cut: intensity. Rapid jump cuts: punch-in crops, a blown-out frame, a grey frame.
  float I = P1; float k = floor(t * 5.0); float h = hash1(k * 1.3 + 0.7);
  vec2 off = (hash2(vec2(k, 4.0)) - 0.5) * 0.3;
  vec3 img = base;
  if (h < 0.22) img = base;
  else if (h < 0.47) img = src((uv - 0.5) / 1.45 + 0.5 + off * 0.5).rgb;
  else if (h < 0.66) img = mix(src((uv - 0.5) / 1.15 + 0.5).rgb, vec3(1.0), 0.72);
  else if (h < 0.84) { vec3 g = src((uv - 0.5) / 1.25 + 0.5 - off * 0.4).rgb; img = vec3(luma(g)) * 1.05; }
  else img = src(unasp(rot(0.06) * p / 1.3)).rgb * 1.1;
  c = mix(base, img, I);
#elif MODE == 10
  // Sharpen Edges: crisp, contrasty detail that pulses on the beat.
  float I = P1; float b = exp(-fract(t * 1.8) * 5.0);
  c = sharpen(uv, 2.5 * I + 2.0 * I * b);
  c = (c - 0.5) * (1.0 + 0.25 * I) + 0.5;
#elif MODE == 11
  // Game Over: intensity. The frame darkens into a heavy vignette and a full-width, red "game over"
  // in bold lowercase slams onto a dark band, holds, then flickers out.
  float I = P1; float cyc = fract(t * 0.33);
  vec3 g = mix(base, vec3(luma(base)), 0.3) * 0.8;
  vec2 vp = p * vec2(1.2, 0.9);
  g *= 1.0 - 0.85 * sat(dot(vp, vp) * 1.6 - 0.05);
  float slam = 1.0 + 2.5 * (1.0 - easeOut(sat(cyc / 0.09)));
  float U = uAspect * 0.96 / 5.95 * slam;
  vec2 q = vec2((p.x + uAspect * 0.48 * slam) / U, (p.y + 0.6 * U) / (1.25 * U));
  float aa = px() / U * 1.2;
  float d = gameOverDist(q);
  float fill = 1.0 - smoothstep(-aa, aa, d);
  float edge = 1.0 - smoothstep(-aa, aa, d - 0.1);
  float band = 1.0 - smoothstep(0.9, 1.0, abs(p.y) / (1.05 * U));
  float on = 1.0 - smoothstep(0.86, 0.98, cyc) * (0.6 + 0.4 * step(0.5, fract(t * 16.0)));
  vec3 outc = mix(g, g * 0.35, band * 0.8 * on);
  outc = mix(outc, vec3(0.02), edge * on);
  outc = mix(outc, mix(vec3(0.72, 0.04, 0.05), vec3(0.95, 0.16, 0.12), sat(q.y)), fill * on);
  c = mix(base, outc, I);
#elif MODE == 12
  // Shiny Stack: intensity, speed. The shot lifts off as a card over a big zoomed copy of itself:
  // first blown out to glowing white, snapping between 3D tilts, then the picture returns and the
  // next card of the stack slides in, each with a bright glowing rim.
  float I = P1; float s = spd(P2); float cyc = fract(t * s * 0.4);
  float white = 1.0 - smoothstep(0.38, 0.5, cyc);
  vec3 bgc = src((uv - 0.5) / 1.6 + 0.5).rgb * 0.75;
  bgc = mix(bgc, scr(bgc, vec3(0.55)), white * 0.7);
  float k = floor(t * s * 3.0); float f = easeOut(sat(fract(t * s * 3.0) * 2.5));
  vec3 a0 = vec3(hash1(k), hash1(k + 3.0), hash1(k + 5.0)) - 0.5;
  vec3 a1 = vec3(hash1(k + 1.0), hash1(k + 4.0), hash1(k + 6.0)) - 0.5;
  vec3 ang = mix(a0, a1, f) * vec3(0.5, 1.0, 0.4) * (0.4 + 0.6 * I);
  mat3 m = rotY(ang.y) * rotX(ang.x) * rotZ(ang.z);
  float cs = 0.6;
  float slide = (1.0 - easeOut(sat((cyc - 0.55) / 0.25))) * step(0.55, cyc);
  vec3 outc = bgc; float glow = 0.0;
  for (int i = 0; i < 2; i++) {
    float fi = float(i);
    vec3 ctr = vec3(0.0, 0.0, 2.2) + m * vec3(0.0, (slide - fi) * (2.0 * cs + 0.08), 0.0);
    vec4 q = card2(uv, m, ctr, cs);
    vec2 e2 = abs(q.xy - 0.5) - 0.5; float dOut = max(max(e2.x, e2.y), 0.0);
    float live = fi < 0.5 ? 1.0 : step(0.001, slide);
    glow += exp(-dOut * 16.0) * step(0.0001, q.w) * live;
    if (q.z > 0.5 && live > 0.5) {
      vec3 img = mix(src(q.xy).rgb, vec3(1.0), white * 0.92);
      float rim = exp(-min(min(q.x, 1.0 - q.x) * uAspect, min(q.y, 1.0 - q.y)) * 90.0);
      outc = img + vec3(1.0) * rim * 0.8;
    }
  }
  outc += vec3(1.0) * glow * (0.25 + 0.9 * white);
  c = outc;
#elif MODE == 13
  // Dance Flash: speed, intensity, sharpen. Strobes between a crisp, inky, saturated frame and a
  // pale washed-out one.
  float s = spd(P1); float I = P2; float k = floor(t * s * 3.0); float b = fract(t * s * 3.0);
  float pale = mod(k, 2.0);
  vec3 sh = sharpen(uv, 1.0 + 3.0 * P3);
  vec3 crisp = (sh - 0.5) * 1.4 + 0.45; crisp = mix(vec3(luma(crisp)), crisp, 1.35);
  vec3 wash = mix(base, vec3(1.0), 0.55); wash = mix(vec3(luma(wash)), wash, 0.45) * vec3(0.96, 1.0, 1.04) + 0.04;
  float halfM = mod(k, 3.0) == 2.0 ? step(0.5, fract(k * 0.37) > 0.5 ? uv.x : 1.0 - uv.x) : 1.0;
  c = mix(crisp, wash, pale * halfM);
  c = mix(c, vec3(1.0), exp(-b * 18.0) * 0.3 * pale);
  c = mix(base, c, I);
#elif MODE == 14
  // Glass Breaking: speed, intensity. Real-looking shards of broken glass drift and tumble in front of
  // the shot: each one refracts and darkens what is behind it, catches a hard specular edge and a sheen
  // as it turns, a star glint flares off the top shard, and fine glass dust floats through.
  float s = spd(P1); float I = P2;
  c = base * mix(1.0, 0.8, I);
  vec4 dust = particles(uv, t * 0.2 * s, 40.0, vec2(0.006, -0.02), 0.02, 0.0, 11.0, 0.6, 0.0);
  c += vec3(0.85, 0.9, 1.0) * dust.a * 0.7 * I;
  vec2 L = normalize(vec2(-0.6, 0.8));
  vec2 glintAt = vec2(-0.2 * uAspect, 0.34);
  for (int i = 0; i < 8; i++) {
    float fi = float(i);
    float h0 = hash1(fi * 7.13 + 1.0); float h1 = hash1(fi * 3.71 + 2.0); float h2 = hash1(fi * 5.29 + 3.0);
    // Scatter round the frame edge like the CapCut pack, keeping the middle clearer.
    float ang = fi * 2.39996 + 0.7;
    float rad = 0.28 + 0.2 * h1;
    vec2 ctr = i == 0 ? glintAt : vec2(cos(ang) * uAspect * 1.05, sin(ang) * 1.05) * rad;
    ctr += vec2(sin(t * 0.35 * s + fi * 1.3), cos(t * 0.3 * s + fi * 2.1)) * 0.02;
    float big = i == 3 || i == 7 ? 1.7 : 1.0;
    float sz = (0.06 + 0.07 * h2) * big;
    float tumble = t * s * (0.4 + 0.5 * h0) + fi;
    float sq = 0.35 + 0.65 * abs(cos(tumble));
    vec2 lp = rot(h0 * 6.28 + 0.25 * sin(t * 0.3 * s + fi)) * (p - ctr);
    lp.x /= sq;
    // An irregular convex quad (long slivers for some).
    float sliver = step(0.7, h1);
    vec2 a0 = vec2(-1.0, -0.55 - 0.3 * h2) * sz; vec2 a1 = vec2(0.9 + 0.4 * h0, -0.3) * sz * vec2(1.0 + sliver * 1.4, 1.0);
    vec2 a2 = vec2(0.5, 0.75 + 0.3 * h1) * sz; vec2 a3 = vec2(-0.75, 0.35) * sz * vec2(1.0, 1.0 - sliver * 0.6);
    float d = -1e3;
    vec2 e; vec2 n;
    e = a1 - a0; n = normalize(vec2(e.y, -e.x)); d = max(d, dot(lp - a0, n));
    e = a2 - a1; n = normalize(vec2(e.y, -e.x)); d = max(d, dot(lp - a1, n));
    e = a3 - a2; n = normalize(vec2(e.y, -e.x)); d = max(d, dot(lp - a2, n));
    e = a0 - a3; n = normalize(vec2(e.y, -e.x)); d = max(d, dot(lp - a3, n));
    float soft = 0.002 * big * big;
    float inside = 1.0 - smoothstep(-soft, soft, d);
    if (inside > 0.0) {
      vec2 rq = uv + (lp / sz) * vec2(0.02, 0.025) * (1.0 + big) + vec2(0.01 * sin(tumble), 0.0);
      vec3 glass = src(rq).rgb * 0.72 + vec3(0.05, 0.06, 0.07);
      float sheen = pow(sat(1.0 - abs(dot(lp / sz, vec2(0.7, 0.7)) - 0.6 * sin(tumble * 1.3))), 6.0);
      glass += vec3(0.85, 0.9, 1.0) * sheen * 0.22;
      float face = sat(0.5 + 0.5 * dot(normalize(lp + 1e-4), L));
      glass += vec3(0.9, 0.95, 1.0) * exp(d / sz * 16.0) * (0.15 + 0.75 * pow(face, 3.0));
      c = mix(c, glass, inside * (0.85 + 0.15 * I));
    }
    c += vec3(0.95, 0.97, 1.0) * exp(-abs(d) * 900.0) * 0.5 * I * (1.0 - inside);
  }
  float pulse = 0.5 + 0.5 * sin(t * 2.4 * s);
  vec2 dg = p - glintAt - vec2(0.03, 0.02);
  c += vec3(1.0) * (star4(dg, 0.3) * 0.9 + exp(-length(dg) * 16.0) * 0.9) * pulse * I;
#elif MODE == 15
  // Negative Panels: speed. Hard-edged panels of the negative image snap across the frame:
  // a strip, a tall column, most of the frame, a narrow bar.
  float s = spd(P1); float k = floor(t * s * 2.3); float b = fract(t * s * 2.3);
  float h = hash1(k + 0.5);
  vec4 R = vec4(0.0, 0.84, 1.0, 1.0);
  if (h > 0.2) R = vec4(0.58, 0.0, 1.0, 1.0);
  if (h > 0.4) R = vec4(0.0, 0.0, 1.0, 0.84);
  if (h > 0.6) R = vec4(0.32, 0.0, 0.6, 1.0);
  if (h > 0.8) R = vec4(0.0, 0.28, 1.0, 0.72);
  float e = easeOut(sat(b * 5.0));
  vec2 ctr = (R.xy + R.zw) * 0.5; vec2 hs = (R.zw - R.xy) * 0.5;
  hs *= mod(k, 2.0) > 0.5 ? vec2(e, 1.0) : vec2(1.0, e);
  vec2 dd = abs(uv - ctr) - hs;
  float inP = step(max(dd.x, dd.y), 0.0);
  vec3 neg = (1.0 - base) * vec3(0.93, 1.0, 1.08);
  c = mix(base, neg, inP);
#elif MODE == 16
  // Darken Flash: speed, intensity. The frame dips dark, then blows out to a soft white flash and
  // recovers.
  float s = spd(P1); float I = P2; float ph = fract(t * s * 0.95);
  float dark = smoothstep(0.24, 0.3, ph) * (1.0 - smoothstep(0.5, 0.54, ph));
  float fl = smoothstep(0.5, 0.55, ph) * exp(-max(ph - 0.55, 0.0) * 8.0);
  vec3 bl = bigBlur(uv, 0.015 + 0.04 * fl);
  c = base * (1.0 - 0.6 * dark * I);
  c = mix(c, mix(bl, vec3(1.0, 0.97, 0.98), 0.78), fl * I);
#elif MODE == 17
  // Horizontal Open: a thin line of picture opens slowly into the full frame.
  float h = 0.004 + 0.5 * pow(sat(t / 2.2), 1.3);
  c = abs(uv.y - 0.5) < h ? base : vec3(0.0);
#elif MODE == 18
  // By the Fireplace: intensity. Glowing embers stream up past the camera as short orange streaks,
  // with big out-of-focus sparks drifting in front.
  float I = P1;
  vec3 warm = base * vec3(0.98, 0.9, 0.8);
  vec3 em = vec3(0.0);
  for (int L = 0; L < 3; L++) {
    float fl = float(L);
    float dens = 6.0 + fl * 5.0;
    vec2 q = asp(uv) * dens + vec2(sin(t * 0.7 + fl) * 0.3, -t * (1.2 + fl * 0.6));
    vec2 cell = floor(q); vec2 f = fract(q) - 0.5;
    float h = hash(cell + fl * 7.0);
    vec2 o = (hash2(cell + fl * 3.1) - 0.5) * 0.6;
    vec2 dir = normalize(vec2(hash1(h * 91.0) - 0.5, 1.0));
    float len = 0.08 + 0.18 * hash1(h * 37.0);
    float d = sdSeg(f, o - dir * len, o + dir * len);
    float fl2 = 0.6 + 0.4 * sin(t * 9.0 + h * 40.0);
    em += vec3(1.0, 0.5, 0.12) * (exp(-d * (32.0 + fl * 16.0)) * 1.3 + exp(-d * 9.0) * 0.25) * step(0.45, h) * fl2;
    em += vec3(1.0, 0.8, 0.5) * exp(-length(f - o) * 22.0) * step(0.85, h) * fl2;
  }
  vec2 bq = asp(uv) * 2.5 + vec2(0.0, -t * 0.3);
  vec2 bc = floor(bq); float bh = hash(bc + 5.0);
  float orb = smoothstep(0.22, 0.12, length(fract(bq) - 0.5 - (hash2(bc) - 0.5) * 0.4)) * step(0.7, bh);
  em += vec3(1.0, 0.75, 0.45) * orb * 0.35;
  c = mix(base, scr(warm, em), I);
#elif MODE == 19
  // Fade In.
  c = base * ease(sat(uProgress * 1.25));
#elif MODE == 20
  // Neon: atmosphere. Outlines glow in the picture's own colours over black.
  float e = sobel(uv);
  float e2 = (sobel(uv + vec2(0.003, 0.0)) + sobel(uv - vec2(0.003, 0.0)) + sobel(uv + vec2(0.0, 0.003)) + sobel(uv - vec2(0.0, 0.003))) * 0.25;
  vec3 hue = base / max(max(base.r, max(base.g, base.b)), 0.05);
  hue = clamp(mix(vec3(luma(hue)), hue, 1.9), 0.0, 1.0);
  vec3 wash = mix(vec3(1.0, 0.55, 0.15), vec3(0.2, 0.85, 1.0), sat(1.2 - uv.y * 1.4));
  hue = mix(wash, hue, smoothstep(0.08, 0.3, length(base - vec3(luma(base)))));
  float fl = 0.92 + 0.08 * sin(t * 17.0);
  c = base * 0.08 * P1 + hue * (smoothstep(0.35, 0.9, e) * 1.25 + smoothstep(0.3, 0.9, e2) * 0.3) * fl;
#elif MODE == 21
  // Vignette Noir: atmosphere.
  float l = luma(base);
  vec3 mono = vec3(pow(l, 1.25) * 1.15);
  mono += (hash(uv * uSize + floor(t * 24.0)) - 0.5) * 0.06;
  mono *= 1.0 - 0.95 * sat(dot(p, p) * 1.7 - 0.05);
  c = mix(base, mono, P1);
#elif MODE == 22
  // Dreamy Halo: glow, range, size, soft light, filter, sharpen, blur. The picture blooms: bright
  // areas overexpose and a wide halo of their own colour spills out around them.
  vec3 img = mix(sharpen(uv, P6 * 2.0), bigBlur(uv, 0.02 * P7), sat(P7 * 1.5));
  vec3 bloom = bigBlur(uv, mix(0.025, 0.09, P2));
  vec3 bloom2 = blurDisc(uv, mix(0.008, 0.025, P2));
  bloom = mix(vec3(luma(bloom)), bloom, 1.5);
  c = scr(img * (1.0 + 0.3 * P1), bloom * (0.3 + 1.2 * P1) * mix(0.6, 1.4, P3) + bloom2 * 0.35 * P1);
  c = mix(c, softLight(c, bloom2), P4 * 0.6);
  c = mix(c, mix(vec3(luma(c)), c, 1.2) * vec3(1.02, 1.0, 1.04) + 0.02, P5);
#elif MODE == 23
  // Lightning Crack: twist, speed, stickers, filter. A grey storm; forked bolts strike and light
  // the whole sky.
  float s = spd(P2); float k = floor(t * s * 2.2); float b = fract(t * s * 2.2);
  vec3 storm = mix(base, vec3(luma(base)) * vec3(0.85, 0.9, 1.0) * 0.85, P4);
  float strike = step(0.25, hash1(k * 1.9));
  float x0 = (hash1(k + 7.0) - 0.5) * uAspect * 1.2;
  float y = p.y;
  float xp = x0 + (fbm(vec2(y * (4.0 + 8.0 * P1), k)) - 0.5) * 0.35 + (y - 0.5) * (hash1(k + 2.0) - 0.5) * 0.5;
  float d = abs(p.x - xp);
  float by = mix(0.5, -0.5, sat(b * 6.0));
  float bolt = (exp(-d * 400.0) * 1.5 + exp(-d * 45.0) * 0.4) * step(by, y);
  float bx = xp + (y - 0.1) * 0.8 + (fbm(vec2(y * 9.0, k + 3.0)) - 0.5) * 0.15;
  float br = exp(-abs(p.x - bx) * 500.0) * step(-0.15, y) * step(y, 0.1) * step(by, y);
  float life = strike * (1.0 - smoothstep(0.35, 0.6, b));
  float flash = strike * exp(-b * 10.0);
  c = storm * (0.7 + 0.3 * step(0.2, hash1(k + 11.0)) ) + vec3(0.85, 0.92, 1.0) * (bolt + br * 0.8) * life * P3;
  c = mix(c, vec3(0.92, 0.95, 1.0), flash * 0.55 * P3);
#elif MODE == 24
  // Narrow Focus: letterbox bars close in, the frame flashes white and turns into a stark black and
  // white halftone print that slowly pushes in.
  float cyc = fract(t * 0.4);
  float bars = 0.24 * easeOut(sat(cyc / 0.15));
  float z = 1.0 + 0.3 * ease(sat((cyc - 0.15) / 0.85));
  vec2 q = (uv - 0.5) / z + 0.5;
  float l = sat((luma(src(q).rgb) - 0.5) * 1.25 + 0.55);
  vec2 g = rot(0.785) * p * 95.0;
  float r = sqrt(1.0 - l) * 0.58;
  float dotm = 1.0 - smoothstep(r - 0.1, r + 0.1, length(fract(g) - 0.5));
  vec3 ht = vec3(1.0 - dotm);
  ht = mix(ht, vec3(1.0), (1.0 - smoothstep(0.05, 0.25, cyc)) * 0.9);
  c = abs(uv.y - 0.5) < 0.5 - bars ? ht : vec3(0.0);
#elif MODE == 25
  // Vignette: texture.
  vec2 vo = p * vec2(1.25, 0.85); float v = pow(sat(dot(vo, vo) * 2.6 - 0.1), 0.8);
  float grain = (hash(uv * uSize * 0.7 + floor(t * 20.0)) - 0.5) * 0.12 * P1;
  c = base * (1.0 - 0.92 * v) + grain;
#elif MODE == 26
  // Offset Slice: speed.
  float s = spd(P1); float k = floor(t * s * 8.0);
  float band = floor(uv.y * 11.0 + hash1(k) * 3.0);
  float on = step(0.62, hash(vec2(band, k)));
  vec2 q = uv + vec2((hash(vec2(band, k + 1.0)) - 0.5) * 0.18 * on, 0.0);
  c = vec3(src(q + vec2(0.012 * on, 0.0)).r, src(q).g, src(q - vec2(0.012 * on, 0.0)).b);
  float row = floor(uv.y * 70.0);
  float stat = step(0.93, hash(vec2(row, k))) * step(0.5, hash(vec2(floor(uv.x * 6.0), row + k)));
  vec3 bar = hsv(hash(vec2(floor(uv.x * 24.0), row + k)), 0.8, 1.0);
  c = mix(c, bar * 0.9, stat * 0.75);
#elif MODE == 27
  // Zoom Lens: speed, range.
  float s = spd(P1); float ph = 0.5 - 0.5 * cos(t * s * 2.4);
  float z = 1.0 + (0.1 + 0.4 * P2) * ph;
  vec3 acc = vec3(0.0);
  for (int i = 0; i < 8; i++) acc += srcm((uv - 0.5) / (z * (1.0 + float(i) * 0.012 * ph)) + 0.5);
  c = acc / 8.0;
#elif MODE == 28
  // Picture Bumps: speed. A montage of bumps on the beat: a zoom punch with motion blur, three
  // stacked panels, a letterboxed frame with a light band, a soft blown-out hit.
  float s = spd(P1); float k = floor(t * s * 1.75); float b = fract(t * s * 1.75);
  float st = mod(k, 4.0); float hit = exp(-b * 5.0);
  if (st < 0.5) {
    vec3 acc = vec3(0.0);
    for (int i = 0; i < 6; i++) acc += src((uv - 0.5) / (1.0 + 0.45 * hit) + 0.5 + vec2(0.0, (float(i) / 5.0 - 0.5) * 0.08 * hit)).rgb;
    c = acc / 6.0;
  } else if (st < 1.5) {
    float col = floor(uv.x * 3.0);
    float sl = (1.0 - easeOut(sat(b * 3.0))) * (col - 1.0) * 0.6;
    vec2 q = col == 1.0 ? (uv - 0.5) / 1.05 + 0.5 : (uv - 0.5) / 1.5 + 0.5 + vec2(0.0, sl);
    c = src(q).rgb * (col == 1.0 ? 1.0 : 0.55);
    if (col != 1.0) c = mix(c, vec3(luma(c)), 0.5);
    float gap = min(fract(uv.x * 3.0), 1.0 - fract(uv.x * 3.0));
    c *= smoothstep(0.004, 0.01, gap);
  } else if (st < 2.5) {
    c = abs(uv.y - 0.5) < 0.3 ? src((uv - 0.5) / (1.2 + 0.2 * hit) + 0.5).rgb : vec3(0.0);
    c = mix(c, vec3(1.0), exp(-abs(uv.y - 0.5 + 0.2 * (b - 0.5)) * 60.0) * hit * 0.9);
  } else {
    c = mix(bigBlur(uv, 0.035 * hit + 0.002), vec3(1.0), 0.45 * hit);
  }
#elif MODE == 29
  // Background Fit: speed. A thin white line splits the dark, columns of the picture open out from it,
  // then the shot settles as a framed card over a big soft grey copy of itself.
  float s = spd(P1); float ph = fract(t * s * 0.3);
  float sc = 0.66;
  vec3 backdrop = vec3(luma(bigBlur((uv - 0.5) * 0.9 + 0.5, 0.02))) * 0.8 + 0.04;
  if (ph < 0.1) {
    c = vec3(0.0) + vec3(1.0) * exp(-abs(p.x) * 900.0) * step(abs(p.y), easeOut(ph / 0.1) * 0.35);
  } else if (ph < 0.4) {
    float x = (ph - 0.1) / 0.3;
    float col = floor(abs(p.x) / (uAspect * 0.12));
    float open = easeOut(sat(x * 2.2 - col * 0.3));
    float fr = fract(abs(p.x) / (uAspect * 0.12));
    c = step(fr, open) > 0.5 ? src((uv - 0.5) / (1.0 + 0.2 * (1.0 - x)) + 0.5).rgb : vec3(0.0);
    c += vec3(1.0) * exp(-abs(fr - open) * uAspect * 0.12 * 500.0) * (1.0 - x) * step(col, 4.0);
  } else {
    float e = easeOut(sat((ph - 0.4) / 0.2));
    float scn = mix(1.0, sc, e);
    vec2 q = (uv - 0.5) / scn + 0.5;
    c = backdrop * e;
    if (inside(q) > 0.5) c = src(q).rgb;
    vec2 e2 = abs(q - 0.5) - 0.5; float bd = max(e2.x, e2.y);
    c = mix(c, vec3(1.0), step(0.0, bd) * step(bd, 0.012 / scn) * e);
  }
#elif MODE == 30
  // Astral 2: intensity, glow, twist, atmosphere. A dreamy beat: the frame smears into a bright
  // swirling blur and glows, then briefly sinks into darkness.
  float b = fract(t * 0.85);
  float sm = exp(-b * 6.0);
  vec3 acc = vec3(0.0);
  for (int i = 0; i < 8; i++) {
    float f = float(i) / 7.0;
    acc += src(unasp(rot((P3 - 0.2) * 0.6 * f * sm) * p / (1.0 + 0.3 * f * sm * P1))).rgb;
  }
  acc /= 8.0;
  vec3 g = scr(acc, bigBlur(uv, 0.05) * vec3(1.0, 0.75, 0.9) * P2 * 1.3 * sm);
  g = mix(g, vec3(1.0, 0.95, 0.97), sm * 0.45 * P1);
  float dk = smoothstep(0.55, 0.58, b) * (1.0 - smoothstep(0.66, 0.72, b));
  c = g * (1.0 - dk * 0.85 * P4);
#elif MODE == 31
  // Wobbly Flash: speed. The picture ripples and flashes white on the beat.
  float s = spd(P1); float b = fract(t * s * 1.1);
  float hit = exp(-b * 5.0);
  vec2 w = vec2(sin(uv.y * 12.0 + t * s * 9.0), cos(uv.x * 10.0 + t * s * 8.0)) * 0.025 * hit;
  c = src(uv + w).rgb;
  c = mix(c, mix(bigBlur(uv + w, 0.01), vec3(1.0), 0.6), hit * 0.85);
#elif MODE == 32
  // Feverish Imprint: speed, intensity. A hot red-to-gold gradient map with a ghosted imprint,
  // flashing to a pale gold wash.
  float s = spd(P1); float I = P2; float k = floor(t * s * 2.0); float b = fract(t * s * 2.0);
  float l = luma(base);
  vec3 gm = mix(mix(vec3(0.95, 0.3, 0.25), vec3(1.0, 0.62, 0.3), sat(l * 2.0)), vec3(1.0, 0.95, 0.65), sat(l * 2.0 - 1.0));
  vec3 ghost = src((uv - 0.5) / (1.0 + 0.06 * (1.0 - b)) + 0.5).rgb;
  vec3 warm = scr(mix(base, gm, 0.55), gm * ghost * 0.3);
  float pale = step(0.5, hash1(k)) * exp(-b * 2.0);
  warm = mix(warm, vec3(1.0, 0.97, 0.7) * (0.8 + 0.25 * l), pale * 0.8);
  warm += vec3(1.0, 0.95, 0.8) * exp(-abs(uv.y - fract(t * s * 0.7)) * 120.0) * 0.25;
  c = mix(base, warm, I);
#elif MODE == 33
  // Tension Zoom: speed, intensity.
  float s = spd(P1); float I = P2; float ph = fract(t * s * 0.4);
  float z = 1.0 + 0.3 * I * easeOut(ph);
  vec2 j = (vec2(vnoise(vec2(t * 20.0, 1.0)), vnoise(vec2(3.0, t * 20.0))) - 0.5) * 0.008 * I * ph;
  c = src((uv - 0.5) / z + 0.5 + j).rgb;
  c *= 1.0 - 0.7 * I * ph * sat(dot(p, p) * 2.0);
#elif MODE == 34
  // Negative: quick bursts into the colour negative.
  float ph = fract(t * 0.8);
  float neg = step(ph, 0.1) + step(0.2, ph) * step(ph, 0.27);
  c = mix(base, 1.0 - base, neg);
#elif MODE == 35
  // Cut Shift: speed. Thick white slashes race across the frame (one with a red fringe); the picture
  // between them shears along the cut, then snaps back.
  float s = spd(P1); float ph = fract(t * s * 0.55);
  vec2 dir = normalize(vec2(0.45, 1.0)); vec2 nrm = vec2(-dir.y, dir.x);
  float across = dot(p, nrm);
  float sweep = mix(-1.4, 1.4, ease(sat(ph / 0.55)));
  float live = 1.0 - smoothstep(0.55, 0.65, ph);
  float strip = floor((across - sweep) * 6.0);
  vec2 q = unasp(p + dir * (hash1(strip) - 0.5) * 0.16 * live);
  c = src(q).rgb;
  float bars = 0.0; float red = 0.0;
  for (int i = 0; i < 4; i++) {
    float fi = float(i);
    float x = across - sweep + fi * 0.2 - 0.3;
    float w = 0.018 + 0.05 * hash1(fi + 3.0);
    bars = max(bars, 1.0 - smoothstep(w, w + 0.006, abs(x)));
    if (i == 0) red = exp(-abs(x + w + 0.012) * 180.0);
  }
  c = mix(c, vec3(1.0), bars * live);
  c = mix(c, vec3(0.95, 0.1, 0.12), red * live * 0.8);
#elif MODE == 36
  // Effect Mashup: speed. A new treatment on every beat: shuffled blocks, an offset card, a
  // blown-out top light, a ripple, a punch-in.
  float s = spd(P1); float k = floor(t * s * 1.6); float b = fract(t * s * 1.6); float st = mod(k, 5.0);
  float hit = exp(-b * 4.0);
  if (st < 0.5) {
    mat3 m = rotY(mix(-0.9, 0.5, easeOut(b))) * rotX(0.15) * rotZ(-0.08);
    vec4 q = card2(uv, m, vec3(0.0, 0.0, 2.3), 0.62);
    c = q.z > 0.5 ? src(q.xy).rgb : vec3(0.0);
  } else if (st < 1.5) {
    vec2 q = (uv - vec2(0.56, 0.46)) / 0.72 + 0.5 + vec2(0.0, 0.3 * (1.0 - easeOut(sat(b * 3.0))));
    c = inside(q) > 0.5 ? src(q).rgb : src(uv).rgb * 0.15;
  } else if (st < 2.5) {
    c = mix(base, vec3(1.0), sat(uv.y * 1.6 - 0.35) * 0.85 * (0.4 + 0.6 * hit));
  } else if (st < 3.5) {
    float r = length(p);
    float wv = sin(r * 45.0 - b * 22.0) * exp(-r * 3.0) * hit;
    c = src(uv + normalize(p + 1e-4) / vec2(uAspect, 1.0) * wv * 0.025).rgb + vec3(1.0) * max(wv, 0.0) * 0.25;
  } else {
    c = src((uv - 0.5) / (1.0 + 0.25 * hit) + 0.5).rgb;
  }
#elif MODE == 37
  // Hexagonal Spot: intensity. The lens opens wide: highlights swell into bright hexagonal bokeh,
  // washed in soft pink light, then pull back into focus.
  float I = P1; float ph = pow(0.5 - 0.5 * cos(t * 2.2), 1.3);
  vec3 bk = hexBokeh(uv, 0.07 * ph + 0.002);
  c = mix(base, bk, sat(ph * 1.5));
  c = scr(c, (max(bk - 0.4, 0.0) * 2.4 * vec3(1.0, 0.85, 0.95) + vec3(0.4, 0.22, 0.32)) * ph * I);
  c = mix(c, vec3(1.0, 0.97, 0.98), pow(ph, 3.0) * 0.55 * I);
  c = mix(base, c, 0.35 + 0.65 * I);
#elif MODE == 38
  // Broken Shine: speed. A white light bar sweeps, the frame bursts outward in a radial blast of
  // light, whites out, then drops back to normal.
  float s = spd(P1); float ph = fract(t * s * 0.45);
  if (ph < 0.22) {
    float x = ph / 0.22;
    float d = dot(p, normalize(vec2(1.0, 0.55))) - mix(-0.9, 0.9, x);
    c = base * 1.05 + vec3(1.0) * (exp(-abs(d) * 18.0) * 0.9 + step(abs(d), 0.07) * 0.6);
  } else if (ph < 0.58) {
    float x = (ph - 0.22) / 0.36;
    vec3 zb = zoomBlur(uv, vec2(0.5, 0.5), 0.55 * (1.0 - x * 0.5));
    zb = mix(zb, vec3(1.0), smoothstep(0.4, 0.75, luma(zb)) * 0.85);
    c = zb * vec3(0.95, 1.0, 1.1) + vec3(1.0) * exp(-length(p) * 3.0) * 0.4;
  } else if (ph < 0.78) {
    float x = (ph - 0.58) / 0.2;
    c = mix(bigBlur(uv, 0.03), vec3(1.0), 0.75 * (1.0 - x));
  } else {
    c = base;
  }
#elif MODE == 39
  // Move Cloud: speed. Thick sunlit clouds roll in over the shot, then part and drift away.
  float s = spd(P1); float ph = fract(t * s * 0.28);
  float cover = 0.5 + 0.5 * cos(ph * TAU);
  vec2 q = vec2(p.x * 1.8 - t * s * 0.12, p.y * 2.4);
  float n = fbm(q) * 0.65 + fbm(q * 2.3 + 4.0) * 0.35;
  float n2 = fbm(q + vec2(0.06, 0.09)) * 0.65 + fbm((q + vec2(0.06, 0.09)) * 2.3 + 4.0) * 0.35;
  float dens = smoothstep(0.38, 0.62, n + (cover - 0.5) * 0.8 + 0.12 * length(p) * (1.0 - cover)) * 0.82;
  float light = sat(0.6 + (n - n2) * 5.0);
  vec3 cloud = mix(vec3(0.6, 0.64, 0.72), vec3(1.0, 1.0, 0.98), light);
  c = mix(base, cloud, dens);
#elif MODE == 40
  // Grim Neo: intensity. A fogged white-out gives way to a letterboxed, zoomed, grey world; the
  // middle keeps its colour inside a glowing neon square that grows, and neon lines trace the bars.
  float I = P1; float cyc = fract(t * 0.33);
  vec3 img = src((uv - 0.5) / 1.35 + 0.5).rgb;
  vec3 g = mix(img, vec3(luma(img)) * 0.9, smoothstep(0.1, 0.45, cyc));
  float size = mix(0.1, 0.25, easeOut(sat((cyc - 0.1) / 0.7)));
  float d = sdBox(p, vec2(size));
  vec3 outc = d < 0.0 ? img * 1.05 : g;
  outc += vec3(1.0) * (exp(-abs(d) * 260.0) * 1.4 + exp(-abs(d) * 35.0) * 0.35) * smoothstep(0.1, 0.3, cyc);
  float yb = abs(uv.y - 0.5) - 0.3;
  if (yb > 0.0) outc = vec3(0.0);
  outc += vec3(1.0) * exp(-abs(yb) * 320.0) * 0.9 + vec3(0.8, 0.9, 1.0) * exp(-abs(yb) * 40.0) * 0.25;
  float wo = 1.0 - smoothstep(0.0, 0.15, cyc);
  outc = mix(outc, scr(zoomBlur(uv, vec2(0.5), 0.3), vec3(0.75)) * (1.0 - 0.7 * dot(p, p)), wo);
  c = mix(base, outc, I);
#elif MODE == 41
  // Chaotic Heat: speed, intensity. Thermal-camera chaos: every hit flips between heat maps, gold
  // washes, near-black frames with an ember and streaking light trails.
  float s = spd(P1); float I = P2; float k = floor(t * s * 5.0); float h = hash1(k);
  vec2 j = (hash2(vec2(k, 2.0)) - 0.5) * 0.06; float z = 1.0 + 0.15 * hash1(k + 3.0);
  vec2 q = (uv - 0.5) / z + 0.5 + j;
  vec3 img = blur9(q, 0.006);
  float l = luma(img);
  vec3 heat = thermal(l * 1.2);
  vec3 outc = heat;
  if (h < 0.3) outc = heat;
  else if (h < 0.5) outc = mix(heat, vec3(1.0, 0.92, 0.0), 0.55);
  else if (h < 0.68) outc = heat * 0.15 + vec3(1.0, 0.6, 0.1) * exp(-length(p - (hash2(vec2(k, 8.0)) - 0.5) * 0.6) * 18.0);
  else outc = mix(img * vec3(1.3, 0.55, 0.25), heat, 0.5);
  vec2 sd = normalize(vec2(1.0, -0.65 + hash1(k + 4.0) * 0.4));
  float dl = dot(p, vec2(-sd.y, sd.x)) - (hash1(k + 5.0) - 0.5) * 0.8;
  float streak = step(0.45, hash1(k + 6.0));
  outc += (vec3(1.0, 0.85, 0.2) * exp(-abs(dl) * 70.0) + vec3(1.0, 0.15, 0.05) * exp(-abs(dl + 0.07) * 55.0) + vec3(1.0) * exp(-abs(dl - 0.05) * 150.0)) * streak;
  c = mix(base, outc, I);
#elif MODE == 42
  // Delicate Rays: glow, range, size, soft light, sharpen, atmosphere. Highlights stretch into fine
  // horizontal light streaks, like an anamorphic lens.
  vec3 sh = sharpen(uv, P5 * 1.5);
  vec3 st = vec3(0.0);
  float reach = mix(0.012, 0.045, P3) * mix(0.6, 1.4, P2);
  for (int i = 1; i <= 8; i++) {
    float fi = float(i); float o = fi * reach;
    vec3 a = src(uv + vec2(o, 0.0)).rgb; vec3 b = src(uv - vec2(o, 0.0)).rgb;
    st += (a * smoothstep(0.62, 0.9, luma(a)) + b * smoothstep(0.62, 0.9, luma(b))) * (1.0 - fi / 9.0);
  }
  st = mix(vec3(luma(st)), st, 0.6);
  c = scr(sh, st * (0.18 + 0.55 * P1) * vec3(0.95, 0.97, 1.0));
  vec3 bloom = bigBlur(uv, 0.03);
  c = mix(c, softLight(c, bloom), P4 * 0.5);
  c *= 1.0 - 0.35 * P6 * sat(dot(p, p) * 1.5);
#elif MODE == 43
  // Shine Zoom: speed. Over a dim, zoomed copy of the shot, the picture turns in as a small card,
  // burns brighter until it glows white with light rays pouring off it, then bursts into a starburst
  // and fills the frame.
  float s = spd(P1); float ph = fract(t * s * 0.42);
  float rv = 1.0 - 0.9 * dot(p, p) * 1.5;
  vec3 bgc = src((uv - 0.5) / 2.0 + 0.5).rgb * 0.3 * rv;
  float rotIn = 1.0 - easeOut(sat(ph / 0.25));
  mat3 m = rotY(0.9 * rotIn) * rotZ(-0.2 * rotIn);
  vec4 q = card2(uv, m, vec3(0.0, 0.0, 2.2), mix(0.18, 0.3, ph));
  float white = smoothstep(0.2, 0.62, ph);
  vec2 e2 = abs(q.xy - 0.5) - 0.5; float dOut = max(max(e2.x, e2.y), 0.0);
  float a = atan(p.y, p.x); float r = length(p);
  float rays = pow(sat(0.5 + 0.5 * sin(a * 22.0 + sin(a * 5.0) * 2.0)), 5.0) * exp(-r * 2.2);
  c = bgc + vec3(1.0, 0.98, 0.95) * (exp(-dOut * 7.0) * (0.25 + white) * 0.7 + rays * white * 0.6) * step(0.0001, q.w);
  if (q.z > 0.5) c = mix(src(q.xy).rgb, vec3(1.0), white * 0.92);
  float burst = exp(-abs(ph - 0.8) * 22.0);
  float star = pow(sat(0.5 + 0.5 * sin(a * 14.0)), 8.0) * exp(-r * 1.6);
  c = mix(c, base, smoothstep(0.8, 0.86, ph));
  c += (hsv(fract(a / TAU + 0.1), 0.55, 1.0) * star * 1.6 + vec3(1.0) * exp(-r * 5.0) * 1.2) * burst;
#elif MODE == 44
  // Magical Tome: intensity. The clip turns into a page of an enchanted book: warm gold grade, a soft
  // spotlight, the page's gilded edge glowing round the frame, golden light rising from the bottom and
  // sparkles drifting up through the picture.
  float I = P1;
  vec3 img = base;
  float l = luma(img);
  vec3 gold = vec3(1.0, 0.78, 0.36);
  vec3 graded = mix(img, img * vec3(1.12, 0.98, 0.78) + vec3(0.05, 0.03, 0.0), I);
  float spot = exp(-dot(p * vec2(1.1, 0.9), p * vec2(1.1, 0.9)) * 2.2);
  graded *= mix(1.0, 0.55 + 0.6 * spot, I);
  // Gilded page edge.
  vec2 e = abs(uv - 0.5) - vec2(0.44, 0.45);
  float edgeD = max(e.x, e.y);
  float rim = exp(-abs(edgeD) * 140.0) * (0.75 + 0.25 * sin(t * 3.0 + uv.x * 12.0 + uv.y * 8.0));
  graded = mix(graded, graded * 0.35, smoothstep(-0.005, 0.02, edgeD) * I);
  graded += gold * rim * 0.9 * I;
  // Light pouring up from below and rising sparkles.
  vec2 o = asp(vec2(0.5, -0.05)); vec2 d = p - o; float ang = atan(d.y, d.x);
  float rays = pow(0.5 + 0.5 * sin(ang * 14.0 + t * 0.9), 3.0) * exp(-length(d) * 2.2);
  graded = scr(graded, gold * (rays * 0.5 + exp(-length(d) * 3.2) * 0.45) * I);
  vec4 sp = particles(uv, t, 16.0, vec2(0.0, -0.22), 0.08, 1.0, 5.0, 0.12, 0.03);
  vec4 dust = particles(uv + 0.37, t * 0.6, 34.0, vec2(0.01, -0.12), 0.05, 0.0, 9.0, 0.12, 0.02);
  graded += gold * (sp.a * 1.3 + dust.a * 0.6) * I;
  c = graded;
#elif MODE == 45
  // 3D Phone Cube: speed. A phone lies on a glowing turntable showing the picture; a glowing white
  // cube hovers above it, then lights up with the picture on every face as it turns.
  float s = spd(P1); float cyc = fract(t * s * 0.3);
  c = bigBlur(uv, 0.06) * 0.42 * vec3(0.9, 0.95, 1.05) + vec3(0.02, 0.025, 0.04);
  vec3 ro = vec3(0.0, 0.55, -3.1); vec3 rd = normalize(vec3(p * 1.05, 1.6) + vec3(0.0, -0.16, 0.0));
  float ang = t * s * 0.8;
  float ringY = -0.75; float tr = (ringY - ro.y) / rd.y;
  if (tr > 0.0) {
    vec3 hp = ro + rd * tr; float rr = length(hp.xz);
    c += vec3(0.03, 0.035, 0.045) * step(rr, 1.35) + vec3(0.02) * (1.0 - smoothstep(1.2, 1.35, rr));
    c += vec3(0.55, 0.75, 1.0) * (exp(-abs(rr - 1.35) * 30.0) * 0.8 + exp(-abs(rr - 1.0) * 60.0) * 0.25);
    vec2 ph2 = rot(ang * 0.35 + 0.4) * hp.xz;
    float bd = sdRB(ph2, vec2(0.42, 0.85), 0.1);
    float sd2 = sdRB(ph2, vec2(0.38, 0.8), 0.08);
    if (bd < 0.0) {
      c = vec3(0.05);
      if (sd2 < 0.0) c = src(vec2(ph2.x / 0.76 + 0.5, ph2.y / 1.6 + 0.5)).rgb * 0.9;
      c += vec3(0.9) * exp(-abs(bd) * 200.0) * 0.4;
    }
  }
  mat3 m = rotY(ang) * rotX(0.5);
  vec3 lro = m * (ro - vec3(0.0, 0.35, 0.0)); vec3 lrd = m * rd;
  vec3 inv = 1.0 / lrd; vec3 b = vec3(0.5);
  vec3 t0 = (-b - lro) * inv; vec3 t1 = (b - lro) * inv;
  vec3 tmin = min(t0, t1); vec3 tmax = max(t0, t1);
  float tn = max(max(tmin.x, tmin.y), tmin.z); float tf = min(min(tmax.x, tmax.y), tmax.z);
  float lit = 1.0;
  vec2 sp2 = p - vec2(0.0, 0.12);
  c += vec3(0.9, 0.95, 1.0) * exp(-length(sp2) * 5.0) * (1.0 - lit) * 0.5;
  if (tn < tf && tn > 0.0) {
    vec3 h = lro + lrd * tn; vec3 n = step(vec3(0.5 - 1e-3), abs(h)) * sign(h);
    vec2 fuv = abs(n.x) > 0.5 ? h.zy : abs(n.y) > 0.5 ? h.xz : h.xy;
    float light = 0.65 + 0.35 * abs(dot(n, normalize(vec3(0.4, 0.8, -0.5))));
    vec3 img = src(fuv + 0.5).rgb * light;
    c = mix(vec3(0.97, 0.98, 1.0) * light, img, lit);
    vec2 ee = abs(fuv) - 0.49; c += vec3(1.0) * exp(-abs(max(ee.x, ee.y)) * 90.0) * 0.5;
  }
#elif MODE == 46
  // Grim Reaper: intensity. The clip drains to a cold, shadowed grade while a smoky reaper silhouette
  // rises at the side, a glowing scythe arc slices across the picture and teal soul-wisps swirl.
  float I = P1; float cyc = fract(t * 0.32);
  vec3 cold = mix(base, vec3(luma(base)) * vec3(0.72, 0.86, 0.9), 0.75 * I);
  cold *= 1.0 - 0.55 * I * sat(dot(p, p) * 1.6);
  // Smoky silhouette on the right third, semi-transparent so the clip shows through.
  float grow = ease(sat(cyc / 0.5));
  vec2 q = (p - vec2(0.32 * uAspect, -0.08)) / (0.55 + 0.25 * grow);
  float hem = (fbm(vec2(q.x * 9.0, t * 0.7)) - 0.5) * 0.1;
  float wid = 0.04 + 0.26 * sat((0.12 - q.y) / 0.6);
  float cloak = max(abs(q.x + 0.03 * sin(q.y * 8.0 + t * 2.0)) - wid, max(q.y - 0.14, -0.55 - q.y + hem));
  float hood = length((q - vec2(0.0, 0.17)) * vec2(1.25, 1.0)) - 0.085;
  float body = min(cloak, hood);
  float smoke = fbm(q * 6.0 + vec2(0.0, -t * 0.6));
  float shadow = (1.0 - smoothstep(-0.02, 0.05, body + (smoke - 0.5) * 0.06)) * grow;
  cold = mix(cold, vec3(0.01, 0.015, 0.02), shadow * 0.82 * I);
  cold += vec3(0.4, 1.0, 0.92) * exp(-abs(body) * 120.0) * 0.45 * grow * I;
  // Scythe slash across the clip.
  float sw = fract(t * 0.55);
  vec2 sp2 = p - vec2(-0.05 * uAspect, 0.05);
  float arcR = 0.42; float ac = abs(length(sp2) - arcR);
  float aa = atan(sp2.y, sp2.x);
  float head = mix(3.4, -0.4, ease(sat(sw / 0.35)));
  float trail = smoothstep(head, head + 1.2, aa) * (1.0 - smoothstep(head + 1.2, head + 2.6, aa));
  float slash = exp(-ac * 120.0) * trail * (1.0 - smoothstep(0.35, 0.6, sw));
  cold += vec3(0.75, 1.0, 1.0) * slash * 1.6 * I;
  // Soul wisps.
  float e = 0.0;
  for (int i = 0; i < 3; i++) {
    float fi = float(i);
    vec2 r2 = rot(t * (1.0 + fi * 0.35) + fi * 2.1) * p;
    float wv = r2.y - 0.07 * sin(r2.x * 9.0 + t * 4.0 + fi) - (fi - 1.0) * 0.14;
    e += exp(-abs(wv) * 80.0) * smoothstep(0.6, 0.15, abs(r2.x)) * (0.5 + 0.5 * sin(r2.x * 20.0 - t * 8.0));
  }
  c = cold + vec3(0.45, 1.0, 0.92) * e * 0.55 * I;
#elif MODE == 47
  // Phone Zoom: speed. Two hands hold a phone up to the scene showing the picture, then the camera
  // pushes into the screen until it fills the frame.
  float s = spd(P1); float ph = fract(t * s * 0.35);
  float z = ease(sat((ph - 0.35) / 0.45));
  float sc = mix(0.6, 1.12, z);
  vec2 ctr = vec2(0.0, mix(0.06, 0.0, z));
  vec2 bh = vec2(0.5 * uAspect, 0.5) * sc;
  vec2 pp = p - ctr;
  c = bigBlur(uv, 0.05) * 0.55;
  float body = sdRB(pp, bh + 0.02, 0.07 * sc);
  if (body < 0.0) c = vec3(0.03);
  c += vec3(0.6) * exp(-abs(body) * 300.0) * 0.5;
  float scr2 = sdRB(pp, bh, 0.055 * sc);
  if (scr2 < 0.0) c = src(unasp(pp / sc)).rgb;
  float isl = sdRB(pp - vec2(0.0, bh.y - 0.05 * sc), vec2(0.08, 0.02) * sc, 0.02 * sc);
  if (isl < 0.0 && scr2 < 0.0) c = vec3(0.0);
  // Hands: wrists rise from the bottom of the frame to grip the phone's lower corners; each part is a
  // shaded capsule so it reads round, with the thumbs resting on the front bezel.
  vec3 skin = vec3(0.82, 0.58, 0.44);
  for (int i = 0; i < 2; i++) {
    float sd = i == 0 ? -1.0 : 1.0;
    vec2 hq = vec2(pp.x * sd, pp.y);
    vec2 w0 = vec2(bh.x * 1.3, -0.62); vec2 w1 = vec2(bh.x * 0.98, -bh.y * 0.78);
    float wr = mix(0.1, 0.075, sat((hq.y - w0.y) / (w1.y - w0.y))) * (0.8 + 0.2 * sc);
    float palm = sdSeg(hq, w0, w1) - wr;
    vec2 t0 = vec2(bh.x * 0.96, -bh.y * 0.74); vec2 t1 = vec2(bh.x * 0.68, -bh.y * 0.5);
    float tr = 0.033 * sc + 0.006;
    float thumb = sdSeg(hq, t0, t1) - tr;
    float fing = 1.0;
    for (int f = 0; f < 3; f++) {
      float fy = -bh.y * 0.42 + float(f) * bh.y * 0.2;
      fing = min(fing, sdSeg(hq, vec2(bh.x * 1.1, fy - 0.03 * sc), vec2(bh.x * 1.0, fy)) - 0.026 * sc);
    }
    float hand = min(min(palm, thumb), fing);
    // Rounded shading from how deep inside each capsule the pixel is.
    float roundP = sat(-palm / wr); float roundT = sat(-thumb / tr); float roundF = sat(-fing / (0.026 * sc));
    float rnd = max(max(sqrt(roundP), sqrt(roundT)), sqrt(roundF));
    vec3 sh = skin * (0.5 + 0.55 * rnd) * (0.92 + 0.12 * fbm(hq * 40.0));
    sh = mix(sh, skin * 0.55, (1.0 - smoothstep(0.0, 0.012, abs(thumb - palm) * 0.5 + max(thumb, palm) * 0.0)) * step(thumb, 0.0) * 0.0);
    float a = 1.0 - smoothstep(-0.003, 0.003, hand);
    c = mix(c, c * 0.55, (1.0 - smoothstep(0.0, 0.035, hand)) * (1.0 - a) * (1.0 - z) * 0.7);
    c = mix(c, sh, a * (1.0 - z));
    // Thumb drawn over the palm edge with a crease so the two read as separate.
    c = mix(c, skin * 0.45, exp(-abs(thumb) * 500.0) * step(palm, 0.0) * (1.0 - z) * 0.8);
  }
#elif MODE == 48
  // Ink Spill: speed, atmosphere. Black ink floods across the frame, white smoke billows out of it
  // and clears to a grey picture, then the colour comes back.
  float s = spd(P1); float ph = fract(t * s * 0.3);
  float n = fbm(p * 3.0 + vec2(t * 0.3, 0.0)); float n2 = fbm(p * 6.0 - vec2(0.0, t * 0.5));
  float front = mix(1.3, -0.72, ease(sat(ph / 0.4)));
  float ink = smoothstep(-0.04, 0.04, p.x - front + (n - 0.5) * 0.7 + (n2 - 0.5) * 0.25);
  float inkA = 1.0 - smoothstep(0.42, 0.56, ph);
  float cover = 1.0 - sat((ph - 0.4) / 0.36);
  float smoke = smoothstep(0.35, 0.7, n + (n2 - 0.5) * 0.4 + cover * 0.9 - 0.45) * step(0.4, ph) * sat(cover * 1.5);
  float sh = sat(0.55 + (n - fbm(p * 3.0 + vec2(t * 0.3, 0.0) + 0.07)) * 5.0);
  vec3 img = mix(vec3(luma(base)), base, smoothstep(0.7, 0.9, ph) + step(ph, 0.4));
  c = mix(img, mix(vec3(0.55, 0.57, 0.6), vec3(0.97), sh), sat(smoke * 1.4) * (0.4 + 0.6 * P2));
  c = mix(c, vec3(0.0), ink * inkA);
#else
  c = base;
#endif
  return vec4(c, 1.0);
}
"""
