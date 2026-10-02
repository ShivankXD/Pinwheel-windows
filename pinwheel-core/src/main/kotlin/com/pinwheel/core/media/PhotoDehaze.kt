package com.pinwheel.core.media

/**
 * Conservative SDR atmospheric-veil correction. The darkest channel bounds the
 * removable white veil; a 0.65 strength cap keeps transmission above 0.35 and
 * avoids inventing clipped blacks. Applying the same affine map to all three
 * channels preserves hue and white highlights while restoring colour contrast.
 *
 * This deliberately uses no spatial estimation or learned scene depth: its
 * output is independent of preview resolution and requires no image-sized cache.
 * Negative strength adds a white veil for a soft, misted look.
 */
class PhotoDehaze(amount: Float) {
    private val strength = amount.takeIf { it.isFinite() }?.coerceIn(-1f, 1f) ?: 0f
    val active = strength != 0f

    fun veil(red: Float, green: Float, blue: Float): Float =
        if (strength > 0f) minOf(red, green, blue).coerceIn(0f, 1f) * strength * .65f
        else strength * .30f

    fun channel(value: Float, veil: Float): Float =
        if (veil >= 0f) ((value - veil) / (1f - veil)).coerceIn(0f, 1f)
        else (value * (1f + veil) - veil).coerceIn(0f, 1f)
}
