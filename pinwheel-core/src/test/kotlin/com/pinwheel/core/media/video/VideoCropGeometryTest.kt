package com.pinwheel.core.media.video

import com.pinwheel.core.model.VideoClipEdits
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoCropGeometryTest {
    @Test fun centeredCropKeepsAspectAndBounds() {
        val crop = VideoCropGeometry.from(VideoClipEdits(cropZoom = 2f))
        assertEquals(-.5f, crop.left, .0001f)
        assertEquals(.5f, crop.right, .0001f)
        assertEquals(-.5f, crop.bottom, .0001f)
        assertEquals(.5f, crop.top, .0001f)
    }

    @Test fun cropAnchorReachesTopRightAndSanitizesInvalidInput() {
        val crop = VideoCropGeometry.from(VideoClipEdits(cropZoom = 4f, cropX = 1f, cropY = 0f))
        assertEquals(.5f, crop.left, .0001f)
        assertEquals(1f, crop.right, .0001f)
        assertEquals(.5f, crop.bottom, .0001f)
        assertEquals(1f, crop.top, .0001f)
        val safe = VideoCropGeometry.from(VideoClipEdits(cropZoom = Float.NaN, cropX = -10f))
        assertEquals(-1f, safe.left, .0001f)
        assertEquals(1f, safe.right, .0001f)
    }
}
