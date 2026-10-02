package com.pinwheel.core.media.video

data class FilterRecipe(
    val id: String, val name: String, val category: String,
    val exposure: Float = 0f, val contrast: Float = 0f, val saturation: Float = 0f, val temperature: Float = 0f,
    val tint: Float = 0f, val fade: Float = 0f, val vibrance: Float = 0f, val curve: Float = 0f,
    val mono: Float = 0f, val hue: Float = 0f, val split: Float = 0f, val bleach: Float = 0f,
    val shadows: Int = 0xFF808080.toInt(), val highlights: Int = 0xFF808080.toInt(),
)

object VideoFilterCatalog {
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
