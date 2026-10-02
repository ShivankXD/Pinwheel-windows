package com.pinwheel.core.media

import com.pinwheel.core.model.Adjustments

/** Curated sample artwork and the exact adjustment recipe used after a photo is selected. */
data class PinwheelTemplate(
    val id: Int,
    val name: String,
    val group: String,
    val asset: String,
    val adjustments: Adjustments
)

object PinwheelTemplates {
    private fun item(id: Int, name: String, group: String, look: String, change: (Adjustments) -> Adjustments = { it }): PinwheelTemplate {
        val base = PhotoLooks.builtIn.first { it.name == look }.adjustments
        val extension = if (id in setOf(14, 18, 19, 20, 21, 22, 23)) "jpg" else "webp"
        return PinwheelTemplate(id, name, group, "template_gallery/%02d.%s".format(id, extension), bold(change(base)))
    }

    /** Push every setting further from neutral so each template transforms its photo at a glance. */
    private fun bold(a: Adjustments, k: Float = 1.85f): Adjustments {
        fun z(v: Float, lo: Float, hi: Float) = (v * k).coerceIn(lo, hi)
        fun one(v: Float, lo: Float, hi: Float) = (1f + (v - 1f) * k).coerceIn(lo, hi)
        return a.copy(exposure = z(a.exposure, -.6f, .6f), contrast = one(a.contrast, .6f, 1.6f), saturation = one(a.saturation, 0f, 1.8f),
            warmth = z(a.warmth, -.9f, .9f), highlights = z(a.highlights, -.8f, .8f), shadows = z(a.shadows, -.8f, .8f),
            blacks = z(a.blacks, -.5f, .5f), whites = z(a.whites, -.5f, .5f), tint = z(a.tint, -.6f, .6f), vibrance = z(a.vibrance, -.8f, .8f),
            fade = z(a.fade, 0f, .5f), vignette = z(a.vignette, 0f, .7f), grain = z(a.grain, 0f, .4f))
    }

    val groups = listOf("Editor's picks", "City & travel", "Portrait studio", "Nature & wildlife", "Pets & play")
    val all = listOf(
        item(10, "Old soul", "Editor's picks", "Soft film") { it.copy(warmth = .24f, vignette = .15f) },
        item(18, "Sunday drive", "Editor's picks", "Golden") { it.copy(fade = .13f, grain = .1f) },
        item(1, "After the rain", "Editor's picks", "Nightfall") { it.copy(shadows = .18f, grain = .09f) },
        item(2, "Citrus hour", "Editor's picks", "Amber") { it.copy(exposure = .12f, vibrance = .12f) },
        item(3, "Gilded dome", "City & travel", "Woodland") { it.copy(contrast = 1.16f, warmth = .22f) },
        item(4, "Baroque light", "City & travel", "Amber") { it.copy(blacks = -.13f, contrast = 1.18f) },
        item(5, "Passing birds", "City & travel", "Silver") { it.copy(contrast = 1.24f, grain = .17f) },
        item(8, "Quiet passage", "City & travel", "Golden") { it.copy(highlights = -.28f, shadows = .2f) },
        item(9, "Forest road", "City & travel", "Woodland") { it.copy(shadows = .19f, fade = .07f) },
        item(13, "Wet pavement", "City & travel", "Cool print") { it.copy(dehaze = .11f, contrast = 1.04f) },
        item(14, "Alpine village", "City & travel", "Alpine") { it.copy(exposure = .1f, shadows = .22f) },
        item(15, "Paris morning", "City & travel", "Soft film") { it.copy(exposure = .14f, warmth = .2f) },
        item(16, "Métro", "City & travel", "Graphite") { it.copy(saturation = .77f, contrast = 1.13f) },
        item(17, "Paris walk", "City & travel", "Paper") { it.copy(saturation = .72f, warmth = .13f) },
        item(11, "Blue eyes", "Portrait studio", "Window") { it.copy(contrast = 1.02f, highlights = -.23f) },
        item(12, "Pearl", "Portrait studio", "Portrait") { it.copy(fade = .12f, warmth = .2f) },
        item(6, "Still water", "Nature & wildlife", "Soft glow") { it.copy(saturation = .83f, highlights = -.24f) },
        item(7, "Wild green", "Nature & wildlife", "Woodland") { it.copy(vibrance = .28f, dehaze = .13f) },
        item(20, "Vintage drive", "Nature & wildlife", "Soft film") { it.copy(vignette = .22f, warmth = .28f) },
        item(19, "Street cats", "Pets & play", "Cool print") { it.copy(grain = .17f, contrast = 1.04f) },
        item(21, "Flower friend", "Pets & play", "Daylight") { it.copy(vibrance = .24f, exposure = .09f) },
        item(22, "Daisy doggo", "Pets & play", "Golden") { it.copy(exposure = .18f, warmth = .27f) },
        item(23, "Road trip", "Pets & play", "Coast") { it.copy(vibrance = .19f, shadows = .26f) },
        item(24, "Dapper dog", "Pets & play", "Amber") { it.copy(contrast = 1.13f, fade = .11f) },
        item(25, "Mountain wanderer", "Pets & play", "Daylight") { it.copy(contrast = 1.1f, vibrance = .21f) }
    )
}
