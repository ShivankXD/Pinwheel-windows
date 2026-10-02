package com.pinwheel.app.debug

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pinwheel.core.media.video.*
import com.pinwheel.render.PreviewTileRenderer
import com.pinwheel.render.qa.*
import kotlinx.coroutines.*
import java.awt.Component
import java.awt.Container
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import javax.imageio.ImageIO

private data class LabFrame(val actual: ImageBitmap, val mobile: ImageBitmap?, val diff: ImageBitmap?, val metrics: String)

/** Development comparison UI. No release source set contains this window or its entry point. */
@Composable fun EffectLabWindow(onClose: () -> Unit, smokeDirectory: Path? = null, onSmokeFailure: (Throwable) -> Unit = {}) {
    val root = Path.of(System.getProperty("pinwheel.home", System.getProperty("user.dir")))
    val refs = Path.of(System.getProperty("pinwheel.refs", "D:/Pinwheel-Windows-refs"))
    val specs = remember { PreviewTileRenderer.catalogSpecs() }
    val noiseIds = remember { org.json.JSONArray(Files.readString(root.resolve("docs/p2-noise-classification.json"))).let { rows ->
        (0 until rows.length()).map { rows.getJSONObject(it) }.filter { it.getString("class") == "noise-driven" }.map { it.getString("id") }.toSet()
    } }
    val noisePolicy = remember { org.json.JSONObject(Files.readString(root.resolve("docs/p2-noise-policy.json"))) }
    var index by remember { mutableIntStateOf(specs.indexOfFirst { it.id == "fx-ct-scene-cut" }) }
    var time by remember { mutableFloatStateOf(.9f) }
    var search by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var values by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var animate by remember { mutableStateOf(false) }
    var loopFrame by remember { mutableIntStateOf(0) }
    var frame by remember { mutableStateOf<LabFrame?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    val dispatcher = remember { Executors.newSingleThreadExecutor { Thread(it, "pinwheel-effect-lab").apply { isDaemon = true } }.asCoroutineDispatcher() }
    val runtime = remember { arrayOfNulls<PreviewTileRenderer>(1) }
    DisposableEffect(Unit) {
        onDispose { CoroutineScope(dispatcher).launch { try { runtime[0]?.close() } finally { dispatcher.close() } } }
    }
    val spec = specs[index]
    LaunchedEffect(animate) {
        while (animate) { loopFrame = (loopFrame + 1) % VideoPreviewRecipe.FRAMES; delay(150) }
    }
    LaunchedEffect(spec.id, time, values, animate, loopFrame) {
        failure = null
        try {
            frame = withContext(dispatcher) {
                val renderer = runtime[0] ?: PreviewTileRenderer(root.resolve("native/windows-x64"), root.resolve("assets")).also { runtime[0] = it }
                val seconds = if (animate) loopFrame * VideoPreviewRecipe.LOOP_SECONDS / VideoPreviewRecipe.FRAMES else time
                val actual = if (animate && values.isEmpty()) renderer.frame(spec, loopFrame) else renderer.renderAt(spec, seconds, values)
                val t = VideoPreviewRecipe.goldenTimes.firstOrNull { it.toFloat() == seconds }
                val path = t?.let { refs.resolve("golden/${spec.id}/$it.png") }
                val reference = path?.takeIf(Files::isRegularFile)?.let { requireNotNull(ImageIO.read(it.toFile())) }
                val diff = root.resolve("evidence/p2/lab/${spec.id}/${t ?: "loop"}.png")
                val result = reference?.let { GoldenImages.compare(it, FrameImages.buffered(actual), diff) }
                val strictMetrics = result?.let { r -> (if (r.passed) "STRICT PASS" else "STRICT FAIL") + "  " +
                    r.channels.mapIndexed { c, metric -> "${"RGBA"[c]} MAE=%.3f p99=%d".format(java.util.Locale.ROOT, metric.meanAbsoluteError, metric.percentile99) }.joinToString("  ") }
                    ?: "Mobile reference unavailable for this time. Golden comparisons use the four fixed times."
                val structural = if (spec.id in noiseIds && reference != null) StructuralGoldens.compare(reference, FrameImages.buffered(actual)) else null
                val limits = StructuralGoldens.Limits(noisePolicy.getDouble("perChannelBlurredMae8Max"), noisePolicy.getDouble("perChannelHistogramWasserstein8Max"),
                    noisePolicy.getDouble("perChannelMeanError8Max"), noisePolicy.getDouble("meanLuminanceError8Max"))
                val metrics = strictMetrics + (structural?.let { s -> "\nNOISE STRUCTURE ${if (s.passes(limits)) "PROVISIONAL PASS" else "FAIL"}  " +
                    s.channels.mapIndexed { c, m -> "${"RGBA"[c]} blur=%.3f hist=%.3f mean=%.3f".format(java.util.Locale.ROOT, m.blurredMae8, m.histogramWasserstein8, m.meanError8) }.joinToString("  ") } ?: "")
                fun bitmap(image: java.awt.image.BufferedImage): ImageBitmap {
                    val stream = java.io.ByteArrayOutputStream(); check(ImageIO.write(image, "png", stream))
                    return org.jetbrains.skia.Image.makeFromEncoded(stream.toByteArray()).toComposeImageBitmap()
                }
                LabFrame(bitmap(FrameImages.buffered(actual)), reference?.let(::bitmap), result?.let { bitmap(ImageIO.read(diff.toFile())) }, metrics)
            }
        } catch (e: Exception) { if (e is CancellationException) throw e; failure = e.stackTraceToString() }
    }
    Window(onCloseRequest = onClose, title = "Pinwheel Effect Lab", state = rememberWindowState(width = 1220.dp, height = 840.dp)) {
        MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF81CAD4), background = Color(0xFF07080B), surface = Color(0xFF1C1F23))) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Effect Lab", style = MaterialTheme.typography.headlineMedium)
                    Text("${spec.name}  •  ${spec.id}  •  ${spec.category}  •  ${if (spec.id in noiseIds) "noise-driven" else "deterministic"}")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(search, { search = it }, label = { Text("Search catalog") }, singleLine = true)
                        Box {
                            Button(onClick = { menu = true }) { Text("Select effect (${index + 1}/${specs.size})") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                                specs.forEachIndexed { i, item -> if (search.isBlank() || item.name.contains(search, true) || item.id.contains(search, true)) {
                                    DropdownMenuItem(text = { Text("${item.name} (${item.id})") }, onClick = { index = i; values = emptyMap(); menu = false })
                                } }
                            }
                        }
                        OutlinedButton(onClick = { index = (index - 1).mod(specs.size); values = emptyMap() }) { Text("Previous") }
                        OutlinedButton(onClick = { index = (index + 1) % specs.size; values = emptyMap() }) { Text("Next") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        VideoPreviewRecipe.goldenTimes.forEach { t -> FilterChip(selected = !animate && time == t.toFloat(), onClick = { time = t.toFloat(); animate = false }, label = { Text("$t s") }) }
                        FilterChip(selected = animate, onClick = { animate = !animate }, label = { Text("Loop preview") })
                        TextButton(onClick = { values = emptyMap() }) { Text("Reset parameters") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Comparison("Mobile reference", frame?.mobile, Modifier.weight(1f))
                        Comparison("Desktop ANGLE", frame?.actual, Modifier.weight(1f))
                        Comparison("Strict pixel difference ×16", frame?.diff, Modifier.weight(1f))
                    }
                    Text(frame?.metrics ?: "Rendering…")
                    Text("Deterministic limits: MAE ≤ 2/255, p99 ≤ 8/255. Noise: Gaussian sigma 8 px, histogram and mean metrics for owner review. Custom sliders do not change golden files.")
                    failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    spec.params.forEach { param ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(param.label, Modifier.width(140.dp))
                            Slider(value = values[param.key] ?: param.default, onValueChange = { values = values + (param.key to it) }, modifier = Modifier.width(350.dp))
                            Text("%.2f".format(java.util.Locale.ROOT, values[param.key] ?: param.default))
                        }
                    }
                }
            }
        }
        if (smokeDirectory != null) LaunchedEffect(Unit) {
            try {
                withTimeout(60_000) { while (frame == null && failure == null) delay(50) }
                check(failure == null) { failure ?: "Effect Lab render failed" }
                check(frame!!.mobile != null && frame!!.diff != null) { "Effect Lab smoke needs real mobile references" }
                withFrameNanos { }; withFrameNanos { }; delay(400)
                Files.createDirectories(smokeDirectory); captureLab(window, smokeDirectory.resolve("effect-lab.png"))
                Files.writeString(smokeDirectory.resolve("effect-lab-smoke.txt"), "PASS real Effect Lab window, mobile/desktop/heatmap panels, fixed metrics\n${frame!!.metrics}\n")
            } catch (e: Throwable) { onSmokeFailure(e) } finally { onClose() }
        }
    }
}

@Composable private fun Comparison(label: String, image: ImageBitmap?, modifier: Modifier) {
    Card(modifier) { Column(Modifier.padding(12.dp)) {
        Text(label); Spacer(Modifier.height(8.dp))
        if (image == null) Box(Modifier.fillMaxWidth().height(340.dp)) { Text("No reference at this time") }
        else Image(image, label, Modifier.fillMaxWidth().height(340.dp))
    } }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
private fun captureLab(window: androidx.compose.ui.awt.ComposeWindow, path: Path) {
    fun layer(component: Component): org.jetbrains.skiko.SkiaLayer? =
        if (component is org.jetbrains.skiko.SkiaLayer) component else (component as? Container)?.components?.firstNotNullOfOrNull { layer(it) }
    window.renderImmediately()
    requireNotNull(layer(window)).screenshot()!!.use { bitmap -> org.jetbrains.skia.Image.makeFromBitmap(bitmap).use { image ->
        requireNotNull(image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)).use { Files.write(path, it.bytes) }
    } }
}

fun main(args: Array<String>) {
    var failure: Throwable? = null
    application(exitProcessOnExit = false) { EffectLabWindow(::exitApplication, args.firstOrNull()?.let(Path::of), { failure = it }) }
    failure?.let { throw it }
}
