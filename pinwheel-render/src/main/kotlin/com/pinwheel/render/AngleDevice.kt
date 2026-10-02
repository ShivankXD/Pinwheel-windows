package com.pinwheel.render

import com.pinwheel.core.RgbaFrame
import org.lwjgl.egl.EGL
import org.lwjgl.egl.EGL10.*
import org.lwjgl.egl.EGL14.*
import org.lwjgl.opengles.GLES
import org.lwjgl.opengles.GLES20.*
import org.lwjgl.system.Configuration
import org.lwjgl.system.JNI
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.file.Path

/** A persistent ES2 ANGLE/D3D11 context. Creation, use and disposal stay on one thread. */
class AngleDevice(nativeDirectory: Path, softwareDiagnostic: Boolean = false) : AutoCloseable {
    private val owner = Thread.currentThread()
    private val display = AngleDisplays.acquire(nativeDirectory, softwareDiagnostic)
    private var surface = EGL_NO_SURFACE
    private var context = EGL_NO_CONTEXT
    private var capabilities: org.lwjgl.opengles.GLESCapabilities? = null
    private var closed = false
    val renderer: String
    val version: String

    init {
        try {
            MemoryStack.stackPush().use { stack ->
                check(eglBindAPI(EGL_OPENGL_ES_API))
                val configs = stack.mallocPointer(1); val count = stack.mallocInt(1)
                check(eglChooseConfig(display, stack.ints(EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
                    EGL_RENDERABLE_TYPE, 4, EGL_RED_SIZE, 8, EGL_GREEN_SIZE, 8,
                    EGL_BLUE_SIZE, 8, EGL_ALPHA_SIZE, 8, EGL_NONE), configs, count) && count[0] > 0)
                surface = eglCreatePbufferSurface(display, configs[0], stack.ints(EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE))
                check(surface != EGL_NO_SURFACE) { "ANGLE pbuffer failed: ${eglGetError()}" }
                context = eglCreateContext(display, configs[0], EGL_NO_CONTEXT, stack.ints(0x3098, 2, EGL_NONE))
                check(context != EGL_NO_CONTEXT) { "ANGLE ES2 context failed: ${eglGetError()}" }
                check(eglMakeCurrent(display, surface, surface, context))
                capabilities = GLES.createCapabilities()
                renderer = requireNotNull(glGetString(GL_RENDERER))
                version = requireNotNull(glGetString(GL_VERSION))
                check(renderer.contains("ANGLE") && renderer.contains("Direct3D11")) { "Unexpected renderer: $renderer" }
            }
        } catch (failure: Throwable) { close(); throw failure }
    }

    fun checkThread() {
        check(Thread.currentThread() === owner) { "ANGLE context accessed from a different thread" }
        check(!closed) { "ANGLE context is closed" }
        if (eglGetCurrentContext() != context) {
            check(eglMakeCurrent(display, surface, surface, context))
            GLES.setCapabilities(capabilities)
        }
    }

    override fun close() {
        check(Thread.currentThread() === owner) { "ANGLE context disposed from a different thread" }
        if (closed) return
        closed = true
        if (eglGetCurrentContext() == context) {
            GLES.setCapabilities(null)
            eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)
        }
        if (context != EGL_NO_CONTEXT) eglDestroyContext(display, context)
        if (surface != EGL_NO_SURFACE) eglDestroySurface(display, surface)
        AngleDisplays.release(display)
    }
}

/** EGL displays are process-wide. One context closing must not terminate another renderer. */
private object AngleDisplays {
    private var loadedPath: Path? = null
    private data class Display(val id: Long, var clients: Int)
    private val displays = HashMap<Boolean, Display>()
    @Synchronized fun acquire(directory: Path, software: Boolean): Long {
        val path = directory.toAbsolutePath().normalize()
        if (loadedPath == null) {
            Configuration.EGL_LIBRARY_NAME.set(path.resolve("libEGL.dll").toString())
            Configuration.OPENGLES_LIBRARY_NAME.set(path.resolve("libGLESv2.dll").toString())
            EGL.getCapabilities(); loadedPath = path
        }
        check(path == loadedPath) { "ANGLE cannot change native libraries in one process" }
        val state = displays.getOrPut(software) { MemoryStack.stackPush().use { stack ->
            check(EGL.getCapabilities().eglGetPlatformDisplayEXT != 0L)
            // WARP is used only by the parity diagnostic, never selected by playback/UI.
            val attributes = if (software) stack.ints(0x3203, 0x3208, 0x3209, 0x320B, EGL_NONE)
                else stack.ints(0x3203, 0x3208, EGL_NONE)
            val display = JNI.callPPP(0x3202, 0L, MemoryUtil.memAddress(attributes),
                EGL.getCapabilities().eglGetPlatformDisplayEXT)
            check(display != EGL_NO_DISPLAY)
            check(eglInitialize(display, stack.mallocInt(1), stack.mallocInt(1)))
            EGL.createDisplayCapabilities(display)
            Display(display, 0)
        } }
        state.clients++
        return state.id
    }
    @Synchronized fun release(value: Long) {
        val entry = displays.entries.first { it.value.id == value }; val state = entry.value
        check(state.clients > 0)
        if (--state.clients == 0) { eglTerminate(value); displays.remove(entry.key) }
    }
}

