package com.dfuzer.birdnote.ui.staff

import android.graphics.Bitmap
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * OpenGL ES 2 output for the quiz staff, with its own EGL context on the staff render thread.
 *
 * A hardware [android.graphics.Canvas] on a [Surface] syncs through the app's single HWUI
 * RenderThread, so staff frames waited behind Compose frames (which can block for a whole vsync
 * in `dequeueBuffer`). This context shares nothing with HWUI. It draws premultiplied bitmap quads
 * and anti-aliased round rects in surface pixels, top-left origin. Textures live as long as the
 * context; [release] frees any still alive. Every call, [release] included,
 * must run on the thread that called [create].
 */
internal class StaffGl private constructor(
    private val display: EGLDisplay,
    private val context: EGLContext,
    private val eglSurface: EGLSurface,
) {
    private val program = linkProgram(VERTEX_SHADER, FRAGMENT_SHADER)
    private val unitQuad: FloatBuffer = ByteBuffer.allocateDirect(UNIT_QUAD.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply { put(UNIT_QUAD).position(0) }
    private val rectUniform = GLES20.glGetUniformLocation(program, "u_rect")
    private val viewportUniform = GLES20.glGetUniformLocation(program, "u_viewport")
    private val colorUniform = GLES20.glGetUniformLocation(program, "u_color")
    private val roundUniform = GLES20.glGetUniformLocation(program, "u_round")
    private var width = 0
    private var height = 0

    init {
        GLES20.glUseProgram(program)
        val unitAttribute = GLES20.glGetAttribLocation(program, "a_unit")
        GLES20.glEnableVertexAttribArray(unitAttribute)
        GLES20.glVertexAttribPointer(unitAttribute, 2, GLES20.GL_FLOAT, false, 0, unitQuad)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "u_texture"), 0)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    /** Copies [bitmap] into a new texture; the bitmap can be recycled afterwards. */
    fun createTexture(bitmap: Bitmap): StaffTexture {
        val id = IntArray(1)
        GLES20.glGenTextures(1, id, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        return StaffTexture(id[0], bitmap.width, bitmap.height)
    }

    fun deleteTexture(texture: StaffTexture) {
        GLES20.glDeleteTextures(1, intArrayOf(texture.id), 0)
    }

    fun begin(surfaceWidth: Int, surfaceHeight: Int, color: Int) {
        width = surfaceWidth
        height = surfaceHeight
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUniform2f(viewportUniform, width.toFloat(), height.toFloat())
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
        GLES20.glClearColor(red(color), green(color), blue(color), 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }

    /** Limits drawing to x ≥ [left], or lifts the limit when [left] is null. */
    fun clipLeft(left: Int?) {
        if (left == null) {
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST)
            return
        }
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST)
        GLES20.glScissor(left, 0, (width - left).coerceAtLeast(0), height)
    }

    /** Draws [texture] at its pixel size with its top-left corner at ([left], [top]). */
    fun drawTexture(texture: StaffTexture, left: Float, top: Float) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.id)
        GLES20.glUniform3f(roundUniform, -1f, 0f, 0f)
        drawQuad(left, top, texture.width.toFloat(), texture.height.toFloat())
    }

    fun drawRoundRect(left: Float, top: Float, rectWidth: Float, rectHeight: Float, radius: Float, color: Int) {
        val alpha = alpha(color)
        GLES20.glUniform4f(colorUniform, red(color) * alpha, green(color) * alpha, blue(color) * alpha, alpha)
        GLES20.glUniform3f(roundUniform, radius, rectWidth, rectHeight)
        drawQuad(left, top, rectWidth, rectHeight)
    }

    /** Queues the frame. False when the surface is gone. */
    fun swap(): Boolean = EGL14.eglSwapBuffers(display, eglSurface)

    /** Destroys the EGL surface and context, which disconnects this producer from the surface. */
    fun release() {
        GLES20.glDeleteProgram(program)
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(display, eglSurface)
        EGL14.eglDestroyContext(display, context)
        EGL14.eglReleaseThread()
    }

    private fun drawQuad(left: Float, top: Float, quadWidth: Float, quadHeight: Float) {
        GLES20.glUniform4f(rectUniform, left, top, quadWidth, quadHeight)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    companion object {
        /** Connects a GL context to [surface] and makes it current, or returns null. */
        fun create(surface: Surface): StaffGl? {
            // The default display is shared with HWUI, so it is never terminated here.
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) return null
            val version = IntArray(2)
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) return null
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            val configAttributes = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_NONE,
            )
            val chosen = EGL14.eglChooseConfig(display, configAttributes, 0, configs, 0, 1, count, 0)
            val config = configs[0]
            if (!chosen || count[0] == 0 || config == null) return null
            val context = EGL14.eglCreateContext(
                display,
                config,
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                0,
            )
            if (context == EGL14.EGL_NO_CONTEXT) return null
            val eglSurface = EGL14.eglCreateWindowSurface(display, config, surface, intArrayOf(EGL14.EGL_NONE), 0)
            if (eglSurface == EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroyContext(display, context)
                return null
            }
            if (!EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)) {
                EGL14.eglDestroySurface(display, eglSurface)
                EGL14.eglDestroyContext(display, context)
                return null
            }
            return StaffGl(display, context, eglSurface)
        }
    }
}

