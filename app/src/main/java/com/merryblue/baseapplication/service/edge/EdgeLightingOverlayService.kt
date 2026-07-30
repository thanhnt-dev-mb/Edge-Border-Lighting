package com.merryblue.baseapplication.service.edge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_CHANGED
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_RESTART
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_STOP
import com.merryblue.baseapplication.ui.view.edgelight.EdgeLightingView
import timber.log.Timber

class EdgeLightingOverlayService: Service() {

    companion object {
        var isRunning = false
    }

    private lateinit var wm: WindowManager
    private var edgeView: EdgeLightingView? = null
    private var lp: WindowManager.LayoutParams? = null
    private var displayManager: DisplayManager? = null
    private var lastDisplaySize: Pair<Int, Int>? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private val prefs by lazy { AppPreferences(applicationContext) }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) {
                mainHandler.post { refreshOverlayLayout() }
            }
        }
    }

    private val edgeStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_EDGE_OVERLAY_CHANGED -> {
                    refreshOverlayLayout(force = true)
                    edgeView?.applyEdgeState(prefs.edgeState)
                }
                ACTION_EDGE_OVERLAY_STOP -> stopSelf()
                ACTION_EDGE_OVERLAY_RESTART -> {
                    hideOverlay()
                    showOverlayIfNeeded()
                    edgeView?.applyEdgeState(prefs.edgeState)
                }
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> showOverlayIfNeeded()
                Intent.ACTION_SCREEN_OFF -> hideOverlay()
                Intent.ACTION_USER_PRESENT -> showOverlayIfNeeded()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (!Settings.canDrawOverlays(this)) {
            isRunning = false
            stopSelf()
            return
        }
        isRunning = true
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(1, buildNotification())

        registerEdgeStateReceiver()
        registerScreenReceiver()
        registerDisplayListener()

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isInteractive) showOverlayIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        unregisterEdgeStateReceiverSafe()
        unregisterReceiverSafe()
        unregisterDisplayListenerSafe()
        mainHandler.removeCallbacksAndMessages(null)
        hideOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshOverlayLayout(force = true)
    }

    private fun registerEdgeStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_EDGE_OVERLAY_CHANGED)
            addAction(ACTION_EDGE_OVERLAY_STOP)
            addAction(ACTION_EDGE_OVERLAY_RESTART)
        }

        ContextCompat.registerReceiver(this, edgeStateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }


    private fun unregisterEdgeStateReceiverSafe() {
        try { unregisterReceiver(edgeStateReceiver) } catch (_: Throwable) {}
    }

    private fun showOverlayIfNeeded() {
        if (!Settings.canDrawOverlays(this)) return

        edgeView?.let {
            it.setAnimationEnabled(true)
            refreshOverlayLayout(force = true)
            return
        }

        val savedState = prefs.edgeState

        val view = EdgeLightingView(this).apply {
            setAnimationEnabled(true)
            applyEdgeState(savedState)
            systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val overlayFlags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            overlayFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }

             windowAnimations = 0
        }

        try {
            lp = params
            edgeView = view
            wm.addView(view, params)
            refreshOverlayLayout(force = true)
        } catch (_: Throwable) {
            edgeView = null
            lp = null
            try { wm.removeView(view) } catch (_: Throwable) {}
        }
    }

    private fun hideOverlay() {
        edgeView?.let {
            try { wm.removeView(it) } catch (_: Throwable) {}
        }
        edgeView = null
        lp = null
        lastDisplaySize = null
    }

    private fun refreshOverlayLayout(force: Boolean = false) {
        val view = edgeView ?: return
        val params = lp ?: return
        val displaySize = getRealScreenSizePx()
        val isSameSize = displaySize == lastDisplaySize
        val isMatchParent = params.width == WindowManager.LayoutParams.MATCH_PARENT &&
                params.height == WindowManager.LayoutParams.MATCH_PARENT

        if (!force && isSameSize && isMatchParent) return

        lastDisplaySize = displaySize
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0

        try {
            wm.updateViewLayout(view, params)
            view.requestLayout()
            view.post {
                view.applyEdgeState(prefs.edgeState)
                view.invalidate()
            }
        } catch (t: Throwable) {
            Timber.w(t, "Failed to refresh Edge Lighting overlay layout")
        }
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(screenReceiver, filter)
        }
    }

    private fun unregisterReceiverSafe() {
        try { unregisterReceiver(screenReceiver) } catch (_: Throwable) {}
    }

    private fun registerDisplayListener() {
        displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        displayManager?.registerDisplayListener(displayListener, mainHandler)
    }

    private fun unregisterDisplayListenerSafe() {
        try { displayManager?.unregisterDisplayListener(displayListener) } catch (_: Throwable) {}
        displayManager = null
    }

    private fun buildNotification(): Notification {
        val channelId = "edge_overlay"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Edge Lighting", NotificationManager.IMPORTANCE_LOW)
        )
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Edge Lighting run")
            .setOngoing(true)
            .build()
    }

    private fun getRealScreenSizePx(): Pair<Int, Int> {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        val fallbackDisplay = wm.defaultDisplay
        val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY) ?: fallbackDisplay
        @Suppress("DEPRECATION")
        display.getRealMetrics(dm)
        return dm.widthPixels to dm.heightPixels
    }
}