/** GL textures use bottom-up rows. CPU adapters return top-down RGBA. */
class GpuTexture(val device: AngleDevice, val width: Int, val height: Int) : AutoCloseable {
    var id: Int = 0
        private set
    init {
        device.checkThread()
        require(width in 1..glGetInteger(GL_MAX_TEXTURE_SIZE) && height in 1..glGetInteger(GL_MAX_TEXTURE_SIZE))
        try {
            id = glGenTextures(); glBindTexture(GL_TEXTURE_2D, id)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null as ByteBuffer?)
            checkGl("texture allocation")
        } catch (failure: Throwable) { close(); throw failure }
    }
    fun upload(frame: RgbaFrame, topDown: Boolean = true, premultiply: Boolean = false) {
        device.checkThread(); check(id != 0); require(frame.width == width && frame.height == height)
        val bytes = MemoryUtil.memAlloc(frame.pixels.size)
        try {
            val stride = width * 4
            for (y in 0 until height) {
                val start = (if (topDown) height - 1 - y else y) * stride
                if (!premultiply) bytes.put(frame.pixels, start, stride)
                else for (x in 0 until width) {
                    val at = start + x * 4; val alpha = frame.pixels[at + 3].toInt() and 255
                    for (c in 0..2) bytes.put((((frame.pixels[at + c].toInt() and 255) * alpha + 127) / 255).toByte())
                    bytes.put(alpha.toByte())
                }
            }
            bytes.flip(); glBindTexture(GL_TEXTURE_2D, id)
            glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, bytes)
            checkGl("texture upload")
        } finally { MemoryUtil.memFree(bytes) }
    }
    override fun close() { device.checkThread(); if (id != 0) { glDeleteTextures(id); id = 0 } }
}

class GpuTarget(device: AngleDevice, width: Int, height: Int) : AutoCloseable {
    val texture = GpuTexture(device, width, height)
    var fbo = 0
        private set
    init {
        try {
            fbo = glGenFramebuffers(); glBindFramebuffer(GL_FRAMEBUFFER, fbo)
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture.id, 0)
            check(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE) { "Incomplete RGBA8 framebuffer" }
        } catch (failure: Throwable) { close(); throw failure }
    }
    fun bind() {
        texture.device.checkThread(); check(fbo != 0)
        glBindFramebuffer(GL_FRAMEBUFFER, fbo); glViewport(0, 0, texture.width, texture.height)
        glDisable(GL_BLEND)
    }
    fun read(topDown: Boolean = true): RgbaFrame {
        bind()
        val stride = texture.width * 4; val bytes = MemoryUtil.memAlloc(stride * texture.height)
        try {
            glReadPixels(0, 0, texture.width, texture.height, GL_RGBA, GL_UNSIGNED_BYTE, bytes)
            checkGl("frame readback")
            val pixels = ByteArray(bytes.remaining())
            for (y in 0 until texture.height) bytes.get((if (topDown) texture.height - 1 - y else y) * stride, pixels, y * stride, stride)
            return RgbaFrame(texture.width, texture.height, pixels)
        } finally { MemoryUtil.memFree(bytes) }
    }
    override fun close() {
        texture.device.checkThread()
        if (fbo != 0) { glDeleteFramebuffers(fbo); fbo = 0 }; texture.close()
    }
}

internal fun checkGl(operation: String) {
    val error = glGetError(); check(error == GL_NO_ERROR) { "GLES $operation failed: 0x${error.toString(16)}" }
}
