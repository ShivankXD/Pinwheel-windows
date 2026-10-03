package com.pinwheel.render

import com.pinwheel.core.model.VideoClipEdits

internal data class FilterRecipe(
    val id: String, val name: String, val category: String,
    val exposure: Float = 0f, val contrast: Float = 0f, val saturation: Float = 0f, val temperature: Float = 0f,
    val tint: Float = 0f, val fade: Float = 0f, val vibrance: Float = 0f, val curve: Float = 0f,
    val mono: Float = 0f, val hue: Float = 0f, val split: Float = 0f, val bleach: Float = 0f,
    val shadows: Int = 0xFF808080.toInt(), val highlights: Int = 0xFF808080.toInt(),
)

internal object VideoFilterCatalog {
    val categories = listOf("Featured", "Portrait", "Life", "Movies", "Retro", "Style", "B&W")
    private fun f(id: String, name: String, cat: String, block: FilterRecipe.() -> FilterRecipe) = FilterRecipe("flt-$id", name, cat).block()
    val filters: List<FilterRecipe> = listOf(
        f("crisp", "Crisp", "Featured") { copy(contrast = .18f, vibrance = .35f, curve = .25f) },
        f("glow", "Glow", "Featured") { copy(exposure = .12f, contrast = -.1f, fade = .25f, temperature = .15f, vibrance = .2f) },
        f("teal-pop", "Teal Pop", "Featured") { copy(contrast = .15f, split = .9f, shadows = 0xFF2E7D8C.toInt(), highlights = 0xFFE8A060.toInt(), vibrance = .2f) },
        f("golden", "Golden Hour", "Featured") { copy(temperature = .55f, exposure = .05f, vibrance = .3f, split = .5f, highlights = 0xFFFFC070.toInt()) },
        f("moody", "Moody", "Featured") { copy(exposure = -.18f, contrast = .25f, saturation = -.25f, split = .6f, shadows = 0xFF203048.toInt(), curve = .3f) },
        f("pop", "Pop", "Featured") { copy(saturation = .45f, contrast = .2f, curve = .2f) },
        f("film-200", "Film 200", "Featured") { copy(fade = .3f, contrast = .1f, temperature = .15f, split = .5f, shadows = 0xFF365A58.toInt(), saturation = -.1f) },
        f("clean", "Clean", "Featured") { copy(exposure = .08f, contrast = .08f, vibrance = .15f, temperature = -.05f) },

        f("porcelain", "Porcelain", "Portrait") { copy(exposure = .15f, contrast = -.12f, saturation = -.2f, temperature = -.1f, fade = .15f) },
        f("peach", "Peach", "Portrait") { copy(temperature = .25f, tint = .15f, exposure = .08f, fade = .15f, split = .4f, highlights = 0xFFFFB090.toInt()) },
        f("warm-skin", "Warm Skin", "Portrait") { copy(temperature = .3f, vibrance = .2f, contrast = .05f) },
        f("rose", "Soft Rose", "Portrait") { copy(tint = .3f, fade = .2f, exposure = .08f, split = .4f, highlights = 0xFFFFA0B8.toInt()) },
        f("honey", "Honey", "Portrait") { copy(temperature = .45f, saturation = .1f, curve = .15f, split = .4f, shadows = 0xFF6A4020.toInt()) },

        f("daylight", "Daylight", "Life") { copy(exposure = .1f, vibrance = .3f, temperature = .05f) },
        f("fresh", "Fresh", "Life") { copy(temperature = -.15f, vibrance = .35f, exposure = .06f, tint = -.1f) },
        f("picnic", "Picnic", "Life") { copy(saturation = .2f, temperature = .2f, exposure = .08f, curve = .1f) },
        f("morning", "Morning", "Life") { copy(exposure = .14f, fade = .2f, temperature = -.08f, contrast = -.05f) },
        f("bright", "Bright", "Life") { copy(exposure = .22f, contrast = .05f, vibrance = .15f) },

        f("blockbuster", "Blockbuster", "Movies") { copy(contrast = .3f, split = 1f, shadows = 0xFF1F6B78.toInt(), highlights = 0xFFF0A050.toInt(), saturation = -.05f, curve = .3f) },
        f("noir-film", "Noir Film", "Movies") { copy(mono = 1f, contrast = .45f, curve = .4f, exposure = -.05f) },
        f("western", "Western", "Movies") { copy(temperature = .6f, saturation = -.15f, contrast = .2f, fade = .1f, split = .5f, shadows = 0xFF5A3A20.toInt()) },
        f("scifi", "Sci-Fi", "Movies") { copy(temperature = -.5f, contrast = .25f, split = .7f, shadows = 0xFF102840.toInt(), highlights = 0xFF90D0FF.toInt()) },
        f("dune", "Desert", "Movies") { copy(temperature = .5f, tint = .05f, fade = .15f, split = .6f, highlights = 0xFFFFC080.toInt(), saturation = -.1f) },
        f("matrix", "Code Green", "Movies") { copy(tint = -.5f, contrast = .25f, saturation = -.3f, split = .6f, shadows = 0xFF103018.toInt(), highlights = 0xFFB0FFB0.toInt()) },
        f("bleach", "Bleach", "Movies") { copy(bleach = .8f, contrast = .2f) },

        f("kodak-70s", "Kodak 70s", "Retro") { copy(temperature = .3f, fade = .3f, saturation = -.1f, split = .6f, shadows = 0xFF3A5050.toInt(), highlights = 0xFFFFD090.toInt()) },
        f("instant", "Instant", "Retro") { copy(fade = .35f, contrast = -.1f, temperature = .1f, tint = .1f, split = .5f, shadows = 0xFF40506A.toInt()) },
        f("camcorder", "90s Tape", "Retro") { copy(saturation = .15f, contrast = -.05f, fade = .2f, tint = .1f, hue = -.05f) },
        f("faded", "Faded", "Retro") { copy(fade = .5f, saturation = -.25f, contrast = -.15f) },
        f("sepia", "Sepia", "Retro") { copy(mono = 1f, split = 1f, shadows = 0xFF503018.toInt(), highlights = 0xFFFFE0B0.toInt(), fade = .15f) },
        f("cross", "Cross Process", "Retro") { copy(contrast = .25f, saturation = .2f, split = .9f, shadows = 0xFF204080.toInt(), highlights = 0xFFFFF070.toInt()) },

        f("pastel", "Pastel", "Style") { copy(fade = .35f, saturation = -.1f, exposure = .12f, contrast = -.2f, tint = .1f) },
        f("cyberpunk", "Cyberpunk", "Style") { copy(contrast = .3f, split = 1f, shadows = 0xFF3010A0.toInt(), highlights = 0xFFFF40C0.toInt(), saturation = .2f) },
        f("vaporwave", "Vaporwave", "Style") { copy(hue = .15f, split = .9f, shadows = 0xFF4020A0.toInt(), highlights = 0xFF40E0FF.toInt(), fade = .15f, saturation = .15f) },
        f("candy", "Candy", "Style") { copy(saturation = .55f, tint = .15f, exposure = .1f, curve = .15f) },
        f("neon-night", "Neon Night", "Style") { copy(exposure = -.1f, contrast = .3f, split = .8f, shadows = 0xFF102060.toInt(), highlights = 0xFFFF60A0.toInt(), vibrance = .4f) },
        f("frost", "Frost", "Style") { copy(temperature = -.6f, exposure = .1f, saturation = -.2f, fade = .2f) },
        f("dramatic", "Dramatic", "Style") { copy(contrast = .45f, saturation = -.2f, exposure = -.08f, curve = .4f) },

        f("mono", "Mono", "B&W") { copy(mono = 1f) },
        f("hi-contrast", "High Contrast", "B&W") { copy(mono = 1f, contrast = .5f, curve = .4f) },
        f("silver", "Silver", "B&W") { copy(mono = 1f, fade = .25f, contrast = -.05f, split = .4f, highlights = 0xFFD0E0F0.toInt()) },
        f("blue-tone", "Cyanotype", "B&W") { copy(mono = 1f, split = 1f, shadows = 0xFF102850.toInt(), highlights = 0xFFC0E0FF.toInt()) },
        f("newsprint", "Newsprint", "B&W") { copy(mono = 1f, contrast = .3f, fade = .3f) },
    )
    private val byId = filters.associateBy { it.id }
    /** Older projects used short look names; they map onto the closest recipe. */
    private val legacy = mapOf("vivid" to "flt-pop", "warm" to "flt-golden", "cool" to "flt-fresh", "cinema" to "flt-blockbuster",
        "fade" to "flt-faded", "mono" to "flt-mono", "noir" to "flt-noir-film")
    fun find(id: String) = byId[id] ?: legacy[id]?.let { byId[it] }


}

