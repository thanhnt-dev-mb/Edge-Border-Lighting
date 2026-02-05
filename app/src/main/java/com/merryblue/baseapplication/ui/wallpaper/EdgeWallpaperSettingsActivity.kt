package com.merryblue.baseapplication.ui.wallpaper

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.databinding.ActivityEdgeWallpaperSettingsBinding
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_EDGE_WALLPAPER_STATE_CHANGED
import com.merryblue.baseapplication.helpers.openProperNetworkSettings
import com.merryblue.baseapplication.service.edge.EdgeLightingOverlayService
import com.merryblue.baseapplication.service.edge.EdgeLightingWallpaperService
import com.merryblue.baseapplication.ui.home.HomeViewModel
import com.merryblue.baseapplication.ui.view.edgelight.model.EdgeLightingState
import com.merryblue.baseapplication.ui.widget.BottomSheetEdgePermission
import com.merryblue.baseapplication.ui.widget.BottomSheetNoInternet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.base.BaseActivity
import org.app.core.base.extensions.toastMsg

@AndroidEntryPoint
class EdgeWallpaperSettingsActivity : BaseActivity<ActivityEdgeWallpaperSettingsBinding>() {
    private lateinit var currentEdgeState: EdgeLightingState
    private val prefs by lazy { AppPreferences(this) }
    private val edgePermissionViewModel: EdgePermissionViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val setLiveWallpaperLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        if (isMyLiveWallpaperActive()) {
            checkPermissionOverlay()
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
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

    override fun setUpObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    edgePermissionViewModel.edgePermission.collect {
                        val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${packageName}".toUri())
                        overlayPermissionLauncher.launch(i)
                    }
                }

                launch {
                    homeViewModel.connectionState.collectLatest {
                        onNetworkStateChanged(it)
                        handleNoInternetBottomSheet(it)
                    }
                }
            }
        }
    }

    private fun handleNoInternetBottomSheet(isConnected: Boolean) {
        val fm = supportFragmentManager
        val current = fm.findFragmentByTag(BottomSheetNoInternet.TAG) as? BottomSheetDialogFragment

        if (isConnected) {
            if (current?.dialog?.isShowing == true) current.dismissAllowingStateLoss()
            return
        }

        if (current?.dialog?.isShowing == true) return

        BottomSheetNoInternet.newInstance {
            this.openProperNetworkSettings()
        }.show(fm, BottomSheetNoInternet.TAG)
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

        openSystemLiveWallpaperPicker(ComponentName(this, EdgeLightingWallpaperService::class.java))
    }

    private fun checkPermissionOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            toastMsg(getString(R.string.live_wallpaper_set_success))
            showBottomSheetEdgePermission()
            return
        }

        toastMsg(getString(R.string.live_wallpaper_set_success))
        startEdgeOverlay()
    }

    private fun startEdgeOverlay() {
        prefs.edgeState = currentEdgeState.copy(isEnableEdgeLighting = true)
        ContextCompat.startForegroundService(this, Intent(this, EdgeLightingOverlayService::class.java))
        finish()
    }

    private fun showBottomSheetEdgePermission() {
        (supportFragmentManager.findFragmentByTag(BottomSheetEdgePermission.TAG) as? BottomSheetDialogFragment)?.dismissAllowingStateLoss()
        BottomSheetEdgePermission.newInstance().show(supportFragmentManager, BottomSheetEdgePermission.TAG)
    }

    private fun isMyLiveWallpaperActive(): Boolean {
        val wm = WallpaperManager.getInstance(this)
        val info = wm.wallpaperInfo ?: return false
        return info.packageName == packageName && info.serviceName == EdgeLightingWallpaperService::class.java.name
    }

    private fun openSystemLiveWallpaperPicker(service: ComponentName) {
        val changeIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, service)
        }

        try {
            when {
                prefs.canChangeLive -> setLiveWallpaperLauncher.launch(changeIntent)
                prefs.canLiveChooser -> setLiveWallpaperLauncher.launch(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
                else -> toastMsg(getString(R.string.this_device_does_not_support_installing_live_wallpaper))
            }
        } catch (_: ActivityNotFoundException) {
            toastMsg(getString(R.string.this_device_does_not_support_installing_live_wallpaper))
        }

    }
}
