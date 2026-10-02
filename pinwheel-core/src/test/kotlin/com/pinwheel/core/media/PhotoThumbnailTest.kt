package com.pinwheel.core.media

import org.junit.Assert.*
import org.junit.Test

class PhotoThumbnailTest {
    @Test fun extremeAspectRatiosCannotAllocateAnUnboundedThumbnail() {
        assertEquals(1 to 192, photoThumbnailSize(1, 50000))
        assertEquals(192 to 1, photoThumbnailSize(50000, 1))
        assertEquals(1 to 192, photoThumbnailSize(1, Int.MAX_VALUE))
        assertEquals(192 to 192, photoThumbnailSize(Int.MAX_VALUE, Int.MAX_VALUE))
    }

    @Test fun normalThumbnailsKeepAspectRatioAndNeverEnlargeSmallInputs() {
        assertEquals(192 to 128, photoThumbnailSize(6000, 4000))
        assertEquals(108 to 192, photoThumbnailSize(1080, 1920))
        assertEquals(80 to 120, photoThumbnailSize(80, 120))
        assertEquals(1 to 1, photoThumbnailSize(1, 1))
    }
}
