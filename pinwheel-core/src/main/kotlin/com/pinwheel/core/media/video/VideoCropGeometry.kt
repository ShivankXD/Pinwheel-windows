package com.pinwheel.core.media.video

import com.pinwheel.core.model.VideoClipEdits

/** Maps a source-relative crop window into Media3's bottom-left GL coordinates. */
data class VideoCropGeometry(val left: Float, val right: Float, val bottom: Float, val top: Float) {
    companion object {
        fun from(rawEdit: VideoClipEdits): VideoCropGeometry {
            val edit = rawEdit.sanitized()
            val extent = 1f / edit.cropZoom
            val x = (1f - extent) * edit.cropX
            val y = (1f - extent) * edit.cropY
            return VideoCropGeometry(
                left = -1f + 2f * x,
                right = -1f + 2f * (x + extent),
                bottom = 1f - 2f * (y + extent),
                top = 1f - 2f * y,
            )
        }
    }
}
