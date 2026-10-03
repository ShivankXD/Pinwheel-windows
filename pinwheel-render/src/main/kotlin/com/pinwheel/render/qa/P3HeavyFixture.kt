package com.pinwheel.render.qa

import com.pinwheel.core.model.*
import com.pinwheel.core.media.video.*
import com.pinwheel.media.*
import java.nio.file.*
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.util.UUID
import kotlin.random.Random

/** Mobile heavy() data recipe. Missing UI/painter stages are explicitly reported by the caller. */
class P3HeavyFixture(private val root:Path,private val directory:Path) {
    private val demo=root.resolve("assets/demo.mp4").toAbsolutePath()
    private val demoMs=LibavDecoderFactory(root.resolve("native/windows-x64")).open(demo,DecodeConfig(decodeAudio=false)).use { it.description.durationUs/1000 }
    private fun still(color:Int,w:Int=1080,h:Int=1920):Path {
        Files.createDirectories(directory)
        val path=directory.resolve("${UUID.randomUUID()}_still.jpg")
        val image=BufferedImage(w,h,BufferedImage.TYPE_INT_RGB)
        val graphics=image.createGraphics();try { graphics.color=java.awt.Color(color,true);graphics.fillRect(0,0,w,h) } finally { graphics.dispose() }
        check(ImageIO.write(image,"jpg",path.toFile()));return path.toAbsolutePath()
    }
    fun heavy(seed: Int = 7, effectCount: Int = MAX_VIDEO_EFFECTS, pip: Boolean = false): StudioProject {
        val r = Random(seed)
        val v = demo.toUri().toString(); val d = demoMs
        val photoA = still(0xffc83c28.toInt()).toUri().toString(); val photoB = still(0xff1e5ac8.toInt(),1920,1080).toUri().toString()
        val transitions = VideoFxCatalog.transitions.shuffled(r)
        fun video(start: Long, end: Long, speed: Float = 1f, i: Int) = Clip(uri = v, name = "demo.mp4", sourceDurationMs = d, startMs = start, endMs = end,
            video = VideoClipEdits(speed = speed, transition = transitions[i % transitions.size].id, transitionDurationMs = 400L + r.nextLong(800),
                lookId = if (i % 3 == 0) "original" else "vivid", exposure = if (i == 4) .2f else 0f))
        fun photo(uri: String, start: Long, end: Long, i: Int) = Clip(uri = uri, name = "photo.jpg", sourceDurationMs = 3000, startMs = start, endMs = end, muted = true,
            video = VideoClipEdits(transition = transitions[i % transitions.size].id, transitionDurationMs = 600, motion = if (i == 1) "Zoom in" else "None"))
        val clips = listOf(
            video(0, 4000, i = 0), photo(photoA, 0, 1500, 1), photo(photoA, 1500, 3000, 2), video(4000, minOf(8000, d), 2f, 3),
            video(2000, 6000, .5f, 4), photo(photoB, 0, 2000, 5), video(minOf(8000, d - 4000), d, i = 6), video(0, 3000, 1.5f, 7))
        val total = clips.sumOf { it.durationMs }
        val stickerIds = VideoAnimatedStickers.entries.shuffled(r).take(5).map { it.id }
        val layers = buildList {
            VideoFxCatalog.overlays.shuffled(r).take(3).forEachIndexed { i, o ->
                add(VideoImageOverlay(uri = "overlay:" + o.id, name = o.name, startMs = i * 6000L, endMs = i * 6000L + 7000, opacity = .8f, lane = 1 + i)) }
            stickerIds.forEachIndexed { i, s ->
                add(VideoImageOverlay(uri = VideoAnimatedStickers.PREFIX + s, name = s, startMs = i * 4000L, endMs = i * 4000L + 5000,
                    x = .2f + .15f * i, y = .3f + .1f * i, width = .3f, rotation = i * 20, lane = 1 + i % 4)) }
            add(VideoImageOverlay(uri = still(0xffffff00.toInt(),600,600).toUri().toString(), name = "Photo layer", startMs = 3000, endMs = 15000,
                x = .7f, y = .7f, width = .3f, mask = "Heart", lane = 2))
            if (pip) add(VideoImageOverlay(uri = v, name = "Video layer", startMs = 2000, endMs = 9000, x = .3f, y = .25f, width = .4f, mask = "Circle",
                video = VideoOverlaySource(d, startMs = 1000, width = 1280, height = 720), lane = 3))
        }
        val targeted = layers.filter { it.uri.startsWith(VideoAnimatedStickers.PREFIX) || it.video != null }.take(3)
        val catalog = VideoFxCatalog.effects.shuffled(r)
        val effects = (0 until effectCount).map { i ->
            val start = r.nextLong(total - 1000)
            val spec = catalog[i % catalog.size]
            val target = if (i < targeted.size) targeted[i] else null
            if (i % 13 == 12) VideoTimedEffect(kind = VIDEO_EFFECT_KINDS[i % VIDEO_EFFECT_KINDS.size], startMs = start, endMs = start + 1000 + r.nextLong(5000), intensity = .7f, lane = i % 4)
            else VideoTimedEffect(kind = spec.id, startMs = target?.startMs ?: start, endMs = target?.endMs ?: (start + 1000 + r.nextLong(6000)),
                intensity = 1f, params = spec.defaults(), lane = i % 4, target = target?.id ?: "")
        }
        val texts = (0 until 5).map { i -> VideoTextOverlay(text = "Title $i âœ¨", startMs = i * 4500L, endMs = i * 4500L + 4000, y = .15f + .15f * i,
            animation = VIDEO_TEXT_ANIMATIONS[i % VIDEO_TEXT_ANIMATIONS.size], lane = i % 3) }
        val captions = (0 until 12).map { i -> VideoCaptionCue(text = "caption number $i says hello", startMs = i * 2000L, endMs = i * 2000L + 1800) }
        val audio = listOf(
            VideoAudioTrack(uri = v, name = "Extracted Â· demo", sourceDurationMs = d, startMs = 0, volume = 1f),
            VideoAudioTrack(uri = v, name = "Fast", sourceDurationMs = d, sourceStartMs = 1000, sourceEndMs = d, startMs = 6000, volume = .5f, speed = 1.5f, fadeInMs = 800, fadeOutMs = 1200),
            VideoAudioTrack(uri = v, name = "Robot", sourceDurationMs = d, startMs = 15000, volume = 1.4f, voiceEffect = "Robot", reduceNoise = true))
        return StudioProject(name = "Stress", kind = ProjectKind.VIDEO, clips = clips, video = VideoProjectEdits(aspectRatio = "9:16",
            texts = texts, images = layers, audio = audio, captions = captions, effects = effects))
    }

}