/** True when a clip needs the grading shader rather than the legacy colour matrix. */
internal fun VideoClipEdits.needsGrade(): Boolean = lookId.startsWith("flt-") || brightness != 0f || highlights != 0f || shadows != 0f ||
    sharpen != 0f || fade != 0f || vignette != 0f || grain != 0f || tint != 0f || vibrance != 0f


internal const val GRADE_FRAGMENT = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
varying highp vec2 vUv;
#else
precision mediump float;
varying mediump vec2 vUv;
#endif
uniform sampler2D uTexture;
uniform vec2 uSize; uniform float uAspect;
uniform vec4 uF0; uniform vec4 uF1; uniform vec4 uF2; uniform vec4 uShadow; uniform vec4 uHigh; uniform float uAmount;
uniform vec4 uA0; uniform vec4 uA1; uniform vec4 uA2; uniform vec4 uA3;
float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }
vec3 hueRotate(vec3 c, float a) {
  mat3 toY = mat3(0.299, 0.596, 0.211, 0.587, -0.274, -0.523, 0.114, -0.322, 0.312);
  mat3 toR = mat3(1.0, 1.0, 1.0, 0.956, -0.272, -1.106, 0.621, -0.647, 1.703);
  vec3 y = toY * c; float h = atan(y.z, y.y) + a * 6.2831853; float ch = length(y.yz);
  return toR * vec3(y.x, ch * cos(h), ch * sin(h));
}
vec3 filterGrade(vec3 c) {
  c = clamp((c * exp2(uF0.x) - 0.5) * (1.0 + uF0.y) + 0.5, 0.0, 1.0);
  c += (c * c * (3.0 - 2.0 * c) - c) * uF1.w;
  c *= vec3(1.0 + uF0.w * 0.14 + uF1.x * 0.04, 1.0 + uF0.w * 0.02 - uF1.x * 0.08, 1.0 - uF0.w * 0.14 + uF1.x * 0.04);
  float l = luma(c);
  c += (uShadow.rgb - 0.5) * (1.0 - smoothstep(0.0, 0.55, l)) * uF2.z * 0.6 + (uHigh.rgb - 0.5) * smoothstep(0.45, 1.0, l) * uF2.z * 0.6;
  if (uF2.y != 0.0) c = hueRotate(c, uF2.y);
  l = luma(c); float s = max(max(c.r, c.g), c.b) - min(min(c.r, c.g), c.b);
  c = vec3(l) + (c - vec3(l)) * (1.0 + uF0.z + uF1.z * (1.0 - clamp(s, 0.0, 1.0)));
  if (uF2.w > 0.0) c += ((c + vec3(l)) * 0.5 * (1.0 + 0.3 * (l - 0.5)) - c) * uF2.w;
  c += (vec3(l) - c) * uF2.x;
  return c * (1.0 - uF1.y * 0.25) + uF1.y * 0.1;
}
void main() {
  vec3 src = texture2D(uTexture, vUv).rgb;
  vec3 c = mix(src, filterGrade(src), uAmount);
  // Adjust: exposure, brightness, contrast, saturation / warmth, tint, highlights, shadows / fade, vibrance, sharpen, vignette.
  if (uA2.z != 0.0) {
    vec2 e = 1.0 / uSize;
    vec3 n = (texture2D(uTexture, vUv + vec2(e.x, 0.0)).rgb + texture2D(uTexture, vUv - vec2(e.x, 0.0)).rgb + texture2D(uTexture, vUv + vec2(0.0, e.y)).rgb + texture2D(uTexture, vUv - vec2(0.0, e.y)).rgb) * 0.25;
    c += (src - n) * uA2.z * 2.2;
  }
  c *= exp2(uA0.x * 1.2); c += uA0.y * 0.2;
  c = (c - 0.5) * (1.0 + uA0.z * 0.8) + 0.5;
  float l = luma(c);
  c += uA1.z * 0.3 * smoothstep(0.45, 1.0, l) + uA1.w * 0.3 * (1.0 - smoothstep(0.0, 0.5, l));
  c *= vec3(1.0 + uA1.x * 0.15 + uA1.y * 0.04, 1.0 + uA1.x * 0.015 - uA1.y * 0.08, 1.0 - uA1.x * 0.15 + uA1.y * 0.04);
  l = luma(c); float s = max(max(c.r, c.g), c.b) - min(min(c.r, c.g), c.b);
  c = vec3(l) + (c - vec3(l)) * max(0.0, 1.0 + uA0.w + uA2.y * (1.0 - clamp(s, 0.0, 1.0)));
  c = c * (1.0 - uA2.x * 0.3) + uA2.x * 0.12;
  vec2 d = (vUv - 0.5) * 2.0; float vig = smoothstep(0.35, 1.6, dot(d, d));
  c = uA2.w >= 0.0 ? c * (1.0 - vig * uA2.w * 0.85) : mix(c, vec3(1.0), vig * -uA2.w * 0.7);
  if (uA3.x > 0.0) {
    vec2 p = floor(vUv * uSize / max(1.0, min(uSize.x, uSize.y) / 540.0));
    float n = fract(sin(dot(p + uA3.y, vec2(12.9898, 78.233))) * 43758.5453) - 0.5;
    c += n * uA3.x * 0.18 * sqrt(max(luma(c), 0.0) + 0.05);
  }
  gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
"""
