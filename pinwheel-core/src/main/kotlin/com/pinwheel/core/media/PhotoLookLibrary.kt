package com.pinwheel.core.media

import com.pinwheel.core.model.Adjustments
import com.pinwheel.core.model.ColorBand
import com.pinwheel.core.model.ToneGrade

/**
 * The expanded look collection. Each look is built from the same engine the sliders drive (tone,
 * curves, colour mix, three-way grading, grain, fade), so tapping one and then opening any panel
 * shows exactly what it did, and every look can be fine-tuned or dialled back with its amount.
 */
object PhotoLookLibrary {
    /** Colour Mix band order used by [mix]. */
    private const val RED = 0; private const val ORANGE = 1; private const val YELLOW = 2; private const val GREEN = 3
    private const val AQUA = 4; private const val BLUE = 5; private const val PURPLE = 6; private const val MAGENTA = 7

    private class Builder(var a: Adjustments = Adjustments()) {
        /** Master tone curve at x = 0, .25, .5, .75, 1. */
        fun curve(vararg y: Float) { a = a.copy(curve = y.toList()) }
        fun red(vararg y: Float) { a = a.copy(redCurve = y.toList()) }
        fun green(vararg y: Float) { a = a.copy(greenCurve = y.toList()) }
        fun blue(vararg y: Float) { a = a.copy(blueCurve = y.toList()) }
        fun shadows(hue: Float, sat: Float, lum: Float = 0f) = grade(0, hue, sat, lum)
        fun mids(hue: Float, sat: Float, lum: Float = 0f) = grade(1, hue, sat, lum)
        fun highs(hue: Float, sat: Float, lum: Float = 0f) = grade(2, hue, sat, lum)
        // The grading wheels are deliberately gentle for slider work; looks need a clearly visible tint.
        private fun grade(i: Int, hue: Float, sat: Float, lum: Float) { a = a.copy(grading = a.grading.toMutableList().also { it[i] = ToneGrade(hue, (sat * 2f).coerceAtMost(1f), lum) }) }
        fun mix(band: Int, hue: Float = 0f, sat: Float = 0f, lum: Float = 0f) { a = a.copy(mix = a.mix.toMutableList().also { it[band] = ColorBand(hue, sat, lum) }) }
    }

    private fun look(name: String, description: String, category: String, base: Adjustments, block: Builder.() -> Unit = {}) =
        PhotoLook(name, description, Builder(base).apply(block).a, category)

    private val matte = floatArrayOf(.06f, .27f, .51f, .75f, .96f)
    private val softS = floatArrayOf(.02f, .22f, .5f, .79f, .99f)
    private val strongS = floatArrayOf(0f, .18f, .5f, .83f, 1f)

