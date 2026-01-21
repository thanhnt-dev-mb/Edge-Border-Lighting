package com.merryblue.baseapplication.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.View
import androidx.core.content.ContextCompat
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.ui.view.edgelight.EdgeLightingView
import com.merryblue.baseapplication.ui.wallpaper.EdgeWallpaperSettingsActivity.Companion.ACTION_EDGE_WALLPAPER_STATE_CHANGED
import timber.log.Timber

class EdgeLightingWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = EdgeEngine()

    inner class EdgeEngine : Engine() {

        private val prefs by lazy { AppPreferences(applicationContext) }

        private val view by lazy { EdgeLightingView(this@EdgeLightingWallpaperService) }

        // IMPORTANT:
        // Choreographer callbacks must run on the main thread/looper.
        // We use this handler to post state updates safely to the main thread.
        private val mainHandler = Handler(Looper.getMainLooper())

        // Wallpaper visibility state. When not visible we stop rendering to save resources.
        private var running = false

        // Used to avoid re-applying the same state on every broadcast / surface callback.
        private var lastStateHash = 0

        // Cache the last surface size so we only re-measure/re-layout the view when size actually changes.
        private var lastW = 0
        private var lastH = 0

        // A simple "heartbeat" so your SettingsActivity can detect whether the live wallpaper engine is alive.
        private var nextHeartbeatAt = 0L
        private val heartbeatIntervalMs = 2000L

        // --- VSYNC render loop ---
        // Instead of postDelayed(33ms) (which easily becomes jittery), use Choreographer to align with VSYNC.
        private val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!running) return

                // Draw one frame to the wallpaper surface.
                drawFrame()

                // Schedule the next frame on the next VSYNC.
                Choreographer.getInstance().postFrameCallback(this)
            }
        }

        // Start rendering frames. Called when wallpaper becomes visible.
        private fun startLoop() {
            if (running) {
                // Reset heartbeat so "is wallpaper alive" checks become accurate right after visibility.
                nextHeartbeatAt = 0L

                // Start the view's internal animator (updates progress / pattern phase, etc.).
                // Note: for wallpaper, the continuous rendering loop is what actually refreshes the Surface.
                view.startAnimationForWallpaper()

                // Make sure we don't register the same callback twice.
                Choreographer.getInstance().removeFrameCallback(frameCallback)
                Choreographer.getInstance().postFrameCallback(frameCallback)
            }
        }

        // Stop rendering frames. Called when wallpaper is no longer visible.
        private fun stopLoop() {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            view.stopAnimationForWallpaper()
        }

        // Listen for SettingsActivity broadcasts indicating the state has changed.
        private val stateChangedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ACTION_EDGE_WALLPAPER_STATE_CHANGED) {
                    // Apply the latest state on the main thread (safe for view updates and Glide usage).
                    mainHandler.post { applyStateIfNeeded(force = true) }
                    Timber.tag("Log_EdgeView").d("ACTION_EDGE_WALLPAPER_STATE_CHANGED")
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            // Register state-change broadcast receiver (not exported).
            ContextCompat.registerReceiver(
                this@EdgeLightingWallpaperService,
                stateChangedReceiver,
                IntentFilter(ACTION_EDGE_WALLPAPER_STATE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

            // Start internal animation and apply the first state (if view has size, it will apply immediately).
            view.startAnimationForWallpaper()
            applyStateIfNeeded(force = true)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            // If surface size changed, re-measure and re-layout the view to match the wallpaper surface.
            if (width != lastW || height != lastH) {
                lastW = width
                lastH = height

                val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
                val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                view.measure(widthSpec, heightSpec)
                view.layout(0, 0, width, height)
            }

            // Re-apply state after size changes (important for shader / bg scaling / path caches).
            mainHandler.post { applyStateIfNeeded(force = true) }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            running = visible
            if (visible) {
                startLoop()
            } else {
                stopLoop()
            }
        }

        override fun onDestroy() {
            running = false
            stopLoop()

            // Unregister receiver safely.
            try {
                this@EdgeLightingWallpaperService.unregisterReceiver(stateChangedReceiver)
            } catch (_: Throwable) {
            }

            super.onDestroy()
        }

        // Apply the latest EdgeLightingState only when necessary (hash changed) or when forced.
        private fun applyStateIfNeeded(force: Boolean) {
            val s = prefs.edgeState
            val hash = s.hashCode()
            if (!force && hash == lastStateHash) return
            lastStateHash = hash

            // If view isn't measured yet, retry later.
            if (view.width == 0 || view.height == 0) {
                mainHandler.post { applyStateIfNeeded(force = true) }
                return
            }

            // Apply all visual parameters (colors, notch, speed, background, etc.).
            view.applyEdgeState(s)
        }

        // Draw a single frame onto the wallpaper Surface.
        private fun drawFrame() {
            // Update heartbeat for "wallpaper alive" detection.
            val now = SystemClock.elapsedRealtime()
            if (now >= nextHeartbeatAt) {
                nextHeartbeatAt = now + heartbeatIntervalMs
                prefs.edgeWallpaperLastSeenElapsed = now
            }

            val holder = surfaceHolder ?: return

            // Prefer HardwareCanvas (API 23+) for smoother path/shader drawing.
            val canvas = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    holder.surface.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }
            } catch (_: Throwable) {
                null
            } ?: return

            try {
                // Clear previous frame (transparent wallpaper surface).
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

                // Ask the view to render itself into this canvas.
                view.draw(canvas)
            } catch (_: Throwable) {
                // Ignore drawing exceptions to avoid crashing wallpaper process.
            } finally {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (_: Throwable) {
                }
            }
        }
    }
}
