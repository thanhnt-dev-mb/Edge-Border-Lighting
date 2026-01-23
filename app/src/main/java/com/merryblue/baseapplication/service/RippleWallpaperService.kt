package com.merryblue.baseapplication.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.EGL14
import android.opengl.EGLExt
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.helpers.ripple.WaterCore
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.concurrent.thread

/**
 * Live Wallpaper Service - Ripple/Water effect
 *
 * Notes:
 * - EGL ES3 bit must be EGLExt.EGL_OPENGL_ES3_BIT_KHR (NOT in EGL14).
 * - We pick EGLConfig with fallback (alpha 8 -> alpha 0), keep ES3 context.
 * - Touch events are handled on wallpaper engine thread, then forwarded to GL render thread safely.
 */
class RippleWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = RippleEngine()

    inner class RippleEngine : Engine(), Runnable {

        private val preferences by lazy { AppPreferences(this@RippleWallpaperService) }
        private val client = OkHttpClient()
        private val core = WaterCore()

        // EGL
        private var eglDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface = EGL14.EGL_NO_SURFACE
        private var eglConfig: android.opengl.EGLConfig? = null

        @Volatile private var running = false
        @Volatile private var visible = false
        private var renderThread: Thread? = null

        // Queue touches from UI thread to render thread
        private val touchQueue = ArrayDeque<FloatArray>(64)
        private val touchLock = Any()

        // Bitmap downloaded from URL (uploaded on GL thread later)
        @Volatile private var pendingBitmap: Bitmap? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)

            // Download image async (NOT GL thread)
            loadBitmapFromUrl(preferences.rippleEffectUrl) { bmp ->
                pendingBitmap = bmp
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) startRender() else stopRender()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            stopRender()
            super.onSurfaceDestroyed(holder)
        }

        override fun onTouchEvent(event: MotionEvent) {
            if (!visible) return
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_MOVE) {
                val frame = surfaceHolder.surfaceFrame
                val w = frame.width().coerceAtLeast(1)
                val h = frame.height().coerceAtLeast(1)
                val nx = (event.x / w.toFloat()).coerceIn(0f, 1f)
                val ny = (event.y / h.toFloat()).coerceIn(0f, 1f)

                synchronized(touchLock) {
                    if (touchQueue.size >= 64) touchQueue.removeFirst()
                    touchQueue.addLast(floatArrayOf(nx, ny))
                }
            }
        }

        private fun startRender() {
            if (running) return
            running = true
            renderThread = Thread(this, "RippleWallpaperRender").also { it.start() }
        }

        private fun stopRender() {
            running = false
            try {
                renderThread?.join(600)
            } catch (_: Throwable) {
            }
            renderThread = null
        }

        override fun run() {
            try {
                initEgl()

                // Important: now we are on GL thread with current context
                core.initGL()

                // Set initial viewport
                val frame = surfaceHolder.surfaceFrame
                core.setViewport(frame.width().coerceAtLeast(1), frame.height().coerceAtLeast(1))

                // Upload bitmap if already downloaded (safe, GL thread)
                pendingBitmap?.let { bmp ->
                    // Recommended approach: upload immediately on GL thread, then recycle safely.
                    core.updateBaseBitmap(bmp) // make sure your WaterCore does NOT defer upload past this call
                    bmp.recycle()
                    pendingBitmap = null
                }

                // Render loop
                while (running) {
                    if (!visible) {
                        try { Thread.sleep(60) } catch (_: Throwable) {}
                        continue
                    }

                    // Update viewport if changed
                    val f = surfaceHolder.surfaceFrame
                    core.setViewport(f.width().coerceAtLeast(1), f.height().coerceAtLeast(1))

                    // Consume touches
                    synchronized(touchLock) {
                        while (touchQueue.isNotEmpty()) {
                            val p = touchQueue.removeFirst()
                            core.addDrop(p[0], p[1], strength = 0.65f, radius = 0.025f)
                        }
                    }

                    // If bitmap arrives later, upload it now
                    pendingBitmap?.let { bmp ->
                        core.updateBaseBitmap(bmp)
                        bmp.recycle()
                        pendingBitmap = null
                    }

                    core.drawFrame()

                    // Present
                    try {
                        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, System.nanoTime())
                    } catch (_: Throwable) {
                        // Some devices may not support presentation time; safe to ignore
                    }
                    EGL14.eglSwapBuffers(eglDisplay, eglSurface)

                    // ~60fps
                    try { Thread.sleep(16) } catch (_: Throwable) {}
                }
            } catch (t: Throwable) {
                Log.e("RippleWallpaper", "Render thread crashed", t)
            } finally {
                // Cleanup
                try { core.releaseGL() } catch (_: Throwable) {}
                destroyEgl()

                // Avoid leaking bitmap
                pendingBitmap?.let {
                    try { if (!it.isRecycled) it.recycle() } catch (_: Throwable) {}
                    pendingBitmap = null
                }
            }
        }

        /**
         * EGL init with robust config selection:
         * - Prefer ES3 config.
         * - Fallback alpha 8 -> alpha 0.
         * - If ES3 config unavailable, fallback to ES2 (ONLY if your renderer supports ES2).
         *
         * If you require ES3-only (recommended for #version 300 es + GLES30),
         * set allowEs2Fallback=false below.
         */
        private fun initEgl() {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) error("No EGL display")

            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                error("eglInitialize failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
            }

            val allowEs2Fallback = false // keep ES3-only by default (your shaders are #version 300 es)

            eglConfig = chooseConfigWithFallback(eglDisplay, allowEs2Fallback)
                ?: error("eglChooseConfig failed (all attempts). err=0x${Integer.toHexString(EGL14.eglGetError())}")

            val cfg = eglConfig!!

            eglContext = createContextEs3(eglDisplay, cfg)
                ?: error("eglCreateContext ES3 failed. err=0x${Integer.toHexString(EGL14.eglGetError())}")

            val surfaceAttrib = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, cfg, surfaceHolder.surface, surfaceAttrib, 0)
            if (eglSurface == EGL14.EGL_NO_SURFACE) {
                error("eglCreateWindowSurface failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
            }

            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                error("eglMakeCurrent failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
            }
        }

        private fun chooseConfigWithFallback(
            display: android.opengl.EGLDisplay,
            allowEs2Fallback: Boolean
        ): android.opengl.EGLConfig? {

            val ES3 = EGLExt.EGL_OPENGL_ES3_BIT_KHR
            val ES2 = EGL14.EGL_OPENGL_ES2_BIT

            // Try list: ES3 alpha8 -> ES3 alpha0 -> (optional) ES2 alpha8 -> ES2 alpha0
            val attempts = if (allowEs2Fallback) {
                arrayOf(
                    intArrayOf(ES3, 8),
                    intArrayOf(ES3, 0),
                    intArrayOf(ES2, 8),
                    intArrayOf(ES2, 0),
                )
            } else {
                arrayOf(
                    intArrayOf(ES3, 8),
                    intArrayOf(ES3, 0),
                )
            }

            for (a in attempts) {
                val renderable = a[0]
                val alpha = a[1]

                val attribList = intArrayOf(
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, alpha,
                    EGL14.EGL_RENDERABLE_TYPE, renderable,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                    EGL14.EGL_NONE
                )

                val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
                val num = IntArray(1)
                val ok = EGL14.eglChooseConfig(display, attribList, 0, configs, 0, 1, num, 0)
                if (ok && num[0] > 0 && configs[0] != null) {
                    return configs[0]
                }
            }
            return null
        }

        private fun createContextEs3(
            display: android.opengl.EGLDisplay,
            config: android.opengl.EGLConfig
        ): android.opengl.EGLContext? {
            val ctxAttrib = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL14.EGL_NONE
            )
            val ctx = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, ctxAttrib, 0)
            return if (ctx != EGL14.EGL_NO_CONTEXT) ctx else null
        }

        private fun destroyEgl() {
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(
                    eglDisplay,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT
                )

                if (eglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                }
                if (eglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                }
                EGL14.eglTerminate(eglDisplay)
            }

            eglDisplay = EGL14.EGL_NO_DISPLAY
            eglSurface = EGL14.EGL_NO_SURFACE
            eglContext = EGL14.EGL_NO_CONTEXT
            eglConfig = null
        }

        private fun loadBitmapFromUrl(url: String, onDone: (Bitmap) -> Unit) {
            thread(name = "RippleWallpaperImageLoader") {
                try {
                    val req = Request.Builder()
                        .url(url)
                        // Some CDNs require UA
                        .header("User-Agent", "Mozilla/5.0")
                        .build()

                    val resp = client.newCall(req).execute()
                    if (!resp.isSuccessful) {
                        Log.e("RippleWallpaper", "Image load failed code=${resp.code}")
                        resp.close()
                        return@thread
                    }

                    val bytes = resp.body?.bytes()
                    resp.close()

                    if (bytes == null || bytes.isEmpty()) {
                        Log.e("RippleWallpaper", "Image body empty")
                        return@thread
                    }

                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp == null) {
                        Log.e("RippleWallpaper", "Bitmap decode failed")
                        return@thread
                    }

                    onDone(bmp)
                } catch (t: Throwable) {
                    Log.e("RippleWallpaper", "Image load exception", t)
                }
            }
        }
    }
}
