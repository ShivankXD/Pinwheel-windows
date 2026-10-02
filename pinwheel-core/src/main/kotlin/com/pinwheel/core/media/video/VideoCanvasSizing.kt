package com.pinwheel.core.media.video

import kotlin.math.min
import kotlin.math.roundToInt

/** Preserves the fitted canvas aspect without asking GL for an unsupported texture size. */
fun videoCanvasSize(inputWidth: Int, inputHeight: Int, aspect: Float, maximumTextureSize: Int): Pair<Int, Int> {
    require(inputWidth > 0 && inputHeight > 0 && aspect.isFinite() && aspect > 0f && maximumTextureSize > 0)
    val inputAspect = inputWidth.toDouble() / inputHeight
    val width = if (aspect > inputAspect) inputHeight * aspect.toDouble() else inputWidth.toDouble()
    val height = if (aspect > inputAspect) inputHeight.toDouble() else inputWidth / aspect.toDouble()
    val scale = min(1.0, maximumTextureSize / maxOf(width, height))
    return (width * scale).roundToInt().coerceIn(1, maximumTextureSize) to
        (height * scale).roundToInt().coerceIn(1, maximumTextureSize)
}
