package com.pinwheel.core.media.video

/*
 * 3D effect families. Each is one fx() whose look is picked by #define MODE; P1 Intensity,
 * P2 Speed, P3 Size, P4 Background (0 = dark studio, 1 = blurred video) unless noted.
 */

/** A floating video card: swing, nod, orbit, flip, spin, hover, floor display, phone and hologram. */
internal const val FX_3D_CARD = """
vec3 studio(vec2 uv, float T) {
  vec3 d = bg(uv) + vec3(0.18, 0.14, 0.32) * exp(-length(asp(uv) - vec2(0.0, 0.1)) * 2.4) * 0.6;
  return mix(d, blurredBg(uv, 0.45), step(0.5, P4));
}
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2); float a = 0.3 + P1 * 0.9;
  vec3 r = vec3(0.0); float s = mix(0.92, 0.62, P3);
  #if MODE == 1
    r.y = sin(T * 1.6) * 0.75 * a;
  #elif MODE == 2
    r.x = sin(T * 1.4) * 0.6 * a;
  #elif MODE == 3
    r = vec3(sin(T) * 0.45, cos(T * 0.8) * 0.6, sin(T * 0.4) * 0.08) * a;
  #elif MODE == 4
    r.y = T * 1.6;
  #elif MODE == 5
    r = vec3(0.3 * a, 0.0, T * 0.9);
  #elif MODE == 6
    r = vec3(0.22 + sin(T) * 0.12, sin(T * 0.7) * 0.4, sin(T * 0.5) * 0.06) * a; s *= 0.85;
  #elif MODE == 7
    r = vec3(-0.95 * a, sin(T * 0.6) * 0.25, 0.0); s *= 0.8;
  #elif MODE == 8
    r = vec3(0.12 * sin(T * 0.9), T * 0.8, 0.0); s *= 0.72;
  #elif MODE == 9
    r = vec3(0.2, sin(T * 0.8) * 0.9 * a, 0.0); s *= 0.7;
  #endif
  vec3 q = card(uv, r, 2.2, s);
  vec3 col = studio(uv, T);
  #if MODE == 7 || MODE == 8 || MODE == 9
    // Glossy floor: the card mirrored below itself.
    vec3 fq = card(vec2(uv.x, 1.0 - uv.y - 0.08), r, 2.2, s);
    if (fq.z > 0.5 && q.z < 0.5) col = mix(col, src(vec2(fq.x, 1.0 - fq.y)).rgb * 0.35, 0.5 * sat(fq.y * 2.0));
  #endif
  if (q.z > 0.5) {
    vec2 cq = q.xy; vec3 v;
    #if MODE == 8
      // Phone: dark bezel with a notch around the playing video.
      vec2 inner = (cq - 0.5) / vec2(0.9, 0.95) + 0.5;
      v = inside(inner) > 0.5 ? src(inner).rgb : vec3(0.05);
      v *= cardMask(cq, 0.08);
    #elif MODE == 9
      // Hologram: cyan light, scanlines and flicker.
      v = src(cq).rgb; float l = luma(v);
      v = vec3(0.2, 0.9, 1.0) * (l * 1.3 + 0.1) * (0.8 + 0.2 * sin(cq.y * uSize.y * 0.8 + T * 20.0));
      v *= 0.85 + 0.15 * step(0.1, hash(vec2(floor(T * 12.0), 3.0)));
    #else
      v = src(cq).rgb * cardMask(cq, 0.03);
    #endif
    float face = abs(cos(r.y) * cos(r.x));
    v *= 0.62 + 0.38 * face;
    v += vec3(1.0) * pow(sat(1.0 - abs(cq.x + cq.y - 1.0 - sin(T * 0.8) * 0.9) * 3.0), 5.0) * 0.22;
    #if MODE == 9
      col = col * 0.5 + v;
    #else
      col = mix(col, v, cardMask(cq, 0.03));
    #endif
  }
  #if MODE == 9
    vec2 g = asp(uv) * 8.0; float grid = max(smoothstep(0.96, 1.0, abs(sin(g.x * PI))), smoothstep(0.96, 1.0, abs(sin((g.y + T) * PI))));
    col += vec3(0.1, 0.6, 0.8) * grid * 0.25 * step(uv.y, 0.35);
  #endif
  return vec4(col, 1.0);
}
"""

