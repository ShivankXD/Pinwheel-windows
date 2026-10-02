package com.nativeoffice.studio.media.video

import android.opengl.GLES20
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Proposed owner-run diagnostic for mobile commit 2a417fd. Not built or executed by Windows. */
@RunWith(AndroidJUnit4::class)
class P2ParityProbe {
    /** Observe storage on the GPU with endpoint colours; a readback tie cannot change the branch. */
    private fun storageObservations(texture: Int, sourceFbo: Int, writer: FxProgram): JSONArray {
        val results = JSONArray(); val textures = IntArray(1); val fbos = IntArray(1)
        val clearColour = FloatArray(4); GLES20.glGetFloatv(GLES20.GL_COLOR_CLEAR_VALUE, clearColour, 0)
        val reader = FxProgram("""
precision highp float;
varying highp vec2 vUv;
uniform sampler2D uTexture;
void main() {
    float stored = texture2D(uTexture, vUv).r;
    gl_FragColor = vec4(stored < 0.5 ? 1.0 : 0.0, stored > 0.5 ? 1.0 : 0.0, stored == 0.5 ? 1.0 : 0.0, 1.0);
}
""")
        try {
            GLES20.glGenTextures(1, textures, 0); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 192, 240, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
            GLES20.glGenFramebuffers(1, fbos, 0); GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbos[0])
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textures[0], 0)
            check(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) == GLES20.GL_FRAMEBUFFER_COMPLETE)
            val raw = ByteBuffer.allocateDirect(192 * 240 * 4).order(ByteOrder.nativeOrder())
            for (origin in listOf("upload-127", "upload-128", "clear-half", "draw-half")) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, sourceFbo)
                when (origin) {
                    "upload-127", "upload-128" -> {
                        val level = if (origin == "upload-127") 127 else 128
                        raw.rewind(); raw.put(ByteArray(raw.capacity()) { if (it % 4 == 3) 255.toByte() else level.toByte() }); raw.rewind()
                        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
                        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, 192, 240, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, raw)
                    }
                    "clear-half" -> { GLES20.glClearColor(.5f, .5f, .5f, 1f); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT) }
                    "draw-half" -> { writer.use(); writer.float("uFlipY", -1f); writer.float("uValue", .5f); writer.draw() }
                }
                raw.rewind(); GLES20.glReadPixels(0, 0, 192, 240, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, raw)
                val levels = (0 until 192 * 240).map { raw.get(it * 4).toInt() and 255 }.toSet().sorted()
                if (origin.startsWith("upload")) check(levels == listOf(if (origin == "upload-127") 127 else 128))
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbos[0])
                reader.use(); reader.float("uFlipY", -1f); reader.texture("uTexture", texture, 0); reader.draw()
                raw.rewind(); GLES20.glReadPixels(0, 0, 192, 240, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, raw)
                check(GLES20.glGetError() == GLES20.GL_NO_ERROR)
                val counts = IntArray(3)
                for (at in 0 until raw.capacity() step 4) {
                    val endpoints = (0..2).map { raw.get(at + it).toInt() and 255 }
                    check(endpoints.all { it == 0 || it == 255 } && endpoints.count { it == 255 } == 1)
                    counts[endpoints.indexOf(255)]++
                }
                results.put(JSONObject().put("origin", origin).put("rgba8Levels", JSONArray(levels))
                    .put("storedBelowHalfPixels", counts[0]).put("storedAboveHalfPixels", counts[1]).put("storedExactlyHalfPixels", counts[2]))
            }
        } finally {
            reader.release(); GLES20.glDeleteFramebuffers(1, fbos, 0); GLES20.glDeleteTextures(1, textures, 0)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, sourceFbo)
            GLES20.glClearColor(clearColour[0], clearColour[1], clearColour[2], clearColour[3])
        }
        return results
    }

    @Test fun hashesAndNeutralSwayTiles() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val renderer = VideoFxPreviewRenderer.get(context)
        val output = File(context.getExternalFilesDir("windows-refs"), "diagnostics").apply { mkdirs() }
        // Run reduced shader math on the existing preview context, without changing its EGL lifetime.
        val dispatcher = renderer.javaClass.getDeclaredField("dispatcher").apply { isAccessible = true }.get(renderer) as CoroutineDispatcher
        withContext(dispatcher) {
            renderer.javaClass.getDeclaredMethod("setUp").apply { isAccessible = true }.invoke(renderer)
            val binding = IntArray(1); val viewport = IntArray(4); val program = IntArray(1); val unit = IntArray(1); val boundTexture = IntArray(1)
            GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, binding, 0); GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, viewport, 0)
            GLES20.glGetIntegerv(GLES20.GL_CURRENT_PROGRAM, program, 0); GLES20.glGetIntegerv(GLES20.GL_ACTIVE_TEXTURE, unit, 0)
            GLES20.glGetIntegerv(GLES20.GL_TEXTURE_BINDING_2D, boundTexture, 0)
            val texture0 = IntArray(1); GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glGetIntegerv(GLES20.GL_TEXTURE_BINDING_2D, texture0, 0)
            val defaultDither = GLES20.glIsEnabled(GLES20.GL_DITHER)
            val textures = IntArray(1); val fbos = IntArray(1)
            val shader = FxProgram("""
precision highp float;
varying highp vec2 vUv;
uniform float uTime;
float hash1(float n) { return fract(sin(n * 12.9898) * 43758.5453); }
void main() {
  float k = floor(uTime * 5.0);
  float h = hash1(k * 1.3 + 0.7);
  gl_FragColor = vec4(h, h, h, 1.0);
}
""")
            try {
                GLES20.glGenTextures(1, textures, 0); GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1, 1, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
                GLES20.glGenFramebuffers(1, fbos, 0); GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbos[0])
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, textures[0], 0)
                check(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) == GLES20.GL_FRAMEBUFFER_COMPLETE)
                GLES20.glViewport(0, 0, 1, 1)
                val range = IntArray(2); val precision = IntArray(1)
                GLES20.glGetShaderPrecisionFormat(GLES20.GL_FRAGMENT_SHADER, GLES20.GL_HIGH_FLOAT, range, 0, precision, 0)
                val mediumRange = IntArray(2); val mediumPrecision = IntArray(1)
                GLES20.glGetShaderPrecisionFormat(GLES20.GL_FRAGMENT_SHADER, GLES20.GL_MEDIUM_FLOAT, mediumRange, 0, mediumPrecision, 0)
                val lowRange = IntArray(2); val lowPrecision = IntArray(1)
                GLES20.glGetShaderPrecisionFormat(GLES20.GL_FRAGMENT_SHADER, GLES20.GL_LOW_FLOAT, lowRange, 0, lowPrecision, 0)
                val math = JSONArray(); val pixels = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
                for (time in listOf(.3f, .9f, 1.5f, 2.1f)) {
                    shader.use(); shader.float("uFlipY", 1f); shader.float("uTime", time); shader.draw()
                    pixels.rewind(); GLES20.glReadPixels(0, 0, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels)
                    check(GLES20.glGetError() == GLES20.GL_NO_ERROR)
                    math.put(JSONObject().put("time", time).put("hash1_sRGB8", pixels.get(0).toInt() and 255))
                }
                // Match Windows' raw RGBA8 numeric probes before Bitmap.copy(RGB_565).
                // These are diagnostic inputs, separate from the 1688 authoritative goldens.
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 192, 240, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
                check(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) == GLES20.GL_FRAMEBUFFER_COMPLETE)
                GLES20.glViewport(0, 0, 192, 240)
                val raw = ByteBuffer.allocateDirect(192 * 240 * 4).order(ByteOrder.nativeOrder())
                fun writeRaw(name: String) {
                    raw.rewind(); GLES20.glReadPixels(0, 0, 192, 240, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, raw)
                    check(GLES20.glGetError() == GLES20.GL_NO_ERROR); raw.rewind()
                    val tile = android.graphics.Bitmap.createBitmap(192, 240, android.graphics.Bitmap.Config.ARGB_8888)
                    try {
                        tile.copyPixelsFromBuffer(raw)
                        File(output, "numeric/$name.png").apply { parentFile!!.mkdirs() }.outputStream().use {
                            check(tile.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
                        }
                    } finally { tile.recycle() }
                }
                val numeric = JSONArray()
                val ditherNumeric = JSONArray(); var storage: JSONArray? = null
                val colour = FxProgram("""
precision highp float;
varying highp vec2 vUv;
uniform float uValue;
void main() { gl_FragColor = vec4(uValue, uValue, uValue, 1.0); }
""")
                try {
                    for (dither in listOf(defaultDither, !defaultDither)) for (value in listOf(.25f, .5f, .65f, .92f)) {
                        if (dither) GLES20.glEnable(GLES20.GL_DITHER) else GLES20.glDisable(GLES20.GL_DITHER)
                        colour.use(); colour.float("uFlipY", -1f); colour.float("uValue", value); colour.draw()
                        writeRaw(if (dither == defaultDither) "constant-$value-rgba8" else "constant-$value-dither-$dither-rgba8")
                        val levels = (0 until 192 * 240).map { raw.get(it * 4).toInt() and 255 }.toSet().sorted()
                        val result = JSONObject().put("value", value).put("rgba8Levels", JSONArray(levels))
                        if (dither == defaultDither) numeric.put(result)
                        ditherNumeric.put(JSONObject(result.toString()).put("ditherEnabled", dither))
                    }
                    if (defaultDither) GLES20.glEnable(GLES20.GL_DITHER) else GLES20.glDisable(GLES20.GL_DITHER)
                    storage = storageObservations(textures[0], fbos[0], colour)
                } finally {
                    if (defaultDither) GLES20.glEnable(GLES20.GL_DITHER) else GLES20.glDisable(GLES20.GL_DITHER)
                    colour.release()
                }
                val grid = FxProgram("""
precision highp float;
varying highp vec2 vUv;
uniform vec2 uGrid;
void main() { gl_FragColor = vec4(floor(vUv * uGrid) / 255.0, 0.0, 1.0); }
""")
                try {
                    for (n in listOf(16f, 32f, 64f, 128f)) {
                        grid.use(); grid.float("uFlipY", -1f); grid.vec2("uGrid", n * (192f / 240), n); grid.draw()
                        writeRaw("grid-${n.toInt()}-cells")
                    }
                } finally { grid.release() }
                File(output, "mobile-math-probe.json").writeText(JSONObject().put("renderer", GLES20.glGetString(GLES20.GL_RENDERER))
                    .put("vendor", GLES20.glGetString(GLES20.GL_VENDOR)).put("version", GLES20.glGetString(GLES20.GL_VERSION))
                    .put("highFloatBits", precision[0]).put("sceneCutMath", math).put("constantColourReadback", numeric)
                    .put("mediumFloatBits", mediumPrecision[0]).put("mediumFloatRange", JSONArray(mediumRange.toList()))
                    .put("lowFloatBits", lowPrecision[0]).put("lowFloatRange", JSONArray(lowRange.toList()))
                    .put("defaultDitherEnabled", defaultDither).put("ditherColourReadback", ditherNumeric).put("halfValueStorage", storage).toString(2))
            } finally {
                shader.release(); GLES20.glDeleteFramebuffers(1, fbos, 0); GLES20.glDeleteTextures(1, textures, 0)
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, binding[0]); GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3])
                if (defaultDither) GLES20.glEnable(GLES20.GL_DITHER) else GLES20.glDisable(GLES20.GL_DITHER)
                GLES20.glUseProgram(program[0]); GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture0[0]); GLES20.glActiveTexture(unit[0])
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, boundTexture[0])
            }
        }
        // Same preparation and SWAY as real catalog previews; isolate input/raster/RGB565 from FX math.
        for (sample in 0..20) for (time in listOf(.3f, .9f, 1.5f, 2.1f)) {
            val spec = VideoFxSpec("fx-diagnostic-copy-$sample", "Neutral copy $sample", "Diagnostic",
                "vec4 fx(vec2 uv) { return src(uv); }", "", emptyList(), sample = sample)
            val tile = requireNotNull(renderer.renderAt(spec, time))
            File(output, "neutral/$sample/$time.png").apply { parentFile!!.mkdirs() }.outputStream().use {
                check(tile.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it))
            }
            tile.recycle()
        }
    }
}
