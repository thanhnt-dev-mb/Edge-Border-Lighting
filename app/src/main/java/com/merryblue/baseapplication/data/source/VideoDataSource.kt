package com.merryblue.baseapplication.data.source

import android.content.Context
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.merryblue.baseapplication.helpers.VideoCache
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object VideoDataSource {

    private fun okHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    fun cachedFactory(context: Context): CacheDataSource.Factory {
        val upstream = OkHttpDataSource.Factory(okHttpClient())
        return CacheDataSource.Factory()
            .setCache(VideoCache.get(context))
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