/** Video wrapped on a spinning cube, a tumbling cube, a dice roll, or a wall of cubes. */
internal const val FX_3D_CUBE = """
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2);
  vec2 cellUv = uv; float phase = 0.0; float zoom = 1.0;
  #if MODE == 4
    vec2 g = vec2(3.0, 3.0); vec2 id = floor(uv * g); cellUv = fract(uv * g); phase = hash(id) * 6.0 + (id.x + id.y) * 0.4; zoom = 0.78;
  #endif
  vec2 p = (cellUv - 0.5) * vec2(uAspect, 1.0) * 2.0;
  vec3 ro = vec3(0.0, 0.0, -3.6 * mix(1.0, 1.5, P3) * zoom);
  vec3 rd = normalize(vec3(p, 2.4));
  float ay; float ax;
  #if MODE == 1
    ay = T * 0.9; ax = 0.35 + sin(T * 0.5) * 0.12 * P1;
  #elif MODE == 2
    ay = T * 0.8; ax = T * 0.55;
  #elif MODE == 3
    float k = floor(T * 0.6); float f = ease(fract(T * 0.6) * 1.6);
    ay = (k + f) * PI * 0.5; ax = 0.4 + (k + f) * PI * 0.5 * 0.5;
  #else
    ay = T * 0.8 + phase; ax = 0.4 + sin(T + phase) * 0.3;
  #endif
  mat3 m = rotY(-ay) * rotX(-ax);
  vec3 o = m * ro; vec3 d = m * rd;
  vec3 b = vec3(uAspect * 0.62, 0.62, uAspect * 0.62);
  vec3 inv = 1.0 / d; vec3 n = inv * o; vec3 k3 = abs(inv) * b;
  vec3 t1 = -n - k3; vec3 t2 = -n + k3;
  float tN = max(max(t1.x, t1.y), t1.z); float tF = min(min(t2.x, t2.y), t2.z);
  vec3 col = mix(bg(uv) + vec3(0.1, 0.12, 0.25) * exp(-length(asp(uv)) * 2.0), blurredBg(uv, 0.4), step(0.5, P4));
  if (tN < tF && tF > 0.0) {
    vec3 pos = o + d * tN;
    vec3 nor = -sign(d) * step(t1.yzx, t1.xyz) * step(t1.zxy, t1.xyz);
    vec2 f;
    if (abs(nor.z) > 0.5) f = vec2(pos.x / b.x * -nor.z, pos.y / b.y);
    else if (abs(nor.x) > 0.5) f = vec2(pos.z / b.z * nor.x, pos.y / b.y);
    else f = vec2(pos.x / b.x, pos.z / b.z * nor.y);
    f = f * 0.5 + 0.5;
    vec3 light = normalize(m * vec3(-0.4, 0.6, -1.0));
    float diff = 0.45 + 0.55 * sat(dot(nor, light));
    vec3 v = src(f).rgb * diff;
    float edge = max(abs(f.x - 0.5), abs(f.y - 0.5));
    v += vec3(1.0) * smoothstep(0.47, 0.5, edge) * 0.35 * P1;
    col = v;
  }
  return vec4(col, 1.0);
}
"""

