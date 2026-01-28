package com.merryblue.baseapplication.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_RIPPLE_BG_CHANGED
import com.merryblue.baseapplication.helpers.ripple.WaterDropRenderer
import timber.log.Timber

class RippleWallpaperService : WallpaperService() {

    private val prefs by lazy { AppPreferences(this) }

    override fun onCreateEngine(): Engine = WaterEngine()

    inner class WaterEngine : Engine() {

        private var renderer: WaterDropRenderer? = null
        private var visible = false

        private var xOffset = 0f
        private var xOffsetStep = 0f

        private var isReceiverRegistered = false

        private val bgChangedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != ACTION_RIPPLE_BG_CHANGED) return

                val path = prefs.backgroundPath
                if (path.isNullOrBlank()) return

                renderer?.apply {
                    setBackgroundFromFilePath(path)
                    if (visible) requestRecreate()
                }
            }
        }

        private fun registerBgReceiverIfNeeded() {
            if (isReceiverRegistered) return
            val filter = IntentFilter(ACTION_RIPPLE_BG_CHANGED)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                this@RippleWallpaperService.registerReceiver(bgChangedReceiver, filter,RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                this@RippleWallpaperService.registerReceiver(bgChangedReceiver, filter)
            }
            isReceiverRegistered = true
        }

        private fun unregisterBgReceiverIfNeeded() {
            if (!isReceiverRegistered) return
            try {
                this@RippleWallpaperService.unregisterReceiver(bgChangedReceiver)
            } catch (_: Throwable) {
            } finally {
                isReceiverRegistered = false
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            registerBgReceiverIfNeeded()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)

            val w = holder.surfaceFrame.width().coerceAtLeast(1)
            val h = holder.surfaceFrame.height().coerceAtLeast(1)

            if (renderer == null) {
                renderer = WaterDropRenderer(this@RippleWallpaperService, holder).also {
                    it.onSurfaceSizeChanged(w, h)
                    it.setOffsets(xOffset, xOffsetStep)
                    it.setPaused(!visible)
                    it.start()

                    val path = prefs.backgroundPath
                    if (!path.isNullOrBlank()) it.setBackgroundFromFilePath(path)
                }
            } else {
                renderer?.onSurfaceSizeChanged(w, h)
                renderer?.requestRecreate()
            }
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            super.onSurfaceRedrawNeeded(holder)
            renderer?.onSurfaceSizeChanged(holder.surfaceFrame.width(), holder.surfaceFrame.height())
            renderer?.requestRecreate()
            renderer?.setPaused(!visible)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            renderer?.onSurfaceSizeChanged(width, height)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            this.visible = visible
            renderer?.apply {
                setPaused(!visible)
                if (visible) requestRecreate()

                setAutoRippleEnabled(visible && prefs.autoRipple)
                setAutoRippleIntervalMs(prefs.autoRippleIntervalMs)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            renderer?.stopAndRelease()
            renderer = null
        }

        override fun onDestroy() {
            super.onDestroy()
            unregisterBgReceiverIfNeeded()
            renderer?.stopAndRelease()
            renderer = null
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            this.xOffset = xOffset
            this.xOffsetStep = xOffsetStep
            renderer?.setOffsets(xOffset, xOffsetStep)
        }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            if (!visible) return
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_MOVE) {
                renderer?.onTouch(event.x, event.y)
            }
        }
    }
}
