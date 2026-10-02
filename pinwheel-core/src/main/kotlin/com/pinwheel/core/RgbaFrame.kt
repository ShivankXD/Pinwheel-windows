package com.pinwheel.core

/** Headless boundary: straight-alpha sRGB8, row zero at the top. No UI or Android types. */
data class RgbaFrame(val width: Int, val height: Int, val pixels: ByteArray) {
    init {
        require(width in 1..16384 && height in 1..16384)
        require(pixels.size.toLong() == width.toLong() * height * 4)
    }
}
