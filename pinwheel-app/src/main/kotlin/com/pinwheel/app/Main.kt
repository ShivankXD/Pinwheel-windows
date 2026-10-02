package com.pinwheel.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pinwheel.core.RgbaFrame
import com.pinwheel.media.FfmpegDecoder
import com.pinwheel.platform.AppPaths
import com.pinwheel.render.AngleTriangle
import com.pinwheel.render.TriangleResult
import kotlinx.coroutines.*
import java.awt.Component
import java.awt.Container
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

private val Ink = Color(0xFF07080B)
private val Panel = Color(0xFF1C1F23)
private val Muted = Color(0xFF9AA1AA)
private val Gradient = listOf(Color(0xFF37D6E6), Color(0xFF5B8CFF), Color(0xFFA86BFF))
private val Theme = darkColorScheme(primary = Color(0xFF81CAD4), onPrimary = Ink,
    background = Ink, surface = Panel, onSurface = Color(0xFFF0F0EE),
    onBackground = Color(0xFFF0F0EE), secondary = Color(0xFFB1C8EA), outline = Color(0xFF303338))

private data class Probe(val triangle: TriangleResult, val demo: RgbaFrame, val ffmpeg: String)

fun main(args: Array<String>) {
    val root = Path.of(System.getProperty("pinwheel.home", System.getProperty("user.dir")))
    val smoke = args.firstOrNull() == "--smoke"
    val evidence = if (smoke) Path.of(args[1]) else null
    var smokeFailure: Throwable? = null
    application {
        val state = rememberWindowState(width = 1100.dp, height = 760.dp)
        var intro by remember { mutableStateOf(true) }
        var probe by remember { mutableStateOf<Probe?>(null) }
        var failure by remember { mutableStateOf<String?>(null) }
        var licences by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            runCatching {
                withContext(Dispatchers.IO) {
                    val native = root.resolve("native/windows-x64")
                    val triangle = AngleTriangle.render(native)
                    val decoder = FfmpegDecoder(native)
                    check(!decoder.configuration().contains("--enable-gpl"))
                    Probe(triangle, decoder.frame(root.resolve("assets/demo.mp4"), 1_000_000), decoder.version())
                }
            }.onSuccess { probe = it }.onFailure {
                failure = it.stackTraceToString()
                if (smoke) { smokeFailure = it; System.err.println(failure); exitApplication() }
            }
        }
        Window(onCloseRequest = ::exitApplication, title = "Pinwheel", state = state) {
            MaterialTheme(colorScheme = Theme) {
                Surface(Modifier.fillMaxSize(), color = Ink, contentColor = Theme.onBackground) {
                    if (intro) PinwheelIntro { intro = false }
                    else Workspace(probe, failure, { intro = true }, { licences = true })
                }
                if (licences) AlertDialog(onDismissRequest = { licences = false },
                    title = { Text("Licences") },
                    text = { Text(remember { Files.walk(root.resolve("licenses")).use { paths ->
                        paths.filter { Files.isRegularFile(it) && (it.toString().endsWith(".txt", true) ||
                            it.toString().endsWith(".md", true) || it.fileName.toString().startsWith("COPYING")) }
                            .sorted().map { it.fileName.toString() + "\n" + Files.readString(it) }.toList().joinToString("\n\n")
                    } }, Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) },
                    confirmButton = { TextButton(onClick = { licences = false }) { Text("Done") } })
            }
            if (smoke) LaunchedEffect(Unit) {
                try {
                Files.createDirectories(evidence!!)
                withFrameNanos { }; withFrameNanos { }; delay(1_250)
                capture(window, evidence.resolve("desktop-intro.png"))
                withTimeout(30_000) { while (intro || probe == null) delay(50) }
                withFrameNanos { }; withFrameNanos { }; delay(700)
                check(window.isShowing && window.title == "Pinwheel")
                capture(window, evidence.resolve("desktop-workspace.png"))
                licences = true
                withFrameNanos { }; withFrameNanos { }; delay(500)
                capture(window, evidence.resolve("desktop-licences.png"), dimmed = true)
                val ready = probe!!
                saveFrame(ready.triangle.frame, evidence.resolve("angle-triangle.png"))
                saveFrame(ready.demo, evidence.resolve("demo-frame.png"))
                val summary = "PASS Compose window opened and intro completed\n" +
                    "PASS ${ready.triangle.renderer}\nPASS ${ready.triangle.version}\n" +
                    "PASS FFmpeg decoded demo.mp4 at 1.000000 s: ${ready.demo.width}x${ready.demo.height} RGBA\n" +
                    "PASS ${ready.ffmpeg}\nPASS licences dialog opened with copied notices\n" +
                    "PASS screenshots captured directly from the app's Skia layer\n"
                Files.writeString(evidence.resolve("smoke-summary.txt"), summary)
                println(summary)
                } catch (error: Throwable) {
                    if (error !is CancellationException) {
                        smokeFailure = error
                        System.err.println(error.stackTraceToString())
                    }
                } finally { exitApplication() }
            }
        }
    }
    smokeFailure?.let { throw it }
}

