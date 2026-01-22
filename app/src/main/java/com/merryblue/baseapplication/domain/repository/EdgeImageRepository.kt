package com.merryblue.baseapplication.domain.repository

import android.graphics.Bitmap
import android.net.Uri

interface EdgeImageRepository {
    suspend fun loadBitmap(source: EdgeImageSource, target: TargetSize): Bitmap?
}

sealed class EdgeImageSource {
    data class Url(val url: String) : EdgeImageSource()
    data class UriSource(val uri: Uri) : EdgeImageSource()
    data class Res(val resId: Int) : EdgeImageSource()
}

data class TargetSize(val width: Int, val height: Int)