package com.merryblue.baseapplication.helpers.ripple

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.SystemClock
import android.view.SurfaceHolder
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import androidx.core.graphics.scale
import androidx.core.graphics.createBitmap

class WaterDropRenderer(
    private val context: Context,
    private val holder: SurfaceHolder,
) : Thread("WaterDropRenderer") {

    private var autoRippleAccNs: Long = 0L
    private var autoRippleNeedImmediate = true
    private val rng = Random(System.nanoTime())
    private val pendingBitmapRef = AtomicReference<Bitmap?>(null)

    // Remember the last chosen background source (so we can reload after GL context loss)
    @Volatile private var rememberedBgPath: String? = null
    @Volatile private var rememberedBgUri: Uri? = null
    @Volatile private var rememberedBgResId: Int? = null
    @Volatile private var rememberedBgBitmap: Bitmap? = null // owned copy (optional)

    @Volatile private var backgroundUrl: String? = null
    @Volatile private var autoRippleIntervalMs: Long = 700L
    @Volatile private var autoRippleMargin = 0.08f
    @Volatile private var surfaceWidth = 1
    @Volatile private var surfaceHeight = 1
    @Volatile private var xOffset = 0f
    @Volatile private var xOffsetStep = 0f
    @Volatile private var autoRippleEnabled = true
    @Volatile private var backgroundSourceLocked = false
    @Volatile private var needRecreateEgl = false
    @Volatile private var running = true
    @Volatile private var paused = false
    @Volatile private var pendingRebuild = true
    @Volatile private var mvpDirty = true

    private var eglDisplay: EGLDisplay? = null
    private var eglContext: EGLContext? = null
    private var eglSurface: EGLSurface? = null

    private var program = 0
    private var aPos = -1
    private var aUv0 = -1
    private var aUv1 = -1
    private var uMvp = -1
    private var uTex0 = -1
    private var textureId = 0

    private lateinit var sim: RippleSimulation
    private lateinit var mesh: WaterMesh

    private data class Transform(val scale: Float, val offsetX: Float, val offsetY: Float)
    @Volatile private var lastTransform = Transform(1f, 0f, 0f)

    private var lastNs = 0L
    private val targetFps = 30
    private val frameNs = 1_000_000_000L / targetFps

    private val mvp = FloatArray(16)
    private val oneTex = IntArray(1)

    private val bgExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "WaterDropBgLoader").apply { isDaemon = true }
    }
    private val bgRequestId = AtomicInteger(0)

    fun setAutoRippleEnabled(enabled: Boolean) {
        autoRippleEnabled = enabled
        autoRippleAccNs = 0L
        autoRippleNeedImmediate = enabled
    }

    fun setAutoRippleIntervalMs(ms: Long) {
        autoRippleIntervalMs = ms.coerceIn(120L, 5000L)
    }

    fun setPaused(p: Boolean) { paused = p }

    fun onSurfaceSizeChanged(w: Int, h: Int) {
        surfaceWidth = max(1, w)
        surfaceHeight = max(1, h)
        pendingRebuild = true
        mvpDirty = true
    }

    fun setOffsets(x: Float, step: Float) {
        xOffset = x
        xOffsetStep = step
    }

    fun requestRecreate() {
        needRecreateEgl = true
    }

    fun onTouch(x: Float, y: Float) {
        if (!::sim.isInitialized) return
        val tr = lastTransform

        val gx = ((x - tr.offsetX) / tr.scale).coerceIn(0f, sim.gridW.toFloat())
        val gy = ((y - tr.offsetY) / tr.scale).coerceIn(0f, sim.gridH.toFloat())

        val desiredRadiusPx = 85f
        val radiusGrid = (desiredRadiusPx / tr.scale).toInt().coerceIn(2, 18)

        sim.addDrop(gx, gy, radius = radiusGrid, strength = -90f)
    }

    fun stopAndRelease() {
        running = false
        interrupt()
        joinQuietly()
        try { bgExecutor.shutdownNow() } catch (_: Throwable) {}
    }

    fun setBackgroundFromFilePath(path: String) {
        backgroundSourceLocked = true
        backgroundUrl = null

        rememberedBgPath = path
        rememberedBgUri = null
        rememberedBgResId = null
        rememberedBgBitmap = null

        submitBackgroundJob {
            BitmapFactory.decodeFile(path) ?: throw IllegalStateException("Decode bitmap failed: $path")
        }
    }

    fun setBackgroundFromBitmap(bitmap: Bitmap?) {
        backgroundUrl = null
        backgroundSourceLocked = true
        if (bitmap == null) return

        val owned = try {
            ownBitmap(bitmap)
        } catch (t: Throwable) {
            return
        }

        // Remember owned bitmap (optional)
        rememberedBgBitmap?.let { old ->
            if (old != owned && !old.isRecycled) old.recycle()
        }
        rememberedBgBitmap = owned
        rememberedBgPath = null
        rememberedBgUri = null
        rememberedBgResId = null

        submitBackgroundJob { owned }
    }

    fun setBackgroundFromUri(uri: Uri) {
        backgroundSourceLocked = true
        backgroundUrl = null

        rememberedBgUri = uri
        rememberedBgPath = null
        rememberedBgResId = null
        rememberedBgBitmap = null

        submitBackgroundJob {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: throw IllegalStateException("Decode bitmap failed: $uri")
        }
    }

    fun setBackgroundFromResId(@androidx.annotation.DrawableRes resId: Int) {
        backgroundSourceLocked = true
        backgroundUrl = null

        rememberedBgResId = resId
        rememberedBgPath = null
        rememberedBgUri = null
        rememberedBgBitmap = null

        submitBackgroundJob {
            BitmapFactory.decodeResource(context.resources, resId)
                ?: throw IllegalStateException("Decode bitmap failed: resId=$resId")
        }
    }

    private fun ownBitmap(src: Bitmap): Bitmap {
        require(!src.isRecycled) { "input bitmap recycled" }
        val cfg = src.config ?: Bitmap.Config.ARGB_8888
        return src.copy(cfg,false)
    }

    private fun submitBackgroundJob(loader: () -> Bitmap?) {
        val reqId = bgRequestId.incrementAndGet()

        bgExecutor.execute {
            try {
                val bmp = loader.invoke() ?: return@execute

                val scaled = scaleDownIfNeeded(bmp)
                if (reqId != bgRequestId.get()) {
                    if (!scaled.isRecycled) scaled.recycle()
                    return@execute
                }

                val oldPending = pendingBitmapRef.getAndSet(scaled)
                if (oldPending != null && oldPending != scaled && !oldPending.isRecycled) {
                    oldPending.recycle()
                }
            } catch (_: Throwable) { }
        }
    }

    /**
     * Called after EGL/GL recreated (context-loss).
     * If we have a remembered background source but no pending bitmap, enqueue reload.
     */
    private fun requestReloadBackgroundIfAny() {
        if (pendingBitmapRef.get() != null) return

        // Prefer bitmap (if remembered)
        rememberedBgBitmap?.let { bmp ->
            if (!bmp.isRecycled) {
                submitBackgroundJob { bmp }
                return
            }
        }

        rememberedBgPath?.let { path ->
            submitBackgroundJob {
                BitmapFactory.decodeFile(path) ?: throw IllegalStateException("Decode bitmap failed: $path")
            }
            return
        }

        rememberedBgUri?.let { uri ->
            submitBackgroundJob {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                } ?: throw IllegalStateException("Decode bitmap failed: $uri")
            }
            return
        }

        rememberedBgResId?.let { resId ->
            submitBackgroundJob {
                BitmapFactory.decodeResource(context.resources, resId)
                    ?: throw IllegalStateException("Decode bitmap failed: resId=$resId")
            }
            return
        }
    }

    private fun scaleDownIfNeeded(bmp: Bitmap, maxSize: Int = 2048): Bitmap {
        val m = max(bmp.width, bmp.height)
        if (m <= maxSize) return bmp

        val s = maxSize.toFloat() / m
        val w = (bmp.width * s).toInt().coerceAtLeast(1)
        val h = (bmp.height * s).toInt().coerceAtLeast(1)

        val out = bmp.scale(w, h)
        if (out != bmp && !bmp.isRecycled) bmp.recycle()
        return out
    }

    override fun run() {
        if (!initEgl()) return
        initGlObjects()

        requestReloadBackgroundIfAny()
        lastNs = System.nanoTime()

        while (running) {
            if (paused) {
                sleepQuietly(50)
                lastNs = System.nanoTime()
                continue
            }

            if (surfaceWidth <= 1 || surfaceHeight <= 1) {
                sleepQuietly(16)
                continue
            }

            if (needRecreateEgl || eglDisplay == null || eglSurface == null || eglContext == null) {
                needRecreateEgl = false
                releaseGl()
                releaseEgl()

                if (!waitSurfaceValid(2000L)) {
                    sleepQuietly(50)
                    continue
                }
                if (!initEgl()) {
                    sleepQuietly(50)
                    continue
                }

                initGlObjects()

                requestReloadBackgroundIfAny()

                pendingRebuild = true
                mvpDirty = true
                lastNs = System.nanoTime()
            }

            if (pendingRebuild) {
                rebuildSimAndMesh(surfaceWidth, surfaceHeight)
                pendingRebuild = false

                autoRippleNeedImmediate = autoRippleEnabled
                autoRippleAccNs = 0L
            }

            val now = System.nanoTime()
            val dtNs = (now - lastNs).coerceAtMost(100_000_000L) // cap 100ms
            lastNs = now
            val dt = dtNs / 1_000_000_000f

            sim.update(dt)

            // Auto ripple
            if (autoRippleEnabled && ::sim.isInitialized) {
                if (autoRippleNeedImmediate) {
                    autoRippleNeedImmediate = false
                    dropRandomOnce()
                    autoRippleAccNs = 0L
                } else {
                    autoRippleAccNs += dtNs
                    val intervalNs = autoRippleIntervalMs * 1_000_000L
                    if (autoRippleAccNs >= intervalNs) {
                        autoRippleAccNs %= intervalNs
                        dropRandomOnce()
                    }
                }
            } else {
                autoRippleAccNs = 0L
                autoRippleNeedImmediate = false
            }

            val tr = computeTransform(surfaceWidth, surfaceHeight, sim.gridW, sim.gridH)
            lastTransform = tr

            mesh.updateFrom(
                sim = sim,
                xOffset = xOffset,
                xOffsetStep = xOffsetStep,
                scale = tr.scale,
                offsetX = tr.offsetX,
                offsetY = tr.offsetY
            )

            // Consume pending bitmap (GL thread uploads texture; recycle after upload)
            pendingBitmapRef.getAndSet(null)?.let { bmp ->
                replaceTextureWithBitmap(bmp)
            }

            drawFrame()
            EGL14.eglSwapBuffers(eglDisplay, eglSurface)

            // Frame pacing
            val used = System.nanoTime() - now
            val sleepNs = frameNs - used
            if (sleepNs > 0) sleepQuietly(sleepNs / 1_000_000)
        }

        releaseGl()
        releaseEgl()

        // Cleanup pending bitmap
        pendingBitmapRef.getAndSet(null)?.let { if (!it.isRecycled) it.recycle() }

        // Cleanup remembered bitmap
        rememberedBgBitmap?.let { if (!it.isRecycled) it.recycle() }
        rememberedBgBitmap = null
    }

    private fun dropRandomOnce() {
        if (!::sim.isInitialized) return

        val marginX = (sim.gridW * autoRippleMargin).toInt().coerceAtLeast(1)
        val marginY = (sim.gridH * autoRippleMargin).toInt().coerceAtLeast(1)

        val gx = rng.nextInt(
            marginX,
            (sim.gridW - marginX).coerceAtLeast(marginX + 1)
        ).toFloat()

        val gy = rng.nextInt(
            marginY,
            (sim.gridH - marginY).coerceAtLeast(marginY + 1)
        ).toFloat()

        val radius = rng.nextInt(3, 10)
        val strength = -rng.nextInt(55, 110).toFloat()

        sim.addDrop(gx, gy, radius = radius, strength = strength)
    }

    private fun replaceTextureWithBitmap(bmp: Bitmap) {
        if (bmp.isRecycled) return

        if (textureId != 0) {
            oneTex[0] = textureId
            GLES20.glDeleteTextures(1, oneTex, 0)
            textureId = 0
        }

        textureId = GLUtil.loadTextureFromBitmap(bmp)

        if (!bmp.isRecycled) bmp.recycle()

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTex0, 0)
    }

    private fun waitSurfaceValid(timeoutMs: Long): Boolean {
        val start = SystemClock.uptimeMillis()
        while (running && SystemClock.uptimeMillis() - start < timeoutMs) {
            if (holder.surface.isValid) return true
            sleepQuietly(16)
        }
        return holder.surface.isValid
    }

    private fun chooseGrid(surfaceW: Int, surfaceH: Int): Pair<Int, Int> {
        val baseW = 60
        val aspect = surfaceW.toFloat() / surfaceH.toFloat()
        val h = (baseW / aspect).toInt().coerceIn(60, 140)
        return baseW to h
    }

    private fun rebuildSimAndMesh(surfaceW: Int, surfaceH: Int) {
        val (gw, gh) = chooseGrid(surfaceW, surfaceH)
        sim = RippleSimulation(gridW = gw, gridH = gh, damping = 0.86f)
        mesh = WaterMesh(sim.gridW, sim.gridH)
    }

    private fun computeTransform(surfaceW: Int, surfaceH: Int, gridW: Int, gridH: Int): Transform {
        val gw = gridW.toFloat()
        val gh = gridH.toFloat()
        val scale = min(surfaceW / gw, surfaceH / gh)
        val drawW = gw * scale
        val drawH = gh * scale
        val offsetX = (surfaceW - drawW) * 0.5f
        val offsetY = (surfaceH - drawH) * 0.5f
        return Transform(scale, offsetX, offsetY)
    }

    private fun initEgl(): Boolean {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) return false

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) return false

        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 0,
            EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val num = IntArray(1)
        if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, num, 0)) return false
        val eglConfig = configs[0] ?: return false

        val ctxAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
        if (eglContext == null || eglContext == EGL14.EGL_NO_CONTEXT) return false

        val surfAttribs = intArrayOf(EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, holder.surface, surfAttribs, 0)
        if (eglSurface == null || eglSurface == EGL14.EGL_NO_SURFACE) return false

        return EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
    }

    private fun initGlObjects() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDisable(GLES20.GL_BLEND)

        program = buildProgram(Shaders.VS, Shaders.FS_TEX2)
        GLES20.glUseProgram(program)

        aPos = GLES20.glGetAttribLocation(program, "a_position")
        aUv0 = GLES20.glGetAttribLocation(program, "a_texCoord0")
        aUv1 = GLES20.glGetAttribLocation(program, "a_texCoord1")
        uMvp = GLES20.glGetUniformLocation(program, "u_projTrans")
        uTex0 = GLES20.glGetUniformLocation(program, "u_texture0")

        if (textureId == 0) textureId = GLUtil.loadSolidColorTexture()

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTex0, 0)
    }

    private fun drawFrame() {
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(program)

        if (mvpDirty) {
            GLUtil.orthoInto(mvp, 0f, surfaceWidth.toFloat(), surfaceHeight.toFloat(), 0f)
            mvpDirty = false
        }
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        mesh.bind(aPos, aUv0, aUv1)
        mesh.draw()
        mesh.unbind(aPos, aUv0, aUv1)
    }

    private fun releaseGl() {
        if (program != 0) GLES20.glDeleteProgram(program)
        program = 0

        if (textureId != 0) {
            oneTex[0] = textureId
            GLES20.glDeleteTextures(1, oneTex, 0)
            textureId = 0
        }

        if (::mesh.isInitialized) mesh.release()
    }

    private fun releaseEgl() {
        try {
            eglDisplay?.let { d ->
                EGL14.eglMakeCurrent(d, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                eglSurface?.let { EGL14.eglDestroySurface(d, it) }
                eglContext?.let { EGL14.eglDestroyContext(d, it) }
                EGL14.eglTerminate(d)
            }
        } catch (_: Throwable) {
        } finally {
            eglSurface = null
            eglContext = null
            eglDisplay = null
        }
    }

    private fun buildProgram(vertexSrc: String, fragmentSrc: String): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertexSrc)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSrc)
        val prog = GLES20.glCreateProgram()
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glLinkProgram(prog)
        val link = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, link, 0)
        if (link[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(prog)
            GLES20.glDeleteProgram(prog)
            throw IllegalStateException("Program link failed: $log")
        }
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(s)
            GLES20.glDeleteShader(s)
            throw IllegalStateException("Shader compile failed: $log")
        }
        return s
    }

    private fun Thread.joinQuietly() { try { join(800) } catch (_: Throwable) {} }
    private fun sleepQuietly(ms: Long) { try { sleep(ms) } catch (_: Throwable) {} }

    private object Shaders {
        const val VS = """
            attribute vec4 a_position;
            attribute vec2 a_texCoord0;
            attribute vec2 a_texCoord1;

            varying float v_light;
            varying vec2 v_texCoords;

            uniform mat4 u_projTrans;

            void main() {
                gl_Position = u_projTrans * a_position;
                v_texCoords = a_texCoord0;
                v_light = (a_texCoord1.y * 4.0) - pow(a_texCoord1.y * 3.0, 2.0);
                v_light = v_light < -0.2 ? -0.2 : v_light > 1.0 ? 1.0 : v_light;
            }
        """

        const val FS_TEX2 = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying float v_light;
            varying vec2 v_texCoords;
            uniform sampler2D u_texture0;
            void main() {
                vec4 c = texture2D(u_texture0, v_texCoords.xy);
                gl_FragColor = c + vec4(v_light, v_light, v_light, 0.0);
            }
        """
    }

    private object GLUtil {

        fun loadTextureFromBitmap(bmp: Bitmap): Int {
            require(!bmp.isRecycled) { "loadTextureFromBitmap: bitmap recycled" }

            val tex = IntArray(1)
            GLES20.glGenTextures(1, tex, 0)
            val id = tex[0]
            if (id == 0) throw IllegalStateException("glGenTextures failed")

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

            android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
            return id
        }

        fun loadSolidColorTexture(): Int {
            val bmp = createBitmap(1, 1)
            bmp.eraseColor(0xFF000000.toInt())
            val id = loadTextureFromBitmap(bmp)
            bmp.recycle()
            return id
        }

        fun orthoInto(out: FloatArray, left: Float, right: Float, bottom: Float, top: Float) {
            val rml = right - left
            val tmb = top - bottom
            out[0] = 2f / rml; out[1] = 0f;       out[2] = 0f; out[3] = 0f
            out[4] = 0f;       out[5] = 2f / tmb; out[6] = 0f; out[7] = 0f
            out[8] = 0f;       out[9] = 0f;       out[10] = 1f; out[11] = 0f
            out[12] = -(right + left) / rml
            out[13] = -(top + bottom) / tmb
            out[14] = 0f
            out[15] = 1f
        }
    }
}
