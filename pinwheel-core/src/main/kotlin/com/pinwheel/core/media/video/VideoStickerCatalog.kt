package com.pinwheel.core.media.video

import kotlin.math.cos
import kotlin.math.sin

/** Original geometric artwork, rendered identically for catalog previews and saved PNG overlays. */
object VideoStickerCatalog {
    data class Entry(val id: String, val name: String, val category: String, val frame: Boolean = false)
    val entries = listOf(
        Entry("arrow", "Arrow", "Marks"), Entry("curved-arrow", "Curve", "Marks"),
        Entry("circle", "Circle", "Marks"), Entry("check", "Check", "Marks"),
        Entry("star", "Star", "Shapes"), Entry("sparkle", "Sparkle", "Shapes"),
        Entry("heart", "Heart", "Shapes"), Entry("pin", "Pin", "Shapes"),
        Entry("corners", "Corners", "Frames", true), Entry("border", "Frame", "Frames", true),
        Entry("focus", "Focus", "Frames", true), Entry("underline", "Underline", "Marks"),
    )
}
