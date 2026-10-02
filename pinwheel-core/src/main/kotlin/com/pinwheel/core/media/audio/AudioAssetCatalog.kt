package com.pinwheel.core.media.audio

import java.io.File
import java.util.UUID

/** Audio bundled with the app. All source pages and license notices are in assets/audio_catalog/LICENSES.md. */
data class AudioCatalogEntry(
    val id: String,
    val name: String,
    val category: String,
    val assetPath: String,
    val durationMs: Long,
    val author: String,
    val sourceUrl: String,
)

object AudioAssetCatalog {
    val entries: List<AudioCatalogEntry> = listOf(
        AudioCatalogEntry("lofi-again", "Lofi Again", "Music", "audio_catalog/lofiagain.ogg", 87809,
            "omfgdude", "https://opengameart.org/content/lofi-again"),
        AudioCatalogEntry("upbeat-theme", "Upbeat Theme", "Music", "audio_catalog/title_in_game.ogg", 29327,
            "beardalaxy", "https://opengameart.org/content/upbeat-title-theme-loop"),
        AudioCatalogEntry("ambient-ish", "Ambient Atmosphere", "Music", "audio_catalog/ambient-ish.ogg", 22572,
            "frosty ham", "https://opengameart.org/content/ambient-ish-stuff"),
        AudioCatalogEntry("door-bell", "Door Bell", "Sound effects", "audio_catalog/door_bell.ogg", 3700,
            "Amada44", "https://commons.wikimedia.org/wiki/File:Sound_Effect_-_Door_Bell.ogg"),
        AudioCatalogEntry("camera-shutter", "Camera Shutter", "Sound effects", "audio_catalog/photo.ogg", 880,
            "themightyglider", "https://opengameart.org/content/camera"),
        AudioCatalogEntry("power-up", "Power Up", "Sound effects", "audio_catalog/power_up_sound_v1.ogg", 3609,
            "Julie Damsgaard / Spring Spring", "https://opengameart.org/content/power-up-sound-effects"),
        AudioCatalogEntry("wind-whoosh", "Wind Whoosh", "Sound effects", "audio_catalog/wind_woosh_loop.ogg", 5959,
            "SketchMan3", "https://opengameart.org/content/wind-whoosh-loop"),
    )

}
