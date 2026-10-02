package com.pinwheel.core.media.video

import org.junit.Assert.*
import org.junit.Test

class VideoCanvasSizingTest {
    @Test fun landscapeFourKIntoPortraitIsBoundedByActualDeviceLimit() {
        assertEquals(2304 to 4096, videoCanvasSize(3840, 2160, 9f / 16f, 4096))
        assertEquals(1152 to 2048, videoCanvasSize(3840, 2160, 9f / 16f, 2048))
    }
    @Test fun ordinaryCanvasPreservesOriginalPixelsWhenDeviceCanFitIt() {
        assertEquals(1920 to 1920, videoCanvasSize(1920, 1080, 1f, 4096))
        assertEquals(1920 to 1080, videoCanvasSize(1920, 1080, 16f / 9f, 4096))
        assertEquals(3413 to 1920, videoCanvasSize(1080, 1920, 16f / 9f, 4096))
    }
    @Test fun extremeInputDoesNotOverflowBeforeClamping() {
        val (width, height) = videoCanvasSize(Int.MAX_VALUE, Int.MAX_VALUE, .01f, 4096)
        assertEquals(41, width); assertEquals(4096, height)
        assertEquals(1 to 1, videoCanvasSize(1, 1, 1f, 4096))
    }
}
