package com.merryblue.baseapplication.helpers.video

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.offline.ProgressiveDownloader
import com.merryblue.baseapplication.helpers.video.VideoDataSource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object VideoPreloader {

    private val executor = Executors.newSingleThreadExecutor()

    // Prevent multiple downloads for the same URL at the same time
    private val inFlight = ConcurrentHashMap<String, Boolean>()

    /**
     * Preload MP4 into Media3 cache (async, non-blocking).
     *
     * Call this from UI thread safely.
     */
    fun preload(context: Context, url: String, onProgress: ((Int) -> Unit)? = null) {
        val appContext = context.applicationContext
        val safeUrl = url.trim()
        if (safeUrl.isBlank()) return

        // If already downloading this URL, skip.
        if (inFlight.putIfAbsent(safeUrl, true) != null) return

        executor.execute {
            try {
                val mediaItem = MediaItem.fromUri(safeUrl)
                val downloader = ProgressiveDownloader(
                    mediaItem,
                    VideoDataSource.cachedFactory(appContext)
                )

                downloader.download { _, _, percentDownloaded ->
                    onProgress?.invoke(percentDownloaded.toInt())
                }
            } catch (_: Throwable) {
                // Swallow errors: preload is best-effort.
            } finally {
                inFlight.remove(safeUrl)
            }
        }
    }
}
