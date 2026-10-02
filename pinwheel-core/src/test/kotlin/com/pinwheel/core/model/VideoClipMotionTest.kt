package com.pinwheel.core.model
import org.junit.Assert.*
import org.junit.Test

class VideoClipMotionTest {
    private fun clip(kind: String, speed: Float = 1f) = Clip(uri="file:///a.mp4",name="Clip",sourceDurationMs=10000,startMs=2000,endMs=6000,video=VideoClipEdits(motion=kind,motionAmount=1f,speed=speed))
    @Test fun zoomUsesTrimmedPlaybackDurationAndResetsAtNextClip() {
        val motion=VideoClipMotion(listOf(clip("Zoom in",2f),clip("Zoom out")))
        assertEquals(1f,motion.at(0).scale,.0001f)
        assertEquals(1.175f,motion.at(1_000_000).scale,.0001f)
        assertTrue(motion.at(1_999_999).scale>1.349f)
        assertEquals(1.35f,motion.at(2_000_000).scale,.0001f)
        assertEquals(1f,motion.at(6_000_000).scale,0f)
    }
    @Test fun panNeverExposesOutsideFrameAndIsDeterministicWhenSeeking() {
        for(kind in listOf("Pan left","Pan right","Pan up","Pan down")) {
            val motion=VideoClipMotion(listOf(clip(kind)))
            for(t in 0L..3_999_999L step 33_333) {
                val f=motion.at(t)
                assertTrue(kotlin.math.abs(f.x)<=f.scale-1f+.00001f)
                assertTrue(kotlin.math.abs(f.y)<=f.scale-1f+.00001f)
                assertEquals(f,motion.at(t))
            }
            assertEquals(0f,motion.at(2_000_000).x,.0001f)
            assertEquals(0f,motion.at(2_000_000).y,.0001f)
        }
    }
    @Test fun unknownAndNonFiniteSettingsSanitizeWithoutDamagingExistingEdits() {
        assertEquals("None",VideoClipEdits(motion="unknown").sanitized().motion)
        assertEquals(.35f,VideoClipEdits(motionAmount=Float.NaN).sanitized().motionAmount,0f)
        assertEquals(VideoClipMotion.Frame(),VideoClipMotion(listOf(clip("None"))).at(1_000_000))
        assertEquals(VideoClipMotion.Frame(),VideoClipMotion(listOf(clip("Zoom in"))).at(-1))
    }
}
