package com.pinwheel.core.model

import org.junit.Assert.*
import org.junit.Test

class VideoEditsTest {
    @Test fun titleAnimationDefaultsAndBoundsAreStable() {
        val title = VideoTextOverlay()
        assertEquals("None", title.animation); assertEquals(300L, title.animationDurationMs)
        for (animation in VIDEO_TEXT_ANIMATIONS) {
            val safe = title.copy(animation = animation, animationDurationMs = 700).sanitized()
            assertEquals(animation, safe.animation); assertEquals(700L, safe.animationDurationMs)
        }
        val malformed = title.copy(animation = "unsupported", animationDurationMs = Long.MIN_VALUE).sanitized()
        assertEquals("None", malformed.animation); assertEquals(100L, malformed.animationDurationMs)
        assertEquals(1500L, title.copy(animationDurationMs = Long.MAX_VALUE).sanitized().animationDurationMs)
        assertEquals(300L, title.copy(endMs = 1).sanitized().animationDurationMs) // Renderer clamps effective entrance/exit.
    }
    @Test fun malformedClipControlsCannotReachRenderer() {
        val v = VideoClipEdits(rotation = -450, exposure = Float.NaN, contrast = Float.POSITIVE_INFINITY,
            saturation = -4f, warmth = 4f, lookIntensity = Float.NaN, volume = 50f,
            cropZoom = Float.POSITIVE_INFINITY, cropX = Float.NaN, cropY = -3f).sanitized()
        assertEquals(270, v.rotation)
        assertEquals(0f, v.exposure); assertEquals(0f, v.contrast)
        assertEquals(-1f, v.saturation); assertEquals(1f, v.warmth)
        assertEquals(1f, v.lookIntensity); assertEquals(2f, v.volume)
        assertEquals(1f, v.cropZoom); assertEquals(.5f, v.cropX); assertEquals(0f, v.cropY)
        assertEquals(v, v.sanitized())
    }

    @Test fun textBoundsSurviveOverflowAndInvalidFloatingPoint() {
        val t = VideoTextOverlay(id = "", text = "a".repeat(1000), startMs = Long.MAX_VALUE, endMs = Long.MIN_VALUE,
            x = Float.NaN, y = -8f, size = Float.POSITIVE_INFINITY, color = -1L).sanitized()
        assertTrue(t.id.isNotBlank()); assertEquals(MAX_VIDEO_TEXT_LENGTH, t.text.length)
        assertTrue(t.startMs >= 0); assertTrue(t.endMs > t.startMs)
        assertEquals(.5f, t.x); assertEquals(0f, t.y); assertEquals(.07f, t.size)
        assertEquals(0xffffffffL, t.color)
        assertEquals(t, t.sanitized())
    }

    @Test fun boundedOverlayListsAndCanvasDefaults() {
        val v = VideoProjectEdits(aspectRatio = "999:0", scaleMode = "unknown",
            texts = List(100) { VideoTextOverlay(id = "t$it") },
            images = listOf(VideoImageOverlay(uri = "", name = "Empty"),
                VideoImageOverlay(id = "same", uri = "file:///a.jpg", name = "A", width = 8f, opacity = -.5f),
                VideoImageOverlay(id = "same", uri = "file:///b.jpg", name = "Duplicate"))).sanitized()
        assertEquals("Original", v.aspectRatio); assertEquals("Fit", v.scaleMode)
        assertEquals(MAX_VIDEO_OVERLAYS, v.texts.size); assertEquals(1, v.images.size)
        assertEquals(1f, v.images.single().width); assertEquals(0f, v.images.single().opacity)
        assertEquals(v, v.sanitized())
    }

    @Test fun splitAndHistoryPreserveVideoEdits() {
        val edits = VideoClipEdits(rotation = 90, mirrored = true, exposure = .4f, volume = .6f)
        val c = Clip(uri = "file:///a.mp4", name = "A", sourceDurationMs = 5000, video = edits)
        val (left, right) = c.split(2000)!!
        assertEquals(edits, left.video); assertEquals(edits, right.video)
        val p = StudioProject(name = "Video", kind = ProjectKind.VIDEO, clips = listOf(c),
            video = VideoProjectEdits(texts = listOf(VideoTextOverlay(text = "Opening"))))
        val history = EditHistory(); history.record(p)
        val changed = p.copy(video = p.video.copy(aspectRatio = "9:16"))
        assertEquals(p, history.undo(changed)); assertEquals(changed, history.redo(p))
    }
}
