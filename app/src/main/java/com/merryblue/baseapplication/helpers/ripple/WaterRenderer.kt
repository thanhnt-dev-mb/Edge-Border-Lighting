package com.merryblue.baseapplication.helpers.ripple

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.os.SystemClock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.random.Random

class WaterRenderer(
    private val context: Context
) : GLSurfaceView.Renderer {

    var damping: Float = 0.9885f
    var refract: Float = 0.020f
    var specular: Float = 0.08f
    var rainEnabled: Boolean = true

    private val simSize = 512

    private data class Drop(val x: Float, val y: Float, val strength: Float, val radius: Float)
    private val pendingDrops = ArrayDeque<Drop>(64)

    // Base texture may be updated later when bitmap arrives
    private var baseTex = 0
    private var pendingBitmap: Bitmap? = null

    fun addDrop(x: Float, y: Float, strength: Float, radius: Float) {
        pendingDrops.addLast(Drop(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f), strength, radius))
    }

    /**
     * Call on GL thread (glView.queueEvent).
     */
    fun updateBaseBitmap(bitmap: Bitmap) {
        // store and upload at next frame, or upload immediately (we are on GL thread already)
        pendingBitmap = bitmap
    }

    private var vao = 0
    private var vbo = 0
    private var simProgram = 0
    private var renderProgram = 0

    private var heightTexA = 0
    private var heightTexB = 0
    private var fboA = 0
    private var fboB = 0
    private var usingAasSrc = true

    private var startMs = 0L
    private var lastRainT = 0f
    private var nextRainInterval = 0.10f

    override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        startMs = SystemClock.uptimeMillis()

        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        GLES30.glDisable(GLES30.GL_CULL_FACE)

        setupFullscreenQuad()
        simProgram = GLUtil.createProgram(Shaders.VS, Shaders.SIM_FS)
        renderProgram = GLUtil.createProgram(Shaders.VS, Shaders.RENDER_FS)

        // Create an initial 1x1 base texture (placeholder) so shader always has something
        baseTex = GLUtil.createSolidTexture1x1()

        heightTexA = GLUtil.createRG16FTexture(simSize, simSize)
        heightTexB = GLUtil.createRG16FTexture(simSize, simSize)
        fboA = GLUtil.createFboForTexture(heightTexA)
        fboB = GLUtil.createFboForTexture(heightTexB)
        clearFbo(fboA, simSize, simSize)
        clearFbo(fboB, simSize, simSize)
    }

    override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
        val t = (SystemClock.uptimeMillis() - startMs) / 1000f

        // Upload pending bitmap (if any)
        pendingBitmap?.let { bmp ->
            // Replace base texture content
            GLUtil.updateTextureFromBitmap(baseTex, bmp)
            pendingBitmap = null
        }

        if (rainEnabled) maybeRain(t)

        val srcTex = if (usingAasSrc) heightTexA else heightTexB
        val dstFbo = if (usingAasSrc) fboB else fboA

        // Pass 1: simulate into dst
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, dstFbo)
        GLES30.glViewport(0, 0, simSize, simSize)

        GLES30.glUseProgram(simProgram)
        GLUtil.bindTex2D(simProgram, "uHeightRG", srcTex, 0)
        GLES30.glUniform2f(GLES30.glGetUniformLocation(simProgram, "uTexel"), 1f / simSize, 1f / simSize)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(simProgram, "uDamping"), damping)

        val drops = FloatArray(8 * 4)
        var count = 0
        while (count < 8 && pendingDrops.isNotEmpty()) {
            val d = pendingDrops.removeFirst()
            drops[count * 4 + 0] = d.x
            drops[count * 4 + 1] = d.y
            drops[count * 4 + 2] = d.strength
            drops[count * 4 + 3] = d.radius
            count++
        }
        GLES30.glUniform1i(GLES30.glGetUniformLocation(simProgram, "uDropCount"), count)
        GLES30.glUniform4fv(GLES30.glGetUniformLocation(simProgram, "uDrops"), 8, drops, 0)

        drawQuad()
        usingAasSrc = !usingAasSrc

        // Pass 2: render to screen
        val heightTex = if (usingAasSrc) heightTexA else heightTexB

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        GLES30.glUseProgram(renderProgram)
        GLUtil.bindTex2D(renderProgram, "uBase", baseTex, 0)
        GLUtil.bindTex2D(renderProgram, "uHeightRG", heightTex, 1)
        GLES30.glUniform2f(GLES30.glGetUniformLocation(renderProgram, "uTexel"), 1f / simSize, 1f / simSize)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(renderProgram, "uRefract"), refract)
        GLES30.glUniform1f(GLES30.glGetUniformLocation(renderProgram, "uSpecular"), specular)

        drawQuad()
    }

    private fun setupFullscreenQuad() {
        val data = floatArrayOf(
            -1f, -1f, 0f, 0f,
            1f, -1f, 1f, 0f,
            -1f,  1f, 0f, 1f,
            1f,  1f, 1f, 1f,
        )
        val fb: FloatBuffer = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(data)
        fb.position(0)

        val ids = IntArray(1)
        GLES30.glGenVertexArrays(1, ids, 0); vao = ids[0]
        GLES30.glGenBuffers(1, ids, 0); vbo = ids[0]

        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, data.size * 4, fb, GLES30.GL_STATIC_DRAW)

        val stride = 4 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, stride, 0)

        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, stride, 2 * 4)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
        GLES30.glBindVertexArray(0)
    }

    private fun drawQuad() {
        GLES30.glBindVertexArray(vao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
    }

    private fun clearFbo(fbo: Int, w: Int, h: Int) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glViewport(0, 0, w, h)
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    private fun maybeRain(t: Float) {
        if (t - lastRainT < nextRainInterval) return
        lastRainT = t
        nextRainInterval = 0.05f + Random.nextFloat() * 0.15f

        val x = Random.nextFloat()
        val y = Random.nextFloat()
        val strength = 0.10f + Random.nextFloat() * 0.12f
        val radius = 0.010f + Random.nextFloat() * 0.012f
        addDrop(x, y, strength, radius)
    }
}