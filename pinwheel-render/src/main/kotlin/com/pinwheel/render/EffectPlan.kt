package com.pinwheel.render

import com.pinwheel.core.media.video.VideoFxCatalog
import com.pinwheel.core.model.*

/** P2 graph slices copied from VideoRenderGraph; clocks use composition microseconds. */
data class EffectPlan(val legacy: List<VideoTimedEffect>, val main: List<VideoTimedEffect>,
    val transitions: List<VideoTimedEffect>, val overlays: List<VideoTimedEffect>,
    val layerEffects: Map<VideoImageOverlay, List<VideoTimedEffect>>) {
    companion object {
        fun from(project: StudioProject): EffectPlan {
            val edit = project.video; val layerIds = edit.images.map { it.id }.toSet()
            val main = edit.effects.filter { it.target.isEmpty() || it.target !in layerIds }
            val cuts = mutableListOf<VideoTimedEffect>(); var cutMs = 0L
            project.clips.forEachIndexed { i, clip ->
                cutMs += clip.durationMs
                if (i == project.clips.lastIndex) return@forEachIndexed
                val spec = VideoFxCatalog.transitionSpec(clip.video.transition) ?: return@forEachIndexed
                val half = minOf(clip.video.transitionDurationMs / 2, clip.durationMs / 2, project.clips[i + 1].durationMs / 2)
                if (half >= 30) cuts += VideoTimedEffect(kind = spec.id, startMs = cutMs - half, endMs = cutMs + half,
                    intensity = 1f, params = mapOf("intensity" to 1f, "speed" to clip.video.transitionSpeed))
            }
            val overlays = edit.images.filter { it.uri.startsWith("overlay:") && it.opacity > 0f }.mapNotNull { layer ->
                val id = layer.uri.removePrefix("overlay:")
                VideoFxCatalog.find(id)?.let { spec -> VideoTimedEffect(id = "ovl-" + layer.id, kind = id,
                    startMs = layer.startMs, endMs = minOf(layer.endMs, project.durationMs),
                    params = spec.defaults() + ("intensity" to (spec.defaults()["intensity"] ?: .85f) * layer.opacity)) }
            }
            val byId = edit.effects.filter { it.target.isNotEmpty() && it.enabled }.groupBy { it.target }
            val layers = edit.images.filter { !it.uri.startsWith("overlay:") && it.video == null && it.id in byId }
                .associateWith { byId.getValue(it.id) }
            // Legacy effects run before catalog passes; Soft Glow runs after main catalog effects.
            // Mobile legacy math counts the first 40 entries in the original list, including catalog entries.
            return EffectPlan(edit.effects, main, cuts, overlays, layers)
        }
    }
}
