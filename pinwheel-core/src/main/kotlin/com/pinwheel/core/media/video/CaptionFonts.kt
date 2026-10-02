package com.pinwheel.core.media.video


/**
 * Bundled OFL/Apache display faces for animated caption templates (see assets/caption_fonts).
 * Typefaces load lazily on first use; before [install] runs a system family is substituted.
 */
object CaptionFonts {
    val files = mapOf(
        "anton" to "Anton-Regular.ttf", "bebas" to "BebasNeue-Regular.ttf", "bangers" to "Bangers-Regular.ttf",
        "luckiest" to "LuckiestGuy-Regular.ttf", "marker" to "PermanentMarker-Regular.ttf", "pacifico" to "Pacifico-Regular.ttf",
        "righteous" to "Righteous-Regular.ttf", "pixel" to "PressStart2P-Regular.ttf", "poppins-black" to "Poppins-Black.ttf",
        "poppins-italic" to "Poppins-ExtraBoldItalic.ttf", "poppins-bold" to "Poppins-Bold.ttf", "dmserif" to "DMSerifDisplay-Regular.ttf",
        "bungee" to "Bungee-Regular.ttf", "titan" to "TitanOne-Regular.ttf", "creepster" to "Creepster-Regular.ttf",
        "lobster" to "Lobster-Regular.ttf", "monoton" to "Monoton-Regular.ttf",
    )
    val fallbacks = mapOf(
        "anton" to "sans-serif-condensed", "bebas" to "sans-serif-condensed", "pacifico" to "cursive", "lobster" to "cursive",
        "marker" to "casual", "dmserif" to "serif", "pixel" to "monospace",
    )
    val keys: Set<String> get() = files.keys
}
