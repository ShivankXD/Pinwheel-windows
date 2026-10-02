package com.pinwheel.core.media

import kotlin.math.roundToInt

/** Bound both dimensions, including extreme panoramas, without enlarging small inputs. */
fun photoThumbnailSize(width: Int, height: Int, longEdge: Int = 192): Pair<Int, Int> {
    require(width > 0 && height > 0 && longEdge > 0)
    val scale = minOf(1.0, longEdge.toDouble() / maxOf(width, height))
    return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
}