/** Spheres: a spinning video globe, a crystal ball that refracts, a floating bubble lens. */
internal const val FX_3D_SPHERE = """
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2);
  vec2 p = asp(uv) * 2.0;
  float R = mix(0.95, 0.55, P3);
  #if MODE == 3
    vec2 c = vec2(sin(T * 0.7) * 0.5 * uAspect, cos(T * 0.9) * 0.35);
  #else
    vec2 c = vec2(0.0);
  #endif
  vec2 d = p - c; float r2 = dot(d, d);
  vec3 col;
  #if MODE == 1
    col = bg(uv);
    vec2 sp = asp(uv) * 30.0; col += vec3(1.0) * step(0.985, hash(floor(sp))) * (0.5 + 0.5 * sin(T * 3.0 + hash(floor(sp)) * 20.0));
  #else
    col = src(uv).rgb;
  #endif
  if (r2 < R * R) {
    float z = sqrt(R * R - r2);
    vec3 nor = normalize(vec3(d, -z));
    #if MODE == 1
      vec3 q = rotY(T * 0.6) * rotX(0.3) * nor;
      vec2 f = vec2(atan(q.x, q.z) / TAU + 0.5, acos(-q.y) / PI);
      vec3 v = srcm(vec2(f.x * 2.0, f.y));
      float diff = 0.35 + 0.65 * sat(dot(nor, normalize(vec3(-0.5, 0.5, -1.0))));
      col = v * diff;
    #else
      vec2 centreUv = unasp(c * 0.5); vec2 local = uv - centreUv; float edge = 1.0 - z / R;
      #if MODE == 2
        // Crystal ball: the scene seen upside down through the glass.
        vec3 v = src(centreUv - local * (0.75 + 0.6 * edge * P1)).rgb;
      #else
        // Bubble: a magnifier that bulges the picture under it.
        vec3 v = src(centreUv + local * (0.45 + 0.5 * edge * edge) * (1.0 - 0.25 * P1)).rgb;
        v += hsv(fract(edge * 1.5 + T * 0.1), 0.5, 1.0) * edge * edge * 0.35;
      #endif
      col = mix(col, v, 0.95);
    #endif
    float rim = pow(1.0 - sat(z / R), 3.0);
    col += vec3(0.6, 0.8, 1.0) * rim * 0.6;
    col += vec3(1.0) * pow(sat(dot(nor, normalize(vec3(-0.4, 0.5, -1.0)))), 40.0) * 0.8;
  } else {
    float halo = exp(-(sqrt(r2) - R) * 8.0);
    col += vec3(0.3, 0.5, 1.0) * halo * 0.25;
  }
  return vec4(col, 1.0);
}
"""

/** Tunnels: round, square room and spiral. */
internal const val FX_3D_TUNNEL = """
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2);
  vec2 p = asp(uv); float a = atan(p.y, p.x);
  #if MODE == 2
    float r = max(abs(p.x), abs(p.y));
  #else
    float r = length(p);
  #endif
  float reps = mix(1.0, 3.0, P3);
  #if MODE == 3
    a += r * 3.0 * P1 + T * 0.3;
  #endif
  vec2 t = vec2(a / TAU * reps, 0.25 / max(r, 0.01) + T * 0.35);
  vec3 col = srcm(t);
  col *= smoothstep(0.0, 0.35, r) * (0.55 + 0.45 * P1);
  col += vec3(0.4, 0.6, 1.0) * exp(-r * 12.0) * 0.6;
  // Keep the real frame at the far end of the tunnel.
  vec2 center = uv + (uv - 0.5) * 2.2;
  float m = inside((uv - 0.5) * 4.5 + 0.5);
  col = mix(col, src((uv - 0.5) * 4.5 + 0.5).rgb, m * (1.0 - P1 * 0.5));
  return vec4(col, 1.0);
}
"""

