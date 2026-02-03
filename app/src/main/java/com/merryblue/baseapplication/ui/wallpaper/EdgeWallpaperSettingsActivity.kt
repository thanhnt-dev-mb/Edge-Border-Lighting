package com.merryblue.baseapplication.ui.wallpaper

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.databinding.ActivityEdgeWallpaperSettingsBinding
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_WALLPAPER_STATE_CHANGED
import com.merryblue.baseapplication.service.edge.EdgeLightingOverlayService
import com.merryblue.baseapplication.service.edge.EdgeLightingWallpaperService
import com.merryblue.baseapplication.ui.view.edgelight.model.EdgeLightingState
import com.merryblue.baseapplication.ui.widget.BottomSheetEdgePermission
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.base.BaseActivity
import org.app.core.base.extensions.toastMsg
import kotlin.getValue

@AndroidEntryPoint
class EdgeWallpaperSettingsActivity : BaseActivity<ActivityEdgeWallpaperSettingsBinding>() {
    private val prefs by lazy { AppPreferences(this) }
    private lateinit var currentEdgeState: EdgeLightingState

    private val overlayPermissionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(this)) {
            startEdgeOverlay()
        } else {
            prefs.edgeState = currentEdgeState.copy(isEnableEdgeLighting = false)
            finish()
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_edge_wallpaper_settings

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        enableEdgeToEdge(binding.main, true)
    }

    override fun setUpViews() {
        showEdgePreview()
        registerOnClick()
    }

    private fun registerOnClick() {
        binding.btnBackWallpaper.setOnClickListener { finish() }
        binding.btnSetWallpaper.setOnClickListener { onClickSetLiveWallpaperOrApply() }
    }

    private fun showEdgePreview() {
        currentEdgeState = prefs.edgeState

        prefs.backgroundPath?.let { path ->
            binding.edgeViewWallpaper.applyEdgeState(currentEdgeState.copy(isEnableEdgeLighting = true))
            binding.edgeViewWallpaper.setBackgroundFromFilePath(path)
        }
    }

    private fun onClickSetLiveWallpaperOrApply() {
        if (isMyLiveWallpaperActive()) {
            sendBroadcast(Intent(ACTION_EDGE_WALLPAPER_STATE_CHANGED).setPackage(packageName))
            checkPermissionOverlay()
            return
        }

        prefs.edgeState = currentEdgeState.copy(isEnableEdgeLighting = true)
        openSystemLiveWallpaperPicker(ComponentName(this, EdgeLightingWallpaperService::class.java))
    }

    private fun checkPermissionOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            showBottomSheetEdgePermission()
            return
        }
        startEdgeOverlay()
    }

    private fun startEdgeOverlay() {
        ContextCompat.startForegroundService(this, Intent(this, EdgeLightingOverlayService::class.java))
        finish()
    }

    private fun showBottomSheetEdgePermission() {
        (supportFragmentManager.findFragmentByTag(BottomSheetEdgePermission.TAG) as? BottomSheetDialogFragment)?.dismissAllowingStateLoss()
        val bottom = BottomSheetEdgePermission.newInstance {
            val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${packageName}".toUri())
            overlayPermissionLauncher.launch(i)
        }
        bottom.show(supportFragmentManager, BottomSheetEdgePermission.TAG)
    }

    private fun isMyLiveWallpaperActive(): Boolean {
        val wm = WallpaperManager.getInstance(this)
        val info = wm.wallpaperInfo ?: return false
        return info.component == ComponentName(this, EdgeLightingWallpaperService::class.java)
    }

    private fun openSystemLiveWallpaperPicker(service: ComponentName) {
        val changeIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, service)
        }

        try {
            when {
                prefs.canChangeLive -> startActivity(changeIntent)
                prefs.canLiveChooser -> startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
                else -> toastMsg(getString(R.string.this_device_does_not_support_installing_live_wallpaper))
            }
        } catch (_: ActivityNotFoundException) {
            toastMsg(getString(R.string.this_device_does_not_support_installing_live_wallpaper))
        }

        checkPermissionOverlay()
    }
}
