package com.merryblue.baseapplication.ui.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
        if (isMyLiveWallpaperActive()) {
            Toast.makeText(this, "applied", Toast.LENGTH_SHORT).show()
            sendBroadcast(Intent(ACTION_EDGE_WALLPAPER_STATE_CHANGED))
        } else {
            openSystemLiveWallpaperPicker()
        }
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

    companion object {
        const val ACTION_EDGE_WALLPAPER_STATE_CHANGED = "ACTION_EDGE_WALLPAPER_STATE_CHANGED"
    }
}