/** Hyperspace: stars streaking past with the video flying through. MODE 2 is stars only. */
internal const val FX_3D_WARP = """
vec3 streaks(vec2 uv, float T) {
  vec2 p = asp(uv); float r = length(p); float a = atan(p.y, p.x);
  vec3 c = vec3(0.0);
  for (int i = 0; i < 3; i++) {
    float fi = float(i); float N = 90.0 + fi * 40.0;
    float s = a / TAU * N + fi * 0.37;
    float id = floor(s); float h = hash1(id + fi * 31.0);
    float z = fract(h + T * (0.25 + 0.15 * fi) * spd(P2));
    float rr = z * z * 1.6; float len = 0.01 + z * z * 0.35 * (0.4 + P1);
    float across = abs(fract(s) - 0.5) / N * TAU * r;
    float along = sat(1.0 - abs(r - rr) / len) * step(r, rr + len * 0.2);
    c += vec3(0.8, 0.9, 1.0) * along * exp(-across * across * 3e5) * (0.5 + h);
  }
  return c;
}
vec4 fx(vec2 uv) {
  float T = uTime;
  vec3 col = vec3(0.01, 0.01, 0.03) + streaks(uv, T);
  #if MODE == 1
    float s = mix(0.7, 0.45, P3) * (1.0 + 0.08 * sin(T * 2.0));
    vec3 q = card(uv, vec3(sin(T * 0.7) * 0.15, sin(T * 0.5) * 0.3, 0.0), 2.2, s);
    if (q.z > 0.5) col = mix(col, src(q.xy).rgb, cardMask(q.xy, 0.04));
  #else
    // Star Rush: the clip surges towards the camera in pulses, smeared radially, with light streaks
    // racing past over it and a bright core. The clip stays the subject throughout.
    float beat = fract(T * 0.7 * spd(P2));
    float push = 0.06 + 0.12 * P1 * (1.0 - beat);
    vec2 c0 = vec2(0.5);
    vec3 acc = vec3(0.0);
    for (int i = 0; i < 10; i++) { float f = float(i) / 9.0; acc += src(c0 + (uv - c0) / (1.0 + push * (0.4 + f))).rgb; }
    acc /= 10.0;
    vec3 sharp = src(c0 + (uv - c0) / (1.0 + push * 0.4)).rgb;
    float r = length(asp(uv));
    vec3 pic = mix(sharp, acc, smoothstep(0.05, 0.45, r));
    pic *= 1.0 - 0.35 * smoothstep(0.35, 0.8, r);
    col = pic + streaks(uv, T) * (0.6 + 0.6 * P1) + vec3(0.75, 0.85, 1.0) * exp(-r * 9.0) * 0.25 * (1.0 - beat);
  #endif
  return vec4(col, 1.0);
}
"""

/** Shattered mirror shards that drift apart and back. */
internal const val FX_3D_SHATTER = """
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2);
  float N = mix(3.5, 9.0, P3);
  vec2 p = asp(uv) * N; vec2 i = floor(p); vec2 f = fract(p);
  float d1 = 8.0; float d2 = 8.0; vec2 best = vec2(0.0); vec2 bestPt = vec2(0.0);
  for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
    vec2 g = vec2(float(x), float(y)); vec2 o = hash2(i + g);
    vec2 dv = g + o - f; float d = dot(dv, dv);
    if (d < d1) { d2 = d1; d1 = d; best = i + g; bestPt = i + g + o; } else if (d < d2) d2 = d;
  }
  float h = hash(best);
  #if MODE == 2
    float k = P1 * ease(sat(uProgress * 1.4));
  #else
    float k = P1 * (0.5 + 0.5 * sin(T * 1.2 + h * 6.0));
  #endif
  vec2 centre = unasp(bestPt / N);
  vec2 push = normalize(centre - 0.5 + 0.001) * 0.18 * k + (hash2(best + 5.0) - 0.5) * 0.08 * k;
  vec2 local = (uv - centre) * rot((h - 0.5) * 1.4 * k) * (1.0 + 0.25 * k * h) + centre;
  vec3 col = src(local - push).rgb * (0.75 + 0.35 * h);
  float edge = sqrt(d2) - sqrt(d1);
  col = mix(vec3(0.02), col, smoothstep(0.02, 0.06 + 0.04 * k, edge));
  col += vec3(1.0) * smoothstep(0.08, 0.0, edge) * 0.4 * (1.0 - k * 0.5);
  return vec4(col, 1.0);
}
"""