    val looks: List<PhotoLook> = listOf(
        // ---- Social: the feed classics, each tuned to its own mood ------------------------------------
        look("Pop", "Cool shadows, bright warm highlights", "Social", Adjustments(contrast = 1.14f, saturation = 1.18f, highlights = -.06f, exposure = .05f)) {
            shadows(205f, .22f); highs(45f, .14f); mix(BLUE, sat = .15f); mix(ORANGE, sat = .05f)
        },
        look("Sunkissed", "Warm, faded summer glow", "Social", Adjustments(exposure = .1f, contrast = .95f, warmth = .28f, fade = .12f, saturation = 1.05f)) {
            highs(40f, .22f); shadows(20f, .12f)
        },
        look("Airy", "Bright and light, calm reds", "Social", Adjustments(exposure = .2f, contrast = .92f, saturation = .9f, highlights = -.15f, shadows = .15f)) {
            mix(RED, sat = -.25f); mix(BLUE, sat = .15f, lum = .1f); mix(GREEN, sat = .1f, lum = .08f)
        },
        look("Soft Muse", "Faded, gentle, understated", "Social", Adjustments(fade = .2f, contrast = .9f, saturation = .78f, exposure = .06f, warmth = .04f)),
        look("Loud", "Deep contrast, saturated colour", "Social", Adjustments(contrast = 1.28f, saturation = 1.3f, blacks = -.05f, vignette = .15f)) { curve(0f, .2f, .5f, .8f, 1f) },
        look("Moonlit", "Silver black and white with punch", "Social", Adjustments(saturation = 0f, contrast = 1.25f, exposure = .05f, highlights = -.1f, fade = .06f)),
        look("Dusty", "Dusty pink-orange, faded", "Social", Adjustments(fade = .18f, warmth = .15f, tint = .05f, saturation = .85f, contrast = .95f)) {
            highs(25f, .22f); shadows(300f, .1f)
        },
        look("Blush Film", "Pastel pink, soft blacks", "Social", Adjustments(fade = .22f, tint = .12f, saturation = .82f, exposure = .1f, contrast = .9f)) { highs(335f, .22f) },
        look("Cream", "Creamy, warm and muted", "Social", Adjustments(saturation = .78f, warmth = .12f, fade = .1f, contrast = .96f, highlights = -.1f)) { highs(45f, .12f) },
        look("Sleepy", "Dark, dreamy and desaturated", "Social", Adjustments(exposure = -.1f, saturation = .7f, fade = .16f, warmth = .1f, vignette = .15f)) { shadows(30f, .15f) },
        look("Frost", "Icy blues, crisp highlights", "Social", Adjustments(warmth = -.3f, contrast = 1.08f, exposure = .08f, saturation = .92f, vignette = .12f)) { highs(195f, .15f) },
        look("Seafoam", "Pastel greens and blues", "Social", Adjustments(exposure = .1f, fade = .12f, saturation = .9f)) {
            mix(GREEN, hue = .2f, lum = .1f); mix(AQUA, sat = .15f, lum = .1f); shadows(170f, .2f)
        },
        look("Cross Pop", "Punchy cross-process with dark edges", "Social", Adjustments(contrast = 1.2f, saturation = 1.15f, vignette = .35f)) {
            red(0f, .18f, .52f, .84f, 1f); blue(.12f, .28f, .48f, .68f, .88f); highs(50f, .15f)
        },
        look("Rosy", "Pink warmth, faded blacks", "Social", Adjustments(fade = .15f, warmth = .12f, tint = .1f, contrast = 1.05f, saturation = 1.05f)) {
            shadows(330f, .18f); highs(40f, .15f)
        },
        look("Bright Room", "Lifted, bright and clean", "Social", Adjustments(exposure = .25f, contrast = .95f, shadows = .2f, highlights = -.2f, saturation = .95f, vignette = -.1f)),
        // ---- Film ----------------------------------------------------------------------------------
        look("Portrait 400", "Soft pastel film, kind to skin", "Film", Adjustments(exposure = .08f, contrast = .96f, warmth = .1f, saturation = .92f, grain = .1f)) {
            curve(*matte); shadows(200f, .16f); highs(38f, .2f); mix(ORANGE, sat = -.08f, lum = .1f); mix(RED, hue = .1f)
        },
        look("Gold 200", "Sunny consumer film", "Film", Adjustments(exposure = .06f, contrast = 1.05f, warmth = .22f, saturation = 1.04f, fade = .05f, grain = .14f)) {
            highs(45f, .28f); mix(YELLOW, hue = -.15f, sat = .2f); mix(GREEN, hue = -.2f); mix(BLUE, sat = -.1f)
        },
        look("Evergreen 400", "Cool greens and teal shadows", "Film", Adjustments(contrast = 1.08f, warmth = -.04f, saturation = .95f, grain = .15f, fade = .04f)) {
            shadows(170f, .26f); highs(50f, .1f); mix(GREEN, hue = .45f, sat = -.05f); mix(YELLOW, hue = .15f); mix(RED, sat = -.05f); mix(MAGENTA, hue = -.2f)
        },
        look("Tungsten 800", "Night film: teal shadows, warm glow", "Film", Adjustments(contrast = 1.1f, warmth = -.08f, vibrance = .1f, grain = .22f, highlights = -.15f)) {
            shadows(192f, .32f); highs(22f, .26f); mix(RED, sat = .12f); mix(ORANGE, sat = .1f)
        },
        look("Vivid 100", "Saturated, fine-grain colour", "Film", Adjustments(contrast = 1.12f, saturation = 1.22f, blacks = -.06f, grain = .05f)) {
            curve(*softS); mix(BLUE, sat = .2f, lum = -.18f); mix(RED, sat = .15f); mix(GREEN, sat = .1f, lum = -.05f)
        },
        look("Slide", "Deep, punchy transparency film", "Film", Adjustments(exposure = .06f, contrast = 1.1f, saturation = 1.28f, blacks = -.03f, warmth = -.04f, highlights = -.1f, shadows = .1f)) {
            curve(*softS); mix(GREEN, sat = .25f, lum = -.08f); mix(BLUE, sat = .2f, lum = -.15f); mix(AQUA, sat = .2f)
        },
        look("Chrome 64", "Rich reds, classic warm slide", "Film", Adjustments(contrast = 1.15f, saturation = 1.06f, blacks = -.08f, warmth = .06f, grain = .08f)) {
            curve(*softS); highs(48f, .16f); shadows(210f, .1f); mix(RED, sat = .2f, lum = -.1f); mix(BLUE, hue = -.25f, sat = .1f); mix(YELLOW, hue = -.1f, sat = .1f)
        },
        look("Expired", "Faded, off-colour, forgotten roll", "Film", Adjustments(fade = .24f, warmth = .2f, tint = .14f, saturation = .8f, grain = .3f, vignette = .2f)) {
            green(.03f, .23f, .47f, .73f, .98f); shadows(300f, .2f); highs(45f, .2f)
        },
        look("Instant", "Instant-print colour, soft edges", "Film", Adjustments(exposure = .12f, contrast = .88f, fade = .28f, saturation = .82f, vignette = .3f, grain = .08f, warmth = .06f)) {
            shadows(180f, .3f); highs(48f, .26f); mix(GREEN, hue = .25f, sat = -.15f)
        },
        look("Pushed 1600", "Grit and contrast from a pushed roll", "Film", Adjustments(contrast = 1.25f, saturation = .85f, blacks = -.14f, grain = .45f, highlights = -.1f)) {
            shadows(212f, .16f); highs(40f, .1f)
        },

        // ---- Portrait --------------------------------------------------------------------------------
        look("Glow Skin", "Bright, even, luminous skin", "Portrait", Adjustments(exposure = .14f, contrast = .94f, highlights = -.18f, shadows = .2f, warmth = .06f, vibrance = .06f)) {
            curve(.03f, .27f, .53f, .77f, .98f); mix(ORANGE, sat = -.12f, lum = .18f); mix(RED, sat = -.05f, lum = .08f)
        },
        look("Peach", "Warm pink highlights, soft light", "Portrait", Adjustments(exposure = .14f, contrast = .92f, warmth = .12f, tint = .1f, fade = .08f)) {
            highs(18f, .26f); shadows(330f, .1f); mix(ORANGE, lum = .1f)
        },
        look("Studio", "Clean, neutral and crisp", "Portrait", Adjustments(exposure = .06f, contrast = 1.08f, highlights = -.12f, shadows = .1f, vibrance = .05f, saturation = .95f)) {
            curve(*softS); mix(ORANGE, sat = -.06f)
        },
        look("Editorial", "Muted colour, magazine contrast", "Portrait", Adjustments(contrast = 1.14f, saturation = .72f, blacks = -.06f, highlights = -.12f, warmth = .04f)) {
            shadows(210f, .12f); highs(40f, .1f); mix(ORANGE, sat = .15f)
        },
        look("Honey", "Golden-hour warmth", "Portrait", Adjustments(exposure = .08f, contrast = 1.04f, warmth = .45f, vibrance = .1f, highlights = -.12f)) {
            highs(40f, .26f); shadows(25f, .12f)
        },

        // ---- Cinematic -------------------------------------------------------------------------------
        look("Teal & Orange", "Blockbuster skin against teal", "Cinematic", Adjustments(contrast = 1.1f, saturation = .95f, highlights = -.08f)) {
            shadows(190f, .5f); highs(30f, .38f); mix(ORANGE, sat = .15f); mix(BLUE, hue = -.4f); mix(GREEN, hue = .5f, sat = -.3f); mix(AQUA, sat = .15f)
        },
        look("Blockbuster", "Heavy teal-orange, deep blacks", "Cinematic", Adjustments(contrast = 1.18f, saturation = .9f, blacks = -.12f, vignette = .22f)) {
            curve(*softS); shadows(195f, .65f); highs(32f, .45f); mix(GREEN, hue = .6f, sat = -.4f); mix(BLUE, hue = -.5f, sat = .1f)
        },
        look("Bleach Bypass", "Silvery, low colour, hard contrast", "Cinematic", Adjustments(exposure = .1f, contrast = 1.2f, saturation = .45f, highlights = -.15f, shadows = .12f, grain = .08f)) {
            curve(*softS)
        },
        look("Neo Noir", "Blue night with magenta light", "Cinematic", Adjustments(exposure = -.14f, contrast = 1.2f, warmth = -.12f, vignette = .3f)) {
            shadows(222f, .45f); highs(328f, .25f); mix(RED, hue = .3f, sat = .1f)
        },
        look("Code Green", "Sickly green, hacker film", "Cinematic", Adjustments(contrast = 1.18f, saturation = .7f, tint = -.14f, blacks = -.05f)) {
            shadows(150f, .28f); mids(120f, .1f); mix(ORANGE, hue = -.1f, sat = -.1f)
        },
        look("Dune", "Bleached desert gold", "Cinematic", Adjustments(contrast = 1.1f, warmth = .5f, saturation = .7f, fade = .08f, highlights = -.12f)) {
            highs(38f, .2f); mix(BLUE, sat = -.6f); mix(AQUA, sat = -.5f); mix(ORANGE, sat = .15f)
        },
        look("Arctic", "Cold and clear", "Cinematic", Adjustments(exposure = .12f, contrast = 1.05f, warmth = -.55f, saturation = .72f)) {
            highs(200f, .18f); shadows(215f, .2f)
        },
        look("Storybook", "Pastel symmetry and pink warmth", "Cinematic", Adjustments(contrast = .92f, warmth = .12f, saturation = .92f, fade = .14f)) {
            mix(RED, hue = .35f, lum = .15f); mix(YELLOW, sat = .2f, lum = .1f); mix(BLUE, hue = -.2f, sat = -.15f); highs(30f, .12f)
        },
        look("After Hours", "Moody anamorphic night", "Cinematic", Adjustments(contrast = 1.15f, warmth = -.06f, highlights = -.1f, vignette = .2f)) {
            shadows(215f, .42f); highs(35f, .3f); mix(BLUE, sat = .15f)
        },

        // ---- Moody ----------------------------------------------------------------------------------
        look("Moody Forest", "Deep, muted greens", "Moody", Adjustments(exposure = -.1f, contrast = 1.1f, fade = .1f, saturation = .92f)) {
            shadows(150f, .2f); mix(GREEN, sat = -.4f, lum = -.3f); mix(YELLOW, hue = -.25f, sat = -.2f); mix(ORANGE, sat = .1f)
        },
        look("Brooding", "Dark, cool and quiet", "Moody", Adjustments(exposure = -.2f, highlights = -.3f, saturation = .75f, blacks = -.05f, fade = .08f, vignette = .3f)) {
            shadows(210f, .26f)
        },
        look("Rainy Day", "Soft, cool and overcast", "Moody", Adjustments(warmth = -.2f, saturation = .7f, fade = .12f, contrast = .95f)) {
            shadows(200f, .25f); highs(210f, .08f)
        },
        look("Dusk", "Violet shadows, amber light", "Moody", Adjustments(exposure = -.05f, contrast = 1.05f)) {
            shadows(262f, .35f); highs(26f, .3f)
        },
        look("Earth", "Warm, muted natural tones", "Moody", Adjustments(warmth = .15f, saturation = .8f, fade = .08f, contrast = 1.04f)) {
            mix(GREEN, hue = -.5f, sat = -.3f); mix(BLUE, sat = -.3f); mix(ORANGE, sat = .1f)
        },

        // ---- Vintage ----------------------------------------------------------------------------------
        look("1970s", "Warm, faded and nostalgic", "Vintage", Adjustments(warmth = .3f, tint = .05f, fade = .2f, saturation = .85f, grain = .25f, vignette = .2f)) {
            green(.02f, .24f, .48f, .74f, .97f); shadows(30f, .2f); highs(50f, .3f)
        },
        look("Sepia", "Brown-toned print", "Vintage", Adjustments(saturation = 0f, contrast = 1.05f, fade = .1f, grain = .2f)) {
            shadows(30f, .5f); mids(35f, .55f); highs(42f, .4f)
        },
        look("Cross Process", "Wild, mismatched chemistry", "Vintage", Adjustments(contrast = 1.1f, saturation = 1.15f)) {
            red(0f, .16f, .52f, .86f, 1f); green(0f, .2f, .54f, .82f, 1f); blue(.2f, .3f, .46f, .62f, .78f)
        },
        look("Lomo", "Saturated with a dark tunnel", "Vintage", Adjustments(contrast = 1.25f, saturation = 1.3f, vignette = .6f, blacks = -.06f)) {
            shadows(220f, .2f); highs(55f, .15f)
        },
        look("Faded Memory", "Washed out and gentle", "Vintage", Adjustments(fade = .3f, contrast = .85f, saturation = .7f, warmth = .08f)),
        look("Super 8", "Home-movie warmth and grain", "Vintage", Adjustments(warmth = .25f, fade = .12f, grain = .35f, vignette = .35f, saturation = .9f)) {
            highs(45f, .3f); shadows(200f, .12f)
        },

        // ---- Aesthetic ---------------------------------------------------------------------------------
        look("Pastel", "Light, airy and soft", "Aesthetic", Adjustments(exposure = .2f, contrast = .85f, saturation = .8f, fade = .15f, highlights = -.2f)),
        look("Y2K", "Glossy blue and magenta", "Aesthetic", Adjustments(saturation = 1.18f, contrast = 1.1f, warmth = -.2f, tint = .12f)) {
            shadows(280f, .32f); highs(190f, .2f); mix(MAGENTA, sat = .3f); mix(BLUE, sat = .2f)
        },
        look("Dreamy Pink", "Rosy haze", "Aesthetic", Adjustments(exposure = .15f, tint = .18f, warmth = .05f, fade = .15f, contrast = .92f)) {
            highs(330f, .3f)
        },
        look("Clean", "Bright and natural, just better", "Aesthetic", Adjustments(exposure = .12f, contrast = 1.02f, highlights = -.15f, shadows = .1f, vibrance = .1f, warmth = .04f, saturation = .95f)),
        look("Lavender", "Soft violet mood", "Aesthetic", Adjustments(saturation = .85f, fade = .1f, contrast = .96f, tint = .12f, warmth = -.08f, exposure = .06f)) {
            shadows(270f, .3f); highs(300f, .15f); mix(BLUE, hue = .3f)
        },
        look("Mint", "Fresh green-teal cool", "Aesthetic", Adjustments(warmth = -.14f, tint = -.1f, fade = .08f, exposure = .08f)) {
            shadows(160f, .3f); mix(GREEN, hue = .3f, sat = -.1f)
        },
        look("Cotton Candy", "Pink and blue pastel pop", "Aesthetic", Adjustments(exposure = .12f, contrast = .9f, fade = .12f, saturation = .95f)) {
            shadows(200f, .3f); highs(330f, .28f); mix(MAGENTA, sat = .2f, lum = .1f)
        },

        // ---- Travel ------------------------------------------------------------------------------------
        look("Mediterranean", "Blue sea, warm stone", "Travel", Adjustments(exposure = .08f, contrast = 1.1f, warmth = .12f, vibrance = .25f, highlights = -.12f)) {
            mix(BLUE, hue = -.15f, sat = .5f, lum = -.12f); mix(AQUA, sat = .5f); mix(ORANGE, sat = .15f, lum = .08f); highs(40f, .12f); shadows(205f, .16f)
        },
        look("Tropical", "Turquoise water, lush greens", "Travel", Adjustments(exposure = .08f, vibrance = .4f, warmth = .1f, contrast = 1.06f)) {
            mix(AQUA, hue = .2f, sat = .6f); mix(GREEN, hue = .25f, sat = .35f); mix(BLUE, hue = -.35f, sat = .35f); mix(YELLOW, hue = .1f, sat = .2f)
        },
        look("Tokyo Night", "Cool blue with neon pink", "Travel", Adjustments(contrast = 1.12f, tint = .16f, warmth = -.3f, saturation = 1.1f)) {
            shadows(240f, .35f); highs(320f, .2f); mix(MAGENTA, sat = .25f)
        },
        look("Safari", "Dry golden savanna", "Travel", Adjustments(warmth = .25f, saturation = .9f, contrast = 1.1f, fade = .05f)) {
            mix(YELLOW, hue = -.25f, sat = .1f); mix(GREEN, hue = -.4f, sat = -.2f)
        },
        look("Nordic", "Quiet and cool", "Travel", Adjustments(saturation = .6f, warmth = -.3f, exposure = .14f, fade = .14f, contrast = .95f)) { shadows(210f, .15f) },

        // ---- Food --------------------------------------------------------------------------------------
        look("Fresh", "Bright, appetising colour", "Food", Adjustments(vibrance = .35f, exposure = .18f, warmth = .06f, contrast = 1.08f, highlights = -.15f, shadows = .12f)) {
            mix(GREEN, sat = .25f, lum = .05f); mix(RED, sat = .2f); mix(ORANGE, sat = .15f); mix(YELLOW, sat = .15f)
        },
        look("Warm Kitchen", "Cosy, golden table", "Food", Adjustments(warmth = .22f, vibrance = .15f, shadows = .15f, contrast = 1.05f)),
        look("Café", "Muted, warm and moody", "Food", Adjustments(warmth = .12f, saturation = .85f, fade = .1f, contrast = 1.05f, blacks = -.05f)) {
            shadows(30f, .15f)
        },

        // ---- Night ------------------------------------------------------------------------------------
        look("Neon Nights", "Electric magenta and blue", "Night", Adjustments(saturation = 1.2f, contrast = 1.15f)) {
            shadows(240f, .3f); mix(MAGENTA, sat = .3f); mix(BLUE, sat = .25f); mix(PURPLE, sat = .2f)
        },
        look("Streetlight", "Sodium orange against blue", "Night", Adjustments(warmth = .3f, contrast = 1.12f)) {
            shadows(210f, .32f); mix(ORANGE, sat = .15f)
        },

        // ---- Seasons ----------------------------------------------------------------------------------
        look("Autumn", "Turn the greens to gold", "Seasons", Adjustments(warmth = .15f, contrast = 1.05f)) {
            mix(GREEN, hue = -.6f, sat = -.1f); mix(YELLOW, hue = -.25f, sat = .15f); mix(ORANGE, sat = .25f)
        },
        look("Winter", "Crisp, cold and bright", "Seasons", Adjustments(warmth = -.45f, saturation = .72f, exposure = .14f, fade = .05f)) {
            highs(200f, .2f)
        },
        look("Blossom", "Pink spring colour", "Seasons", Adjustments(vibrance = .2f, exposure = .12f, fade = .05f)) {
            mix(MAGENTA, sat = .3f); mix(RED, hue = .25f, lum = .1f); mix(GREEN, hue = -.2f, sat = .1f)
        },
        look("Summer", "Warm, bright and saturated", "Seasons", Adjustments(warmth = .15f, vibrance = .25f, contrast = 1.08f, exposure = .08f)),

        // ---- Monochrome --------------------------------------------------------------------------------
        look("Noir", "Hard shadows, dark edges", "Monochrome", Adjustments(exposure = .08f, saturation = 0f, contrast = 1.3f, blacks = -.08f, shadows = .1f, vignette = .3f, grain = .15f)) { curve(*softS) },
        look("Tri-X", "Gritty street black and white", "Monochrome", Adjustments(saturation = 0f, contrast = 1.25f, grain = .4f, blacks = -.08f)),
        look("Selenium", "Cool-toned silver print", "Monochrome", Adjustments(saturation = 0f, contrast = 1.12f)) { shadows(222f, .22f); highs(40f, .08f) },
        look("High Key", "Bright, airy monochrome", "Monochrome", Adjustments(saturation = 0f, exposure = .35f, contrast = .9f, highlights = -.1f)),
    )
}
