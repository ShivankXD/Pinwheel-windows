package com.pinwheel.core.media

import com.pinwheel.core.model.*
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class PhotoColorRowTest {
    @Test fun compiledRowsMatchIndividualPixelsIncludingAlphaAndCreativeEffects() {
        val input = IntArray(1024) { i -> (i * 1103515245 + 12345) }
        val recipes = listOf(Adjustments(), Adjustments(exposure = -.75f, warmth = .2f, tint = -.4f, fade = .3f),
            Adjustments(mix = List(8) { ColorBand(.3f, -.2f, .4f) }, grain = .7f, vignette = -.4f),
            Adjustments(dehaze = .6f, saturation = .3f, grading = List(3) { ToneGrade(230f, .4f, -.1f) }))
        recipes.forEach { recipe ->
            val transform = PhotoColorTransform(recipe)
            val expected = IntArray(input.size) { transform.pixel(input[it], (it + .5f) / input.size, .31f) }
            val actual = IntArray(input.size)
            transform.row(input, actual, .31f)
            assertArrayEquals(expected, actual)
        }
    }
}