/** Timings and geometry copied from mobile ui/HomeKit.kt; only resource loading changes. */
@Composable private fun PinwheelIntro(onDone: () -> Unit) {
    val appear = remember { Animatable(0f) }
    val spin = rememberInfiniteTransition(label = "fan")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "angle")
    var letters by remember { mutableIntStateOf(0) }
    val word = "Pinwheel"
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        for (i in 1..word.length) { letters = i; delay(55) }
        delay(650); onDone()
    }
    Box(Modifier.fillMaxSize().background(Ink), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PinwheelMark(angle, Modifier.size(150.dp, 180.dp).graphicsLayer {
                val scale = .7f + .3f * appear.value
                scaleX = scale; scaleY = scale; alpha = appear.value
            })
            Spacer(Modifier.height(8.dp))
            Row {
                word.forEachIndexed { i, ch ->
                    val lift by animateFloatAsState(if (i < letters) 0f else 1f,
                        tween(360, easing = FastOutSlowInEasing), label = "l$i")
                    Text(ch.toString(), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.graphicsLayer { alpha = 1f - lift; translationY = lift * 28f })
                }
            }
            Spacer(Modifier.height(10.dp))
            val tag by animateFloatAsState(if (letters >= word.length) 1f else 0f, tween(500), label = "tag")
            Text("Create. Edit. Make it yours.", color = Muted, fontSize = 13.sp,
                modifier = Modifier.graphicsLayer { alpha = tag })
        }
    }
}

@Composable private fun PinwheelMark(angle: Float, modifier: Modifier) {
    val fan = remember { resourceImage("brand/pinwheel_fan.png") }
    val fill = remember { resourceImage("brand/pinwheel_fan_fill.png") }
    Canvas(modifier) {
        val r = minOf(size.width / 2f, size.height / 2.4f)
        val hub = Offset(size.width / 2f, r)
        val sw = r * .105f
        drawRoundRect(Color.White, Offset(hub.x - sw / 2, hub.y + r * .3f), Size(sw, r * 1.1f), CornerRadius(sw / 2, sw / 2))
        val at = IntOffset((hub.x - r).toInt(), (hub.y - r).toInt())
        val dim = IntSize((2 * r).toInt(), (2 * r).toInt())
        rotate(angle, hub) {
            drawImage(fill, dstOffset = at, dstSize = dim, colorFilter = ColorFilter.tint(Ink), filterQuality = FilterQuality.High)
            drawImage(fan, dstOffset = at, dstSize = dim, colorFilter = ColorFilter.tint(Color.White), filterQuality = FilterQuality.High)
        }
    }
}

