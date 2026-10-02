package com.pinwheel.core.model

import org.junit.Assert.*
import org.junit.Test

class VideoTimelineTrimTest {
    private val clip = Clip(uri="file:///test.mp4",name="Example",sourceDurationMs=10_000,startMs=1000,endMs=9000,video=VideoClipEdits(speed=2f))
    @Test fun startHandleMapsPlaybackDragToSourceTime() { assertEquals(2000L,timelineTrim(clip,true,500.0).startMs) }
    @Test fun endHandleCanRecoverPreviouslyTrimmedSource() { assertEquals(10_000L,timelineTrim(clip,false,1000.0).endMs) }
    @Test fun handlesCannotCrossOrMakeLessThan100msOfPlayback() {
        assertEquals(100L,timelineTrim(clip,true,20_000.0).durationMs)
        assertEquals(100L,timelineTrim(clip,false,-20_000.0).durationMs)
        assertEquals(0L,timelineTrim(clip,true,-20_000.0).startMs)
    }
    @Test fun invalidGestureCannotCorruptClip() { assertEquals(clip,timelineTrim(clip,true,Double.NaN));assertEquals(clip,timelineTrim(clip,false,Double.POSITIVE_INFINITY)) }
}
