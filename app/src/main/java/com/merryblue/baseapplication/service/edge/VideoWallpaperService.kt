package com.merryblue.baseapplication.service.edge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.merryblue.baseapplication.coredata.local.AppPreferences
import com.merryblue.baseapplication.helpers.video.VideoDataSource
import com.merryblue.baseapplication.helpers.ServiceState.ACTION_VIDEO_WALLPAPER_STATE_CHANGED
import com.merryblue.baseapplication.helpers.video.VideoPreloader

class VideoWallpaperService : WallpaperService() {

    private val preferences by lazy { AppPreferences(this@VideoWallpaperService) }

    // We need access to the current Engine's holder to refresh immediately on broadcast.
    // This reference is updated when a new Engine is created.
    @Volatile private var engineRef: VideoEngine? = null

    private val reloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_VIDEO_WALLPAPER_STATE_CHANGED) {
                // Ask the currently running engine (if any) to re-check URL and restart player.
                engineRef?.requestReload()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Register receiver once per service lifecycle (not per engine).
        val filter = IntentFilter(ACTION_VIDEO_WALLPAPER_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) {
            // For Android 13+, explicitly mark as not exported.
            registerReceiver(reloadReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(reloadReceiver, filter)
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(reloadReceiver)
        } catch (_: Throwable) { }
        super.onDestroy()
    }

    override fun onCreateEngine(): Engine {
        val e = VideoEngine()
        engineRef = e
        return e
    }

    inner class VideoEngine : Engine() {
        private var player: ExoPlayer? = null
        private var currentUrl: String? = null
        private var isVisible = false

        // Keep the last known SurfaceHolder so we can re-check URL and rebind surface
        // when the wallpaper becomes visible again without recreating the surface.
        private var currentHolder: SurfaceHolder? = null

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            currentHolder = holder
            ensurePlayer(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible

            if (visible) {
                // Some launchers/ROMs won't recreate the surface when user changes settings.
                // Re-check the URL and restart player if needed.
                currentHolder?.let { ensurePlayer(it) }
            }

            // Pause/Resume playback based on visibility.
            player?.playWhenReady = visible
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            currentHolder = null
            release()
        }

        override fun onDestroy() {
            release()
            if (engineRef == this) engineRef = null
            super.onDestroy()
        }

        /**
         * Called by the service receiver to reload immediately without opening system UI.
         */
        fun requestReload() {
            currentHolder?.let { ensurePlayer(it) }
        }

        /**
         * Ensure the ExoPlayer instance is playing the latest URL.
         *
         * - If URL is blank -> release player (avoid keeping old video).
         * - If URL unchanged and player exists -> just rebind surface and update play state.
         * - If URL changed (or player missing) -> restart player with new URL.
         */
        private fun ensurePlayer(holder: SurfaceHolder) {
            val url = preferences.videoUrl.trim()

            if (url.isBlank()) {
                currentUrl = null
                release()
                return
            }

            if (url == currentUrl && player != null) {
                // URL unchanged: keep current player, but make sure surface/play state is correct.
                player?.setVideoSurface(holder.surface)
                player?.playWhenReady = isVisible
                return
            }

            // URL changed or player not created yet: restart player.
            currentUrl = url
            startPlayer(holder, url)
        }

        /**
         * Create a fresh ExoPlayer instance, bind to the wallpaper surface, and start buffering.
         */
        @OptIn(UnstableApi::class)
        private fun startPlayer(holder: SurfaceHolder, url: String) {
            release()

            // Optional: warm up cache to reduce first-frame latency.
            VideoPreloader.preload(applicationContext, url)

            val mediaSourceFactory = DefaultMediaSourceFactory(VideoDataSource.cachedFactory(applicationContext))

            player = ExoPlayer.Builder(applicationContext)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    // "Fill 4 corners" effect: scale to fill while cropping excess (center-crop).
                    videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    repeatMode = Player.REPEAT_MODE_ONE

                    // Render video directly onto the wallpaper surface.
                    setVideoSurface(holder.surface)

                    setMediaItem(MediaItem.fromUri(url))
                    prepare()

                    // Start only when wallpaper is visible.
                    playWhenReady = isVisible
                }
        }

        /**
         * Release player and detach surface to avoid leaks.
         */
        private fun release() {
            player?.apply {
                try {
                    playWhenReady = false
                    clearVideoSurface()
                } catch (_: Throwable) {
                }
                release()
            }
            player = null
        }
    }
}