@Composable private fun Workspace(probe: Probe?, failure: String?, replay: () -> Unit, showLicences: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).padding(28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PinwheelMark(0f, Modifier.size(40.dp, 48.dp))
            Spacer(Modifier.width(14.dp)); Text("Pinwheel", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f)); Text("Windows", color = Muted)
        }
        Spacer(Modifier.height(32.dp))
        Text("Create. Edit. Make it yours.", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(3.dp).background(Brush.horizontalGradient(Gradient)))
        Spacer(Modifier.height(20.dp))
        Text("P0 setup workspace", color = Muted)
        Text("Editing tools arrive in the next phases. This window verifies the desktop foundation.", color = Muted)
        Spacer(Modifier.height(24.dp))
        if (failure != null) Text("Setup failed\n$failure", color = MaterialTheme.colorScheme.error)
        else if (probe == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ProbeCard("ANGLE + LWJGL", probe.triangle.frame, Modifier.weight(1f))
                ProbeCard("FFmpeg demo decode", probe.demo, Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            Text("D3D11 graphics ready  •  Demo frame decoded  •  Kotlin / Compose Desktop", color = Color(0xFF81CAD4))
            Spacer(Modifier.height(8.dp))
            Text(probe.triangle.renderer, color = Muted, fontSize = 12.sp)
            Text("Project storage: ${AppPaths.windows().projects}", color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = replay) { Text("Replay intro") }
            TextButton(onClick = showLicences) { Text("Licences") }
        }
    }
}

@Composable private fun ProbeCard(label: String, frame: RgbaFrame, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            val image = remember(frame) { frameImage(frame) }
            Image(image, label, Modifier.fillMaxWidth().height(170.dp))
            Spacer(Modifier.height(8.dp)); Text("${frame.width} × ${frame.height}", color = Muted, fontSize = 12.sp)
        }
    }
}

private fun resourceImage(name: String): ImageBitmap =
    requireNotNull(Thread.currentThread().contextClassLoader.getResourceAsStream(name)).use {
        org.jetbrains.skia.Image.makeFromEncoded(it.readBytes()).toComposeImageBitmap()
    }

private fun frameImage(frame: RgbaFrame): ImageBitmap {
    val encoded = java.io.ByteArrayOutputStream()
    ImageIO.write(buffered(frame), "png", encoded)
    return org.jetbrains.skia.Image.makeFromEncoded(encoded.toByteArray()).toComposeImageBitmap()
}

private fun buffered(frame: RgbaFrame): BufferedImage = BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_ARGB).apply {
    val rgb = IntArray(frame.width * frame.height) { pixel ->
        val at = pixel * 4
        ((frame.pixels[at + 3].toInt() and 255) shl 24) or ((frame.pixels[at].toInt() and 255) shl 16) or
            ((frame.pixels[at + 1].toInt() and 255) shl 8) or (frame.pixels[at + 2].toInt() and 255)
    }
    setRGB(0, 0, frame.width, frame.height, rgb, 0, frame.width)
}

private fun saveFrame(frame: RgbaFrame, path: Path) { check(ImageIO.write(buffered(frame), "png", path.toFile())) }

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
private fun capture(window: androidx.compose.ui.awt.ComposeWindow, path: Path, dimmed: Boolean = false) {
    // Capture only this application's render surface, even when another window covers it.
    fun findLayer(component: Component): org.jetbrains.skiko.SkiaLayer? {
        if (component is org.jetbrains.skiko.SkiaLayer) return component
        return (component as? Container)?.components?.firstNotNullOfOrNull { findLayer(it) }
    }
    window.renderImmediately()
    val layer = requireNotNull(findLayer(window)) { "Compose Skia layer was not found" }
    requireNotNull(layer.screenshot()) { "Skia layer did not produce a screenshot" }.use { bitmap ->
        org.jetbrains.skia.Image.makeFromBitmap(bitmap).use { image ->
            requireNotNull(image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)).use { png ->
                val bytes = png.bytes
                val decoded = ImageIO.read(java.io.ByteArrayInputStream(bytes))
                check(decoded.width > 600 && decoded.height > 400)
                val background = decoded.getRGB(5, 5) and 0xFFFFFF
                check(if (dimmed) background in 0..0x07080B else background == 0x07080B) {
                    "Screenshot background is not Pinwheel's Ink colour or its dialog scrim"
                }
                Files.write(path, bytes)
            }
        }
    }
}
