package com.merryblue.baseapplication.data.repoimpl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.merryblue.baseapplication.domain.repository.EdgeImageRepository
import com.merryblue.baseapplication.domain.repository.EdgeImageSource
import com.merryblue.baseapplication.domain.repository.TargetSize
import dagger.hilt.android.qualifiers.ApplicationContext

class EdgeImageRepositoryImpl(
    @ApplicationContext private val appContext: Context
) : EdgeImageRepository {

    private val imageLoader: ImageLoader = ImageLoader.Builder(appContext)
        .crossfade(false)
        .build()

    override suspend fun loadBitmap(source: EdgeImageSource, target: TargetSize): Bitmap? {
        val w = target.width
        val h = target.height
        if (w <= 0 || h <= 0) return null

        val data: Any = when (source) {
            is EdgeImageSource.Url -> source.url
            is EdgeImageSource.UriSource -> source.uri
            is EdgeImageSource.Res -> source.resId
        }

        val request = ImageRequest.Builder(appContext)
            .data(data)
            .size(w, h)                     // resize full screen
            .allowHardware(false)                  // to get Bitmap
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .build()

        val result = imageLoader.execute(request)
        val drawable = (result as? SuccessResult)?.drawable ?: return null
        return (drawable as? BitmapDrawable)?.bitmap
    }
}
