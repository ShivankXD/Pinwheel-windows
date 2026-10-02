package com.pinwheel.render

import com.pinwheel.core.media.video.*
import org.lwjgl.BufferUtils
import org.lwjgl.opengles.GLES20.*

/** Mobile FxProgram's GLES2 binding; unused uniforms are skipped without changing shader text. */
class FxProgram(fragment: String, vertexSource: String = FX_VERTEX) : AutoCloseable {
    private val owner = Thread.currentThread()
    private val locations = HashMap<String, Int>()
    private val quad = BufferUtils.createFloatBuffer(8).apply { put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)); flip() }
    var id: Int = 0
        private set
    private val position: Int
    init {
        val vertex = compile(GL_VERTEX_SHADER, vertexSource)
        val shader = try { compile(GL_FRAGMENT_SHADER, fragment) } catch (e: Throwable) { glDeleteShader(vertex); throw e }
        try {
            id = glCreateProgram(); glAttachShader(id, vertex); glAttachShader(id, shader); glLinkProgram(id)
            check(glGetProgrami(id, GL_LINK_STATUS) != 0) { "Effect program link failed: ${glGetProgramInfoLog(id)}" }
            position = glGetAttribLocation(id, "aPosition"); check(position >= 0)
        } catch (e: Throwable) { if (id != 0) glDeleteProgram(id); id = 0; throw e }
        finally { glDeleteShader(vertex); glDeleteShader(shader) }
    }
    private fun checkOwner() { check(Thread.currentThread() === owner && id != 0) { "FxProgram is closed or used on another thread" } }
    private fun location(name: String): Int { checkOwner(); return locations.getOrPut(name) { glGetUniformLocation(id, name) } }
    fun use() { checkOwner(); glUseProgram(id) }
    fun float(name: String, value: Float) { val l = location(name); if (l >= 0) glUniform1f(l, value) }
    fun vec2(name: String, x: Float, y: Float) { val l = location(name); if (l >= 0) glUniform2f(l, x, y) }
    fun vec4(name: String, values: FloatArray, offset: Int = 0) {
        val l = location(name); if (l >= 0) glUniform4f(l, values[offset], values[offset + 1], values[offset + 2], values[offset + 3])
    }
    fun texture(name: String, texture: Int, unit: Int) {
        val l = location(name); if (l < 0) return
        glActiveTexture(GL_TEXTURE0 + unit); glBindTexture(GL_TEXTURE_2D, texture); glUniform1i(l, unit)
    }
    fun draw() {
        checkOwner(); glEnableVertexAttribArray(position)
        glVertexAttribPointer(position, 2, GL_FLOAT, false, 0, quad)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); glDisableVertexAttribArray(position)
    }
    override fun close() { check(Thread.currentThread() === owner); if (id != 0) { glDeleteProgram(id); id = 0 } }
    companion object {
        fun source(spec: VideoFxSpec) = FxShaderSources.source(spec)
        val COPY get() = FxShaderSources.COPY
        val TRAIL get() = FxShaderSources.TRAIL
        val SWAY get() = FxShaderSources.SWAY
        private fun compile(type: Int, source: String): Int {
            val shader = glCreateShader(type); glShaderSource(shader, source); glCompileShader(shader)
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
                val log = glGetShaderInfoLog(shader); glDeleteShader(shader)
                error("Effect shader compile failed: $log")
            }
            return shader
        }
    }
}
