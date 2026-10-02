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
                val colour = FxProgram("""
precision highp float;
varying highp vec2 vUv;
uniform float uValue;
void main() { gl_FragColor = vec4(uValue, uValue, uValue, 1.0); }
""")
                try {
                    for (value in listOf(.25f, .5f, .65f, .92f)) {
                        colour.use(); colour.float("uFlipY", -1f); colour.float("uValue", value); colour.draw()
                        writeRaw("constant-$value-rgba8")
                        val levels = (0 until 192 * 240).map { raw.get(it * 4).toInt() and 255 }.toSet().sorted()
                        numeric.put(JSONObject().put("value", value).put("rgba8Levels", JSONArray(levels)))
                    }
                } finally { colour.release() }
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
                    .put("highFloatBits", precision[0]).put("sceneCutMath", math).put("constantColourReadback", numeric).toString(2))
            } finally {
                shader.release(); GLES20.glDeleteFramebuffers(1, fbos, 0); GLES20.glDeleteTextures(1, textures, 0)
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, binding[0]); GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3])
                GLES20.glUseProgram(program[0]); GLES20.glActiveTexture(unit[0])
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
