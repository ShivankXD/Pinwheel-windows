package com.pinwheel.core.media.video

/** How each word (or the whole page) arrives. Durations are in [CaptionMotionTemplate.enterMs]. */
enum class CaptionEnter { None, Pop, Rise, Fade, Zoom, Drop, Bounce, Slam, Swing, Blur, Stretch }

/** Continuous motion evaluated on the timeline clock, so preview and export agree frame for frame. */
enum class CaptionLoop { None, Pulse, Shimmer, Hue, Flicker, Wave, Shake, Breathe, Glitch }

/**
 * A Pinwheel animated caption template. Sizes such as stroke widths and glow radii are in em
 * (multiples of the rendered font size), so a template looks identical at any resolution.
 */
data class CaptionMotionTemplate(
    val id: String,
    val name: String,
    val category: String,
    val font: String,
    val caps: Boolean = false,
    val lower: Boolean = false,
    val skew: Float = 0f,
    val tracking: Float = 0f,
    val scale: Float = 1f,
    /** Words shown at once. Zero keeps the whole cue on screen. */
    val wordsPerPage: Int = 3,
    val maxLines: Int = 2,
    val oneWordPerLine: Boolean = false,
    val lineGap: Float = .04f,
    val rotation: Float = 0f,
    val fill: Int = WHITE,
    val gradient: IntArray? = null,
    val palette: IntArray? = null,
    val stroke: Int = 0,
    val strokeWidth: Float = 0f,
    val outerStroke: Int = 0,
    val outerStrokeWidth: Float = 0f,
    val shadow: Int = 0,
    val shadowDx: Float = 0f,
    val shadowDy: Float = 0f,
    val shadowBlur: Float = 0f,
    val glow: Int = 0,
    val glowRadius: Float = 0f,
    val box: Int = 0,
    val boxGradient: IntArray? = null,
    val boxRadius: Float = .22f,
    val boxPad: Float = .28f,
    val activeFill: Int? = null,
    val activeGradient: IntArray? = null,
    val activeStroke: Int? = null,
    val activeGlow: Int = 0,
    val activeGlowRadius: Float = 0f,
    val activeScale: Float = 1f,
    val activeLift: Float = 0f,
    val activeBox: Int = 0,
    val activeBoxSweep: Boolean = false,
    /** Words already spoken keep this colour (karaoke fill). */
    val readFill: Int? = null,
    /** Opacity of words not yet spoken when words are not revealed one by one. */
    val unreadAlpha: Float = 1f,
    val enter: CaptionEnter = CaptionEnter.Pop,
    val enterPerWord: Boolean = false,
    val enterMs: Long = 0,
    val typewriter: Boolean = false,
    val loop: CaptionLoop = CaptionLoop.None,
    val accent: Int = 0xFFFFE14D.toInt(),
) {
    val animatesContinuously: Boolean get() = loop != CaptionLoop.None
}

private const val WHITE = 0xFFFFFFFF.toInt()
private fun c(value: Long) = value.toInt()
private fun colors(vararg values: Long) = IntArray(values.size) { values[it].toInt() }

object CaptionMotionCatalog {
    val categories = listOf("Trending", "Glow", "Highlight", "Bold", "Neon", "Fun", "Elegant", "Retro")

