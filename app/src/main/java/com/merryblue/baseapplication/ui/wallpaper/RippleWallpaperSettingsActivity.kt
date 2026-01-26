package com.merryblue.baseapplication.ui.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.databinding.ActivityRippleWallpaperSettingsBinding
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_RIPPLE_BG_CHANGED
import com.merryblue.baseapplication.helpers.ripple.WaterDropRenderer
import com.merryblue.baseapplication.service.RippleWallpaperService
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.base.BaseActivity
import timber.log.Timber

@AndroidEntryPoint
class RippleWallpaperSettingsActivity : BaseActivity<ActivityRippleWallpaperSettingsBinding>(), SurfaceHolder.Callback {
    private val prefs by lazy { AppPreferences(this) }
    private var renderer: WaterDropRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(binding.main, true)
    }

    override fun getLayoutId(): Int = R.layout.activity_ripple_wallpaper_settings

    override fun surfaceCreated(holder: SurfaceHolder) {
        renderer = WaterDropRenderer(this, holder).also {
            it.onSurfaceSizeChanged(holder.surfaceFrame.width(), holder.surfaceFrame.height())
            it.setAutoRippleEnabled(prefs.autoRipple)                   // bật random sóng
            it.setAutoRippleIntervalMs(prefs.autoRippleIntervalMs)      // time random
            it.start()

            val path = prefs.backgrondPath
            if (!path.isNullOrBlank()) it.setBackgroundFromFilePath(path)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        renderer?.onSurfaceSizeChanged(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        renderer?.stopAndRelease()
        renderer = null
    }

    override fun onResume() {
        super.onResume()
        renderer?.setPaused(false)
        renderer?.requestRecreate()
    }

    override fun onPause() {
        super.onPause()
        renderer?.setPaused(true)
    }

    override fun setUpViews() {
        initSurfaceView()
        eventClick()
    }

    private fun initSurfaceView() {
        binding.surfaceView.holder.addCallback(this)

        binding.surfaceView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_MOVE) {
                renderer?.onTouch(event.x, event.y)
            }
            true
        }
    }

    private fun eventClick() {
        binding.btnSetWallpaper.setOnClickListener {
            if (isMyLiveWallpaperActive()) {
                sendBroadcast(Intent(ACTION_RIPPLE_BG_CHANGED).setPackage(packageName))
            } else openSetLiveWallpaper()
            finish()
        }

        binding.btnBackWallpaper.setOnClickListener {
            finish()
        }
    }

    private fun openSetLiveWallpaper() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@RippleWallpaperSettingsActivity, RippleWallpaperService::class.java)
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun isMyLiveWallpaperActive(): Boolean {
        val wm = WallpaperManager.getInstance(this)
        val info = wm.wallpaperInfo ?: return false
        return info.component == ComponentName(this, RippleWallpaperService::class.java)
    }

}