package com.merryblue.baseapplication.ui.wallpaper

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.databinding.ActivityStaticWallpaperSettingsBinding
import com.merryblue.baseapplication.service.edge.EdgeLightingOverlayService
import com.merryblue.baseapplication.ui.widget.BottomSheetEdgePermission
import com.merryblue.baseapplication.ui.widget.BottomSheetWallpaperTarget
import com.merryblue.baseapplication.ui.widget.WallpaperTarget
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.base.BaseActivity
import org.app.core.base.extensions.toastMsg

@AndroidEntryPoint
class StaticWallpaperSettingsActivity : BaseActivity<ActivityStaticWallpaperSettingsBinding>() {

    private val prefs by lazy { AppPreferences(this) }
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