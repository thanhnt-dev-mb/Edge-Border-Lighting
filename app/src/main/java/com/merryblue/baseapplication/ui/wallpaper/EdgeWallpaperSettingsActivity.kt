package com.merryblue.baseapplication.ui.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.databinding.ActivityEdgeWallpaperSettingsBinding
import com.merryblue.baseapplication.service.EdgeLightingWallpaperService
import org.app.core.base.BaseActivity

class EdgeWallpaperSettingsActivity : BaseActivity<ActivityEdgeWallpaperSettingsBinding>() {

    private val prefs by lazy { AppPreferences(this) }

    override fun getLayoutId(): Int = R.layout.activity_edge_wallpaper_settings

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        enableEdgeToEdge(binding.main, true)
    }

    override fun setUpViews() {
        val state = prefs.edgeState
        binding.apply {
            edgeViewWallpaper.applyEdgeState(state)
            btnBackWallpaper.setOnClickListener { finish() }
            btnSetWallpaper.setOnClickListener { onClickSetLiveWallpaperOrApply() }
        }
    }

    private fun onClickSetLiveWallpaperOrApply() {
        if (!isMyLiveWallpaperActive()) {
            openSystemLiveWallpaperPicker()
            return
        }
        sendBroadcast(Intent(ACTION_EDGE_WALLPAPER_STATE_CHANGED))
        finish()
    }


    private fun isMyLiveWallpaperActive(): Boolean {
        val last = prefs.edgeWallpaperLastSeenElapsed
        val now = android.os.SystemClock.elapsedRealtime()
        return last > 0L && (now - last) <= 6000L
    }

    private fun openSystemLiveWallpaperPicker() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@EdgeWallpaperSettingsActivity, EdgeLightingWallpaperService::class.java)
            )
        }
        startActivity(intent)
        finish()
    }

    companion object {
        const val ACTION_EDGE_WALLPAPER_STATE_CHANGED = "ACTION_EDGE_WALLPAPER_STATE_CHANGED"
    }
}
