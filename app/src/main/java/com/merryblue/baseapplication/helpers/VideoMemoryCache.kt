package com.merryblue.baseapplication.helpers

object VideoMemoryCache {
    var videoUrl: String? = null

    fun clear() {
        videoUrl = null
    }
}