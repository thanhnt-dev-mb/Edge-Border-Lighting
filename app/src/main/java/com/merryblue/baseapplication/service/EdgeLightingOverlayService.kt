package com.merryblue.baseapplication.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_CHANGED
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_RESTART
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_OVERLAY_STOP
import com.merryblue.baseapplication.ui.view.edgelight.EdgeLightingView

class EdgeLightingOverlayService: Service() {

    private lateinit var wm: WindowManager
    private var edgeView: EdgeLightingView? = null
    private var lp: WindowManager.LayoutParams? = null

    private val prefs by lazy { AppPreferences(applicationContext) }

    private val edgeStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_EDGE_OVERLAY_CHANGED -> edgeView?.applyEdgeState(prefs.edgeState)
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
            stopSelf()
            return
        }

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(1, buildNotification())

        registerEdgeStateReceiver()
        registerScreenReceiver()

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isInteractive) showOverlayIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        unregisterEdgeStateReceiverSafe()
        unregisterReceiverSafe()
        hideOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerEdgeStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_EDGE_OVERLAY_CHANGED)
            addAction(ACTION_EDGE_OVERLAY_STOP)
            addAction(ACTION_EDGE_OVERLAY_RESTART)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(edgeStateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(edgeStateReceiver, filter)
        }
    }

    private fun unregisterEdgeStateReceiverSafe() {
        try { unregisterReceiver(edgeStateReceiver) } catch (_: Throwable) {}
    }


    private fun showOverlayIfNeeded() {
        if (!Settings.canDrawOverlays(this)) return

        edgeView?.let {
            it.setAnimationEnabled(true)
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

        val (realW, realH) = getRealScreenSizePx()

        val params = WindowManager.LayoutParams(realW, realH, type, overlayFlags, PixelFormat.TRANSLUCENT).apply {
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

    private fun buildNotification(): Notification {
        val channelId = "edge_overlay"
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Edge Lighting", NotificationManager.IMPORTANCE_LOW)
            )
        }
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle("Edge Lighting run")
            .setOngoing(true)
            .build()
    }

    private fun getRealScreenSizePx(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= 30) {
            val bounds = wm.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION") val display = wm.defaultDisplay
            @Suppress("DEPRECATION") display.getRealMetrics(dm)
            dm.widthPixels to dm.heightPixels
        }
    }
}
