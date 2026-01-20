package com.merryblue.baseapplication.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.ui.view.edgelight.EdgeLightingView
import com.merryblue.baseapplication.ui.wallpaper.EdgeWallpaperSettingsActivity.Companion.ACTION_EDGE_WALLPAPER_STATE_CHANGED

class EdgeLightingWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = EdgeEngine()

    inner class EdgeEngine : Engine() {

        private val prefs by lazy { AppPreferences(applicationContext) }
        private val view by lazy { EdgeLightingView(this@EdgeLightingWallpaperService) }
        private val handler = Handler(Looper.getMainLooper())
        private var running = false
        private val frameDelayMs = 16L
        private var lastStateHash = 0
        private var lastW = 0
        private var lastH = 0

        private val frame = object : Runnable {
            override fun run() {
                if (!running) return
                drawFrame()
                handler.postDelayed(this, frameDelayMs)
            }
        }

        private val stateChangedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ACTION_EDGE_WALLPAPER_STATE_CHANGED) {
                    handler.post { applyStateIfNeeded(force = true) }
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            ContextCompat.registerReceiver(this@EdgeLightingWallpaperService, stateChangedReceiver, IntentFilter(ACTION_EDGE_WALLPAPER_STATE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
            view.startAnimationForWallpaper()
            applyStateIfNeeded(force = true)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            if (width != lastW || height != lastH) {
                lastW = width
                lastH = height
                view.layout(0, 0, width, height)
            }

            applyStateIfNeeded(force = true)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            running = visible
            handler.removeCallbacks(frame)
            if (visible) {
                view.startAnimationForWallpaper()
                handler.post(frame)
            } else {
                view.stopAnimationForWallpaper()
            }
        }

        override fun onDestroy() {
            running = false
            handler.removeCallbacks(frame)
            view.stopAnimationForWallpaper()
            try {
                this@EdgeLightingWallpaperService.unregisterReceiver(stateChangedReceiver)
            } catch (_: Throwable) {}
            super.onDestroy()
        }

        private fun applyStateIfNeeded(force: Boolean) {
            val s = prefs.edgeState
            val hash = s.hashCode()
            if (!force && hash == lastStateHash) return
            lastStateHash = hash
            view.applyEdgeState(s)
        }

        private fun drawFrame() {
            val holder = surfaceHolder ?: return
            val canvas = try { holder.lockCanvas() } catch (_: Throwable) { null } ?: return
            try {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                view.draw(canvas)
            } finally {
                try { holder.unlockCanvasAndPost(canvas) } catch (_: Throwable) {}
            }
        }
    }
}