/** Accordion fold (vertical strips) and window blinds (horizontal slats), with 3D shading. */
internal const val FX_3D_FOLD = """
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2);
  float N = floor(mix(4.0, 14.0, P3));
  #if MODE == 2
    float x = uv.y; float other = uv.x;
  #else
    float x = uv.x; float other = uv.y;
  #endif
  float amt = P1 * (0.5 + 0.5 * sin(T * 1.3));
  float strip = floor(x * N); float s = fract(x * N);
  float side = mod(strip, 2.0);
  float slope = side < 0.5 ? s : 1.0 - s;
  float squeeze = 1.0 - amt * 0.35;
  float xx = (x - 0.5) / squeeze + 0.5;
  float persp = 1.0 + (slope - 0.5) * amt * 0.3;
  float yy = (other - 0.5) * persp + 0.5;
  #if MODE == 2
    vec2 q = vec2(yy, xx);
  #else
    vec2 q = vec2(xx, yy);
  #endif
  vec3 col = inside(q) > 0.5 ? src(q).rgb : vec3(0.03);
  col *= mix(1.0, 0.45 + 0.75 * slope, amt);
  return vec4(col, 1.0);
}
"""

/** Tiles flip over in a wave across the frame. */
internal const val FX_3D_FLIPTILES = """
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2);
  vec2 g = vec2(MODE == 1 ? 3.0 : 6.0) * vec2(1.0, 1.0 / max(uAspect, 0.3));
  g = floor(mix(g, g * 1.6, P3)) + 1.0;
  vec2 id = floor(uv * g); vec2 f = fract(uv * g);
  float ph = fract(T * 0.25 - (id.x + id.y) * 0.06);
  float ang = ease(sat(ph * 3.0)) * PI;
  float c = cos(ang);
  float x = (f.x - 0.5) / max(abs(c), 0.001) + 0.5;
  float persp = 1.0 + (f.x - 0.5) * sin(ang) * 0.6 * P1;
  float y = (f.y - 0.5) * persp + 0.5;
  vec3 col = vec3(0.02);
  if (x >= 0.0 && x <= 1.0 && y >= 0.0 && y <= 1.0) {
    vec2 q = (id + vec2(c < 0.0 ? 1.0 - x : x, y)) / g;
    col = src(q).rgb * (0.55 + 0.45 * abs(c));
    if (c < 0.0) col = mix(col, vec3(luma(col)) * vec3(0.7, 0.85, 1.1), 0.6);
  }
  return vec4(col, 1.0);
}
"""

/** Everything else 3D: doors, depth parallax, vertigo, carousel, card stack, cloth, kaleido,
 * tiny planet, prism, bounce, slab slices, infinite zoom, cylinder, neon grid floor. */
