package com.merryblue.baseapplication.ui.wallpaper

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_VIDEO_WALLPAPER_STATE_CHANGED
import com.merryblue.baseapplication.helpers.video.VideoDataSource
import com.merryblue.baseapplication.helpers.video.VideoPreloader
import com.merryblue.baseapplication.service.edge.EdgeLightingOverlayService
import com.merryblue.baseapplication.service.edge.VideoWallpaperService
import com.merryblue.baseapplication.ui.widget.BottomSheetEdgePermission
import dagger.hilt.android.AndroidEntryPoint
import org.app.core.base.BaseActivity
import org.app.core.base.extensions.toastMsg
import kotlin.getValue

@AndroidEntryPoint
class VideoWallpaperSettingsActivity : BaseActivity<ActivityVideoWallpaperSettingsBinding>() {

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

    private fun registerOnClick() {
        binding.btnSetWallpaper.setOnClickListener { onClickSetLiveWallpaperOrApply() }
        binding.btnBackWallpaper.setOnClickListener { finish() }
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

    private fun isMyLiveWallpaperActive(): Boolean {
        val wm = WallpaperManager.getInstance(this)
        val info = wm.wallpaperInfo ?: return false
        return info.component == ComponentName(this, VideoWallpaperService::class.java)
    }

    override fun onDestroy() {
        binding.playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
