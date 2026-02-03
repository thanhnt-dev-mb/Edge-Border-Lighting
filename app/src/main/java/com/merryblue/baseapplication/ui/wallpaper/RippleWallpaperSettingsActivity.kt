package com.merryblue.baseapplication.ui.wallpaper

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.databinding.ActivityRippleWallpaperSettingsBinding
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_RIPPLE_BG_CHANGED
import com.merryblue.baseapplication.helpers.ripple.WaterDropRenderer
import com.merryblue.baseapplication.service.edge.EdgeLightingOverlayService
import com.merryblue.baseapplication.service.edge.RippleWallpaperService
import com.merryblue.baseapplication.ui.widget.BottomSheetEdgePermission
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.base.BaseActivity
import org.app.core.base.extensions.toastMsg

@AndroidEntryPoint
class RippleWallpaperSettingsActivity : BaseActivity<ActivityRippleWallpaperSettingsBinding>(), SurfaceHolder.Callback {

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
    private var renderer: WaterDropRenderer? = null

    override fun getLayoutId(): Int = R.layout.activity_ripple_wallpaper_settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(binding.main, true)
    }

    override fun setUpViews() {
        initSurfaceView()
        registerClicks()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initSurfaceView() {
        binding.surfaceView.holder.addCallback(this)

        binding.surfaceView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_MOVE) {
                renderer?.onTouch(event.x, event.y)
            }
            true
        }
    }

    private fun registerClicks() {
        binding.btnSetWallpaper.setOnClickListener { onClickSetLiveWallpaperOrApply() }
        binding.btnBackWallpaper.setOnClickListener { finish() }
    }

    private fun onClickSetLiveWallpaperOrApply() {
        if (isMyLiveWallpaperActive()) {
            sendBroadcast(Intent(ACTION_RIPPLE_BG_CHANGED).setPackage(packageName))
            checkPermissionOverlay()
            return
        }

        openSystemLiveWallpaperPicker(ComponentName(this, RippleWallpaperService::class.java))
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
        return info.component == ComponentName(this, RippleWallpaperService::class.java)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        renderer = WaterDropRenderer(this, holder).also {
            it.onSurfaceSizeChanged(holder.surfaceFrame.width(), holder.surfaceFrame.height())
            it.setAutoRippleEnabled(prefs.autoRipple)
            it.setAutoRippleIntervalMs(prefs.autoRippleIntervalMs)
            it.start()

            val path = prefs.backgroundPath
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
}
