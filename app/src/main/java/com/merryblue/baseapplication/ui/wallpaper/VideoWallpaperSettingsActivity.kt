package com.merryblue.baseapplication.ui.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.helpers.video.VideoDataSource
import com.merryblue.baseapplication.databinding.ActivityVideoWallpaperSettingsBinding
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_VIDEO_WALLPAPER_STATE_CHANGED
import com.merryblue.baseapplication.helpers.video.VideoPreloader
import com.merryblue.baseapplication.service.VideoWallpaperService
import org.app.core.base.BaseActivity

class VideoWallpaperSettingsActivity : BaseActivity<ActivityVideoWallpaperSettingsBinding>() {

    private val preferences by lazy { AppPreferences(this) }

    private var player: ExoPlayer? = null

    override fun getLayoutId(): Int = R.layout.activity_video_wallpaper_settings

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        enableEdgeToEdge(binding.main, true)
    }

    override fun setUpViews() {

        val videoUrl = preferences.videoUrl
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

        binding.btnSetWallpaper.setOnClickListener {
            preferences.clearCacheEdgeState()

            if (isMyLiveWallpaperActive()) {
                sendBroadcast(Intent(ACTION_VIDEO_WALLPAPER_STATE_CHANGED).setPackage(packageName))
            } else openSetLiveWallpaper()
            finish()
        }

        binding.btnBackWallpaper.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        binding.playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun openSetLiveWallpaper() {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this@VideoWallpaperSettingsActivity, VideoWallpaperService::class.java)
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun isMyLiveWallpaperActive(): Boolean {
        val wm = WallpaperManager.getInstance(this)
        val info = wm.wallpaperInfo ?: return false
        return info.component == ComponentName(this, VideoWallpaperService::class.java)
    }
}