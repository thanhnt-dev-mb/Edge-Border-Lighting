package com.merryblue.baseapplication.ui.wallpaper

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.databinding.ActivityStaticWallpaperSettingsBinding
import com.merryblue.baseapplication.helpers.openProperNetworkSettings
import com.merryblue.baseapplication.service.edge.EdgeLightingOverlayService
import com.merryblue.baseapplication.ui.home.HomeViewModel
import com.merryblue.baseapplication.ui.widget.BottomSheetEdgePermission
import com.merryblue.baseapplication.ui.widget.BottomSheetNoInternet
import com.merryblue.baseapplication.ui.widget.BottomSheetWallpaperTarget
import com.merryblue.baseapplication.ui.widget.WallpaperTarget
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.base.BaseActivity
import org.app.core.base.extensions.toastMsg

@AndroidEntryPoint
class StaticWallpaperSettingsActivity : BaseActivity<ActivityStaticWallpaperSettingsBinding>() {

    private val prefs by lazy { AppPreferences(this) }
    private val edgePermissionViewModel: EdgePermissionViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val overlayPermissionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(this)) {
            prefs.edgeState = prefs.edgeState.copy(isEnableEdgeLighting = true)
            startEdgeOverlay()
        } else {
            prefs.edgeState = prefs.edgeState.copy(isEnableEdgeLighting = false)
            finish()
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_static_wallpaper_settings

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        enableEdgeToEdge(binding.main, true)
    }

    override fun setUpViews() {
        prefs.backgroundPath?.let { path ->
            val bitmap = BitmapFactory.decodeFile(path)
            binding.ivStaticWallpaper.setImageBitmap(bitmap)
        }
        registerClicks()
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
        BottomSheetEdgePermission.newInstance().show(supportFragmentManager, BottomSheetEdgePermission.TAG)
    }

    private fun registerClicks() {
        binding.btnSetWallpaper.setOnClickListener { onClickSetLiveWallpaperOrApply() }
        binding.btnBackWallpaper.setOnClickListener { finish() }
    }

    private fun onClickSetLiveWallpaperOrApply() {
        (supportFragmentManager.findFragmentByTag(BottomSheetWallpaperTarget.TAG) as? BottomSheetDialogFragment)?.dismissAllowingStateLoss()
        val bottom = BottomSheetWallpaperTarget { applyStaticWallpaperFromBackground(it) }
        bottom.show(supportFragmentManager, BottomSheetWallpaperTarget.TAG)
    }

    private fun applyStaticWallpaperFromBackground(target: WallpaperTarget) {
        val path = prefs.backgroundPath
        if (path.isNullOrBlank()) {
            toastMsg(getString(R.string.wallpaper_set_failed))
            checkPermissionOverlay()
            return
        }

        try {
            val bitmap = BitmapFactory.decodeFile(path)
            if (bitmap == null) {
                toastMsg(getString(R.string.wallpaper_set_failed))
                checkPermissionOverlay()
                return
            }

            val wm = WallpaperManager.getInstance(this)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                when (target) {
                    WallpaperTarget.HOME -> wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    WallpaperTarget.LOCK -> wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    WallpaperTarget.BOTH -> {
                        wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    }
                }
            } else {
                wm.setBitmap(bitmap)
            }

            toastMsg(getString(R.string.wallpaper_set_success))
        } catch (_: Exception) {
            toastMsg(getString(R.string.wallpaper_set_failed))
        } finally {
            checkPermissionOverlay()
        }
    }

}