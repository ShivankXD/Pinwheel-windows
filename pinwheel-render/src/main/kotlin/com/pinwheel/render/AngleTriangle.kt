package com.pinwheel.render

import com.pinwheel.core.RgbaFrame
import org.lwjgl.opengles.GLES20.*
import org.lwjgl.system.MemoryStack
import java.nio.file.Path

data class TriangleResult(val frame: RgbaFrame, val renderer: String, val version: String)

/** P0 diagnostic now uses the same reference-counted display lifetime as P2. */
object AngleTriangle {
    @Synchronized fun render(nativeDirectory: Path, width: Int = 320, height: Int = 240): TriangleResult {
        require(width in 16..4096 && height in 16..4096)
        AngleDevice(nativeDirectory).use { device ->
            GpuTarget(device, width, height).use { target ->
                FxProgram("""
                    precision highp float;
                    varying highp vec2 uv;
                    void main() { gl_FragColor = vec4(uv.x, uv.y, 0.35, 1.0); }
                """.trimIndent(), """
                    attribute vec2 aPosition;
                    varying highp vec2 uv;
                    void main() { gl_Position = vec4(aPosition, 0.0, 1.0); uv = aPosition * 0.5 + 0.5; }
                """.trimIndent()).use { p ->
                    target.bind(); glClearColor(7f / 255f, 8f / 255f, 11f / 255f, 1f); glClear(GL_COLOR_BUFFER_BIT)
                    p.use(); val position = glGetAttribLocation(p.id, "aPosition")
                    MemoryStack.stackPush().use { stack ->
                        glEnableVertexAttribArray(position)
                        glVertexAttribPointer(position, 2, GL_FLOAT, false, 0, stack.floats(-.8f, -.7f, .8f, -.7f, 0f, .8f))
                        glDrawArrays(GL_TRIANGLES, 0, 3); glDisableVertexAttribArray(position)
                    }
                    checkGl("triangle"); return TriangleResult(target.read(), device.renderer, device.version)
                }
            }
        }
    }
}
