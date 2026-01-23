package com.merryblue.baseapplication.helpers

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object VideoCache {

    var videoUrl: String? = null

    fun clear() {
        videoUrl = null
    }

    @Volatile private var cache: SimpleCache? = null

    fun get(context: Context): SimpleCache {
        return cache ?: synchronized(this) {
            cache ?: run {
                val cacheDir = File(context.cacheDir, "wallpaper_video_cache")
                val evictor = LeastRecentlyUsedCacheEvictor(300L * 1024 * 1024) // 300MB
                val dbProvider = StandaloneDatabaseProvider(context)
                SimpleCache(cacheDir, evictor, dbProvider).also { cache = it }
            }
        }
    }
}