    val templates: List<CaptionMotionTemplate> = listOf(
        // Trending: short, punchy word groups with an emphasised spoken word.
        CaptionMotionTemplate("spotlight", "Spotlight", "Trending", "poppins-black", caps = true, scale = 1.2f, wordsPerPage = 3,
            stroke = c(0xFF000000), strokeWidth = .09f, shadow = c(0xB3000000), shadowDy = .06f, shadowBlur = .08f,
            activeFill = c(0xFFFFE83B), activeScale = 1.14f, enter = CaptionEnter.Pop),
        CaptionMotionTemplate("hype-green", "Hype", "Trending", "poppins-black", caps = true, scale = 1.35f, wordsPerPage = 2,
            stroke = c(0xFF000000), strokeWidth = .1f, shadow = c(0xCC000000), shadowDy = .07f, shadowBlur = .05f,
            activeFill = c(0xFF3CFF6B), activeScale = 1.2f, activeLift = .05f, enter = CaptionEnter.Slam, enterPerWord = true),
        CaptionMotionTemplate("word-punch", "Punch", "Trending", "anton", caps = true, scale = 2f, wordsPerPage = 1,
            stroke = c(0xFF000000), strokeWidth = .07f, shadow = c(0x99000000), shadowDy = .05f, shadowBlur = .1f,
            enter = CaptionEnter.Zoom),
        CaptionMotionTemplate("glow-white", "Halo", "Trending", "poppins-black", scale = 1.3f, wordsPerPage = 2,
            glow = c(0xE6FFFFFF), glowRadius = .38f, loop = CaptionLoop.Pulse, enter = CaptionEnter.Pop),
        CaptionMotionTemplate("kinetic-stack", "Stack", "Trending", "anton", caps = true, scale = 1.35f, wordsPerPage = 3, maxLines = 3,
            oneWordPerLine = true, lineGap = -.08f, stroke = c(0xFF000000), strokeWidth = .05f,
            activeFill = c(0xFFFF4D6D), activeScale = 1.12f, unreadAlpha = .0f, enter = CaptionEnter.Rise, enterPerWord = true),
        CaptionMotionTemplate("tilt-pop", "Tilt", "Trending", "poppins-black", caps = true, scale = 1.25f, wordsPerPage = 2, rotation = -6f,
            stroke = c(0xFF000000), strokeWidth = .1f, activeFill = c(0xFFFF3B3B), activeScale = 1.1f, enter = CaptionEnter.Swing),

        // Glow
        CaptionMotionTemplate("glow-gold", "Gold glow", "Glow", "anton", caps = true, scale = 1.35f, wordsPerPage = 3,
            fill = c(0xFFFFE55C), glow = c(0xFFFFB400), glowRadius = .42f, loop = CaptionLoop.Pulse, enter = CaptionEnter.Pop),
        CaptionMotionTemplate("ember", "Ember", "Glow", "bangers", caps = true, scale = 1.6f, wordsPerPage = 1, tracking = .02f,
            gradient = colors(0xFFFFF6A8, 0xFFFFB21E, 0xFFFF5A00), stroke = c(0xFF6B1300), strokeWidth = .07f,
            glow = c(0xFFFF6A00), glowRadius = .5f, loop = CaptionLoop.Pulse, enter = CaptionEnter.Pop, enterPerWord = true),
        CaptionMotionTemplate("inferno", "Inferno", "Glow", "anton", caps = true, scale = 1.4f, wordsPerPage = 2,
            gradient = colors(0xFFFFF3A0, 0xFFFF7A00, 0xFFFF1E00), glow = c(0xFFFF4500), glowRadius = .45f,
            loop = CaptionLoop.Pulse, enter = CaptionEnter.Zoom),
        CaptionMotionTemplate("lavender-haze", "Lavender", "Glow", "luckiest", caps = true, scale = 1.3f, wordsPerPage = 2,
            fill = c(0xFFF1E4FF), stroke = c(0xFF6E3BD1), strokeWidth = .09f, glow = c(0xFFB98CFF), glowRadius = .6f,
            loop = CaptionLoop.Pulse, enter = CaptionEnter.Bounce),
        CaptionMotionTemplate("active-glow", "Pink aura", "Glow", "poppins-black", lower = true, scale = 1.2f, wordsPerPage = 3,
            activeGlow = c(0xFFFF4FD8), activeGlowRadius = .5f, activeScale = 1.1f, activeFill = c(0xFFFFE6FA),
            shadow = c(0x99000000), shadowDy = .05f, shadowBlur = .1f, enter = CaptionEnter.Fade),
        CaptionMotionTemplate("script-glow", "Dreamy", "Glow", "pacifico", scale = 1.25f, wordsPerPage = 2,
            glow = c(0xFFFF7AD9), glowRadius = .35f, loop = CaptionLoop.Wave, enter = CaptionEnter.Blur, enterPerWord = true),
        CaptionMotionTemplate("frost", "Frost", "Glow", "bebas", caps = true, scale = 1.45f, wordsPerPage = 4, tracking = .04f,
            fill = c(0xFFDDF6FF), glow = c(0xFF53C7FF), glowRadius = .3f, unreadAlpha = .35f,
            activeFill = WHITE, activeScale = 1.08f, activeGlow = c(0xFF9BE3FF), activeGlowRadius = .45f, enter = CaptionEnter.Rise),

        // Highlight: karaoke boxes and sweeping markers.
        CaptionMotionTemplate("karaoke-violet", "Karaoke", "Highlight", "poppins-black", caps = true, scale = 1.05f, wordsPerPage = 4,
            shadow = c(0x99000000), shadowDy = .05f, shadowBlur = .08f, activeBox = c(0xFF7B3CFF), activeScale = 1.06f,
            enter = CaptionEnter.Pop),
        CaptionMotionTemplate("pill-blue", "Blue pill", "Highlight", "poppins-bold", caps = true, scale = 1f, wordsPerPage = 5,
            shadow = c(0x80000000), shadowDy = .04f, shadowBlur = .06f, activeBox = c(0xFF2F7BFF), enter = CaptionEnter.Rise),
        CaptionMotionTemplate("ice-pill", "Ice", "Highlight", "poppins-bold", lower = true, scale = 1.1f, wordsPerPage = 3,
            shadow = c(0x99000000), shadowDy = .04f, shadowBlur = .08f, activeBox = c(0xFF00C2FF), activeScale = 1.05f,
            enter = CaptionEnter.Stretch),
        CaptionMotionTemplate("highlighter", "Marker", "Highlight", "poppins-black", scale = 1.1f, wordsPerPage = 4,
            shadow = c(0xB3000000), shadowDy = .05f, shadowBlur = .08f, activeBox = c(0xFFFFE14D), activeBoxSweep = true,
            activeFill = c(0xFF111111), readFill = c(0xFFFFE98A), enter = CaptionEnter.Fade),
        CaptionMotionTemplate("karaoke-fill", "Sing along", "Highlight", "poppins-black", caps = true, scale = 1.1f, wordsPerPage = 5,
            fill = c(0xFFFFFFFF), stroke = c(0xFF000000), strokeWidth = .08f, readFill = c(0xFF22E3FF), activeFill = c(0xFF22E3FF),
            activeScale = 1.1f, enter = CaptionEnter.Pop),

        // Bold
        CaptionMotionTemplate("comic", "Comic", "Bold", "luckiest", caps = true, scale = 1.35f, wordsPerPage = 2,
            gradient = colors(0xFFFFF27A, 0xFFFFC21A), stroke = c(0xFF0C1A7A), strokeWidth = .1f,
            outerStroke = WHITE, outerStrokeWidth = .17f, shadow = c(0x99000000), shadowDy = .08f, shadowBlur = .06f,
            enter = CaptionEnter.Pop, enterPerWord = true, loop = CaptionLoop.Shake),
        CaptionMotionTemplate("street", "Street", "Bold", "bungee", caps = true, scale = 1.2f, wordsPerPage = 2,
            fill = c(0xFFFFD400), stroke = c(0xFF111111), strokeWidth = .06f, shadow = c(0xFFFF2E63), shadowDx = .07f, shadowDy = .07f,
            enter = CaptionEnter.Slam, enterPerWord = true),
        CaptionMotionTemplate("bubble", "Bubble", "Bold", "titan", scale = 1.35f, wordsPerPage = 2,
            stroke = c(0xFF16A6C9), strokeWidth = .13f, shadow = c(0xB3003E52), shadowDy = .08f, shadowBlur = .02f,
            enter = CaptionEnter.Bounce, enterPerWord = true),
        CaptionMotionTemplate("chroma", "Chroma", "Bold", "poppins-black", caps = true, scale = 1.2f, wordsPerPage = 3,
            stroke = c(0xFF000000), strokeWidth = .08f, loop = CaptionLoop.Hue, enter = CaptionEnter.Pop),
        CaptionMotionTemplate("ticker", "Breaking", "Bold", "poppins-black", caps = true, scale = .95f, wordsPerPage = 4,
            box = c(0xFFE63946), boxRadius = .12f, activeFill = c(0xFFFFE14D), enter = CaptionEnter.Rise),

        // Neon
        CaptionMotionTemplate("neon-pink", "Neon pink", "Neon", "righteous", lower = true, scale = 1.35f, wordsPerPage = 2,
            fill = c(0xFFFFE9F7), glow = c(0xFFFF3DB8), glowRadius = .38f, loop = CaptionLoop.Flicker, enter = CaptionEnter.Fade),
        CaptionMotionTemplate("neon-violet", "Neon tube", "Neon", "monoton", caps = true, scale = 1.25f, wordsPerPage = 2,
            fill = c(0xFFF3E1FF), glow = c(0xFFA24DFF), glowRadius = .34f, loop = CaptionLoop.Flicker, enter = CaptionEnter.Fade),
        CaptionMotionTemplate("cyber", "Glitch", "Neon", "bebas", caps = true, scale = 1.55f, wordsPerPage = 2, tracking = .03f,
            stroke = c(0xFF06121F), strokeWidth = .05f, loop = CaptionLoop.Glitch, enter = CaptionEnter.Stretch),
        CaptionMotionTemplate("neon-green", "Acid", "Neon", "righteous", caps = true, scale = 1.25f, wordsPerPage = 3,
            fill = c(0xFFE9FFE0), glow = c(0xFF39FF14), glowRadius = .32f, unreadAlpha = .3f, activeScale = 1.08f,
            loop = CaptionLoop.Pulse, enter = CaptionEnter.Fade),

        // Fun
        CaptionMotionTemplate("candy", "Candy", "Fun", "titan", caps = true, scale = 1.2f, wordsPerPage = 3,
            palette = colors(0xFFFF5FA2, 0xFFFFD93D, 0xFF6BCB77, 0xFF4D96FF), stroke = WHITE, strokeWidth = .12f,
            shadow = c(0x80000000), shadowDy = .07f, shadowBlur = .05f, enter = CaptionEnter.Bounce, enterPerWord = true),
        CaptionMotionTemplate("wave", "Wavy", "Fun", "luckiest", scale = 1.25f, wordsPerPage = 3,
            stroke = c(0xFFFF4FA3), strokeWidth = .11f, loop = CaptionLoop.Wave, enter = CaptionEnter.Pop),
        CaptionMotionTemplate("marker-green", "Doodle", "Fun", "marker", lower = true, scale = 1.5f, wordsPerPage = 1,
            fill = c(0xFF3EE06B), shadow = c(0xCC06240F), shadowDy = .05f, shadowBlur = .04f, enter = CaptionEnter.Swing),
        CaptionMotionTemplate("drip", "Spooky", "Fun", "creepster", caps = true, scale = 1.45f, wordsPerPage = 2, tracking = .03f,
            fill = c(0xFFC8FF3E), shadow = c(0xE6112000), shadowDy = .06f, shadowBlur = .1f, glow = c(0x8039FF14), glowRadius = .3f,
            loop = CaptionLoop.Shake, enter = CaptionEnter.Drop),
        CaptionMotionTemplate("soft-pink", "Sticky note", "Fun", "poppins-bold", lower = true, scale = .95f, wordsPerPage = 5,
            fill = c(0xFF6B1030), box = c(0xFFFFB3C7), boxRadius = .3f, activeFill = c(0xFFD6004C), enter = CaptionEnter.Rise),

        // Elegant
        CaptionMotionTemplate("serif-story", "Story", "Elegant", "dmserif", scale = 1.3f, wordsPerPage = 4,
            shadow = c(0x99000000), shadowDy = .04f, shadowBlur = .12f, enter = CaptionEnter.Blur, enterPerWord = true),
        CaptionMotionTemplate("editorial", "Editorial", "Elegant", "dmserif", scale = 1.2f, wordsPerPage = 5,
            fill = c(0xFFF6EEE0), unreadAlpha = .35f, activeFill = WHITE, shadow = c(0x80000000), shadowDy = .03f, shadowBlur = .1f,
            enter = CaptionEnter.Rise),
        CaptionMotionTemplate("minimal", "Minimal", "Elegant", "poppins-bold", scale = 1.05f, wordsPerPage = 8, maxLines = 3,
            shadow = c(0xB3000000), shadowDy = .04f, shadowBlur = .12f, unreadAlpha = .45f, activeScale = 1.04f, enter = CaptionEnter.Fade),
        CaptionMotionTemplate("shimmer-gold", "Gilded", "Elegant", "bebas", caps = true, scale = 1.5f, wordsPerPage = 3, tracking = .06f,
            gradient = colors(0xFFFFF1B8, 0xFFE8B84A, 0xFFB07A1C), shadow = c(0xB3000000), shadowDy = .05f, shadowBlur = .08f,
            loop = CaptionLoop.Shimmer, enter = CaptionEnter.Rise),
        CaptionMotionTemplate("subtitle-bar", "Subtitle", "Elegant", "poppins-bold", scale = .95f, wordsPerPage = 0, maxLines = 3,
            box = c(0xB3000000), boxRadius = .18f, unreadAlpha = .55f, activeFill = c(0xFFFFE14D), enter = CaptionEnter.Fade),

        // Retro
        CaptionMotionTemplate("retro-pixel", "8-bit", "Retro", "pixel", caps = true, scale = 1f, wordsPerPage = 2, lineGap = .4f,
            stroke = c(0xFF1E2A78), strokeWidth = .08f, shadow = c(0xFFFF3D7F), shadowDx = .1f, shadowDy = .1f,
            enter = CaptionEnter.Drop, enterPerWord = true),
        CaptionMotionTemplate("typewriter", "Typewriter", "Retro", "sys:monospace", scale = .95f, wordsPerPage = 0, maxLines = 3,
            box = c(0xD9101215), boxRadius = .1f, typewriter = true, enter = CaptionEnter.None),
        CaptionMotionTemplate("lobster-retro", "Diner", "Retro", "lobster", scale = 1.35f, wordsPerPage = 2,
            palette = colors(0xFF3FD26B, 0xFFFF4B4B), stroke = WHITE, strokeWidth = .09f, shadow = c(0xE6000000), shadowDx = .05f,
            shadowDy = .06f, enter = CaptionEnter.Swing),
        CaptionMotionTemplate("brush-gold", "Brush", "Retro", "dmserif", scale = 1.05f, wordsPerPage = 3,
            fill = c(0xFF2A1A05), boxGradient = colors(0xFFF7D774, 0xFFD9A441, 0xFFF3CF6A), boxRadius = .08f, activeFill = c(0xFFB3261E), activeScale = 1.06f,
            enter = CaptionEnter.Stretch),
    )

    private val byId = templates.associateBy { it.id }
    fun find(id: String): CaptionMotionTemplate? = byId[id]
}
