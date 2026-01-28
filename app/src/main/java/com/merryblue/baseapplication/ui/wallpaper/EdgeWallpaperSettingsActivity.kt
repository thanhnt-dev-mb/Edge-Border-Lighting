package com.merryblue.baseapplication.ui.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.databinding.ActivityEdgeWallpaperSettingsBinding
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_WALLPAPER_STATE_CHANGED
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
        showEdgePreview()

        binding.btnBackWallpaper.setOnClickListener { finish() }

        binding.btnSetWallpaper.setOnClickListener { onClickSetLiveWallpaperOrApply() }
    }

    private fun showEdgePreview() {
        val state = prefs.edgeState
        prefs.backgroundPath?.let { path ->
            binding.edgeViewWallpaper.applyEdgeState(state)
            binding.edgeViewWallpaper.setBackgroundFromFilePath(path)
        }
    }

    private fun onClickSetLiveWallpaperOrApply() {
        prefs.clearCacheEdgeState()

        if (isMyLiveWallpaperActive()) {
            sendBroadcast(Intent(ACTION_EDGE_WALLPAPER_STATE_CHANGED).setPackage(packageName))
        } else {
            openSystemLiveWallpaperPicker()
        }
        finish()
    }

    private fun isMyLiveWallpaperActive(): Boolean {
        val wm = WallpaperManager.getInstance(this)
        val info = wm.wallpaperInfo ?: return false
        return info.component == ComponentName(this, EdgeLightingWallpaperService::class.java)
    }

    private fun openSystemLiveWallpaperPicker() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@EdgeWallpaperSettingsActivity, EdgeLightingWallpaperService::class.java)
            )
        }
        startActivity(intent)
    }
}
