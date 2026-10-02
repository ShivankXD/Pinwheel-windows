package com.pinwheel.render

import com.pinwheel.core.RgbaFrame
import org.lwjgl.egl.EGL
import org.lwjgl.egl.EGL10.*
import org.lwjgl.egl.EGL14.EGL_OPENGL_ES_API
import org.lwjgl.egl.EGL14.EGL_RENDERABLE_TYPE
import org.lwjgl.egl.EGL14.eglBindAPI
import org.lwjgl.opengles.GLES
import org.lwjgl.opengles.GLES20.*
import org.lwjgl.system.Configuration
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.file.Path

data class TriangleResult(val frame: RgbaFrame, val renderer: String, val version: String)

/** P0 only. ES2 pbuffer on an explicitly requested ANGLE D3D11 backend. */
object AngleTriangle {
    private var loaded = false

    @Synchronized
    fun render(nativeDirectory: Path, width: Int = 320, height: Int = 240): TriangleResult {
        require(width in 16..4096 && height in 16..4096)
        if (!loaded) {
            Configuration.EGL_LIBRARY_NAME.set(nativeDirectory.resolve("libEGL.dll").toAbsolutePath().toString())
            Configuration.OPENGLES_LIBRARY_NAME.set(nativeDirectory.resolve("libGLESv2.dll").toAbsolutePath().toString())
            // Static initialization loads the libraries once. Never share context ownership.
            EGL.getCapabilities()
            loaded = true
        }
        MemoryStack.stackPush().use { stack ->
            // EGL_PLATFORM_ANGLE_ANGLE, EGL_PLATFORM_ANGLE_TYPE_ANGLE, TYPE_D3D11_ANGLE.
            check(EGL.getCapabilities().eglGetPlatformDisplayEXT != 0L) {
                "EGL platform function unavailable; client extensions: ${eglQueryString(EGL_NO_DISPLAY, EGL_EXTENSIONS)}"
            }
            // LWJGL 3.3.6 EXTPlatformBase rejects native_display == 0. ANGLE's default display
            // is explicitly zero, so call this one entry through LWJGL JNI with the same ABI.
            val attributes = stack.ints(0x3203, 0x3208, EGL_NONE)
            val display = org.lwjgl.system.JNI.callPPP(0x3202, 0L,
                MemoryUtil.memAddress(attributes), EGL.getCapabilities().eglGetPlatformDisplayEXT)
            check(display != EGL_NO_DISPLAY) { "ANGLE D3D11 display failed: ${eglGetError()}" }
            check(eglInitialize(display, stack.mallocInt(1), stack.mallocInt(1))) { "EGL initialize failed: ${eglGetError()}" }
            var surface = EGL_NO_SURFACE
            var context = EGL_NO_CONTEXT
            try {
                EGL.createDisplayCapabilities(display)
                check(eglBindAPI(EGL_OPENGL_ES_API))
                val configs = stack.mallocPointer(1)
                val count = stack.mallocInt(1)
                check(eglChooseConfig(display, stack.ints(
                    EGL_SURFACE_TYPE, EGL_PBUFFER_BIT, EGL_RENDERABLE_TYPE, 4,
                    EGL_RED_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_BLUE_SIZE, 8, EGL_ALPHA_SIZE, 8, EGL_NONE,
                ), configs, count) && count[0] > 0) { "No ES2 RGBA8 pbuffer config" }
                surface = eglCreatePbufferSurface(display, configs[0], stack.ints(EGL_WIDTH, width, EGL_HEIGHT, height, EGL_NONE))
                check(surface != EGL_NO_SURFACE) { "EGL pbuffer failed: ${eglGetError()}" }
                context = eglCreateContext(display, configs[0], EGL_NO_CONTEXT, stack.ints(0x3098, 2, EGL_NONE))
                check(context != EGL_NO_CONTEXT) { "EGL ES2 context failed: ${eglGetError()}" }
                check(eglMakeCurrent(display, surface, surface, context))
                GLES.createCapabilities()
                val renderer = requireNotNull(glGetString(GL_RENDERER))
                check(renderer.contains("ANGLE") && renderer.contains("Direct3D11")) { "Unexpected backend: $renderer" }
                val version = requireNotNull(glGetString(GL_VERSION))
                val vertex = compile(GL_VERTEX_SHADER, """
                    attribute vec2 aPosition;
                    varying highp vec2 uv;
                    void main() { gl_Position = vec4(aPosition, 0.0, 1.0); uv = aPosition * 0.5 + 0.5; }
                """.trimIndent())
                var fragment = 0
                var program = 0
                try {
                    fragment = compile(GL_FRAGMENT_SHADER, """
                        precision highp float;
                        varying highp vec2 uv;
                        void main() { gl_FragColor = vec4(uv.x, uv.y, 0.35, 1.0); }
                    """.trimIndent())
                    program = glCreateProgram()
                    glAttachShader(program, vertex); glAttachShader(program, fragment); glLinkProgram(program)
                    check(glGetProgrami(program, GL_LINK_STATUS) != 0) { glGetProgramInfoLog(program) }
                    glViewport(0, 0, width, height)
                    glClearColor(7f / 255f, 8f / 255f, 11f / 255f, 1f); glClear(GL_COLOR_BUFFER_BIT)
                    glUseProgram(program)
                    val position = glGetAttribLocation(program, "aPosition")
                    val vertices = stack.floats(-.8f, -.7f, .8f, -.7f, 0f, .8f)
                    glEnableVertexAttribArray(position)
                    glVertexAttribPointer(position, 2, GL_FLOAT, false, 0, vertices)
                    glDrawArrays(GL_TRIANGLES, 0, 3)
                    glDisableVertexAttribArray(position)
                    glFinish()
                    val readback = MemoryUtil.memAlloc(width * height * 4)
                    try {
                        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, readback)
                        check(glGetError() == GL_NO_ERROR) { "GLES readback failed" }
                        val topDown = ByteArray(readback.remaining())
                        val stride = width * 4
                        for (y in 0 until height) readback.get((height - 1 - y) * stride, topDown, y * stride, stride)
                        return TriangleResult(RgbaFrame(width, height, topDown), renderer, version)
                    } finally { MemoryUtil.memFree(readback) }
                } finally {
                    if (program != 0) glDeleteProgram(program)
                    if (fragment != 0) glDeleteShader(fragment)
                    glDeleteShader(vertex)
                }
            } finally {
                GLES.setCapabilities(null)
                eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)
                if (context != EGL_NO_CONTEXT) eglDestroyContext(display, context)
                if (surface != EGL_NO_SURFACE) eglDestroySurface(display, surface)
                eglTerminate(display)
            }
        }
    }

    private fun compile(type: Int, source: String): Int {
        val shader = glCreateShader(type)
        glShaderSource(shader, source); glCompileShader(shader)
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            val log = glGetShaderInfoLog(shader)
            glDeleteShader(shader)
            error("GLES shader compile failed: $log")
        }
        return shader
    }
}
