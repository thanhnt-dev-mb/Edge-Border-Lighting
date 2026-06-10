package com.merryblue.baseapplication.ui.wallpaper

import android.app.ActivityManager
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.databinding.ActivityVideoWallpaperSettingsBinding
import com.merryblue.baseapplication.enums.InterstitialFunction
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_VIDEO_WALLPAPER_STATE_CHANGED
import com.merryblue.baseapplication.helpers.openProperNetworkSettings
import com.merryblue.baseapplication.helpers.video.VideoDataSource
import com.merryblue.baseapplication.helpers.video.VideoPreloader
import com.merryblue.baseapplication.service.edge.EdgeLightingOverlayService
import com.merryblue.baseapplication.service.edge.VideoWallpaperService
import com.merryblue.baseapplication.ui.home.HomeViewModel
import com.merryblue.baseapplication.ui.widget.BottomSheetEdgePermission
import com.merryblue.baseapplication.ui.widget.BottomSheetNoInternet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.app.core.base.BaseActivity
import org.app.core.base.binding.setOnSingleClickListener
import org.app.core.base.extensions.toastMsg

@AndroidEntryPoint
class VideoWallpaperSettingsActivity : BaseActivity<ActivityVideoWallpaperSettingsBinding>() {

    private val prefs by lazy { AppPreferences(this) }
    private val edgePermissionViewModel: EdgePermissionViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()

    override val nativeHeight: Int
        get() = -1

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
    private var player: ExoPlayer? = null

    override fun getLayoutId(): Int = R.layout.activity_video_wallpaper_settings

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        enableEdgeToEdge(binding.main, true)
    }

    override fun setUpViews() {
        initVideoPreview()
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
        binding.btnSetWallpaper.setOnSingleClickListener {
            showInterstitialBy(InterstitialFunction.SetVideoWallpaper.name) {
                homeViewModel.increaseUsageCount()
                onClickSetLiveWallpaperOrApply()
            }
        }
        binding.btnBackWallpaper.setOnSingleClickListener { finish() }
    }

    @OptIn(UnstableApi::class)
    private fun initVideoPreview() {
        val videoUrl = prefs.videoUrl
        if (videoUrl.isBlank()) {
            binding.loading.visibility = View.GONE
            binding.btnSetWallpaper.isEnabled = false
            binding.btnSetWallpaper.alpha = 0.5f
            binding.btnSetWallpaper.text = getString(R.string.wallpaper_set_failed)
            return
        }

        VideoPreloader.preload(this, videoUrl)

        val mediaSourceFactory = DefaultMediaSourceFactory(VideoDataSource.cachedFactory(this))

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = true

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        binding.loading.visibility = if (state == Player.STATE_READY) View.GONE else View.VISIBLE
                    }
                })

                setMediaItem(MediaItem.fromUri(videoUrl))
                prepare()
            }

        binding.playerView.player = player
    }

    private fun onClickSetLiveWallpaperOrApply() {
        if (isMyLiveWallpaperActive()) {
            sendBroadcast(Intent(ACTION_VIDEO_WALLPAPER_STATE_CHANGED).setPackage(packageName))
            checkPermissionOverlay()
            return
        }

        openSystemLiveWallpaperPicker(ComponentName(this, VideoWallpaperService::class.java))
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

    private fun isMyLiveWallpaperActive(): Boolean {
        val activityManager = this.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return activityManager.getRunningServices(Int.MAX_VALUE).any { it.service.className == VideoWallpaperService::class.java.name }
    }

    override fun onDestroy() {
        binding.playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