internal const val FX_3D_MISC = """
vec4 fx(vec2 uv) {
  float T = uTime * spd(P2);
  vec3 col = vec3(0.0);
  #if MODE == 1
    // Double doors swing open and closed.
    float ang = (0.5 + 0.5 * sin(T * 0.9)) * 1.3 * P1;
    float halfW = 0.5 * cos(ang);
    col = blurredBg(uv, 0.35);
    if (uv.x < halfW) {
      float u = uv.x / halfW; float yy = (uv.y - 0.5) / (1.0 + u * sin(ang) * 0.35) + 0.5;
      if (yy > 0.0 && yy < 1.0) col = src(vec2(u * 0.5, yy)).rgb * (1.0 - 0.45 * sin(ang) * u);
    } else if (uv.x > 1.0 - halfW) {
      float u = (1.0 - uv.x) / halfW; float yy = (uv.y - 0.5) / (1.0 + u * sin(ang) * 0.35) + 0.5;
      if (yy > 0.0 && yy < 1.0) col = src(vec2(1.0 - u * 0.5, yy)).rgb * (1.0 - 0.45 * sin(ang) * u);
    }
  #elif MODE == 2 || MODE == 3
    // Depth parallax: a centre-weighted, luminance-aware depth orbits like a 3D photo.
    float depth = sat(mix(1.0 - length(asp(uv)) * 1.2, luma(blur9(uv, 0.02)), 0.35));
    #if MODE == 2
      vec2 orbit = vec2(sin(T), cos(T * 0.8)) * 0.035 * P1;
    #else
      vec2 orbit = vec2(sin(T * 0.6) * 0.05, 0.0) * P1;
    #endif
    float zoom = 1.0 - 0.06 * P1 * (0.5 + 0.5 * sin(T * 0.5));
    col = src((uv - 0.5) * zoom + 0.5 + orbit * (depth - 0.4)).rgb;
  #elif MODE == 4
    // Vertigo: centre and edges zoom in opposite directions.
    vec2 p = uv - 0.5; float r = length(asp(uv));
    float z = sin(T * 0.8) * 0.25 * P1;
    col = src(p * (1.0 - z * (1.0 - r * 1.4)) + 0.5).rgb;
  #elif MODE == 5
    // Carousel of copies rotating around the viewer.
    col = mix(bg(uv), blurredBg(uv, 0.3), step(0.5, P4)); float bestT = 1e9;
    float R = 1.25 * max(uAspect, 0.8); float s = mix(0.62, 0.45, P3);
    for (int i = 0; i < 6; i++) {
      float th = float(i) / 6.0 * TAU + T * 0.5;
      vec3 c = vec3(sin(th) * R, 0.0, 3.0 + cos(th) * R);
      vec4 q = card2(uv, rotY(th), c, s);
      if (q.z > 0.5 && q.w < bestT) { bestT = q.w; col = src(q.xy).rgb * (0.45 + 0.55 * sat(-cos(th))); }
    }
  #elif MODE == 6
    // Stack of frames receding diagonally.
    col = mix(bg(uv), blurredBg(uv, 0.3), step(0.5, P4));
    for (int k = 3; k >= 0; k--) {
      float fk = float(k);
      vec3 c = vec3(fk * 0.35 * uAspect + sin(T) * 0.05, fk * 0.22, 2.4 + fk * 0.9);
      vec4 q = card2(uv, rotY(0.35 + sin(T * 0.6) * 0.1) * rotX(-0.12), c, mix(0.62, 0.5, P3));
      if (q.z > 0.5) col = src(q.xy).rgb * (1.0 - fk * 0.18) * cardMask(q.xy, 0.03) + col * (1.0 - cardMask(q.xy, 0.03));
    }
  #elif MODE == 7
    // Cloth wave with light and shadow along the folds.
    float w = sin(uv.x * 9.0 + T * 2.0) * 0.5 + sin(uv.y * 7.0 - T * 1.4) * 0.5;
    vec2 q = uv + vec2(0.0, w * 0.02 * P1);
    col = src(q).rgb * (0.85 + 0.25 * P1 * cos(uv.x * 9.0 + T * 2.0));
  #elif MODE == 8
    // Kaleidoscope drilling inward.
    vec2 p = asp(uv) * rot(T * 0.2); float r = length(p); float a = atan(p.y, p.x);
    float seg = TAU / floor(mix(4.0, 10.0, P3)); a = abs(mod(a, seg) - seg * 0.5);
    vec2 q = vec2(cos(a), sin(a)) * r;
    q = q * (1.0 + 0.3 * sin(T)) + vec2(0.0, T * 0.05);
    col = srcm(unasp(q));
  #elif MODE == 9
    // Tiny planet.
    vec2 p = asp(uv) * rot(T * 0.15); float r = length(p); float a = atan(p.y, p.x);
    vec2 q = vec2(a / TAU + 0.5, sat(atan(r * mix(2.5, 5.0, P1)) / (PI * 0.5)));
    col = srcm(q);
  #elif MODE == 10
    // Prism: three angled faces.
    float f = uv.x * 3.0; float id = floor(f); float s = fract(f);
    float ang = (id - 1.0) * 0.5 * P1 * (0.7 + 0.3 * sin(T));
    float x = (s - 0.5) * (1.0 + sin(ang) * 0.3) + 0.5;
    col = src(vec2((id + x) / 3.0 + sin(T) * 0.02 * (id - 1.0), (uv.y - 0.5) * (1.0 + (s - 0.5) * sin(ang) * 0.4) + 0.5)).rgb;
    col *= 0.7 + 0.3 * cos(ang * 2.0 + s);
    col += vec3(0.6, 0.8, 1.0) * smoothstep(0.03, 0.0, min(s, 1.0 - s)) * 0.4;
  #elif MODE == 11
    // Bounce toward the camera with squash and stretch.
    float b = abs(sin(T * 2.2));
    float s = mix(0.8, 1.0, b); vec2 sq = vec2(1.0 + (1.0 - b) * 0.12 * P1, 1.0 - (1.0 - b) * 0.12 * P1);
    vec2 q = (uv - vec2(0.5, 0.5 - (1.0 - s) * 0.4)) / (s * sq) + 0.5;
    col = mix(blurredBg(uv, 0.35), src(q).rgb, inside(q));
  #elif MODE == 12
    // Three slabs rotating like a puzzle cube.
    col = vec3(0.02);
    for (int k = 0; k < 3; k++) {
      float fk = float(k);
      vec3 q = card(uv, vec3(sin(T + fk * 1.3) * 0.6 * P1, 0.0, 0.0), 2.2, 1.0);
      float lo = fk / 3.0; float hi = lo + 1.0 / 3.0;
      vec3 qr = card(uv, vec3(0.0, sin(T * 0.9 + fk * 1.7) * 0.7 * P1, 0.0), 2.2, 0.98);
      if (uv.y >= lo && uv.y < hi && qr.z > 0.5 && qr.y >= lo && qr.y < hi) col = src(qr.xy).rgb * (0.7 + 0.3 * abs(cos(sin(T * 0.9 + fk * 1.7) * 0.7 * P1)));
    }
  #elif MODE == 13
    // Infinite zoom through nested frames.
    vec2 p = uv - 0.5; float z = exp2(fract(T * 0.35));
    p /= z * mix(1.0, 1.4, P3);
    for (int k = 0; k < 6; k++) { if (max(abs(p.x), abs(p.y)) > 0.5) break; p *= 2.0; }
    p *= 0.5;
    col = src(p + 0.5).rgb;
    float border = max(abs(p.x), abs(p.y));
    col = mix(col, vec3(1.0), smoothstep(0.235, 0.25, border) * 0.7 * P1);
  #elif MODE == 14
    // Rotating cylinder with the video wrapped around it.
    col = bg(uv);
    vec2 p = asp(uv) * 2.0; float R = mix(0.95, 0.7, P3) * uAspect;
    if (abs(p.x) < R) {
      float a = asin(p.x / R); float u = (a / PI + 0.5) * 0.5 + T * 0.08;
      col = srcm(vec2(u * 2.0, uv.y * 1.1 - 0.05)) * (0.35 + 0.65 * cos(a));
      col += vec3(1.0) * pow(cos(a - 0.5), 30.0) * 0.3;
    }
  #elif MODE == 15
    // Neon grid floor with the video floating above it.
    col = vec3(0.02, 0.0, 0.05);
    if (uv.y < 0.42) {
      float z = 0.42 / (0.42 - uv.y + 0.02); vec2 g = vec2((uv.x - 0.5) * z * 4.0, z + T * 1.5);
      float line = max(smoothstep(0.93, 1.0, abs(sin(g.x * PI))), smoothstep(0.9, 1.0, abs(sin(g.y * PI))));
      col += vec3(1.0, 0.2, 0.8) * line * sat(1.3 - z * 0.08);
    }
    col += vec3(1.0, 0.4, 0.2) * exp(-abs(uv.y - 0.42) * 30.0) * 0.6;
    vec3 q = card(uv, vec3(0.1 + sin(T) * 0.08, sin(T * 0.6) * 0.35 * P1, 0.0), 2.2, mix(0.62, 0.45, P3));
    if (q.z > 0.5) col = mix(col, src(q.xy).rgb, cardMask(q.xy, 0.03));
  #endif
  return vec4(col, 1.0);
}
"""