internal class StaffTexture(val id: Int, val width: Int, val height: Int)

private val UNIT_QUAD = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)

// u_rect is (left, top, width, height) in surface pixels; y grows downward like a Canvas.
private const val VERTEX_SHADER = """
attribute vec2 a_unit;
uniform vec4 u_rect;
uniform vec2 u_viewport;
varying vec2 v_unit;
void main() {
    v_unit = a_unit;
    vec2 pixel = u_rect.xy + a_unit * u_rect.zw;
    gl_Position = vec4(pixel.x / u_viewport.x * 2.0 - 1.0, 1.0 - pixel.y / u_viewport.y * 2.0, 0.0, 1.0);
}
"""

// u_round.x < 0 samples the texture; otherwise it is (radius, width, height) of a round rect
// filled with premultiplied u_color and a one-pixel anti-aliased edge. highp keeps strip texture
// coordinates exact across a few thousand pixels.
private const val FRAGMENT_SHADER = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
uniform sampler2D u_texture;
uniform vec4 u_color;
uniform vec3 u_round;
varying vec2 v_unit;
void main() {
    if (u_round.x < 0.0) {
        gl_FragColor = texture2D(u_texture, v_unit);
    } else {
        vec2 half_size = u_round.yz * 0.5;
        vec2 corner = abs(v_unit * u_round.yz - half_size) - (half_size - u_round.x);
        float distance = length(max(corner, 0.0)) + min(max(corner.x, corner.y), 0.0) - u_round.x;
        gl_FragColor = u_color * clamp(0.5 - distance, 0.0, 1.0);
    }
}
"""

private fun linkProgram(vertexSource: String, fragmentSource: String): Int {
    val program = GLES20.glCreateProgram()
    val vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
    val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
    GLES20.glAttachShader(program, vertex)
    GLES20.glAttachShader(program, fragment)
    GLES20.glLinkProgram(program)
    GLES20.glDeleteShader(vertex)
    GLES20.glDeleteShader(fragment)
    val status = IntArray(1)
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
    check(status[0] == GLES20.GL_TRUE) { "Staff program: ${GLES20.glGetProgramInfoLog(program)}" }
    return program
}

private fun compileShader(type: Int, source: String): Int {
    val shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, source)
    GLES20.glCompileShader(shader)
    val status = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
    check(status[0] == GLES20.GL_TRUE) { "Staff shader: ${GLES20.glGetShaderInfoLog(shader)}" }
    return shader
}

private fun alpha(color: Int): Float = (color ushr 24 and 0xFF) / 255f
private fun red(color: Int): Float = (color shr 16 and 0xFF) / 255f
private fun green(color: Int): Float = (color shr 8 and 0xFF) / 255f
private fun blue(color: Int): Float = (color and 0xFF) / 255f
