package com.pinwheel.app.debug

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pinwheel.core.model.*
import com.pinwheel.media.*
import com.pinwheel.render.GpuFrameEvaluator
import com.pinwheel.render.qa.FrameImages
import kotlinx.coroutines.*
import java.awt.Component
import java.awt.Container
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

/** Real hardware PCM output and engine controls, confined to the debug source set. */
@Composable fun PlayerLabWindow(onClose: () -> Unit, smokeDirectory: Path? = null, onSmokeFailure: (Throwable) -> Unit = {}) {
    val root = Path.of(System.getProperty("pinwheel.home", System.getProperty("user.dir")))
    val project = remember {
        StudioProject(name = "P3 PIP playback", kind = ProjectKind.VIDEO,
            clips = listOf(Clip(uri = root.resolve("assets/p3/pip-base.mp4").toUri().toString(), name = "Blue main clip", sourceDurationMs = 4000, muted = true)),
            video = VideoProjectEdits(images = listOf(VideoImageOverlay(uri = root.resolve("assets/p3/pip-layer.mp4").toUri().toString(), name = "Moving PIP",
                startMs = 1000, endMs = 3000, x = .7f, y = .4f, width = .5f, mask = "Circle",
                video = VideoOverlaySource(3000, startMs = 500, width = 160, height = 120))),
                audio = listOf(VideoAudioTrack(uri = root.resolve("assets/demo.mp4").toUri().toString(), name = "Demo audio", sourceDurationMs = 30000,
                    sourceEndMs = 4000, volume = .25f, fadeOutMs = 100))))
    }
    val openedAt = remember { System.nanoTime() }
    val player = remember { VideoPlayer(project, { GpuFrameEvaluator(root.resolve("native/windows-x64"), it, 320, 240) }, LibavDecoderFactory(root.resolve("native/windows-x64"))) }
    var status by remember { mutableStateOf(player.status) }
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var displayed by remember { mutableLongStateOf(-1) }
    DisposableEffect(player) { onDispose { player.close() } }
    LaunchedEffect(player) {
        while (isActive) {
            status = player.status
            player.latestFrame?.takeIf { status.renderedFrames != displayed && it.generation == status.generation }?.let { frame ->
                val encoded = ByteArrayOutputStream(); check(ImageIO.write(FrameImages.buffered(frame.pixels), "png", encoded))
                bitmap = org.jetbrains.skia.Image.makeFromEncoded(encoded.toByteArray()).toComposeImageBitmap()
                displayed = status.renderedFrames
            }
            delay(33)
        }
    }
    Window(onCloseRequest = onClose, title = "Pinwheel Player Lab", state = rememberWindowState(width = 900.dp, height = 680.dp)) {
        MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF37D6E6), background = Color(0xFF07080B), surface = Color(0xFF1C1F23))) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Player Lab", style = MaterialTheme.typography.headlineMedium)
                    Text("Main clip + moving circular PIP + audio")
                    Card(Modifier.fillMaxWidth()) {
                        Box(Modifier.fillMaxWidth().height(340.dp)) {
                            bitmap?.let { Image(it, "Engine playback preview", Modifier.fillMaxSize()) }
                        }
                    }
                    Text("%.3f s / %.3f s".format(java.util.Locale.ROOT, status.timeUs / 1e6, project.durationMs / 1e3))
                    Slider(value = (status.timeUs / 1e6).toFloat(), onValueChange = { player.seek((it * 1e6).toLong()) }, valueRange = 0f..project.durationMs / 1e3f)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { if (status.playing) player.pause() else player.play() }, enabled = status.error == null) { Text(if (status.playing) "Pause" else "Play") }
                        OutlinedButton(onClick = { player.seek(1_400_000) }, enabled = status.error == null) { Text("Seek to 1.4 s") }
                        OutlinedButton(onClick = { player.seek(0) }, enabled = status.error == null) { Text("Restart") }
                        Text("${status.renderedFrames} frames   ${status.droppedFrames} dropped")
                    }
                    status.error?.let { Text(it.stackTraceToString(), color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        if (smokeDirectory != null) LaunchedEffect(Unit) {
            try {
                withTimeout(30_000) { while (!player.status.ready) { player.status.error?.let { throw it }; delay(25) } }
                val firstFrameMs = (System.nanoTime() - openedAt) / 1e6
                player.play()
                withTimeout(15_000) { while (player.status.timeUs < 2_600_000) { player.status.error?.let { throw it }; delay(25) } }
                player.pause()
                val seekStart = System.nanoTime(); val generation = player.seek(1_400_000)
                withTimeout(5_000) { while (player.latestFrame?.generation != generation) { player.status.error?.let { throw it }; delay(10) } }
                val seekMs = (System.nanoTime() - seekStart) / 1e6
                val frame = requireNotNull(player.latestFrame)
                val center = (0..2).map { frame.pixels.pixels[(96 * 320 + 224) * 4 + it].toInt() and 255 }
                check(center[0] > 190 && center[1] < 65 && center[2] < 65) { "PIP seek pixel: $center" }
                withFrameNanos { }; withFrameNanos { }; delay(300)
                Files.createDirectories(smokeDirectory); capturePlayerLab(window, smokeDirectory.resolve("player-lab.png"))
                FrameImages.write(frame.pixels, smokeDirectory.resolve("frames/pip/1.4.png"))
                val result = org.json.JSONObject().put("output", "JavaSound SourceDataLine, 48 kHz stereo, real consumed PCM frame clock")
                    .put("playedToUs", 2_600_000).put("seekTimeUs", frame.timeUs).put("firstFrameMs", firstFrameMs).put("seekMs", seekMs)
                    .put("centerRgb", center).put("renderedFrames", player.status.renderedFrames).put("droppedFrames", player.status.droppedFrames)
                    .put("scope", "Debug engine window, not P4 editor UI parity or Iris Xe qualification")
                Files.writeString(smokeDirectory.resolve("player-lab-smoke.json"), result.toString(2) + "\n")
                println("PASS real Player Lab, hardware audio clock, playback through 2.6 s, accurate paused PIP seek; firstFrameMs=$firstFrameMs seekMs=$seekMs")
            } catch (failure: Throwable) { onSmokeFailure(failure) } finally { onClose() }
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
private fun capturePlayerLab(window: androidx.compose.ui.awt.ComposeWindow, path: Path) {
    fun layer(component: Component): org.jetbrains.skiko.SkiaLayer? =
        if (component is org.jetbrains.skiko.SkiaLayer) component else (component as? Container)?.components?.firstNotNullOfOrNull { layer(it) }
    window.renderImmediately()
    requireNotNull(layer(window)).screenshot()!!.use { bitmap -> org.jetbrains.skia.Image.makeFromBitmap(bitmap).use { image ->
        requireNotNull(image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)).use { Files.write(path, it.bytes) }
    } }
}

fun main(args: Array<String>) {
    var failure: Throwable? = null
    application(exitProcessOnExit = false) { PlayerLabWindow(::exitApplication, args.firstOrNull()?.let(Path::of), { failure = it }) }
    failure?.let { throw it }
}
