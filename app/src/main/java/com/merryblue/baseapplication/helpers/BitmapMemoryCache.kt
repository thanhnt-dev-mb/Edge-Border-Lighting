package com.merryblue.baseapplication.helpers

import android.graphics.Bitmap
import androidx.collection.LruCache
import timber.log.Timber

object BitmapMemoryCache {
    private val maxKb = (Runtime.getRuntime().maxMemory() / 1024 / 6).toInt() // ~16% heap
    private val lru = object : LruCache<String, Bitmap>(maxKb) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1024
    }

    fun get(key: String): Bitmap? {
        val b = lru[key]
        Timber.tag("BMP_CACHE").d("get($key) = ${b?.width}x${b?.height}, recycled=${b?.isRecycled}")
        return b
    }

    fun put(key: String, bmp: Bitmap) { lru.put(key, bmp) }
}