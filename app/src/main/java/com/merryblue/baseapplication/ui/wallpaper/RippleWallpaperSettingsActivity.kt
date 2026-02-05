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
import com.merryblue.baseapplication.databinding.ActivityRippleWallpaperSettingsBinding
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_RIPPLE_BG_CHANGED
import com.merryblue.baseapplication.helpers.openProperNetworkSettings
import com.merryblue.baseapplication.helpers.ripple.WaterDropRenderer
import com.merryblue.baseapplication.service.edge.EdgeLightingOverlayService
import com.merryblue.baseapplication.service.edge.RippleWallpaperService
import com.merryblue.baseapplication.ui.home.HomeViewModel
import com.merryblue.baseapplication.ui.widget.BottomSheetEdgePermission
import com.merryblue.baseapplication.ui.widget.BottomSheetNoInternet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.base.BaseActivity
import org.app.core.base.extensions.toastMsg

@AndroidEntryPoint
class RippleWallpaperSettingsActivity : BaseActivity<ActivityRippleWallpaperSettingsBinding>(), SurfaceHolder.Callback {

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
                prefs.canChangeLive -> setLiveWallpaperLauncher.launch(changeIntent)
                prefs.canLiveChooser -> setLiveWallpaperLauncher.launch(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
                else -> toastMsg(getString(R.string.this_device_does_not_support_installing_live_wallpaper))
            }
        } catch (_: ActivityNotFoundException) {
            toastMsg(getString(R.string.this_device_does_not_support_installing_live_wallpaper))
        }
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
